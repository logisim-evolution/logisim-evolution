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
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableSelectionModel extends AttributeSetTableModel implements
		Selection.Listener {
	private Project project;
	private Frame frame;

	public AttrTableSelectionModel(Project project, Frame frame) {
		super(frame.getCanvas().getSelection().getAttributeSet());
		this.project = project;
		this.frame = frame;
		frame.getCanvas().getSelection().addListener(this);
	}

	@Override
	public String getTitle() {
		ComponentFactory wireFactory = null;
		ComponentFactory factory = null;
		int factoryCount = 0;
		int totalCount = 0;
		boolean variousFound = false;

		Selection selection = frame.getCanvas().getSelection();
		for (Component comp : selection.getComponents()) {
			ComponentFactory fact = comp.getFactory();
			if (fact.equals(factory)) {
				factoryCount++;
			} else if (comp instanceof Wire) {
				wireFactory = fact;
				if (factory == null) {
					factoryCount++;
				}
			} else if (factory == null) {
				factory = fact;
				factoryCount = 1;
			} else {
				variousFound = true;
			}
			if (!(comp instanceof Wire)) {
				totalCount++;
			}
		}

		if (factory == null) {
			factory = wireFactory;
		}

		if (variousFound) {
			return Strings.get("selectionVarious", "" + totalCount);
		} else if (factoryCount == 0) {
			String circName = frame.getCanvas().getCircuit().getName();
			return Strings.get("circuitAttrTitle", circName);
		} else if (factoryCount == 1) {
			return Strings.get("selectionOne", factory.getDisplayName());
		} else {
			return Strings.get("selectionMultiple", factory.getDisplayName(),
					"" + factoryCount);
		}
	}

	//
	// Selection.Listener methods
	public void selectionChanged(Event event) {
		fireTitleChanged();
		frame.setAttrTableModel(this);
	}

	@Override
	public void setValueRequested(Attribute<Object> attr, Object value)
			throws AttrTableSetException {
		Selection selection = frame.getCanvas().getSelection();
		Circuit circuit = frame.getCanvas().getCircuit();
		if (selection.isEmpty() && circuit != null) {
			AttrTableCircuitModel circuitModel = new AttrTableCircuitModel(
					project, circuit);
			circuitModel.setValueRequested(attr, value);
		} else {
			SetAttributeAction act = new SetAttributeAction(circuit,
					Strings.getter("selectionAttributeAction"));
			for (Component comp : selection.getComponents()) {
				if (!(comp instanceof Wire)) {
					act.set(comp, attr, value);
				}
			}
			project.doAction(act);
		}
	}
}
