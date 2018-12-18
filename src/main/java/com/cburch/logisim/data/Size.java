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

package com.cburch.logisim.data;

/**
 * Represents the dimensions of a rectangle. This is analogous to java.awt's
 * <code>Dimension</code> class, except that objects of this type are immutable.
 */
public class Size {
	public static Size create(int wid, int ht) {
		return new Size(wid, ht);
	}

	private final int wid;
	private final int ht;

	private Size(int wid, int ht) {
		this.wid = wid;
		this.ht = ht;
	}

	public boolean contains(int x, int y) {
		return x >= 0 && y >= 0 && x < this.wid && y < this.ht;
	}

	public boolean contains(int x, int y, int wid, int ht) {
		int oth_x = (wid <= 0 ? x : x + wid - 1);
		int oth_y = (ht <= 0 ? y : y + wid - 1);
		return contains(x, y) && contains(oth_x, oth_y);
	}

	public boolean contains(Location p) {
		return contains(p.getX(), p.getY());
	}

	public boolean contains(Size bd) {
		return contains(bd.wid, bd.ht);
	}

	@Override
	public boolean equals(Object other_obj) {
		if (!(other_obj instanceof Size))
			return false;
		Size other = (Size) other_obj;
		return wid == other.wid && ht == other.ht;
	}

	public int getHeight() {
		return ht;
	}

	public int getWidth() {
		return wid;
	}

	public java.awt.Dimension toAwtDimension() {
		return new java.awt.Dimension(wid, ht);
	}

	@Override
	public String toString() {
		return wid + "x" + ht;
	}

}
