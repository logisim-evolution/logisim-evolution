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

import java.awt.Color;

public class FpToInt extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPToInt";

  public static final AttributeOption CEILING_OPTION =
      new AttributeOption("ceil", "ceil", S.getter("ceilOption"));
  public static final AttributeOption FLOOR_OPTION =
      new AttributeOption("floor", "floor", S.getter("floorOption"));
  public static final AttributeOption ROUND_OPTION =
      new AttributeOption("round", "round", S.getter("roundOption"));
  public static final AttributeOption RINT_OPTION =
      new AttributeOption("rint", "rint", S.getter("rintOption"));
  public static final AttributeOption TRUNCATE_OPTION =
      new AttributeOption("truncate", "truncate", S.getter("truncateOption"));
  public static final Attribute<AttributeOption> MODE_ATTRIBUTE =
      Attributes.forOption(
          "mode",
          S.getter("fpToIntType"),
          new AttributeOption[] {CEILING_OPTION, FLOOR_OPTION, ROUND_OPTION, RINT_OPTION, TRUNCATE_OPTION});

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  public FpToInt() {
    super(_ID, S.getter("fpToIntComponent"));
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, StdAttr.FP_WIDTH, MODE_ATTRIBUTE},
        new Object[] {BitWidth.create(8), BitWidth.create(32), ROUND_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("FP\u2192I", 2));

    final var ps = new Port[3];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("fpToIntInputTip"));
    ps[OUT].setToolTip(S.getter("fpToIntOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(IN);
    painter.drawPort(OUT, "F\u2192I", Direction.WEST);
    painter.drawPort(ERR);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidthOut = state.getAttributeValue(StdAttr.WIDTH);
    final var roundMode = state.getAttributeValue(MODE_ATTRIBUTE);

    // compute outputs
    final var a = state.getPortValue(IN);
    final var a_val = a.toDoubleValueFromAnyFloat();

    final long out_val;

    if (roundMode.getValue().equals("ceil")) out_val = (long) Math.ceil(a_val);
    else if (roundMode.getValue().equals("floor")) out_val = (long) Math.floor(a_val);
    else if (roundMode.getValue().equals("round")) out_val = Math.round(a_val);
    else if (roundMode.getValue().equals("rint")) out_val = (long) Math.rint(a_val);
    else out_val = (long) a_val;

    final var out = Value.createKnown(dataWidthOut, out_val);

    // propagate them
    final var delay = (dataWidthOut.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(a_val) ? 1 : 0), delay);
  }
}
