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

import java.util.Iterator;

import com.cburch.logisim.data.Location;

class WireIterator implements Iterator<Location> {
	private int curX;
	private int curY;
	private int destX;
	private int destY;
	private int deltaX;
	private int deltaY;
	private boolean destReturned;

	public WireIterator(Location e0, Location e1) {
		curX = e0.getX();
		curY = e0.getY();
		destX = e1.getX();
		destY = e1.getY();
		destReturned = false;
		if (curX < destX)
			deltaX = 10;
		else if (curX > destX)
			deltaX = -10;
		else
			deltaX = 0;
		if (curY < destY)
			deltaY = 10;
		else if (curY > destY)
			deltaY = -10;
		else
			deltaY = 0;

		int offX = (destX - curX) % 10;
		if (offX != 0) { // should not happen, but in case it does...
			destX = curX + deltaX * ((destX - curX) / 10);
		}
		int offY = (destY - curY) % 10;
		if (offY != 0) { // should not happen, but in case it does...
			destY = curY + deltaY * ((destY - curY) / 10);
		}
	}

	public boolean hasNext() {
		return !destReturned;
	}

	public Location next() {
		Location ret = Location.create(curX, curY);
		destReturned |= curX == destX && curY == destY;
		curX += deltaX;
		curY += deltaY;
		return ret;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
