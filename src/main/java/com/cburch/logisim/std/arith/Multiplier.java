/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.math.BigInteger;

public class Multiplier extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Multiplier";

  static BigInteger extend(int w, long v, boolean unsigned) {
    long mask = w == 64 ? 0 : (-1L) << w;
    mask ^= 0xFFFFFFFFFFFFFFFFL;
    long value = v & mask;
    if (!unsigned && (value >> (w - 1)) != 0) value |= ~mask;
    if (unsigned) return new BigInteger(Long.toUnsignedString(value));
    return new BigInteger(Long.toString(value));
  }

  static Value[] computeProduct(BitWidth width, Value a, Value b, Value c_in, boolean unsigned) {
    int w = width.getWidth();
    if (c_in == Value.NIL || c_in.isUnknown()) c_in = Value.createKnown(width, 0);
    if (a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined()) {
      BigInteger aa = extend(w, a.toLongValue(), unsigned);
      BigInteger bb = extend(w, b.toLongValue(), unsigned);
      BigInteger cc = extend(w, c_in.toLongValue(), unsigned);
      BigInteger rr = aa.multiply(bb).add(cc);
      long mask = w == 64 ? 0 : (-1L) << w;
      mask ^= 0xFFFFFFFFFFFFFFFFL;
      long lo = rr.and(BigInteger.valueOf(mask)).longValue();
      long hi = rr.shiftRight(w).and(BigInteger.valueOf(mask)).longValue();
      return new Value[] {Value.createKnown(width, lo), Value.createKnown(width, hi)};
    } else {
      Value[] avals = a.getAll();
      int aOk = findUnknown(avals);
      int aErr = findError(avals);
      int ax = getKnown(avals);
      Value[] bvals = b.getAll();
      int bOk = findUnknown(bvals);
      int bErr = findError(bvals);
      int bx = getKnown(bvals);
      Value[] cvals = c_in.getAll();
      int cOk = findUnknown(cvals);
      int cErr = findError(cvals);
      int cx = getKnown(cvals);

      int known = Math.min(Math.min(aOk, bOk), cOk);
      int error = Math.min(Math.min(aErr, bErr), cErr);

      // fixme: this is probably wrong, but the inputs were bad anyway
      BigInteger aa = extend(w, ax, unsigned);
      BigInteger bb = extend(w, bx, unsigned);
      BigInteger cc = extend(w, cx, unsigned);
      BigInteger rr = aa.multiply(bb).add(cc);
      long ret = rr.longValue();

      Value[] bits = new Value[w];
      for (int i = 0; i < w; i++) {
        if (i < known) {
          bits[i] = ((ret & (1 << i)) != 0 ? Value.TRUE : Value.FALSE);
        } else if (i < error) {
          bits[i] = Value.UNKNOWN;
        } else {
          bits[i] = Value.ERROR;
        }
      }
      return new Value[] {
        Value.create(bits), error < w ? Value.createError(width) : Value.createUnknown(width)
      };
    }
  }

  private static int findError(Value[] vals) {
    for (int i = 0; i < vals.length; i++) {
      if (vals[i].isErrorValue()) return i;
    }
    return vals.length;
  }

  private static int findUnknown(Value[] vals) {
    for (int i = 0; i < vals.length; i++) {
      if (!vals[i].isFullyDefined()) return i;
    }
    return vals.length;
  }

  private static int getKnown(Value[] vals) {
    int ret = 0;
    for (int i = 0; i < vals.length; i++) {
      int val = (int) vals[i].toLongValue();
      if (val < 0) return ret;
      ret |= val << i;
    }
    return ret;
  }

  static final int PER_DELAY = 1;
  public static final int IN0 = 0;
  public static final int IN1 = 1;
  public static final int OUT = 2;
  public static final int C_IN = 3;
  public static final int C_OUT = 4;

  public Multiplier() {
    super(_ID, S.getter("multiplierComponent"), new MultiplierHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, Comparator.MODE_ATTR},
        new Object[] {BitWidth.create(8), Comparator.UNSIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u00d7"));

    Port[] ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[C_IN] = new Port(-20, -20, Port.INPUT, StdAttr.WIDTH);
    ps[C_OUT] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
    ps[IN0].setToolTip(S.getter("multiplierInputTip"));
    ps[IN1].setToolTip(S.getter("multiplierInputTip"));
    ps[OUT].setToolTip(S.getter("multiplierOutputTip"));
    ps[C_IN].setToolTip(S.getter("multiplierCarryInTip"));
    ps[C_OUT].setToolTip(S.getter("multiplierCarryOutTip"));
    setPorts(ps);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == Comparator.MODE_ATTR) instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(C_IN, "c in", Direction.NORTH);
    painter.drawPort(C_OUT, "c out", Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(x - 15, y - 5, x - 5, y + 5);
    g.drawLine(x - 15, y + 5, x - 5, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    boolean unsigned =
        state.getAttributeValue(Comparator.MODE_ATTR).equals(Comparator.UNSIGNED_OPTION);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value c_in = state.getPortValue(C_IN);
    Value[] outs = computeProduct(dataWidth, a, b, c_in, unsigned);

    // propagate them
    int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(C_OUT, outs[1], delay);
  }
}
