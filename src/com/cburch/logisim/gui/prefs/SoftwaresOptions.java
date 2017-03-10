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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Softwares;

public class SoftwaresOptions extends OptionsPanel {

	private class MyListener implements ActionListener,
			PreferenceChangeListener {

		@Override
		public void actionPerformed(ActionEvent ae) {
			Object source = ae.getSource();

			if (source == questaPathButton) {
				Softwares.setQuestaPath(getPreferencesFrame());
			}
			if (source == questaValidationCheckBox) {
				AppPreferences.QUESTA_VALIDATION
						.setBoolean(questaValidationCheckBox.isSelected());
			}
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent pce) {
			String property = pce.getKey();

			if (property.equals(AppPreferences.QUESTA_PATH.getIdentifier())) {
				questaPathField.setText(AppPreferences.QUESTA_PATH.get());
			}
			if (property.equals(AppPreferences.QUESTA_VALIDATION
					.getIdentifier())) {
				questaValidationCheckBox
						.setSelected(AppPreferences.QUESTA_VALIDATION
								.getBoolean());
			}
		}

	}

	private static final long serialVersionUID = 1L;

	private MyListener myListener = new MyListener();

	private JCheckBox questaValidationCheckBox = new JCheckBox();
	private JLabel questaPathLabel = new JLabel();
	private JTextField questaPathField = new JTextField(40);
	private JButton questaPathButton = new JButton();

	public SoftwaresOptions(PreferencesFrame window) {
		super(window);

		questaValidationCheckBox.addActionListener(myListener);
		questaPathButton.addActionListener(myListener);
		AppPreferences.getPrefs().addPreferenceChangeListener(myListener);

		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(layout);

		c.insets = new Insets(2, 4, 4, 2);
		c.anchor = GridBagConstraints.BASELINE_LEADING;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.NONE;
		add(questaValidationCheckBox, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(sep, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.NONE;
		add(questaPathLabel, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(questaPathField, c);

		c.gridx = 2;
		c.gridy = 3;
		c.fill = GridBagConstraints.NONE;
		add(questaPathButton, c);

		questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION
				.getBoolean());

		questaPathField.setText(AppPreferences.QUESTA_PATH.get());
		questaPathField.setEditable(false);
	}

	@Override
	public String getHelpText() {
		return Strings.get("softwaresHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("softwaresTitle");
	}

	@Override
	public void localeChanged() {
		questaValidationCheckBox.setText(Strings
				.get("softwaresQuestaValidationLabel"));
		questaPathButton.setText(Strings.get("softwaresQuestaPathButton"));
		questaPathLabel.setText(Strings.get("softwaresQuestaPathLabel"));
	}

}
