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
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
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

public class FpTrigonometry extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPTrigonometry";

  static final AttributeOption TRIG =
      new AttributeOption("trig", S.getter("fpTrigonometryTypeTrigonometry"));
  static final AttributeOption ARC =
      new AttributeOption("arc", S.getter("fpTrigonometryTypeInverseTrigonometry"));
  static final AttributeOption HYP =
      new AttributeOption("hyp", S.getter("fpTrigonometryTypeHyperbolicTrigonometry"));
  static final Attribute<AttributeOption> TRIG_MODE =
      Attributes.forOption(
          "type",
          S.getter("fpTrigonometryType"),
          new AttributeOption[] {
            TRIG,
            ARC,
            HYP
          });

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int SIN = 1;
  private static final int TAN = 2;
  private static final int COS = 3;
  private static final int ERR = 4;

  public FpTrigonometry() {
    super(_ID, S.getter("fpTrigonometryComponent"));
    setAttributes(
      new Attribute[] {StdAttr.FP_WIDTH, TRIG_MODE},
      new Object[] {BitWidth.create(32), TRIG});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u25B3",3));

    final Port[] ps = new Port[5];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);
    ps[SIN] = new Port(0, -10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[TAN] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[COS] = new Port(0, 10, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("fpTrigonometryInputTip"));
    ps[SIN].setToolTip(S.getter("fpTrigonometrySinTip"));
    ps[TAN].setToolTip(S.getter("fpTrigonometryTanTip"));
    ps[COS].setToolTip(S.getter("fpTrigonometryCosTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));

    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(IN);
    painter.drawPort(ERR);

    final var mode = painter.getAttributeValue(TRIG_MODE);
    if(mode == TRIG) {
      painter.drawPort(SIN,"sin", Direction.WEST);
      painter.drawPort(TAN,"tan", Direction.WEST);
      painter.drawPort(COS,"cos", Direction.WEST);
    } else if(mode == ARC) {
      painter.drawPort(SIN,"asin", Direction.WEST);
      painter.drawPort(TAN,"atan", Direction.WEST);
      painter.drawPort(COS,"acos", Direction.WEST);
    } else {
      painter.drawPort(SIN,"sinh", Direction.WEST);
      painter.drawPort(TAN,"tanh", Direction.WEST);
      painter.drawPort(COS,"cosh", Direction.WEST);
    }

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(x - 35, y - 15, x - 35, y + 5);
    g.drawLine(x - 35, y - 15, x - 25, y - 15);
    g.drawLine(x - 35, y - 5, x - 25, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var mode = state.getAttributeValue(TRIG_MODE);

    // compute outputs
    final var a = state.getPortValue(IN);

    final var a_val = a.toDoubleValueFromAnyFloat();

    final double sin_val;
    final double tan_val;
    final double cos_val;

    if(mode == TRIG) {
      sin_val = Math.sin(a_val);
      tan_val = Math.tan(a_val);
      cos_val = Math.cos(a_val);
    } else if(mode == ARC) {
      sin_val = Math.asin(a_val);
      tan_val = Math.atan(a_val);
      cos_val = Math.acos(a_val);
    } else {
      sin_val = Math.sinh(a_val);
      tan_val = Math.tanh(a_val);
      cos_val = Math.cosh(a_val);
    }

    final var sin = Value.createKnown(dataWidth, sin_val);
    final var tan = Value.createKnown(dataWidth, tan_val);
    final var cos = Value.createKnown(dataWidth, cos_val);

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(SIN, sin, delay);
    state.setPort(TAN, tan, delay);
    state.setPort(COS, cos, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(a_val) ? 1 : 0), delay);
  }
}
