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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
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

public class Adder extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Adder";

  static Value[] computeSum(BitWidth width, Value valueA, Value valueB, Value cIn) {
    final var w = width.getWidth();
    if (cIn == Value.UNKNOWN || cIn == Value.NIL) cIn = Value.FALSE;
    if (valueA.isFullyDefined() && valueB.isFullyDefined() && cIn.isFullyDefined()) {
      if (w == 64) {
        final var ax = valueA.toLongValue();
        final var bx = valueB.toLongValue();
        final var cx = cIn.toLongValue();
        final var mask = ~(1L << 63);
        final var aLast = (ax < 0);
        final var bLast = (bx < 0);
        final var cInLast = (((ax & mask) + (bx & mask) + cx) < 0);
        final var cOut = (aLast && bLast) || (aLast && cInLast) || (bLast && cInLast);
        final var sum = valueA.toLongValue() + valueB.toLongValue() + cIn.toLongValue();
        return new Value[] {Value.createKnown(width, sum), cOut ? Value.TRUE : Value.FALSE};
      } else {
        final var sum = valueA.toLongValue() + valueB.toLongValue() + cIn.toLongValue();
        return new Value[] {
          Value.createKnown(width, sum), ((sum >> w) & 1) == 0 ? Value.FALSE : Value.TRUE
        };
      }
    } else {
      final var bits = new Value[w];
      var carry = cIn;
      for (int i = 0; i < w; i++) {
        if (carry == Value.ERROR) {
          bits[i] = Value.ERROR;
        } else if (carry == Value.UNKNOWN) {
          bits[i] = Value.UNKNOWN;
        } else {
          Value ab = valueA.get(i);
          Value bb = valueB.get(i);
          if (ab == Value.ERROR || bb == Value.ERROR) {
            bits[i] = Value.ERROR;
            carry = Value.ERROR;
          } else if (ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
            bits[i] = Value.UNKNOWN;
            carry = Value.UNKNOWN;
          } else {
            int sum =
                (ab == Value.TRUE ? 1 : 0)
                    + (bb == Value.TRUE ? 1 : 0)
                    + (carry == Value.TRUE ? 1 : 0);
            bits[i] = (sum & 1) == 1 ? Value.TRUE : Value.FALSE;
            carry = (sum >= 2) ? Value.TRUE : Value.FALSE;
          }
        }
      }
      return new Value[] {Value.create(bits), carry};
    }
  }

  static final int PER_DELAY = 1;

  public static final int IN0 = 0;
  public static final int IN1 = 1;
  public static final int OUT = 2;
  public static final int C_IN = 3;
  public static final int C_OUT = 4;

  public Adder() {
    super(_ID, S.getter("adderComponent"), new AdderHdlGeneratorFactory());
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("+"));

    final var ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[C_IN] = new Port(-20, -20, Port.INPUT, 1);
    ps[C_OUT] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("adderInputTip"));
    ps[IN1].setToolTip(S.getter("adderInputTip"));
    ps[OUT].setToolTip(S.getter("adderOutputTip"));
    ps[C_IN].setToolTip(S.getter("adderCarryInTip"));
    ps[C_OUT].setToolTip(S.getter("adderCarryOutTip"));
    setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    return (nrOfBits == 1) ? "FullAdder" : CorrectLabel.getCorrectLabel(getName());
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
    g.drawLine(x - 15, y, x - 5, y);
    g.drawLine(x - 10, y - 5, x - 10, y + 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value cIn = state.getPortValue(C_IN);
    Value[] outs = Adder.computeSum(dataWidth, a, b, cIn);

    // propagate them
    int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(C_OUT, outs[1], delay);
  }
}
