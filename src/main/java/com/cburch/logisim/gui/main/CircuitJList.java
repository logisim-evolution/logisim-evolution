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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JList;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;

@SuppressWarnings("rawtypes")
class CircuitJList extends JList {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public CircuitJList(Project proj, boolean includeEmpty) {
		LogisimFile file = proj.getLogisimFile();
		Circuit current = proj.getCurrentCircuit();
		Vector<Circuit> options = new Vector<Circuit>();
		boolean currentFound = false;
		for (Circuit circ : file.getCircuits()) {
			if (!includeEmpty || circ.getBounds() != Bounds.EMPTY_BOUNDS) {
				if (circ == current)
					currentFound = true;
				options.add(circ);
			}
		}

		setListData(options);
		if (currentFound)
			setSelectedValue(current, true);
		setVisibleRowCount(Math.min(6, options.size()));
	}

	public List<Circuit> getSelectedCircuits() {
		Object[] selected = getSelectedValuesList().toArray();
		if (selected != null && selected.length > 0) {
			ArrayList<Circuit> ret = new ArrayList<Circuit>(selected.length);
			for (Object sel : selected) {
				if (sel instanceof Circuit)
					ret.add((Circuit) sel);
			}
			return ret;
		} else {
			return Collections.emptyList();
		}
	}

}
