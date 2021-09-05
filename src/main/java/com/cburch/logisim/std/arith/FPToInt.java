/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import java.awt.Color;

public class FPToInt extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPToInt";

  public static final AttributeOption CEILING_OPTION =
      new AttributeOption("ceil", "ceil", S.getter("ceilOption"));
  public static final AttributeOption FLOOR_OPTION =
      new AttributeOption("floor", "floor", S.getter("floorOption"));
  public static final AttributeOption ROUND_OPTION =
      new AttributeOption("round", "round", S.getter("roundOption"));
  public static final AttributeOption TRUNCATE_OPTION =
      new AttributeOption("truncate", "truncate", S.getter("truncateOption"));
  public static final Attribute<AttributeOption> MODE_ATTRIBUTE =
      Attributes.forOption(
          "mode",
          S.getter("fpToIntType"),
          new AttributeOption[] {CEILING_OPTION, FLOOR_OPTION, ROUND_OPTION, TRUNCATE_OPTION});

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  public FPToInt() {
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
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN);
    painter.drawPort(OUT, "F\u2192I", Direction.WEST);
    painter.drawPort(ERR);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidthIn = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var dataWidthOut = state.getAttributeValue(StdAttr.WIDTH);
    final var roundMode = state.getAttributeValue(MODE_ATTRIBUTE);

    // compute outputs
    final var a = state.getPortValue(IN);
    final var a_val = dataWidthIn.getWidth() == 64 ? a.toDoubleValue() : a.toFloatValue();

    long out_val;
  
    if (roundMode.getValue().equals("ceil")) out_val = (long) Math.ceil(a_val);
    else if (roundMode.getValue().equals("floor")) out_val = (long) Math.floor(a_val);
    else if (roundMode.getValue().equals("round")) out_val = (long) Math.round(a_val);
    else out_val = (long) a_val;

    final var out = Value.createKnown(dataWidthOut, out_val);

    // propagate them
    final var delay = (dataWidthOut.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(a_val) ? 1 : 0), delay);
  }
}
