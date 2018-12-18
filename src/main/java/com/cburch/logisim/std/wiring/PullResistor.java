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

package com.cburch.logisim.std.wiring;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

public class PullResistor extends InstanceFactory {
	private static Value getPullValue(AttributeSet attrs) {
		AttributeOption opt = attrs.getValue(ATTR_PULL_TYPE);
		return (Value) opt.getValue();
	}

	public static Value getPullValue(Instance instance) {
		return getPullValue(instance.getAttributeSet());
	}

	public static final Attribute<AttributeOption> ATTR_PULL_TYPE = Attributes
			.forOption(
					"pull",
					Strings.getter("pullTypeAttr"),
					new AttributeOption[] {
							new AttributeOption(Value.FALSE, "0", Strings
									.getter("pullZeroType")),
							new AttributeOption(Value.TRUE, "1", Strings
									.getter("pullOneType")),
							new AttributeOption(Value.ERROR, "X", Strings
									.getter("pullErrorType")) });
	public static final PullResistor FACTORY = new PullResistor();

	private static final Icon ICON_SHAPED = Icons.getIcon("pullshap.gif");

	private static final Icon ICON_RECTANGULAR = Icons.getIcon("pullrect.gif");

	public PullResistor() {
		super("Pull Resistor", Strings.getter("pullComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING, ATTR_PULL_TYPE },
				new Object[] { Direction.SOUTH, ATTR_PULL_TYPE.parse("0") });
		setFacingAttribute(StdAttr.FACING);
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		instance.setPorts(new Port[] { new Port(0, 0, Port.INOUT,
				BitWidth.UNKNOWN) });
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		if (facing == Direction.EAST) {
			return Bounds.create(-42, -6, 42, 12);
		} else if (facing == Direction.WEST) {
			return Bounds.create(0, -6, 42, 12);
		} else if (facing == Direction.NORTH) {
			return Bounds.create(-6, 0, 12, 42);
		} else {
			return Bounds.create(-6, -42, 12, 42);
		}
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
		} else if (attr == ATTR_PULL_TYPE) {
			instance.fireInvalidated();
		}
	}

	private void paintBase(InstancePainter painter, Value pullValue,
			Color inColor, Color outColor) {
		boolean color = painter.shouldDrawColor();
		Direction facing = painter.getAttributeValue(StdAttr.FACING);
		Graphics g = painter.getGraphics();
		Color baseColor = g.getColor();
		GraphicsUtil.switchToWidth(g, 3);
		if (color && inColor != null)
			g.setColor(inColor);
		if (facing == Direction.EAST) {
			GraphicsUtil.drawText(g, pullValue.toDisplayString(), -32, 0,
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
		} else if (facing == Direction.WEST) {
			GraphicsUtil.drawText(g, pullValue.toDisplayString(), 32, 0,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		} else if (facing == Direction.NORTH) {
			GraphicsUtil.drawText(g, pullValue.toDisplayString(), 0, 32,
					GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
		} else {
			GraphicsUtil.drawText(g, pullValue.toDisplayString(), 0, -32,
					GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
		}

		double rotate = 0.0;
		if (g instanceof Graphics2D) {
			rotate = Direction.SOUTH.toRadians() - facing.toRadians();
			if (rotate != 0.0)
				((Graphics2D) g).rotate(rotate);
		}
		g.drawLine(0, -30, 0, -26);
		g.drawLine(-6, -30, 6, -30);
		if (color && outColor != null)
			g.setColor(outColor);
		g.drawLine(0, -4, 0, 0);
		g.setColor(baseColor);
		GraphicsUtil.switchToWidth(g, 2);
		if (painter.getGateShape() == AppPreferences.SHAPE_SHAPED) {
			int[] xp = { 0, -5, 5, -5, 5, -5, 0 };
			int[] yp = { -25, -23, -19, -15, -11, -7, -5 };
			g.drawPolyline(xp, yp, xp.length);
		} else {
			g.drawRect(-5, -25, 10, 20);
		}
		if (rotate != 0.0) {
			((Graphics2D) g).rotate(-rotate);
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Value pull = getPullValue(painter.getAttributeSet());
		paintBase(painter, pull, null, null);
	}

	//
	// graphics methods
	//
	@Override
	public void paintIcon(InstancePainter painter) {
		Icon icon;
		if (painter.getGateShape() == AppPreferences.SHAPE_SHAPED) {
			icon = ICON_SHAPED;
		} else {
			icon = ICON_RECTANGULAR;
		}
		icon.paintIcon(painter.getDestination(), painter.getGraphics(), 2, 2);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();
		Graphics g = painter.getGraphics();
		g.translate(x, y);
		Value pull = getPullValue(painter.getAttributeSet());
		Value actual = painter.getPortValue(0);
		paintBase(painter, pull, pull.getColor(), actual.getColor());
		g.translate(-x, -y);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		; // nothing to do - handled by CircuitWires
	}
}