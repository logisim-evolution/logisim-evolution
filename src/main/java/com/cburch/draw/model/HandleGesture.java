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

import java.awt.event.InputEvent;

public class HandleGesture {
	private Handle handle;
	private int dx;
	private int dy;
	private int modifiersEx;
	private Handle resultingHandle;

	public HandleGesture(Handle handle, int dx, int dy, int modifiersEx) {
		this.handle = handle;
		this.dx = dx;
		this.dy = dy;
		this.modifiersEx = modifiersEx;
	}

	public int getDeltaX() {
		return dx;
	}

	public int getDeltaY() {
		return dy;
	}

	public Handle getHandle() {
		return handle;
	}

	public int getModifiersEx() {
		return modifiersEx;
	}

	public Handle getResultingHandle() {
		return resultingHandle;
	}

	public boolean isAltDown() {
		return (modifiersEx & InputEvent.ALT_DOWN_MASK) != 0;
	}

	public boolean isControlDown() {
		return (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0;
	}

	public boolean isShiftDown() {
		return (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;
	}

	public void setResultingHandle(Handle value) {
		resultingHandle = value;
	}

	@Override
	public String toString() {
		return ("HandleGesture() [" + dx + ", " + dy + " : "
				+ handle.getObject() + "/" + handle.getX() + ", "
				+ handle.getY() + "]");
	}
}
