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

import java.net.URL;

import javax.swing.ImageIcon;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

/**
 * Manufactures a counter that iterates over Gray codes. This demonstrates
 * several additional features beyond the SimpleGrayCounter class.
 */
class GrayCounter extends InstanceFactory {
	public GrayCounter() {
		super("Gray Counter");
		setOffsetBounds(Bounds.create(-30, -15, 30, 30));
		setPorts(new Port[] { new Port(-30, 0, Port.INPUT, 1),
				new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH), });

		// We'll have width, label, and label font attributes. The latter two
		// attributes allow us to associate a label with the component (though
		// we'll also need configureNewInstance to configure the label's
		// location).
		setAttributes(new Attribute[] { StdAttr.WIDTH, StdAttr.LABEL,
				StdAttr.LABEL_FONT }, new Object[] { BitWidth.create(4), "",
				StdAttr.DEFAULT_LABEL_FONT });

		// The following method invocation sets things up so that the instance's
		// state can be manipulated using the Poke Tool.
		setInstancePoker(CounterPoker.class);

		// These next two lines set it up so that the explorer window shows a
		// customized icon representing the component type. This should be a
		// 16x16 image.
		URL url = getClass().getClassLoader().getResource(
				"com/cburch/gray/counter.gif");
		if (url != null)
			setIcon(new ImageIcon(url));
	}

	/**
	 * The configureNewInstance method is invoked every time a new instance is
	 * created. In the superclass, the method doesn't do anything, since the new
	 * instance is pretty thoroughly configured already by default. But
	 * sometimes you need to do something particular to each instance, so you
	 * would override the method. In this case, we need to set up the location
	 * for its label.
	 */
	@Override
	protected void configureNewInstance(Instance instance) {
		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		// This is essentially the same as with SimpleGrayCounter, except for
		// the invocation of painter.drawLabel to make the label be drawn.
		painter.drawBounds();
		painter.drawClock(0, Direction.EAST);
		painter.drawPort(1);
		painter.drawLabel();

		if (painter.getShowState()) {
			BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
			CounterData state = CounterData.get(painter, width);
			Bounds bds = painter.getBounds();
			GraphicsUtil.drawCenteredText(painter.getGraphics(), StringUtil
					.toHexString(width.getWidth(), state.getValue()
							.toIntValue()), bds.getX() + bds.getWidth() / 2,
					bds.getY() + bds.getHeight() / 2);
		}
	}

	@Override
	public void propagate(InstanceState state) {
		// This is the same as with SimpleGrayCounter, except that we use the
		// StdAttr.WIDTH attribute to determine the bit width to work with.
		BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
		CounterData cur = CounterData.get(state, width);
		boolean trigger = cur.updateClock(state.getPortValue(0));
		if (trigger)
			cur.setValue(GrayIncrementer.nextGray(cur.getValue()));
		state.setPort(1, cur.getValue(), 9);
	}
}
