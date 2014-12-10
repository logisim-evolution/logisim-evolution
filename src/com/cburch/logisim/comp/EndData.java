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

package com.cburch.logisim.comp;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;

public class EndData {
	public static final int INPUT_ONLY = 1;
	public static final int OUTPUT_ONLY = 2;
	public static final int INPUT_OUTPUT = 3;

	private Location loc;
	private BitWidth width;
	private int i_o;
	private boolean exclusive;

	public EndData(Location loc, BitWidth width, int type) {
		this(loc, width, type, type == OUTPUT_ONLY);
	}

	public EndData(Location loc, BitWidth width, int type, boolean exclusive) {
		this.loc = loc;
		this.width = width;
		this.i_o = type;
		this.exclusive = exclusive;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof EndData))
			return false;
		if (other == this)
			return true;
		EndData o = (EndData) other;
		return o.loc.equals(this.loc) && o.width.equals(this.width)
				&& o.i_o == this.i_o && o.exclusive == this.exclusive;
	}

	public Location getLocation() {
		return loc;
	}

	public int getType() {
		return i_o;
	}

	public BitWidth getWidth() {
		return width;
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public boolean isInput() {
		return (i_o & INPUT_ONLY) != 0;
	}

	public boolean isOutput() {
		return (i_o & OUTPUT_ONLY) != 0;
	}
}
