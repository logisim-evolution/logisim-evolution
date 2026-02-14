/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
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

public class FpDivider extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPDivider";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int OUT1 = 2;
  private static final int OUT2 = 3;
  private static final int ERR = 4;

  public FpDivider() {
    super(_ID, S.getter("fpDividerComponent"));
    setAttributes(new Attribute[] {StdAttr.FP_WIDTH}, new Object[] {BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u00f7"));

    final var ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[OUT1] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[OUT2] = new Port(-10, 20, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("fpDividerDividendTip"));
    ps[IN1].setToolTip(S.getter("dividerDivisorTip"));
    ps[OUT1].setToolTip(S.getter("dividerOutputTip"));
    ps[OUT2].setToolTip(S.getter("dividerRemainderTip"));
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
    painter.drawPort(IN1);
    painter.drawPort(OUT1);
    painter.drawPort(OUT2);
    painter.drawPort(ERR);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.fillOval(x - 12, y - 7, 4, 4);
    g.drawLine(x - 15, y, x - 5, y);
    g.fillOval(x - 12, y + 3, 4, 4);

    g.drawLine(x - 35, y - 15, x - 35, y + 5);
    g.drawLine(x - 35, y - 15, x - 25, y - 15);
    g.drawLine(x - 35, y - 5, x - 25, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);

    // compute outputs
    final var a = state.getPortValue(IN0);
    final var b = state.getPortValue(IN1);

    final var a_val = a.toDoubleValueFromAnyFloat();
    final var b_val = b.toDoubleValueFromAnyFloat();

    final var out_val = a_val / b_val;
    final var out = Value.createKnown(dataWidth, out_val);

    final var rem_val =  Math.IEEEremainder(a_val, b_val);
    final var rem = Value.createKnown(dataWidth, rem_val);


    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT1, out, delay);
    state.setPort(OUT2, rem, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
