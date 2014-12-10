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

import java.util.concurrent.CopyOnWriteArraySet;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;

class WireBundle {
	private BitWidth width = BitWidth.UNKNOWN;
	private Value pullValue = Value.UNKNOWN;
	private WireBundle parent;
	private Location widthDeterminant = null;
	private boolean isBuss = false;
	WireThread[] threads = null;
	CopyOnWriteArraySet<Location> points = new CopyOnWriteArraySet<Location>(); // points
																				// bundle
																				// hits
	private WidthIncompatibilityData incompatibilityData = null;

	WireBundle() {
		parent = this;
	}

	void addPullValue(Value val) {
		pullValue = pullValue.combine(val);
	}

	WireBundle find() {
		WireBundle ret = this;
		if (ret.parent != ret) {
			do
				ret = ret.parent;
			while (ret.parent != ret);
			this.parent = ret;
		}
		return ret;
	}

	Value getPullValue() {
		return pullValue;
	}

	BitWidth getWidth() {
		if (incompatibilityData != null) {
			return BitWidth.UNKNOWN;
		} else {
			return width;
		}
	}

	Location getWidthDeterminant() {
		if (incompatibilityData != null) {
			return null;
		} else {
			return widthDeterminant;
		}
	}

	WidthIncompatibilityData getWidthIncompatibilityData() {
		return incompatibilityData;
	}

	boolean isBus() {
		return isBuss;
	}

	void isolate() {
		parent = this;
	}

	boolean isValid() {
		return incompatibilityData == null;
	}

	void setWidth(BitWidth width, Location det) {
		if (width == BitWidth.UNKNOWN)
			return;
		if (incompatibilityData != null) {
			incompatibilityData.add(det, width);
			return;
		}
		if (this.width != BitWidth.UNKNOWN) {
			if (width.equals(this.width)) {
				isBuss = width.getWidth() > 1;
				return; // the widths match, and the bundle is already set;
						// nothing to do
			} else { // the widths are broken: Create incompatibilityData
						// holding this info
				incompatibilityData = new WidthIncompatibilityData();
				incompatibilityData.add(widthDeterminant, this.width);
				incompatibilityData.add(det, width);
				return;
			}
		}
		this.width = width;
		this.widthDeterminant = det;
		this.threads = new WireThread[width.getWidth()];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new WireThread();
		}
	}

	void unite(WireBundle other) {
		WireBundle group = this.find();
		WireBundle group2 = other.find();
		if (group != group2)
			group.parent = group2;
	}
}
