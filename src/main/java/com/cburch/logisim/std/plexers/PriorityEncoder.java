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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class PriorityEncoder extends InstanceFactory {
	static final int OUT = 0;
	static final int EN_IN = 1;
	static final int EN_OUT = 2;
	static final int GS = 3;

	public PriorityEncoder() {
		super("Priority Encoder", Strings.getter("priorityEncoderComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING, Plexers.ATTR_SELECT,
				Plexers.ATTR_DISABLED }, new Object[] { Direction.EAST,
				BitWidth.create(3), Plexers.DISABLED_ZERO });
		setKeyConfigurator(new BitWidthConfigurator(Plexers.ATTR_SELECT, 1, 5,
				0));
		setIconName("priencod.gif");
		setFacingAttribute(StdAttr.FACING);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction dir = attrs.getValue(StdAttr.FACING);
		BitWidth select = attrs.getValue(Plexers.ATTR_SELECT);
		int inputs = 1 << select.getWidth();
		int offs = -5 * inputs;
		int len = 10 * inputs + 10;
		if (dir == Direction.NORTH) {
			return Bounds.create(offs, 0, len, 40);
		} else if (dir == Direction.SOUTH) {
			return Bounds.create(offs, -40, len, 40);
		} else if (dir == Direction.WEST) {
			return Bounds.create(0, offs, 40, len);
		} else { // dir == Direction.EAST
			return Bounds.create(-40, offs, 40, len);
		}
	}

	@Override
	public boolean HasThreeStateDrivers(AttributeSet attrs) {
		return (attrs.getValue(Plexers.ATTR_DISABLED) == Plexers.DISABLED_FLOATING);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new PriorityEncoderHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING || attr == Plexers.ATTR_SELECT) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == Plexers.ATTR_SELECT) {
			updatePorts(instance);
		} else if (attr == Plexers.ATTR_DISABLED) {
			instance.fireInvalidated();
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Direction facing = painter.getAttributeValue(StdAttr.FACING);

		painter.drawBounds();
		Bounds bds = painter.getBounds();
		g.setColor(Color.GRAY);
		int x0;
		int y0;
		int halign;
		if (facing == Direction.WEST) {
			x0 = bds.getX() + bds.getWidth() - 3;
			y0 = bds.getY() + 15;
			halign = GraphicsUtil.H_RIGHT;
		} else if (facing == Direction.NORTH) {
			x0 = bds.getX() + 10;
			y0 = bds.getY() + bds.getHeight() - 2;
			halign = GraphicsUtil.H_CENTER;
		} else if (facing == Direction.SOUTH) {
			x0 = bds.getX() + 10;
			y0 = bds.getY() + 12;
			halign = GraphicsUtil.H_CENTER;
		} else {
			x0 = bds.getX() + 3;
			y0 = bds.getY() + 15;
			halign = GraphicsUtil.H_LEFT;
		}
		GraphicsUtil.drawText(g, "0", x0, y0, halign, GraphicsUtil.V_BASELINE);
		g.setColor(Color.BLACK);
		GraphicsUtil.drawCenteredText(g, "Pri",
				bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight()
						/ 2);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth select = state.getAttributeValue(Plexers.ATTR_SELECT);
		int n = 1 << select.getWidth();
		boolean enabled = state.getPortValue(n + EN_IN) != Value.FALSE;

		int out = -1;
		Value outDefault;
		if (enabled) {
			outDefault = Value.createUnknown(select);
			for (int i = n - 1; i >= 0; i--) {
				if (state.getPortValue(i) == Value.TRUE) {
					out = i;
					break;
				}
			}
		} else {
			Object opt = state.getAttributeValue(Plexers.ATTR_DISABLED);
			Value base = opt == Plexers.DISABLED_ZERO ? Value.FALSE
					: Value.UNKNOWN;
			outDefault = Value.repeat(base, select.getWidth());
		}
		if (out < 0) {
			state.setPort(n + OUT, outDefault, Plexers.DELAY);
			state.setPort(n + EN_OUT, enabled ? Value.TRUE : Value.FALSE,
					Plexers.DELAY);
			state.setPort(n + GS, Value.FALSE, Plexers.DELAY);
		} else {
			state.setPort(n + OUT, Value.createKnown(select, out),
					Plexers.DELAY);
			state.setPort(n + EN_OUT, Value.FALSE, Plexers.DELAY);
			state.setPort(n + GS, Value.TRUE, Plexers.DELAY);
		}
	}

	private void updatePorts(Instance instance) {
		Object dir = instance.getAttributeValue(StdAttr.FACING);
		BitWidth select = instance.getAttributeValue(Plexers.ATTR_SELECT);
		int n = 1 << select.getWidth();
		Port[] ps = new Port[n + 4];
		if (dir == Direction.NORTH || dir == Direction.SOUTH) {
			int x = -5 * n + 10;
			int y = dir == Direction.NORTH ? 40 : -40;
			for (int i = 0; i < n; i++) {
				ps[i] = new Port(x + 10 * i, y, Port.INPUT, 1);
			}
			ps[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
			ps[n + EN_IN] = new Port(x + 10 * n, y / 2, Port.INPUT, 1);
			ps[n + EN_OUT] = new Port(x - 10, y / 2, Port.OUTPUT, 1);
			ps[n + GS] = new Port(10, 0, Port.OUTPUT, 1);
		} else {
			int x = dir == Direction.EAST ? -40 : 40;
			int y = -5 * n + 10;
			for (int i = 0; i < n; i++) {
				ps[i] = new Port(x, y + 10 * i, Port.INPUT, 1);
			}
			ps[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
			ps[n + EN_IN] = new Port(x / 2, y + 10 * n, Port.INPUT, 1);
			ps[n + EN_OUT] = new Port(x / 2, y - 10, Port.OUTPUT, 1);
			ps[n + GS] = new Port(0, 10, Port.OUTPUT, 1);
		}

		for (int i = 0; i < n; i++) {
			ps[i].setToolTip(Strings.getter("priorityEncoderInTip", "" + i));
		}
		ps[n + OUT].setToolTip(Strings.getter("priorityEncoderOutTip"));
		ps[n + EN_IN].setToolTip(Strings.getter("priorityEncoderEnableInTip"));
		ps[n + EN_OUT]
				.setToolTip(Strings.getter("priorityEncoderEnableOutTip"));
		ps[n + GS].setToolTip(Strings.getter("priorityEncoderGroupSignalTip"));

		instance.setPorts(ps);
	}

}
