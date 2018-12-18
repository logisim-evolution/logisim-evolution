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

package com.cburch.gray;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;

/** Represents the state of a counter. */
class CounterData implements InstanceData, Cloneable {
	/**
	 * Retrieves the state associated with this counter in the circuit state,
	 * generating the state if necessary.
	 */
	public static CounterData get(InstanceState state, BitWidth width) {
		CounterData ret = (CounterData) state.getData();
		if (ret == null) {
			// If it doesn't yet exist, then we'll set it up with our default
			// values and put it into the circuit state so it can be retrieved
			// in future propagations.
			ret = new CounterData(null, Value.createKnown(width, 0));
			state.setData(ret);
		} else if (!ret.value.getBitWidth().equals(width)) {
			ret.value = ret.value.extendWidth(width.getWidth(), Value.FALSE);
		}
		return ret;
	}

	/** The last clock input value observed. */
	private Value lastClock;

	/** The current value emitted by the counter. */
	private Value value;

	/** Constructs a state with the given values. */
	public CounterData(Value lastClock, Value value) {
		this.lastClock = lastClock;
		this.value = value;
	}

	/** Returns a copy of this object. */
	@Override
	public Object clone() {
		// We can just use what super.clone() returns: The only instance
		// variables are
		// Value objects, which are immutable, so we don't care that both the
		// copy
		// and the copied refer to the same Value objects. If we had mutable
		// instance
		// variables, then of course we would need to clone them.
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	/** Returns the current value emitted by the counter. */
	public Value getValue() {
		return value;
	}

	/** Updates the current value emitted by the counter. */
	public void setValue(Value value) {
		this.value = value;
	}

	/** Updates the last clock observed, returning true if triggered. */
	public boolean updateClock(Value value) {
		Value old = lastClock;
		lastClock = value;
		return old == Value.FALSE && value == Value.TRUE;
	}
}
