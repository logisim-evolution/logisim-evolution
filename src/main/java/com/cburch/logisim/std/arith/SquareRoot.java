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

public class SquareRoot extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "SquareRoot";

  static Value[] computeResult(BitWidth width, Value a, Value upper) {
    int w = width.getWidth();
    if (upper == Value.NIL || upper.isUnknown()) upper = Value.createKnown(width, 0);
    if (a.isFullyDefined() && upper.isFullyDefined()) {
      BigInteger uu = upper.toBigInteger(true);
      BigInteger aa = a.toBigInteger(true);

      BigInteger num = uu.shiftLeft(w).or(aa);

      BigInteger[] res = num.sqrtAndRemainder();
      long result = res[0].longValue();
      long rem = res[1].longValue();
      return new Value[] {Value.createKnown(width, result), Value.createKnown(width, rem)};
    } else if (a.isErrorValue() || upper.isErrorValue()) {
      return new Value[] {Value.createError(width), Value.createError(width)};
    } else {
      return new Value[] {Value.createUnknown(width), Value.createUnknown(width)};
    }
  }

  static final int PER_DELAY = 1;
  public static final int IN = 0;
  public static final int OUT = 1;
  public static final int UPPER = 2;
  public static final int REM = 3;

  public SquareRoot() {
    super(_ID, S.getter("squareRootComponent"));
    setAttributes(
        new Attribute[] {StdAttr.WIDTH},
        new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u221A"));

    Port[] ps = new Port[4];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.WIDTH);
    ps[UPPER] = new Port(-20, -20, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[REM] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
    ps[IN].setToolTip(S.getter("squareRootRadicandLowerTip"));
    ps[UPPER].setToolTip(S.getter("squareRootRadicandUpperTip"));
    ps[OUT].setToolTip(S.getter("squareRootOutputTip"));
    ps[REM].setToolTip(S.getter("squareRootRemainderTip"));
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
    painter.drawPort(IN);
    painter.drawPort(OUT);
    painter.drawPort(UPPER, S.get("dividerUpperInput"), Direction.NORTH);
    painter.drawPort(REM, S.get("dividerRemainderOutput"), Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(x - 15, y, x - 12, y + 5);
    g.drawLine(x - 12, y + 5, x - 9, y - 5);
    g.drawLine(x - 9, y - 5, x - 5, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value a = state.getPortValue(IN);
    Value upper = state.getPortValue(UPPER);
    Value[] outs = computeResult(dataWidth, a, upper);

    // propagate them
    int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(REM, outs[1], delay);
  }
}
