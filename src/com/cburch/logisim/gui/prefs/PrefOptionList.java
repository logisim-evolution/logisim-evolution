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

package com.cburch.logisim.gui.prefs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.util.StringGetter;

class PrefOptionList implements ActionListener, PropertyChangeListener {
	private PrefMonitor<String> pref;
	private StringGetter labelStr;

	private JLabel label;
	private JComboBox<PrefOption> combo;

	public PrefOptionList(PrefMonitor<String> pref, StringGetter labelStr,
			PrefOption[] options) {
		this.pref = pref;
		this.labelStr = labelStr;

		label = new JLabel(labelStr.toString() + " ");
		combo = new JComboBox<>();
		for (PrefOption opt : options) {
			combo.addItem(opt);
		}

		combo.addActionListener(this);
		pref.addPropertyChangeListener(this);
		selectOption(pref.get());
	}

	public void actionPerformed(ActionEvent e) {
		PrefOption x = (PrefOption) combo.getSelectedItem();
		pref.set((String) x.getValue());
	}

	JPanel createJPanel() {
		JPanel ret = new JPanel();
		ret.add(label);
		ret.add(combo);
		return ret;
	}

	JComboBox<PrefOption> getJComboBox() {
		return combo;
	}

	JLabel getJLabel() {
		return label;
	}

	void localeChanged() {
		label.setText(labelStr.toString() + " ");
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (pref.isSource(event)) {
			selectOption(pref.get());
		}
	}

	private void selectOption(Object value) {
		for (int i = combo.getItemCount() - 1; i >= 0; i--) {
			PrefOption opt = (PrefOption) combo.getItemAt(i);
			if (opt.getValue().equals(value)) {
				combo.setSelectedItem(opt);
				return;
			}
		}
		combo.setSelectedItem(combo.getItemAt(0));
	}
}
