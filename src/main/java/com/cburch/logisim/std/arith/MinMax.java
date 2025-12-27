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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;

import java.awt.Color;

public class MinMax extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "MinMax";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int MIN = 2;
  private static final int MAX = 3;
  public MinMax() {
    super(_ID, S.getter("minMaxComponent"));
    setAttributes(
      new Attribute[] {StdAttr.WIDTH, Comparator.MODE_ATTR},
      new Object[] {BitWidth.create(8), Comparator.UNSIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("minmax", 3));

    final var ps = new Port[4];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[MAX] = new Port(0, 10, Port.OUTPUT, StdAttr.WIDTH);
    ps[MIN] = new Port(0, -10, Port.OUTPUT, StdAttr.WIDTH);
    ps[IN0].setToolTip(S.getter("minMaxInputTip"));
    ps[IN1].setToolTip(S.getter("minMaxInputTip"));
    ps[MIN].setToolTip(S.getter("minMaxMinimumTip"));
    ps[MAX].setToolTip(S.getter("minMaxMaximumTip"));
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
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    final var unsigned = state.getAttributeValue(Comparator.MODE_ATTR) == Comparator.UNSIGNED_OPTION;

    // compute outputs
    final var a = state.getPortValue(IN0);
    final var b = state.getPortValue(IN1);
    final Value min, max;

    if (a.isFullyDefined() && b.isFullyDefined()) {
      final var a_val = a.toBigInteger(unsigned);
      final var b_val = b.toBigInteger(unsigned);

      final var min_val = a_val.min(b_val).longValue();
      final var max_val = a_val.max(b_val).longValue();

      min = Value.createKnown(dataWidth.getWidth(), min_val);
      max = Value.createKnown(dataWidth.getWidth(), max_val);
    } else {
      min = Value.createError(dataWidth);
      max = Value.createError(dataWidth);
    }

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;

    state.setPort(MIN, min, delay);
    state.setPort(MAX, max, delay);
  }
}
