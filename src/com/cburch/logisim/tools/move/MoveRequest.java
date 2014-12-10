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

package com.cburch.logisim.tools.move;

class MoveRequest {
	private MoveGesture gesture;
	private int dx;
	private int dy;

	public MoveRequest(MoveGesture gesture, int dx, int dy) {
		this.gesture = gesture;
		this.dx = dx;
		this.dy = dy;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof MoveRequest) {
			MoveRequest o = (MoveRequest) other;
			return this.gesture == o.gesture && this.dx == o.dx
					&& this.dy == o.dy;
		} else {
			return false;
		}
	}

	public int getDeltaX() {
		return dx;
	}

	public int getDeltaY() {
		return dy;
	}

	public MoveGesture getMoveGesture() {
		return gesture;
	}

	@Override
	public int hashCode() {
		return (gesture.hashCode() * 31 + dx) * 31 + dy;
	}
}
