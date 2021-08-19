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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;

public class FPComparator extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPComparator";

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int GT = 2;
  private static final int EQ = 3;
  private static final int LT = 4;
  private static final int ERR = 5;

  public FPComparator() {
    super(_ID, S.getter("fpComparatorComponent"));
    setAttributes(new Attribute[] {StdAttr.FP_WIDTH}, new Object[] {BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u2276"));

    final var ps = new Port[6];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
    ps[GT] = new Port(0, -10, Port.OUTPUT, 1);
    ps[EQ] = new Port(0, 0, Port.OUTPUT, 1);
    ps[LT] = new Port(0, 10, Port.OUTPUT, 1);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("comparatorInputATip"));
    ps[IN1].setToolTip(S.getter("comparatorInputBTip"));
    ps[GT].setToolTip(S.getter("comparatorGreaterTip"));
    ps[EQ].setToolTip(S.getter("comparatorEqualTip"));
    ps[LT].setToolTip(S.getter("comparatorLessTip"));
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
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(GT, ">", Direction.WEST);
    painter.drawPort(EQ, "=", Direction.WEST);
    painter.drawPort(LT, "<", Direction.WEST);
    painter.drawPort(ERR);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
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

    final var a_val = dataWidth.getWidth() == 64 ? a.toDoubleValue() : a.toFloatValue();
    final var b_val = dataWidth.getWidth() == 64 ? b.toDoubleValue() : b.toFloatValue();

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(GT, Value.createKnown(1, a_val > b_val ? 1 : 0), delay);
    state.setPort(EQ, Value.createKnown(1, a_val == b_val ? 1 : 0), delay);
    state.setPort(LT, Value.createKnown(1, a_val < b_val ? 1 : 0), delay);
    state.setPort(ERR, Value.createKnown(1, (Double.isNaN(a_val) || Double.isNaN(b_val)) ? 1 : 0), delay);
  }
}
