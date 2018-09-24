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

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableComponentModel extends AttributeSetTableModel {
	Project proj;
	Circuit circ;
	Component comp;

	AttrTableComponentModel(Project proj, Circuit circ, Component comp) {
		super(comp.getAttributeSet());
		this.proj = proj;
		this.circ = circ;
		this.comp = comp;
		SetInstance(comp.getFactory());
	}

	public Circuit getCircuit() {
		return circ;
	}

	public Component getComponent() {
		return comp;
	}

	@Override
	public String getTitle() {
		return comp.getFactory().getDisplayName();
	}

	@Override
	public void setValueRequested(Attribute<Object> attr, Object value)
			throws AttrTableSetException {
		if (!proj.getLogisimFile().contains(circ)) {
			String msg = Strings.get("cannotModifyCircuitError");
			throw new AttrTableSetException(msg);
		} else {
			SetAttributeAction act = new SetAttributeAction(circ,
					Strings.getter("changeAttributeAction"));
			act.set(comp, attr, value);
			proj.doAction(act);
		}
	}
}
