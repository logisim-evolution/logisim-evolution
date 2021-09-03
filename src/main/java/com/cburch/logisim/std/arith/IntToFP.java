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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import java.awt.Color;
import java.math.BigInteger;

public class IntToFP extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "IntToFP";

  public static final AttributeOption SIGNED_OPTION = Comparator.SIGNED_OPTION;
  public static final AttributeOption UNSIGNED_OPTION = Comparator.UNSIGNED_OPTION;
  public static final Attribute<AttributeOption> MODE_ATTR = Comparator.MODE_ATTRIBUTE;

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  static BigInteger extend(int w, long v, boolean unsigned) {
    long mask = w == 64 ? 0 : (-1L) << w;
    mask ^= 0xFFFFFFFFFFFFFFFFL;
    long value = v & mask;
    if (!unsigned && (value >> (w - 1)) != 0) value |= ~mask;
    if (unsigned) return new BigInteger(Long.toUnsignedString(value));
    return new BigInteger(Long.toString(value));
  }

  public IntToFP() {
    super(_ID, S.getter("intToFPComponent"));
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, StdAttr.FP_WIDTH, MODE_ATTR},
        new Object[] {BitWidth.create(8), BitWidth.create(32), SIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("I\u2192FP", 2));

    final var ps = new Port[3];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("intToFPInputTip"));
    ps[OUT].setToolTip(S.getter("intToFPOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN);
    painter.drawPort(OUT, "I\u2192F", Direction.WEST);
    painter.drawPort(ERR);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidthIn = state.getAttributeValue(StdAttr.WIDTH);
    final var dataWidthOut = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var unsigned = state.getAttributeValue(MODE_ATTR).equals(UNSIGNED_OPTION);

    // compute outputs
    final var a = state.getPortValue(IN);
    final var a_val = extend(dataWidthIn.getWidth(), a.toLongValue(), unsigned);

    final var out_val = a.isFullyDefined() ? a_val.doubleValue() : Double.NaN;
    final var out = dataWidthOut.getWidth() == 64 ? Value.createKnown(out_val) : Value.createKnown((float) out_val);

    // propagate them
    final var delay = (dataWidthIn.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
