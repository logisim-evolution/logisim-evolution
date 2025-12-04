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

public class FpExponent extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPExponent";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  static final AttributeOption EXP =
      new AttributeOption("exp", S.getter("fpExponentExponent"));
  static final AttributeOption EXPM1 =
      new AttributeOption("expm1", S.getter("fpExponentExponentMinusOne"));
  static final Attribute<AttributeOption> EXP_MODE =
      Attributes.forOption(
          "mode",
          S.getter("fpExponentModeAttr"),
          new AttributeOption[] {
            EXP,
            EXPM1
          });

  public FpExponent() {
    super(_ID, S.getter("fpExponentComponent"));
    setAttributes(
      new Attribute[] {StdAttr.FP_WIDTH, EXP_MODE},
      new Object[] {BitWidth.create(32), EXP});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("e\u02E3",2));

    final var ps = new Port[3];
    ps[IN0] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("exponentInputTip"));
    ps[OUT].setToolTip(S.getter("fpExponentOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(IN0);
    painter.drawPort(ERR);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();

    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    final var mode = painter.getAttributeValue(EXP_MODE);
    if(mode == EXP){
      painter.drawPort(OUT, "e\u02E3", Direction.WEST);
    } else {
      painter.drawPort(OUT, "e\u02E3-1", Direction.WEST);
    }

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
    final var a = state.getPortValue(IN0);

    final var a_val = switch (dataWidth.getWidth()) {
      case 16 -> a.toFloatValueFromFP16();
      case 32 -> a.toFloatValue();
      case 64 -> a.toDoubleValue();
      default -> Double.NaN;
    };

    final var out_val = mode == EXP ? Math.exp(a_val) : Math.expm1(a_val);

    final var out = switch (dataWidth.getWidth()) {
      case 16 -> Value.createKnown(16, Float.floatToFloat16((float) out_val));
      case 32 -> Value.createKnown((float) out_val);
      case 64 -> Value.createKnown(out_val);
      default -> Value.ERROR;
    };

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
