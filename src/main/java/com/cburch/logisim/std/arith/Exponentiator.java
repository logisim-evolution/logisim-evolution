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
import java.math.BigInteger;

public class Exponentiator extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */


  public static final String _ID = "Exponentiator";

  static Value[] computePower(BitWidth width, Value a, Value b, boolean unsigned) {
    int w = width.getWidth();
    if (a.isFullyDefined() && b.isFullyDefined()) {
      if(a.toLongValue() == 1) {
        return new Value[] {Value.createKnown(width, 1), Value.createKnown(width, 0)};
      }

      BigInteger aa = a.toBigInteger(unsigned);
      int b_val = Math.max((int)b.toLongValue(), 0);

      var rr = aa.pow(b_val);

      long lo = rr.longValue();
      long hi = rr.shiftRight(w).longValue();

      return new Value[] {Value.createKnown(width, lo), Value.createKnown(width, hi)};
    } else if (a.isErrorValue() || b.isErrorValue()) {
      return new Value[] {Value.createError(width), Value.createError(width)};
    } else {
      return new Value[] {Value.createUnknown(width), Value.createUnknown(width)};
    }
  }

  static final int PER_DELAY = 1;
  private static final int BASE = 0;
  private static final int EXP = 1;
  private static final int LOW_OUT = 2;
  private static final int UPP_OUT = 3;

  public Exponentiator() {
    super(_ID, S.getter("exponentiatorComponent"));
    setAttributes(
      new Attribute[] {StdAttr.WIDTH, Comparator.MODE_ATTR},
      new Object[] {BitWidth.create(32), Comparator.UNSIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("y\u02E3",2));

    final var ps = new Port[4];
    ps[BASE] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[EXP] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[LOW_OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[UPP_OUT] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
    ps[BASE].setToolTip(S.getter("exponentiatorBaseTip"));
    ps[EXP].setToolTip(S.getter("exponentiatorExponentTip"));
    ps[LOW_OUT].setToolTip(S.getter("exponentiatorLowerOutputTip"));
    ps[UPP_OUT].setToolTip(S.getter("exponentiatorUpperOutputTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(LOW_OUT,"y\u02E3",Direction.WEST);
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(UPP_OUT, S.get("dividerUpperInput"), Direction.SOUTH);
    painter.drawPort(BASE);
    painter.drawPort(EXP);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    final var unsigned =
          state.getAttributeValue(Comparator.MODE_ATTR) == Comparator.UNSIGNED_OPTION;

    // compute outputs
    final var a = state.getPortValue(BASE);
    final var b = state.getPortValue(EXP);

    final var results = computePower(dataWidth, a, b, unsigned);

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(LOW_OUT, Value.createKnown(dataWidth, results[0].toLongValue()), delay);
    state.setPort(UPP_OUT, Value.createKnown(dataWidth, results[1].toLongValue()), delay);
  }
}
