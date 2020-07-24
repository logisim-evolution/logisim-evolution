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

package com.cburch.logisim.std.fsm;



import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;



class FSMEntityState implements InstanceData, Cloneable, FSMModelListener,
		AttributeListener {

	private Instance parent;
	private FSMContent content;

	public FSMEntityState(Instance parent, FSMContent content) {
		this.content = content;
		this.content.addFSMModelListener(this);
		this.parent = parent;

		if (this.parent != null)
			parent.getAttributeSet().addAttributeListener(this);
	}

	@Override
	public void attributeListChanged(AttributeEvent e) {

	}

	@Override
	public void attributeValueChanged(AttributeEvent e) {

	}

	@Override
	public FSMEntityState clone() {
		try {
			FSMEntityState ret = (FSMEntityState) super.clone();
			ret.parent = null;
			ret.content = content.clone();
			ret.content.addFSMModelListener(ret);
			return ret;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public void contentSet(FSMContent source) {

	}

	public FSMContent getContent() {
		return this.content;
	}

	void setFSMEntity(Instance value) {
		if (parent == value)
			return;

		if (parent != null)
			parent.getAttributeSet().removeAttributeListener(this);

		parent = value;
		if (value != null)
			value.getAttributeSet().addAttributeListener(this);
	}

}
