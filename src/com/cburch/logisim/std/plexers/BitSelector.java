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

package com.cburch.logisim.std.plexers;

import java.awt.Color;
import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class BitSelector extends InstanceFactory {
	public static final Attribute<BitWidth> GROUP_ATTR = Attributes
			.forBitWidth("group", Strings.getter("bitSelectorGroupAttr"));

	public BitSelector() {
		super("BitSelector", Strings.getter("bitSelectorComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING, StdAttr.WIDTH,
				GROUP_ATTR }, new Object[] { Direction.EAST,
				BitWidth.create(8), BitWidth.ONE });
		setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(
				GROUP_ATTR, 1, Value.MAX_WIDTH, 0), new BitWidthConfigurator(
				StdAttr.WIDTH)));

		setIconName("bitSelector.gif");
		setFacingAttribute(StdAttr.FACING);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
		if (attrs.getValue(GROUP_ATTR).getWidth() > 1)
			CompleteName.append("_bus");
		return CompleteName.toString();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		Bounds base = Bounds.create(-30, -15, 30, 30);
		return base.rotate(Direction.EAST, facing, 0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new BitSelectorHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == StdAttr.WIDTH || attr == GROUP_ATTR) {
			updatePorts(instance);
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Plexers.drawTrapezoid(painter.getGraphics(), painter.getBounds(),
				painter.getAttributeValue(StdAttr.FACING), 9);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Direction facing = painter.getAttributeValue(StdAttr.FACING);

		Plexers.drawTrapezoid(g, painter.getBounds(), facing, 9);
		Bounds bds = painter.getBounds();
		g.setColor(Color.BLACK);
		GraphicsUtil.drawCenteredText(g, "Sel",
				bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight()
						/ 2);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		Value data = state.getPortValue(1);
		Value select = state.getPortValue(2);
		BitWidth groupBits = state.getAttributeValue(GROUP_ATTR);
		Value group;
		if (!select.isFullyDefined()) {
			group = Value.createUnknown(groupBits);
		} else {
			int shift = select.toIntValue() * groupBits.getWidth();
			if (shift >= data.getWidth()) {
				group = Value.createKnown(groupBits, 0);
			} else if (groupBits.getWidth() == 1) {
				group = data.get(shift);
			} else {
				Value[] bits = new Value[groupBits.getWidth()];
				for (int i = 0; i < bits.length; i++) {
					if (shift + i >= data.getWidth()) {
						bits[i] = Value.FALSE;
					} else {
						bits[i] = data.get(shift + i);
					}
				}
				group = Value.create(bits);
			}
		}
		state.setPort(0, group, Plexers.DELAY);
	}

	private void updatePorts(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		BitWidth data = instance.getAttributeValue(StdAttr.WIDTH);
		BitWidth group = instance.getAttributeValue(GROUP_ATTR);
		int groups = (data.getWidth() + group.getWidth() - 1)
				/ group.getWidth() - 1;
		int selectBits = 1;
		if (groups > 0) {
			while (groups != 1) {
				groups >>= 1;
				selectBits++;
			}
		}
		BitWidth select = BitWidth.create(selectBits);

		Location inPt;
		Location selPt;
		if (facing == Direction.WEST) {
			inPt = Location.create(30, 0);
			selPt = Location.create(10, 10);
		} else if (facing == Direction.NORTH) {
			inPt = Location.create(0, 30);
			selPt = Location.create(-10, 10);
		} else if (facing == Direction.SOUTH) {
			inPt = Location.create(0, -30);
			selPt = Location.create(-10, -10);
		} else {
			inPt = Location.create(-30, 0);
			selPt = Location.create(-10, 10);
		}

		Port[] ps = new Port[3];
		ps[0] = new Port(0, 0, Port.OUTPUT, group.getWidth());
		ps[1] = new Port(inPt.getX(), inPt.getY(), Port.INPUT, data.getWidth());
		ps[2] = new Port(selPt.getX(), selPt.getY(), Port.INPUT,
				select.getWidth());
		ps[0].setToolTip(Strings.getter("bitSelectorOutputTip"));
		ps[1].setToolTip(Strings.getter("bitSelectorDataTip"));
		ps[2].setToolTip(Strings.getter("bitSelectorSelectTip"));
		instance.setPorts(ps);
	}
}
