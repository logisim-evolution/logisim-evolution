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

import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.std.memory.Mem.MemListener;

public class RamState extends MemState implements InstanceData,
		AttributeListener {

	private Instance parent;
	private MemListener listener;
	private ClockState clockState;
	private int CurrentData = 0;

	RamState(Instance parent, MemContents contents, MemListener listener) {
		super(contents);
		this.parent = parent;
		this.listener = listener;
		this.clockState = new ClockState();
		if (parent != null) {
			parent.getAttributeSet().addAttributeListener(this);
		}
		contents.addHexModelListener(listener);
	}

	@Override
	public void attributeListChanged(AttributeEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attributeValueChanged(AttributeEvent e) {
		AttributeSet attrs = e.getSource();
		BitWidth addrBits = attrs.getValue(Mem.ADDR_ATTR);
		BitWidth dataBits = attrs.getValue(Mem.DATA_ATTR);
		getContents().setDimensions(addrBits.getWidth(), dataBits.getWidth(),false);
	}

	@Override
	public RamState clone() {
		RamState ret = (RamState) super.clone();
		ret.parent = null;
		ret.clockState = this.clockState.clone();
		ret.getContents().addHexModelListener(listener);
		return ret;
	}

	int GetCurrentData() {
		return CurrentData;
	}

	public boolean setClock(Value newClock, Object trigger) {
		return clockState.updateClock(newClock, trigger);
	}

	void SetCurrentData(int data) {
		CurrentData = data;
	}

	void setRam(Instance value) {
		if (parent == value) {
			return;
		}
		if (parent != null) {
			parent.getAttributeSet().removeAttributeListener(this);
		}
		parent = value;
		if (value != null) {
			value.getAttributeSet().addAttributeListener(this);
		}
	}

}
