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

package com.cburch.logisim.circuit;

import java.util.ArrayList;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;

public class WidthIncompatibilityData {
	private ArrayList<Location> points;
	private ArrayList<BitWidth> widths;

	public WidthIncompatibilityData() {
		points = new ArrayList<Location>();
		widths = new ArrayList<BitWidth>();
	}

	public void add(Location p, BitWidth w) {
		for (int i = 0; i < points.size(); i++) {
			if (p.equals(points.get(i)) && w.equals(widths.get(i)))
				return;
		}
		points.add(p);
		widths.add(w);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof WidthIncompatibilityData))
			return false;
		if (this == other)
			return true;

		WidthIncompatibilityData o = (WidthIncompatibilityData) other;
		if (this.size() != o.size())
			return false;
		for (int i = 0; i < this.size(); i++) {
			Location p = this.getPoint(i);
			BitWidth w = this.getBitWidth(i);
			boolean matched = false;
			for (int j = 0; j < o.size(); j++) {
				Location q = this.getPoint(j);
				BitWidth x = this.getBitWidth(j);
				if (p.equals(q) && w.equals(x)) {
					matched = true;
					break;
				}
			}
			if (!matched)
				return false;
		}
		return true;
	}

	public BitWidth getBitWidth(int i) {
		return widths.get(i);
	}

	public Location getPoint(int i) {
		return points.get(i);
	}

	@Override
	public int hashCode() {
		return this.size();
	}

	public int size() {
		return points.size();
	}
}
