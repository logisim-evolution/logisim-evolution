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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.math.BigInteger;

public class Divider extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Divider";

  static Value[] computeResult(BitWidth width, Value a, Value b, Value upper, boolean unsigned) {
    int w = width.getWidth();
    if (upper == Value.NIL || upper.isUnknown()) upper = Value.createKnown(width, 0);
    if (a.isFullyDefined() && b.isFullyDefined() && upper.isFullyDefined()) {
      BigInteger uu = Multiplier.extend(w, upper.toLongValue(), unsigned);
      BigInteger aa = Multiplier.extend(w, a.toLongValue(), unsigned);
      BigInteger bb = Multiplier.extend(w, b.toLongValue(), unsigned);

      BigInteger num = uu.shiftLeft(w).or(aa);
      BigInteger den = bb.equals(BigInteger.ZERO) ? BigInteger.valueOf(1) : bb;

      BigInteger[] res = num.divideAndRemainder(den);
      long mask = w == 64 ? 0 : (-1L) << w;
      mask ^= 0xFFFFFFFFFFFFFFFFL;
      long result = res[0].and(BigInteger.valueOf(mask)).longValue();
      long rem = res[1].and(BigInteger.valueOf(mask)).longValue();
      return new Value[] {Value.createKnown(width, result), Value.createKnown(width, rem)};
    } else if (a.isErrorValue() || b.isErrorValue() || upper.isErrorValue()) {
      return new Value[] {Value.createError(width), Value.createError(width)};
    } else {
      return new Value[] {Value.createUnknown(width), Value.createUnknown(width)};
    }
  }

  static final int PER_DELAY = 1;
  public static final int IN0 = 0;
  public static final int IN1 = 1;
  public static final int OUT = 2;
  public static final int UPPER = 3;
  public static final int REM = 4;

  public Divider() {
    super(_ID, S.getter("dividerComponent"));
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, Comparator.MODE_ATTR},
        new Object[] {BitWidth.create(8), Comparator.UNSIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u00f7"));

    Port[] ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[UPPER] = new Port(-20, -20, Port.INPUT, StdAttr.WIDTH);
    ps[REM] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
    ps[IN0].setToolTip(S.getter("dividerDividendLowerTip"));
    ps[IN1].setToolTip(S.getter("dividerDivisorTip"));
    ps[OUT].setToolTip(S.getter("dividerOutputTip"));
    ps[UPPER].setToolTip(S.getter("dividerDividendUpperTip"));
    ps[REM].setToolTip(S.getter("dividerRemainderTip"));
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
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(UPPER, S.get("dividerUpperInput"), Direction.NORTH);
    painter.drawPort(REM, S.get("dividerRemainderOutput"), Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    g.fillOval(x - 12, y - 7, 4, 4);
    g.drawLine(x - 15, y, x - 5, y);
    g.fillOval(x - 12, y + 3, 4, 4);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    boolean unsigned = state.getAttributeValue(Comparator.MODE_ATTR).equals(Comparator.UNSIGNED_OPTION);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value upper = state.getPortValue(UPPER);
    Value[] outs = computeResult(dataWidth, a, b, upper, unsigned);

    // propagate them
    int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(REM, outs[1], delay);
  }
}
