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

public class FpMultiplier extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPMultiplier";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int OUT = 2;
  private static final int ERR = 3;
  private static final int IN2 = 4;

  static final AttributeOption MUL =
      new AttributeOption("multiply", S.getter("fpMultiplierMultiply"));
  static final AttributeOption FMA =
      new AttributeOption("fusedMultiplyAdd", S.getter("fpMultiplierFusedMultiplyAdd"));
  static final Attribute<AttributeOption> MUL_MODE =
      Attributes.forOption(
          "multiplyMode",
          S.getter("fpMultiplierMultiplyMode"),
          new AttributeOption[] {
            MUL,
            FMA
          });

  public FpMultiplier() {
    super(_ID, S.getter("fpMultiplierComponent"));
    setAttributes(
      new Attribute[] {StdAttr.FP_WIDTH, MUL_MODE},
      new Object[] {BitWidth.create(32), MUL});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u00d7"));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == MUL_MODE) {
      configurePorts(instance);
    }
  }
  private void configurePorts(Instance instance) {
    final var isFMA = instance.getAttributeValue(MUL_MODE) == FMA;
    final Port[] ps;
    if(isFMA) {
      ps = new Port[5];
      ps[IN2] = new Port(-20, -20, Port.INPUT, StdAttr.FP_WIDTH);
      ps[IN2].setToolTip(S.getter("multiplierCarryInTip"));
    }
    else{
      ps = new Port[4];
    }
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("multiplierInputTip"));
    ps[IN1].setToolTip(S.getter("multiplierInputTip"));
    ps[OUT].setToolTip(S.getter("fpMultiplierOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    instance.setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(ERR);

    final var mulMode = painter.getAttributeValue(MUL_MODE);
    if(mulMode == FMA){
      painter.drawPort(IN2);
    }

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(x - 15, y - 5, x - 5, y + 5);
    g.drawLine(x - 15, y + 5, x - 5, y - 5);

    g.drawLine(x - 35, y - 15, x - 35, y + 5);
    g.drawLine(x - 35, y - 15, x - 25, y - 15);
    g.drawLine(x - 35, y - 5, x - 25, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var mulMode = state.getAttributeValue(MUL_MODE);

    // compute outputs
    final var a = state.getPortValue(IN0);
    final var b = state.getPortValue(IN1);

    final var a_val = a.toDoubleValueFromAnyFloat();
    final var b_val = b.toDoubleValueFromAnyFloat();

    final double out_val;
    if(mulMode == MUL){
      out_val = a_val * b_val;
    } else {
      final var c = state.getPortValue(IN2);
      final var c_val = c.toDoubleValueFromAnyFloat();
      out_val = Math.fma(a_val, b_val, c_val);
    }

    final var out = Value.createKnown(dataWidth, out_val);

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
