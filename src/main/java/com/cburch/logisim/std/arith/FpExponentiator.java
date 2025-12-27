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

public class FpExponentiator extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPExponentiator";

  static final AttributeOption ARB =
      new AttributeOption("arb", S.getter("fpExponentiatorModeArbitrary"));
  static final AttributeOption EXP =
      new AttributeOption("exp", S.getter("fpExponentiatorModeExponent"));
  static final AttributeOption EXPM1 =
      new AttributeOption("expm1", S.getter("fpExponentiatorModeExponentMinusOne"));
  static final Attribute<AttributeOption> EXP_MODE =
      Attributes.forOption(
          "mode",
          S.getter("fpExponentiatorMode"),
          new AttributeOption[] {
            ARB,
            EXP,
            EXPM1
          });

  static final int PER_DELAY = 1;
  private static final int EXPO = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;
  private static final int BASE = 3;

  public FpExponentiator() {
    super(_ID, S.getter("fpExponentiatorComponent"));
    setAttributes(
      new Attribute[] {StdAttr.FP_WIDTH, EXP_MODE},
      new Object[] {BitWidth.create(32), ARB});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("y\u02E3", 2));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == EXP_MODE) {
      configurePorts(instance);
    }
  }
  private void configurePorts(Instance instance) {
    final var isSingleInput = instance.getAttributeValue(EXP_MODE) != ARB;
    final Port[] ps;
    if (isSingleInput) {
      ps = new Port[3];

      ps[EXPO] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);

    } else {
      ps = new Port[4];

      ps[EXPO] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
      ps[BASE] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
      ps[BASE].setToolTip(S.getter("exponentiatorBaseTip"));
    }
    ps[EXPO].setToolTip(S.getter("exponentiatorExponentTip"));
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[OUT].setToolTip(S.getter("fpExponentiatorOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));

    instance.setPorts(ps);
  }
  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();

    final var mode = painter.getAttributeValue(EXP_MODE);
    if (mode == ARB) {
      painter.drawPort(OUT, "y\u02E3", Direction.WEST);
      g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
      painter.drawPort(BASE);
      painter.drawPort(EXPO);
    } else {
      if (mode == EXP) {
        painter.drawPort(OUT, "e\u02E3", Direction.WEST);
      } else {
        painter.drawPort(OUT, "e\u02E3-1", Direction.WEST);
      }
      g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
      painter.drawPort(EXPO);
    }

    painter.drawPort(ERR);

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
    final var mode = state.getAttributeValue(EXP_MODE);

    // compute outputs
    final var expo = state.getPortValue(EXPO);

    final double out_val;
    final var expo_val = expo.toDoubleValueFromAnyFloat();

    if (mode == ARB) {
      final var base = state.getPortValue(BASE);
      final var base_val = base.toDoubleValueFromAnyFloat();
      out_val = Math.pow(base_val, expo_val);
    } else {
      out_val = mode == EXP ? Math.exp(expo_val) : Math.expm1(expo_val);
    }

    final var out = Value.createKnown(dataWidth, out_val);


    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
