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

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;

@SuppressWarnings("rawtypes")
class SelectionList extends JList {
	private class Model extends AbstractListModel implements ModelListener {
		private static final long serialVersionUID = 1L;

		public void entryAdded(ModelEvent event, Value[] values) {
		}

		public void filePropertyChanged(ModelEvent event) {
		}

		public Object getElementAt(int index) {
			return selection.get(index);
		}

		public int getSize() {
			return selection == null ? 0 : selection.size();
		}

		public void selectionChanged(ModelEvent event) {
			fireContentsChanged(this, 0, getSize());
		}
	}

	private class MyCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getListCellRendererComponent(JList list,
				Object value, int index, boolean isSelected, boolean hasFocus) {
			java.awt.Component ret = super.getListCellRendererComponent(list,
					value, index, isSelected, hasFocus);
			if (ret instanceof JLabel && value instanceof SelectionItem) {
				JLabel label = (JLabel) ret;
				SelectionItem item = (SelectionItem) value;
				Component comp = item.getComponent();
				label.setIcon(new ComponentIcon(comp));
				label.setText(item.toString() + " - " + item.getRadix());
			}
			return ret;
		}
	}

	private static final long serialVersionUID = 1L;

	private Selection selection;

	@SuppressWarnings("unchecked")
	public SelectionList() {
		selection = null;
		setModel(new Model());
		setCellRenderer(new MyCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public void localeChanged() {
		repaint();
	}

	public void setSelection(Selection value) {
		if (selection != value) {
			Model model = (Model) getModel();
			if (selection != null)
				selection.removeModelListener(model);
			selection = value;
			if (selection != null)
				selection.addModelListener(model);
			model.selectionChanged(null);
		}
	}
}
