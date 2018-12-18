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

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;

public class Selection {

	private CircuitState root;
	private Model model;
	private ArrayList<SelectionItem> components;

	public Selection(CircuitState root, Model model) {
		this.root = root;
		this.model = model;
		components = new ArrayList<SelectionItem>();
	}

	public void add(SelectionItem item) {
		components.add(item);
		model.fireSelectionChanged(new ModelEvent());
	}

	public void addModelListener(ModelListener l) {
		model.addModelListener(l);
	}

	public boolean contains(SelectionItem item) {
		return components.contains(item);
	}

	public SelectionItem get(int index) {
		return components.get(index);
	}

	public CircuitState getCircuitState() {
		return root;
	}

	public int indexOf(SelectionItem value) {
		return components.indexOf(value);
	}

	public void move(int fromIndex, int toIndex) {
		if (fromIndex == toIndex) {
			return;
		}
		SelectionItem o = components.remove(fromIndex);
		components.add(toIndex, o);
		model.fireSelectionChanged(new ModelEvent());
	}

	public void remove(int index) {
		components.remove(index);
		model.fireSelectionChanged(new ModelEvent());
	}

	public void removeModelListener(ModelListener l) {
		model.removeModelListener(l);
	}

	public int size() {
		return components.size();
	}
}
