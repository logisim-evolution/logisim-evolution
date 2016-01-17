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
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
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

public class Multiplexer extends InstanceFactory {
	static void drawSelectCircle(Graphics g, Bounds bds, Location loc) {
		int locDelta = Math.max(bds.getHeight(), bds.getWidth()) <= 50 ? 8 : 6;
		Location circLoc;
		if (bds.getHeight() >= bds.getWidth()) { // vertically oriented
			if (loc.getY() < bds.getY() + bds.getHeight() / 2) { // at top
				circLoc = loc.translate(0, locDelta);
			} else { // at bottom
				circLoc = loc.translate(0, -locDelta);
			}
		} else {
			if (loc.getX() < bds.getX() + bds.getWidth() / 2) { // at left
				circLoc = loc.translate(locDelta, 0);
			} else { // at right
				circLoc = loc.translate(-locDelta, 0);
			}
		}
		g.setColor(Color.LIGHT_GRAY);
		g.fillOval(circLoc.getX() - 3, circLoc.getY() - 3, 6, 6);
	}

	public Multiplexer() {
		super("Multiplexer", Strings.getter("multiplexerComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING,
				Plexers.ATTR_SELECT_LOC, Plexers.ATTR_SELECT, StdAttr.WIDTH,
				Plexers.ATTR_DISABLED, Plexers.ATTR_ENABLE }, new Object[] {
				Direction.EAST, Plexers.SELECT_BOTTOM_LEFT,
				Plexers.DEFAULT_SELECT, BitWidth.ONE, Plexers.DISABLED_ZERO,
				Plexers.DEFAULT_ENABLE });
		setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(
				Plexers.ATTR_SELECT, 1, 5, 0), new BitWidthConfigurator(
				StdAttr.WIDTH)));
		setIconName("multiplexer.gif");
		setFacingAttribute(StdAttr.FACING);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		updatePorts(instance);
	}

	@Override
	public boolean contains(Location loc, AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		return Plexers.contains(loc, getOffsetBounds(attrs), facing);
	}

	@Override
	public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
		if (attr == Plexers.ATTR_ENABLE) {
			int newer = ver.compareTo(LogisimVersion.get(2, 6, 4));
			return Boolean.valueOf(newer >= 0);
		} else {
			return super.getDefaultAttributeValue(attr, ver);
		}
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
		if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1)
			CompleteName.append("_bus");
		CompleteName.append("_"
				+ Integer.toString(1 << attrs.getValue(Plexers.ATTR_SELECT)
						.getWidth()));
		return CompleteName.toString();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction dir = attrs.getValue(StdAttr.FACING);
		BitWidth select = attrs.getValue(Plexers.ATTR_SELECT);
		int inputs = 1 << select.getWidth();
		if (inputs == 2) {
			return Bounds.create(-30, -25, 30, 50).rotate(Direction.EAST, dir,
					0, 0);
		} else {
			int offs = -(inputs / 2) * 10 - 10;
			int length = inputs * 10 + 20;
			return Bounds.create(-40, offs, 40, length).rotate(Direction.EAST,
					dir, 0, 0);
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
			MyHDLGenerator = new MultiplexerHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING || attr == Plexers.ATTR_SELECT_LOC
				|| attr == Plexers.ATTR_SELECT) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == StdAttr.WIDTH || attr == Plexers.ATTR_ENABLE) {
			updatePorts(instance);
		} else if (attr == Plexers.ATTR_DISABLED) {
			instance.fireInvalidated();
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Direction facing = painter.getAttributeValue(StdAttr.FACING);
		BitWidth select = painter.getAttributeValue(Plexers.ATTR_SELECT);
		Bounds bds = painter.getBounds();

		if (select.getWidth() == 1) {
			if (facing == Direction.EAST || facing == Direction.WEST) {
				Plexers.drawTrapezoid(
						painter.getGraphics(),
						Bounds.create(bds.getX(), bds.getY() + 5,
								bds.getWidth(), bds.getHeight() - 10), facing,
						10);
			} else {
				Plexers.drawTrapezoid(
						painter.getGraphics(),
						Bounds.create(bds.getX() + 5, bds.getY(),
								bds.getWidth() - 10, bds.getHeight()), facing,
						10);
			}
		} else {
			Plexers.drawTrapezoid(painter.getGraphics(), bds, facing, 20);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();
		Direction facing = painter.getAttributeValue(StdAttr.FACING);
		BitWidth select = painter.getAttributeValue(Plexers.ATTR_SELECT);
		boolean enable = painter.getAttributeValue(Plexers.ATTR_ENABLE)
				.booleanValue();
		int inputs = 1 << select.getWidth();

		// draw stubs for select/enable inputs that aren't on instance boundary
		GraphicsUtil.switchToWidth(g, 3);
		boolean vertical = facing != Direction.NORTH
				&& facing != Direction.SOUTH;
		Object selectLoc = painter.getAttributeValue(Plexers.ATTR_SELECT_LOC);
		int selMult = selectLoc == Plexers.SELECT_BOTTOM_LEFT ? 1 : -1;
		int dx = vertical ? 0 : -selMult;
		int dy = vertical ? selMult : 0;
		if (inputs == 2) { // draw select wire
			Location pt = painter.getInstance().getPortLocation(inputs);
			if (painter.getShowState()) {
				g.setColor(painter.getPortValue(inputs).getColor());
			}
			g.drawLine(pt.getX() - 2 * dx, pt.getY() - 2 * dy, pt.getX(),
					pt.getY());
		}
		if (enable) {
			Location en = painter.getInstance().getPortLocation(inputs + 1);
			if (painter.getShowState()) {
				g.setColor(painter.getPortValue(inputs + 1).getColor());
			}
			int len = inputs == 2 ? 6 : 4;
			g.drawLine(en.getX() - len * dx, en.getY() - len * dy, en.getX(),
					en.getY());
		}
		GraphicsUtil.switchToWidth(g, 1);

		// draw a circle indicating where the select input is located
		Multiplexer.drawSelectCircle(g, bds, painter.getInstance()
				.getPortLocation(inputs));

		// draw a 0 indicating where the numbering starts for inputs
		int x0;
		int y0;
		int halign;
		if (facing == Direction.WEST) {
			x0 = bds.getX() + bds.getWidth() - 3;
			y0 = bds.getY() + 15 + (inputs == 2 ? 5 : 0);
			halign = GraphicsUtil.H_RIGHT;
		} else if (facing == Direction.NORTH) {
			x0 = bds.getX() + 10 + (inputs == 2 ? 5 : 0);
			y0 = bds.getY() + bds.getHeight() - 2;
			halign = GraphicsUtil.H_CENTER;
		} else if (facing == Direction.SOUTH) {
			x0 = bds.getX() + 10 + (inputs == 2 ? 5 : 0);
			y0 = bds.getY() + 12;
			halign = GraphicsUtil.H_CENTER;
		} else {
			x0 = bds.getX() + 3;
			y0 = bds.getY() + 15 + (inputs == 2 ? 5 : 0);
			halign = GraphicsUtil.H_LEFT;
		}
		g.setColor(Color.GRAY);
		GraphicsUtil.drawText(g, "0", x0, y0, halign, GraphicsUtil.V_BASELINE);

		// draw the trapezoid, "MUX" string, the individual ports
		g.setColor(Color.BLACK);
		if (inputs == 2) {
			if (facing == Direction.EAST || facing == Direction.WEST) {
				Plexers.drawTrapezoid(
						g,
						Bounds.create(bds.getX(), bds.getY() + 5,
								bds.getWidth(), bds.getHeight() - 10), facing,
						10);
			} else {
				Plexers.drawTrapezoid(
						g,
						Bounds.create(bds.getX() + 5, bds.getY(),
								bds.getWidth() - 10, bds.getHeight()), facing,
						10);
			}
		} else {
			Plexers.drawTrapezoid(g, bds, facing, 20);
		}
		GraphicsUtil.drawCenteredText(g, "MUX",
				bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight()
						/ 2);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth data = state.getAttributeValue(StdAttr.WIDTH);
		BitWidth select = state.getAttributeValue(Plexers.ATTR_SELECT);
		boolean enable = state.getAttributeValue(Plexers.ATTR_ENABLE)
				.booleanValue();
		int inputs = 1 << select.getWidth();
		Value en = enable ? state.getPortValue(inputs + 1) : Value.TRUE;
		Value out;
		if (en == Value.FALSE) {
			Object opt = state.getAttributeValue(Plexers.ATTR_DISABLED);
			Value base = opt == Plexers.DISABLED_ZERO ? Value.FALSE
					: Value.UNKNOWN;
			out = Value.repeat(base, data.getWidth());
		} else if (en == Value.ERROR && state.isPortConnected(inputs + 1)) {
			out = Value.createError(data);
		} else {
			Value sel = state.getPortValue(inputs);
			if (sel.isFullyDefined()) {
				out = state.getPortValue(sel.toIntValue());
			} else if (sel.isErrorValue()) {
				out = Value.createError(data);
			} else {
				out = Value.createUnknown(data);
			}
		}
		state.setPort(inputs + (enable ? 2 : 1), out, Plexers.DELAY);
	}

	private void updatePorts(Instance instance) {
		Direction dir = instance.getAttributeValue(StdAttr.FACING);
		Object selectLoc = instance.getAttributeValue(Plexers.ATTR_SELECT_LOC);
		BitWidth data = instance.getAttributeValue(StdAttr.WIDTH);
		BitWidth select = instance.getAttributeValue(Plexers.ATTR_SELECT);
		boolean enable = instance.getAttributeValue(Plexers.ATTR_ENABLE)
				.booleanValue();

		int selMult = selectLoc == Plexers.SELECT_BOTTOM_LEFT ? 1 : -1;
		int inputs = 1 << select.getWidth();
		Port[] ps = new Port[inputs + (enable ? 3 : 2)];
		Location sel;
		if (inputs == 2) {
			Location end0;
			Location end1;
			if (dir == Direction.WEST) {
				end0 = Location.create(30, -10);
				end1 = Location.create(30, 10);
				sel = Location.create(20, selMult * 20);
			} else if (dir == Direction.NORTH) {
				end0 = Location.create(-10, 30);
				end1 = Location.create(10, 30);
				sel = Location.create(selMult * -20, 20);
			} else if (dir == Direction.SOUTH) {
				end0 = Location.create(-10, -30);
				end1 = Location.create(10, -30);
				sel = Location.create(selMult * -20, -20);
			} else {
				end0 = Location.create(-30, -10);
				end1 = Location.create(-30, 10);
				sel = Location.create(-20, selMult * 20);
			}
			ps[0] = new Port(end0.getX(), end0.getY(), Port.INPUT,
					data.getWidth());
			ps[1] = new Port(end1.getX(), end1.getY(), Port.INPUT,
					data.getWidth());
		} else {
			int dx = -(inputs / 2) * 10;
			int ddx = 10;
			int dy = -(inputs / 2) * 10;
			int ddy = 10;
			if (dir == Direction.WEST) {
				dx = 40;
				ddx = 0;
				sel = Location.create(20, selMult * (dy + 10 * inputs));
			} else if (dir == Direction.NORTH) {
				dy = 40;
				ddy = 0;
				sel = Location.create(selMult * dx, 20);
			} else if (dir == Direction.SOUTH) {
				dy = -40;
				ddy = 0;
				sel = Location.create(selMult * dx, -20);
			} else {
				dx = -40;
				ddx = 0;
				sel = Location.create(-20, selMult * (dy + 10 * inputs));
			}
			for (int i = 0; i < inputs; i++) {
				ps[i] = new Port(dx, dy, Port.INPUT, data.getWidth());
				dx += ddx;
				dy += ddy;
			}
		}
		Location en = sel.translate(dir, 10);
		ps[inputs] = new Port(sel.getX(), sel.getY(), Port.INPUT,
				select.getWidth());
		if (enable) {
			ps[inputs + 1] = new Port(en.getX(), en.getY(), Port.INPUT,
					BitWidth.ONE);
		}
		ps[ps.length - 1] = new Port(0, 0, Port.OUTPUT, data.getWidth());

		for (int i = 0; i < inputs; i++) {
			ps[i].setToolTip(Strings.getter("multiplexerInTip", "" + i));
		}
		ps[inputs].setToolTip(Strings.getter("multiplexerSelectTip"));
		if (enable) {
			ps[inputs + 1].setToolTip(Strings.getter("multiplexerEnableTip"));
		}
		ps[ps.length - 1].setToolTip(Strings.getter("multiplexerOutTip"));

		instance.setPorts(ps);
	}
}
