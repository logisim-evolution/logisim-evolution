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

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;

public abstract class AppearanceElement extends AbstractCanvasObject {
	private Location location;

	public AppearanceElement(Location location) {
		this.location = location;
	}

	@Override
	public boolean canRemove() {
		return false;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return Collections.emptyList();
	}

	protected Bounds getBounds(int radius) {
		return Bounds.create(location.getX() - radius,
				location.getY() - radius, 2 * radius, 2 * radius);
	}

	public Location getLocation() {
		return location;
	}

	@Override
	public Location getRandomPoint(Bounds bds, Random rand) {
		return null; // this is only used to determine what lies on top of what
						// - but the elements will always be on top anyway
	}

	@Override
	public <V> V getValue(Attribute<V> attr) {
		return null;
	}

	protected boolean isInCircle(Location loc, int radius) {
		int dx = loc.getX() - location.getX();
		int dy = loc.getY() - location.getY();
		return dx * dx + dy * dy < radius * radius;
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof AppearanceElement) {
			AppearanceElement that = (AppearanceElement) other;
			return this.location.equals(that.location);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return location.hashCode();
	}

	@Override
	public void translate(int dx, int dy) {
		location = location.translate(dx, dy);
	}

	@Override
	protected void updateValue(Attribute<?> attr, Object value) {
		// nothing to do
	}
}
