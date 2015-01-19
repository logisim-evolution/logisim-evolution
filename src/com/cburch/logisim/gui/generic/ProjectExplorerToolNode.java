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

package com.cburch.logisim.gui.generic;

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;

public class ProjectExplorerToolNode extends ProjectExplorerModel.Node<Tool>
		implements CircuitListener {

	private static final long serialVersionUID = 1L;
	private Circuit circuit;

	public ProjectExplorerToolNode(ProjectExplorerModel model, Tool tool) {
		super(model, tool);
		if (tool instanceof AddTool) {
			Object factory = ((AddTool) tool).getFactory();

			if (factory instanceof SubcircuitFactory) {
				circuit = ((SubcircuitFactory) factory).getSubcircuit();
				circuit.addCircuitListener(this);
			}
		}
	}

	public void circuitChanged(CircuitEvent event) {
		int act = event.getAction();

		if (act == CircuitEvent.ACTION_SET_NAME) {
			fireStructureChanged();
			// The following almost works - but the labels aren't made
			// bigger, so you get "..." behavior with longer names.
			// fireNodesChanged(findPath(this));
		}
	}

	@Override
	ProjectExplorerToolNode create(Tool userObject) {
		return new ProjectExplorerToolNode(getModel(), userObject);
	}

	@Override
	void decommission() {
		if (circuit != null) {
			circuit.removeCircuitListener(this);
		}
	}

}
