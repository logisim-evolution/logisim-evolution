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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

class ClockState implements Cloneable {
	private Value lastClock;

	public ClockState() {
		lastClock = Value.FALSE;
	}

	@Override
	public ClockState clone() {
		try {
			return (ClockState) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public boolean updateClock(Value newClock, Object trigger) {
		Value oldClock = lastClock;
		lastClock = newClock;
		if (trigger == null || trigger == StdAttr.TRIG_RISING) {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		} else if (trigger == StdAttr.TRIG_FALLING) {
			return oldClock == Value.TRUE && newClock == Value.FALSE;
		} else if (trigger == StdAttr.TRIG_HIGH) {
			return newClock == Value.TRUE;
		} else if (trigger == StdAttr.TRIG_LOW) {
			return newClock == Value.FALSE;
		} else {
			return oldClock == Value.FALSE && newClock == Value.TRUE;
		}
	}
}
