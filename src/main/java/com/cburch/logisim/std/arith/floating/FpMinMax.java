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

public class FpMinMax extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPMinMax";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int MIN = 2;
  private static final int MAX = 3;
  private static final int ERR = 4;

  public FpMinMax() {
    super(_ID, S.getter("fpMinMaxComponent"));
    setAttributes(new Attribute[] {StdAttr.FP_WIDTH}, new Object[] {BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("minmax", 3));

    final var ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[MAX] = new Port(0, 10, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[MIN] = new Port(0, -10, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("minMaxInputTip"));
    ps[IN1].setToolTip(S.getter("minMaxInputTip"));
    ps[MIN].setToolTip(S.getter("minMaxMinimumTip"));
    ps[MAX].setToolTip(S.getter("minMaxMaximumTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    setPorts(ps);
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(MIN, "Min", Direction.WEST);
    painter.drawPort(MAX, "Max", Direction.WEST);
    painter.drawPort(ERR);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
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

    final var min_val = Math.min(a_val, b_val);
    final var max_val = Math.max(a_val, b_val);

    final var min = Value.createKnown(dataWidth, min_val);
    final var max = Value.createKnown(dataWidth, max_val);

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;

    state.setPort(MIN, min, delay);
    state.setPort(MAX, max, delay);
    state.setPort(
        ERR, Value.createKnown(1, (Double.isNaN(a_val) || Double.isNaN(b_val)) ? 1 : 0), delay);
  }
}
