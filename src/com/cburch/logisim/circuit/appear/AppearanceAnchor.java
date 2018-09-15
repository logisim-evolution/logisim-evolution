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

package com.cburch.logisim.circuit.appear;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

public class AppearanceAnchor extends AppearanceElement {
	public static final Attribute<Direction> FACING = Attributes.forDirection(
			"facing", Strings.getter("appearanceFacingAttr"));
	static final List<Attribute<?>> ATTRIBUTES = UnmodifiableList
			.create(new Attribute<?>[] { FACING });

	private static final int RADIUS = 3;
	private static final int INDICATOR_LENGTH = 8;
	private static final Color SYMBOL_COLOR = new Color(0, 128, 0);

	private Direction facing;

	public AppearanceAnchor(Location location) {
		super(location);
		facing = Direction.EAST;
	}

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		if (super.isInCircle(loc, RADIUS)) {
			return true;
		} else {
			Location center = getLocation();
			Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
			if (facing == Direction.EAST || facing == Direction.WEST) {
				return Math.abs(loc.getY() - center.getY()) < 2
						&& (loc.getX() < center.getX()) != (loc.getX() < end
								.getX());
			} else {
				return Math.abs(loc.getX() - center.getX()) < 2
						&& (loc.getY() < center.getY()) != (loc.getY() < end
								.getY());
			}
		}
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}

	@Override
	public Bounds getBounds() {
		Bounds bds = super.getBounds(RADIUS);
		Location center = getLocation();
		Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
		return bds.add(end);
	}

	@Override
	public String getDisplayName() {
		return Strings.get("circuitAnchor");
	}

	public Direction getFacing() {
		return facing;
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		Location c = getLocation();
		Location end = c.translate(facing, RADIUS + INDICATOR_LENGTH);
		return UnmodifiableList.create(new Handle[] { new Handle(this, c),
				new Handle(this, end) });
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == FACING) {
			return (V) facing;
		} else {
			return super.getValue(attr);
		}
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof AppearanceAnchor) {
			AppearanceAnchor that = (AppearanceAnchor) other;
			return super.matches(that) && this.facing.equals(that.facing);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return super.matchesHashCode() * 31 + facing.hashCode();
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		Location location = getLocation();
		int x = location.getX();
		int y = location.getY();
		g.setColor(SYMBOL_COLOR);
		g.drawOval(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
		Location e0 = location.translate(facing, RADIUS);
		Location e1 = location.translate(facing, RADIUS + INDICATOR_LENGTH);
		g.drawLine(e0.getX(), e0.getY(), e1.getX(), e1.getY());
	}

	@Override
	public Element toSvgElement(Document doc) {
		Location loc = getLocation();
		Element ret = doc.createElement("circ-anchor");
		ret.setAttribute("x", "" + (loc.getX() - RADIUS));
		ret.setAttribute("y", "" + (loc.getY() - RADIUS));
		ret.setAttribute("width", "" + 2 * RADIUS);
		ret.setAttribute("height", "" + 2 * RADIUS);
		ret.setAttribute("facing", facing.toString());
		return ret;
	}

	@Override
	protected void updateValue(Attribute<?> attr, Object value) {
		if (attr == FACING) {
			facing = (Direction) value;
		} else {
			super.updateValue(attr, value);
		}
	}
}
