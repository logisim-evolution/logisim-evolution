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

package com.cburch.logisim.analyze.gui;

import java.awt.event.ItemListener;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;

class OutputSelector {
	@SuppressWarnings("rawtypes")
	private class Model extends AbstractListModel implements ComboBoxModel,
			VariableListListener {
		private static final long serialVersionUID = 1L;
		private Object selected;

		public Object getElementAt(int index) {
			return source.get(index);
		}

		public Object getSelectedItem() {
			return selected;
		}

		public int getSize() {
			return source.size();
		}

		public void listChanged(VariableListEvent event) {
			int index;
			String variable;
			Object selection;
			switch (event.getType()) {
			case VariableListEvent.ALL_REPLACED:
				computePrototypeValue();
				fireContentsChanged(this, 0, getSize());
				if (source.isEmpty()) {
					select.setSelectedItem(null);
				} else {
					select.setSelectedItem(source.get(0));
				}
				break;
			case VariableListEvent.ADD:
				variable = event.getVariable();
				if (prototypeValue == null
						|| variable.length() > prototypeValue.length()) {
					computePrototypeValue();
				}

				index = source.indexOf(variable);
				fireIntervalAdded(this, index, index);
				if (select.getSelectedItem() == null) {
					select.setSelectedItem(variable);
				}
				break;
			case VariableListEvent.REMOVE:
				variable = event.getVariable();
				if (variable.equals(prototypeValue))
					computePrototypeValue();
				index = ((Integer) event.getData()).intValue();
				fireIntervalRemoved(this, index, index);
				selection = select.getSelectedItem();
				if (selection != null && selection.equals(variable)) {
					selection = source.isEmpty() ? null : source.get(0);
					select.setSelectedItem(selection);
				}
				break;
			case VariableListEvent.MOVE:
				fireContentsChanged(this, 0, getSize());
				break;
			case VariableListEvent.REPLACE:
				variable = event.getVariable();
				if (variable.equals(prototypeValue))
					computePrototypeValue();
				index = ((Integer) event.getData()).intValue();
				fireContentsChanged(this, index, index);
				selection = select.getSelectedItem();
				if (selection != null && selection.equals(variable)) {
					select.setSelectedItem(event.getSource().get(index));
				}
				break;
			}
		}

		public void setSelectedItem(Object value) {
			selected = value;
		}
	}

	private VariableList source;
	private JLabel label = new JLabel();
	@SuppressWarnings("rawtypes")
	private JComboBox select = new JComboBox<>();
	private String prototypeValue = null;

	@SuppressWarnings("unchecked")
	public OutputSelector(AnalyzerModel model) {
		this.source = model.getOutputs();

		Model listModel = new Model();
		select.setModel(listModel);
		source.addVariableListListener(listModel);
	}

	public void addItemListener(ItemListener l) {
		select.addItemListener(l);
	}

	@SuppressWarnings("unchecked")
	private void computePrototypeValue() {
		String newValue;
		if (source.isEmpty()) {
			newValue = "xx";
		} else {
			newValue = "xx";
			for (int i = 0, n = source.size(); i < n; i++) {
				String candidate = source.get(i);
				if (candidate.length() > newValue.length())
					newValue = candidate;
			}
		}
		if (prototypeValue == null
				|| newValue.length() != prototypeValue.length()) {
			prototypeValue = newValue;
			select.setPrototypeDisplayValue(prototypeValue + "xx");
			select.revalidate();
		}
	}

	public JPanel createPanel() {
		JPanel ret = new JPanel();
		ret.add(label);
		ret.add(select);
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public JComboBox getComboBox() {
		return select;
	}

	public JLabel getLabel() {
		return label;
	}

	public String getSelectedOutput() {
		String value = (String) select.getSelectedItem();
		if (value != null && !source.contains(value)) {
			if (source.isEmpty()) {
				value = null;
			} else {
				value = source.get(0);
			}
			select.setSelectedItem(value);
		}
		return value;
	}

	void localeChanged() {
		label.setText(Strings.get("outputSelectLabel"));
	}

	public void removeItemListener(ItemListener l) {
		select.removeItemListener(l);
	}
}
