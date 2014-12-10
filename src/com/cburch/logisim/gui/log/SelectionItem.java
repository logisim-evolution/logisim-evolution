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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;

public class SelectionItem implements AttributeListener, CircuitListener {
	private Model model;
	private Component[] path;
	private Component comp;
	private Object option;
	private int radix = 2;
	private String shortDescriptor;
	private String longDescriptor;

	public SelectionItem(Model model, Component[] path, Component comp,
			Object option) {
		this.model = model;
		this.path = path;
		this.comp = comp;
		this.option = option;
		computeDescriptors();

		if (path != null) {
			model.getCircuitState().getCircuit().addCircuitListener(this);
			for (int i = 0; i < path.length; i++) {
				path[i].getAttributeSet().addAttributeListener(this);
				SubcircuitFactory circFact = (SubcircuitFactory) path[i]
						.getFactory();
				circFact.getSubcircuit().addCircuitListener(this);
			}
		}
		comp.getAttributeSet().addAttributeListener(this);
	}

	public void attributeListChanged(AttributeEvent e) {
	}

	public void attributeValueChanged(AttributeEvent e) {
		if (computeDescriptors()) {
			model.fireSelectionChanged(new ModelEvent());
		}
	}

	public void circuitChanged(CircuitEvent event) {
		int action = event.getAction();
		if (action == CircuitEvent.ACTION_CLEAR
				|| action == CircuitEvent.ACTION_REMOVE) {
			Circuit circ = event.getCircuit();
			Component circComp = null;
			if (circ == model.getCircuitState().getCircuit()) {
				circComp = path != null && path.length > 0 ? path[0] : comp;
			} else if (path != null) {
				for (int i = 0; i < path.length; i++) {
					SubcircuitFactory circFact = (SubcircuitFactory) path[i]
							.getFactory();
					if (circ == circFact.getSubcircuit()) {
						circComp = i + 1 < path.length ? path[i + 1] : comp;
					}
				}
			}
			if (circComp == null)
				return;

			if (action == CircuitEvent.ACTION_REMOVE
					&& event.getData() != circComp) {
				return;
			}

			int index = model.getSelection().indexOf(this);
			if (index < 0)
				return;
			model.getSelection().remove(index);
		}
	}

	private boolean computeDescriptors() {
		boolean changed = false;

		Loggable log = (Loggable) comp.getFeature(Loggable.class);
		String newShort = log.getLogName(option);
		if (newShort == null || newShort.equals("")) {
			newShort = comp.getFactory().getDisplayName()
					+ comp.getLocation().toString();
			if (option != null) {
				newShort += "." + option.toString();
			}
		}
		if (!newShort.equals(shortDescriptor)) {
			changed = true;
			shortDescriptor = newShort;
		}

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < path.length; i++) {
			if (i > 0)
				buf.append(".");
			String label = path[i].getAttributeSet().getValue(StdAttr.LABEL);
			if (label != null && !label.equals("")) {
				buf.append(label);
			} else {
				buf.append(path[i].getFactory().getDisplayName());
				buf.append(path[i].getLocation());
			}
			buf.append(".");
		}
		buf.append(shortDescriptor);
		String newLong = buf.toString();
		if (!newLong.equals(longDescriptor)) {
			changed = true;
			longDescriptor = newLong;
		}

		return changed;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		SelectionItem item = (SelectionItem) obj;
		return this.longDescriptor.equals(item.longDescriptor);
	}

	public Value fetchValue(CircuitState root) {
		CircuitState cur = root;
		for (int i = 0; i < path.length; i++) {
			SubcircuitFactory circFact = (SubcircuitFactory) path[i]
					.getFactory();
			cur = circFact.getSubstate(cur, path[i]);
		}
		Loggable log = (Loggable) comp.getFeature(Loggable.class);
		return log == null ? Value.NIL : log.getLogValue(cur, option);
	}

	public Component getComponent() {
		return comp;
	}

	public Object getOption() {
		return option;
	}

	public Component[] getPath() {
		return path;
	}

	public int getRadix() {
		return radix;
	}

	public void setRadix(int value) {
		radix = value;
		model.fireSelectionChanged(new ModelEvent());
	}

	public String toShortString() {
		return shortDescriptor;
	}

	@Override
	public String toString() {
		return longDescriptor;
	}
}
