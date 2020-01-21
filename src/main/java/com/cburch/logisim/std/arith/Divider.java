/**
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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
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
import java.awt.Graphics;
import java.math.BigInteger;

public class Divider extends InstanceFactory {

  public static final AttributeOption SIGNED_OPTION = Comparator.SIGNED_OPTION;
  public static final AttributeOption UNSIGNED_OPTION = Comparator.UNSIGNED_OPTION;
  public static final Attribute<AttributeOption> MODE_ATTR = Comparator.MODE_ATTRIBUTE;

  static Value[] computeResult(BitWidth width, Value a, Value b, Value upper, boolean unsigned) {
    int w = width.getWidth();
    if (upper == Value.NIL || upper.isUnknown()) upper = Value.createKnown(width, 0);
    if (a.isFullyDefined() && b.isFullyDefined() && upper.isFullyDefined()) {
      BigInteger uu = Multiplier.extend(w, upper.toLongValue(), unsigned);
      BigInteger aa = Multiplier.extend(w, a.toLongValue(), unsigned);
      BigInteger bb = Multiplier.extend(w, b.toLongValue(), unsigned);

      BigInteger num = uu.shiftLeft(w).or(aa);
      BigInteger den = bb.equals(BigInteger.ZERO) ? BigInteger.valueOf(1) : bb;

      BigInteger res[] = num.divideAndRemainder(den);
  	  long mask = w == 64 ? 0 : (-1L) << w;
  	  mask ^= 0xFFFFFFFFFFFFFFFFL;
      long result = res[0].and(BigInteger.valueOf(mask)).longValue();
      long rem = res[1].and(BigInteger.valueOf(mask)).longValue();
      return new Value[] {Value.createKnown(width, result), Value.createKnown(width, rem)};
    } else if (a.isErrorValue() || b.isErrorValue() || upper.isErrorValue()) {
      return new Value[] {Value.createError(width), Value.createError(width)};
    } else {
      return new Value[] {Value.createUnknown(width), Value.createUnknown(width)};
    }
  }

  static final int PER_DELAY = 1;
  static final int IN0 = 0;
  static final int IN1 = 1;
  static final int OUT = 2;
  static final int UPPER = 3;
  static final int REM = 4;

  public Divider() {
    super("Divider", S.getter("dividerComponent"));
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, MODE_ATTR},
        new Object[] {BitWidth.create(8), UNSIGNED_OPTION});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u00f7"));

    Port[] ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[UPPER] = new Port(-20, -20, Port.INPUT, StdAttr.WIDTH);
    ps[REM] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
    ps[IN0].setToolTip(S.getter("dividerDividendLowerTip"));
    ps[IN1].setToolTip(S.getter("dividerDivisorTip"));
    ps[OUT].setToolTip(S.getter("dividerOutputTip"));
    ps[UPPER].setToolTip(S.getter("dividerDividendUpperTip"));
    ps[REM].setToolTip(S.getter("dividerRemainderTip"));
    setPorts(ps);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }
  
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == MODE_ATTR) instance.fireInvalidated();
  }  

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(UPPER, S.get("dividerUpperInput"), Direction.NORTH);
    painter.drawPort(REM, S.get("dividerRemainderOutput"), Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    g.fillOval(x - 12, y - 7, 4, 4);
    g.drawLine(x - 15, y, x - 5, y);
    g.fillOval(x - 12, y + 3, 4, 4);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    boolean unsigned = state.getAttributeValue(MODE_ATTR).equals(UNSIGNED_OPTION);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value upper = state.getPortValue(UPPER);
    Value[] outs = computeResult(dataWidth, a, b, upper, unsigned);

    // propagate them
    int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(REM, outs[1], delay);
  }
}
