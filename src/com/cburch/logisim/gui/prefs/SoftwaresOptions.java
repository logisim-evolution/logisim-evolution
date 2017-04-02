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

import com.bfh.logisim.fpgagui.FPGACommanderGui;
import com.bfh.logisim.settings.VendorSoftware;
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
			} else
			if (source == questaValidationCheckBox) {
				AppPreferences.QUESTA_VALIDATION
						.setBoolean(questaValidationCheckBox.isSelected());
			} else if (source == QuartusPathButton) {
				FPGACommanderGui.selectToolPath(VendorSoftware.VendorAltera);
			} else if (source == ISEPathButton) {
				FPGACommanderGui.selectToolPath(VendorSoftware.VendorXilinx);
			} else if (source == VivadoPathButton) {
				FPGACommanderGui.selectToolPath(VendorSoftware.VendorVivado);
			}
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent pce) {
			String property = pce.getKey();

			if (property.equals(AppPreferences.QUESTA_PATH.getIdentifier())) {
				questaPathField.setText(AppPreferences.QUESTA_PATH.get());
			} else
			if (property.equals(AppPreferences.QUESTA_VALIDATION
					.getIdentifier())) {
				questaValidationCheckBox
						.setSelected(AppPreferences.QUESTA_VALIDATION
								.getBoolean());
			} else if (property.equals(AppPreferences.QuartusToolPath.getIdentifier())) {
				QuartusPathField.setText(AppPreferences.QuartusToolPath.get());
			} else if (property.equals(AppPreferences.ISEToolPath.getIdentifier())) {
				ISEPathField.setText(AppPreferences.ISEToolPath.get());
			} else if (property.equals(AppPreferences.VivadoToolPath.getIdentifier())) {
				VivadoPathField.setText(AppPreferences.VivadoToolPath.get());
			}
		}

	}

	private static final long serialVersionUID = 1L;

	private MyListener myListener = new MyListener();

	private JCheckBox questaValidationCheckBox = new JCheckBox();
	private JLabel questaPathLabel = new JLabel();
	private JTextField questaPathField = new JTextField(40);
	private JButton questaPathButton = new JButton();
	private JLabel QuartusPathLabel = new JLabel();
	private JTextField QuartusPathField = new JTextField(40);
	private JButton QuartusPathButton = new JButton();
	private JLabel ISEPathLabel = new JLabel();
	private JTextField ISEPathField = new JTextField(40);
	private JButton ISEPathButton = new JButton();
	private JLabel VivadoPathLabel = new JLabel();
	private JTextField VivadoPathField = new JTextField(40);
	private JButton VivadoPathButton = new JButton();

	public SoftwaresOptions(PreferencesFrame window) {
		super(window);
        
		questaValidationCheckBox.addActionListener(myListener);
		questaPathButton.addActionListener(myListener);
		QuartusPathButton.addActionListener(myListener);
		ISEPathButton.addActionListener(myListener);
		VivadoPathButton.addActionListener(myListener);
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
		
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(new JSeparator(JSeparator.HORIZONTAL), c);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(QuartusPathLabel,c);
		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(QuartusPathField, c);
		c.gridx = 2;
		c.gridy = 6;
		c.fill = GridBagConstraints.NONE;
		add(QuartusPathButton, c);

		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(new JSeparator(JSeparator.HORIZONTAL), c);

		c.gridx = 0;
		c.gridy = 8;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(ISEPathLabel,c);
		c.gridx = 0;
		c.gridy = 9;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(ISEPathField, c);
		c.gridx = 2;
		c.gridy = 9;
		c.fill = GridBagConstraints.NONE;
		add(ISEPathButton, c);

		c.gridx = 0;
		c.gridy = 10;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(new JSeparator(JSeparator.HORIZONTAL), c);

		c.gridx = 0;
		c.gridy = 11;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(VivadoPathLabel,c);
		c.gridx = 0;
		c.gridy = 12;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(VivadoPathField, c);
		c.gridx = 2;
		c.gridy = 12;
		c.fill = GridBagConstraints.NONE;
		add(VivadoPathButton, c);


		questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION
				.getBoolean());

		QuartusPathField.setText(AppPreferences.QuartusToolPath.get());
		QuartusPathField.setEditable(false);
		ISEPathField.setText(AppPreferences.ISEToolPath.get());
		ISEPathField.setEditable(false);
		VivadoPathField.setText(AppPreferences.VivadoToolPath.get());
		VivadoPathField.setEditable(false);
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
		QuartusPathButton.setText(Strings.get("softwaresQuestaPathButton"));
		QuartusPathLabel.setText(Strings.get("QuartusToolPath"));
		ISEPathButton.setText(Strings.get("softwaresQuestaPathButton"));
		ISEPathLabel.setText(Strings.get("ISEToolPath"));
		VivadoPathButton.setText(Strings.get("softwaresQuestaPathButton"));
		VivadoPathLabel.setText(Strings.get("VivadoToolPath"));
	}

}
