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

public class CircuitEvent {
	public final static int ACTION_SET_NAME = 0; // name changed
	public final static int ACTION_ADD = 1; // component added
	public final static int ACTION_REMOVE = 2; // component removed
	public final static int ACTION_CHANGE = 3; // component changed
	public final static int ACTION_INVALIDATE = 4; // component invalidated (pin
													// types changed)
	public final static int ACTION_CLEAR = 5; // entire circuit cleared
	public final static int TRANSACTION_DONE = 6;
	public final static int CHANGE_DEFAULT_BOX_APPEARANCE = 7;
	public final static int ACTION_CHECK_NAME = 8;

	private int action;
	private Circuit circuit;
	private Object data;

	CircuitEvent(int action, Circuit circuit, Object data) {
		this.action = action;
		this.circuit = circuit;
		this.data = data;
	}

	// access methods
	public int getAction() {
		return action;
	}

	public Circuit getCircuit() {
		return circuit;
	}

	public Object getData() {
		return data;
	}

	public CircuitTransactionResult getResult() {
		return (CircuitTransactionResult) data;
	}
}
