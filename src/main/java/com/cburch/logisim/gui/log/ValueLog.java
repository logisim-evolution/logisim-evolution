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

package com.cburch.logisim.gui.log;

import com.cburch.logisim.data.Value;

class ValueLog {
	private static final int LOG_SIZE = 400;

	private Value[] log;
	private short curSize;
	private short firstIndex;

	public ValueLog() {
		log = new Value[LOG_SIZE];
		curSize = 0;
		firstIndex = 0;
	}

	public void append(Value val) {
		if (curSize < LOG_SIZE) {
			log[curSize] = val;
			curSize++;
		} else {
			log[firstIndex] = val;
			firstIndex++;
			if (firstIndex >= LOG_SIZE)
				firstIndex = 0;
		}
	}

	public Value get(int index) {
		int i = firstIndex + index;
		if (i >= LOG_SIZE)
			i -= LOG_SIZE;
		return log[i];
	}

	public Value getLast() {
		return curSize < LOG_SIZE ? (curSize == 0 ? null : log[curSize - 1])
				: (firstIndex == 0 ? log[curSize - 1] : log[firstIndex - 1]);
	}

	public int size() {
		return curSize;
	}
}
