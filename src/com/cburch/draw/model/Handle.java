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

package com.cburch.draw.model;

import com.cburch.logisim.data.Location;

public class Handle {
	private CanvasObject object;
	private int x;
	private int y;

	public Handle(CanvasObject object, int x, int y) {
		this.object = object;
		this.x = x;
		this.y = y;
	}

	public Handle(CanvasObject object, Location loc) {
		this(object, loc.getX(), loc.getY());
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Handle) {
			Handle that = (Handle) other;
			return this.object.equals(that.object) && this.x == that.x
					&& this.y == that.y;
		} else {
			return false;
		}
	}

	public Location getLocation() {
		return Location.create(x, y);
	}

	public CanvasObject getObject() {
		return object;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		return (this.object.hashCode() * 31 + x) * 31 + y;
	}

	public boolean isAt(int xq, int yq) {
		return x == xq && y == yq;
	}

	public boolean isAt(Location loc) {
		return x == loc.getX() && y == loc.getY();
	}
}
