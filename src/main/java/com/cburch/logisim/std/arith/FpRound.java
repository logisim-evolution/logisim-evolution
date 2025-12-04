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

public class FpRound extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPRound";

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  public FpRound() {
    super(_ID, S.getter("fpRoundComponent"));
    setAttributes(
        new Attribute[] {StdAttr.FP_WIDTH, FpToInt.MODE_ATTRIBUTE},
        new Object[] {BitWidth.create(32), FpToInt.ROUND_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u230A\u2309", 2));

    final var ps = new Port[3];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("fpRoundInputTip"));
    ps[OUT].setToolTip(S.getter("fpRoundOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var roundMode = painter.getAttributeValue(FpToInt.MODE_ATTRIBUTE);

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(IN);

    if (roundMode.getValue().equals("ceil"))
      painter.drawPort(OUT, "\u2308x\u2309", Direction.WEST);
    else if (roundMode.getValue().equals("floor"))
       painter.drawPort(OUT, "\u230Ax\u230B", Direction.WEST);
    else if (roundMode.getValue().equals("round"))
       painter.drawPort(OUT, "\u27E6x\u27E7", Direction.WEST);
    else if (roundMode.getValue().equals("rint"))
       painter.drawPort(OUT, "\u27E6x\u27E7*", Direction.WEST);
    else painter.drawPort(OUT, "Trunc", Direction.WEST);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawLine(x - 35, y - 15, x - 35, y + 5);
    g.drawLine(x - 35, y - 15, x - 25, y - 15);
    g.drawLine(x - 35, y - 5, x - 25, y - 5);
    GraphicsUtil.switchToWidth(g, 1);

    painter.drawPort(ERR);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var roundMode = state.getAttributeValue(FpToInt.MODE_ATTRIBUTE);

    // compute outputs
    final var a = state.getPortValue(IN);
    final var a_val = switch (dataWidth.getWidth()) {
      case 16 -> a.toFloatValueFromFP16();
      case 32 -> a.toFloatValue();
      case 64 -> a.toDoubleValue();
      default -> Double.NaN;
    };

    final double roundedValue;
    if (roundMode.getValue().equals("ceil")) roundedValue = Math.ceil(a_val);
    else if (roundMode.getValue().equals("floor")) roundedValue = Math.floor(a_val);
    else if (roundMode.getValue().equals("round")) roundedValue = Math.round(a_val);
    else if (roundMode.getValue().equals("rint")) roundedValue = Math.rint(a_val);
    else roundedValue = (long) a_val;

    final var out = switch (dataWidth.getWidth()) {
      case 16 -> Value.createKnown(16, Float.floatToFloat16((float) roundedValue));
      case 32 -> Value.createKnown((float)roundedValue);
      case 64 -> Value.createKnown(roundedValue);
      default -> Value.ERROR;
    };

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(a_val) ? 1 : 0), delay);
  }
}
