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
import java.awt.Color;

public class Absolute extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Absolute";

  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int OVERFLOW = 2;

  public Absolute() {
    super(_ID, S.getter("absoluteComponent"));
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("abs", 3));

    final var ps = new Port[3];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[OVERFLOW] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("absoluteInputTip"));
    ps[OUT].setToolTip(S.getter("absoluteOutputTip"));
    ps[OVERFLOW].setToolTip(S.getter("absoluteOverflowTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(OUT, "Abs", Direction.WEST);
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(IN);
    painter.drawPort(OVERFLOW);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value in = state.getPortValue(IN);
    final Value out, overflow;
    if (in.isFullyDefined()) {
      var result = in.toBigInteger(false).abs().longValue();
      out = Value.createKnown(in.getBitWidth(), result);
      System.out.println(result);
      overflow = result == 1 << (dataWidth.getWidth() - 1) ? Value.TRUE : Value.FALSE;
    } else {
      Value[] bits = in.getAll();
      Value fill = Value.FALSE;
      int pos = 0;
      while (pos < bits.length) {
        if (bits[pos] == Value.FALSE) {
          bits[pos] = fill;
        } else if (bits[pos] == Value.TRUE) {
          if (fill != Value.FALSE) bits[pos] = fill;
          pos++;
          break;
        } else if (bits[pos] == Value.ERROR) {
          fill = Value.ERROR;
        } else {
          if (fill == Value.FALSE) fill = bits[pos];
          else bits[pos] = fill;
        }
        pos++;
      }
      out = Value.create(bits);
      overflow = Value.ERROR;
    }

    // propagate them
    int delay = (dataWidth.getWidth() + 2) * Adder.PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(OVERFLOW, overflow, delay);
  }
}