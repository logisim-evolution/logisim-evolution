/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Divider extends InstanceFactory {
	static Value[] computeResult(BitWidth width, Value a, Value b, Value upper) {
		int w = width.getWidth();
		if (upper == Value.NIL || upper.isUnknown())
			upper = Value.createKnown(width, 0);
		if (a.isFullyDefined() && b.isFullyDefined() && upper.isFullyDefined()) {
			long num = ((long) upper.toIntValue() << w)
					| ((long) a.toIntValue() & 0xFFFFFFFFL);
			long den = (long) b.toIntValue() & 0xFFFFFFFFL;
			if (den == 0)
				den = 1;
			long result = num / den;
			long rem = num % den;
			if (rem < 0) {
				if (den >= 0) {
					rem += den;
					result--;
				} else {
					rem -= den;
					result++;
				}
			}
			return new Value[] { Value.createKnown(width, (int) result),
					Value.createKnown(width, (int) rem) };
		} else if (a.isErrorValue() || b.isErrorValue() || upper.isErrorValue()) {
			return new Value[] { Value.createError(width),
					Value.createError(width) };
		} else {
			return new Value[] { Value.createUnknown(width),
					Value.createUnknown(width) };
		}
	}

	static final int PER_DELAY = 1;
	private static final int IN0 = 0;
	private static final int IN1 = 1;
	private static final int OUT = 2;
	private static final int UPPER = 3;

	private static final int REM = 4;

	public Divider() {
		super("Divider", Strings.getter("dividerComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH },
				new Object[] { BitWidth.create(8) });
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setOffsetBounds(Bounds.create(-40, -20, 40, 40));
		setIconName("divider.gif");

		Port[] ps = new Port[5];
		ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
		ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
		ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
		ps[UPPER] = new Port(-20, -20, Port.INPUT, StdAttr.WIDTH);
		ps[REM] = new Port(-20, 20, Port.OUTPUT, StdAttr.WIDTH);
		ps[IN0].setToolTip(Strings.getter("dividerDividendLowerTip"));
		ps[IN1].setToolTip(Strings.getter("dividerDivisorTip"));
		ps[OUT].setToolTip(Strings.getter("dividerOutputTip"));
		ps[UPPER].setToolTip(Strings.getter("dividerDividendUpperTip"));
		ps[REM].setToolTip(Strings.getter("dividerRemainderTip"));
		setPorts(ps);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawBounds();

		g.setColor(Color.GRAY);
		painter.drawPort(IN0);
		painter.drawPort(IN1);
		painter.drawPort(OUT);
		painter.drawPort(UPPER, Strings.get("dividerUpperInput"),
				Direction.NORTH);
		painter.drawPort(REM, Strings.get("dividerRemainderOutput"),
				Direction.SOUTH);

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

		// compute outputs
		Value a = state.getPortValue(IN0);
		Value b = state.getPortValue(IN1);
		Value upper = state.getPortValue(UPPER);
		Value[] outs = Divider.computeResult(dataWidth, a, b, upper);

		// propagate them
		int delay = dataWidth.getWidth() * (dataWidth.getWidth() + 2)
				* PER_DELAY;
		state.setPort(OUT, outs[0], delay);
		state.setPort(REM, outs[1], delay);
	}
}
