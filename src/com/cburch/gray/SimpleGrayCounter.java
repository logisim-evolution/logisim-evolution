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

package com.cburch.gray;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

/**
 * Manufactures a simple counter that iterates over the 4-bit Gray Code. This
 * example illustrates how a component can maintain its own internal state. All
 * of the code relevant to state, though, appears in CounterData class.
 */
class SimpleGrayCounter extends InstanceFactory {
	private static final BitWidth BIT_WIDTH = BitWidth.create(4);

	// Again, notice how we don't have any instance variables related to an
	// individual instance's state. We can't put that here, because only one
	// SimpleGrayCounter object is ever created, and its job is to manage all
	// instances that appear in any circuits.

	public SimpleGrayCounter() {
		super("Gray Counter (Simple)");
		setOffsetBounds(Bounds.create(-30, -15, 30, 30));
		setPorts(new Port[] { new Port(-30, 0, Port.INPUT, 1),
				new Port(0, 0, Port.OUTPUT, BIT_WIDTH.getWidth()), });
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		painter.drawBounds();
		painter.drawClock(0, Direction.EAST); // draw a triangle on port 0
		painter.drawPort(1); // draw port 1 as just a dot

		// Display the current counter value centered within the rectangle.
		// However, if the context says not to show state (as when generating
		// printer output), then skip this.
		if (painter.getShowState()) {
			CounterData state = CounterData.get(painter, BIT_WIDTH);
			Bounds bds = painter.getBounds();
			GraphicsUtil.drawCenteredText(painter.getGraphics(), StringUtil
					.toHexString(BIT_WIDTH.getWidth(), state.getValue()
							.toIntValue()), bds.getX() + bds.getWidth() / 2,
					bds.getY() + bds.getHeight() / 2);
		}
	}

	@Override
	public void propagate(InstanceState state) {
		// Here I retrieve the state associated with this component via a helper
		// method. In this case, the state is in a CounterData object, which is
		// also where the helper method is defined. This helper method will end
		// up creating a CounterData object if one doesn't already exist.
		CounterData cur = CounterData.get(state, BIT_WIDTH);

		boolean trigger = cur.updateClock(state.getPortValue(0));
		if (trigger)
			cur.setValue(GrayIncrementer.nextGray(cur.getValue()));
		state.setPort(1, cur.getValue(), 9);

		// (You might be tempted to determine the counter's current value
		// via state.getPortValue(1). This is erroneous, though, because another
		// component may be pushing a value onto the same point, which would
		// "corrupt" the value found there. We really do need to store the
		// current value in the instance.)
	}
}
