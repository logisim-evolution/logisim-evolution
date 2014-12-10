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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.Value;

class GateFunctions {
	static Value computeAnd(Value[] inputs, int numInputs) {
		Value ret = inputs[0];
		for (int i = 1; i < numInputs; i++) {
			ret = ret.and(inputs[i]);
		}
		return ret;
	}

	static Value computeExactlyOne(Value[] inputs, int numInputs) {
		int width = inputs[0].getWidth();
		Value[] ret = new Value[width];
		for (int i = 0; i < width; i++) {
			int count = 0;
			for (int j = 0; j < numInputs; j++) {
				Value v = inputs[j].get(i);
				if (v == Value.TRUE) {
					count++;
				} else if (v == Value.FALSE) {
					; // do nothing
				} else {
					count = -1;
					break;
				}
			}
			if (count < 0) {
				ret[i] = Value.ERROR;
			} else if (count == 1) {
				ret[i] = Value.TRUE;
			} else {
				ret[i] = Value.FALSE;
			}
		}
		return Value.create(ret);
	}

	static Value computeOddParity(Value[] inputs, int numInputs) {
		Value ret = inputs[0];
		for (int i = 1; i < numInputs; i++) {
			ret = ret.xor(inputs[i]);
		}
		return ret;
	}

	static Value computeOr(Value[] inputs, int numInputs) {
		Value ret = inputs[0];
		for (int i = 1; i < numInputs; i++) {
			ret = ret.or(inputs[i]);
		}
		return ret;
	}

	private GateFunctions() {
	}
}
