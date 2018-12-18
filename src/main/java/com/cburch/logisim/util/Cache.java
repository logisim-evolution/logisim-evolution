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

package com.cburch.logisim.util;

/**
 * Allows immutable objects to be cached in memory in order to reduce the
 * creation of duplicate objects.
 */
public class Cache {
	private int mask;
	private Object[] data;

	public Cache() {
		this(8);
	}

	public Cache(int logSize) {
		if (logSize > 12)
			logSize = 12;

		data = new Object[1 << logSize];
		mask = data.length - 1;
	}

	public Object get(int hashCode) {
		return data[hashCode & mask];
	}

	public Object get(Object value) {
		if (value == null)
			return null;
		int code = value.hashCode() & mask;
		Object ret = data[code];
		if (ret != null && ret.equals(value)) {
			return ret;
		} else {
			data[code] = value;
			return value;
		}
	}

	public void put(int hashCode, Object value) {
		if (value != null) {
			data[hashCode & mask] = value;
		}
	}
}
