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

import java.util.Arrays;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.prefs.AppPreferences;

class ShiftRegisterData extends ClockState implements InstanceData {
	private BitWidth width;
	private Value[] vs;
	private int vsPos;

	public ShiftRegisterData(BitWidth width, int len) {
		this.width = width;
		this.vs = new Value[len];
		Arrays.fill(this.vs, (AppPreferences.Memory_Startup_Unknown.get())?
				Value.createUnknown(width):Value.createKnown(width, 0));
		this.vsPos = 0;
	}

	public void clear() {
		Arrays.fill(vs, Value.createKnown(width, 0));
		vsPos = 0;
	}

	@Override
	public ShiftRegisterData clone() {
		ShiftRegisterData ret = (ShiftRegisterData) super.clone();
		ret.vs = this.vs.clone();
		return ret;
	}

	public Value get(int index) {
		int i = vsPos + index;
		Value[] v = vs;
		if (i >= v.length)
			i -= v.length;
		return v[i];
	}

	public int getLength() {
		return vs.length;
	}

	public void push(Value v) {
		int pos = vsPos;
		vs[pos] = v;
		vsPos = pos >= vs.length - 1 ? 0 : pos + 1;
	}

	public void set(int index, Value val) {
		int i = vsPos + index;
		Value[] v = vs;
		if (i >= v.length)
			i -= v.length;
		v[i] = val;
	}

	public void setDimensions(BitWidth newWidth, int newLength) {
		Value[] v = vs;
		BitWidth oldWidth = width;
		int oldW = oldWidth.getWidth();
		int newW = newWidth.getWidth();
		if (v.length != newLength) {
			Value[] newV = new Value[newLength];
			int j = vsPos;
			int copy = Math.min(newLength, v.length);
			for (int i = 0; i < copy; i++) {
				newV[i] = v[j];
				j++;
				if (j == v.length)
					j = 0;
			}
			Arrays.fill(newV, copy, newLength, Value.createKnown(newWidth, 0));
			v = newV;
			vsPos = 0;
			vs = newV;
		}
		if (oldW != newW) {
			for (int i = 0; i < v.length; i++) {
				Value vi = v[i];
				if (vi.getWidth() != newW) {
					v[i] = vi.extendWidth(newW, Value.FALSE);
				}
			}
			width = newWidth;
		}
	}
}