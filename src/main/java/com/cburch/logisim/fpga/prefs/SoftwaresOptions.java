/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.prefs;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.FPGACommander;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.prefs.OptionsPanel;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Softwares;
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

public class SoftwaresOptions extends OptionsPanel {

  private class MyListener implements ActionListener, PreferenceChangeListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      Object source = ae.getSource();

      if (source == questaPathButton) {
        Softwares.setQuestaPath(getPreferencesFrame());
      } else if (source == AutoUpdateCheckbox) {
        AppPreferences.AutomaticUpdateCheck.setBoolean(AutoUpdateCheckbox.isSelected());
        ;
      } else if (source == questaValidationCheckBox) {
        AppPreferences.QUESTA_VALIDATION.setBoolean(questaValidationCheckBox.isSelected());
      } else if (source == QuartusPathButton) {
        FPGACommander.selectToolPath(VendorSoftware.VendorAltera);
      } else if (source == ISEPathButton) {
        FPGACommander.selectToolPath(VendorSoftware.VendorXilinx);
      } else if (source == VivadoPathButton) {
        FPGACommander.selectToolPath(VendorSoftware.VendorVivado);
      }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent pce) {
      String property = pce.getKey();

      if (property.equals(AppPreferences.QUESTA_PATH.getIdentifier())) {
        questaPathField.setText(AppPreferences.QUESTA_PATH.get());
      } else if (property.equals(AppPreferences.AutomaticUpdateCheck.getIdentifier())) {
        AutoUpdateCheckbox.setSelected(AppPreferences.AutomaticUpdateCheck.getBoolean());
      } else if (property.equals(AppPreferences.QUESTA_VALIDATION.getIdentifier())) {
        questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION.getBoolean());
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

  private JCheckBox AutoUpdateCheckbox = new JCheckBox();
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
    AutoUpdateCheckbox.addActionListener(myListener);
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
    add(AutoUpdateCheckbox, c);

    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), c);

    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.NONE;
    add(questaValidationCheckBox, c);

    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(sep, c);

    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.NONE;
    add(questaPathLabel, c);

    c.gridx = 0;
    c.gridy = 5;
    c.gridwidth = 2;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(questaPathField, c);

    c.gridx = 2;
    c.gridy = 5;
    c.fill = GridBagConstraints.NONE;
    add(questaPathButton, c);

    c.gridx = 0;
    c.gridy = 6;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), c);

    c.gridx = 0;
    c.gridy = 7;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(QuartusPathLabel, c);
    c.gridx = 0;
    c.gridy = 8;
    c.gridwidth = 2;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(QuartusPathField, c);
    c.gridx = 2;
    c.gridy = 8;
    c.fill = GridBagConstraints.NONE;
    add(QuartusPathButton, c);

    c.gridx = 0;
    c.gridy = 9;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), c);

    c.gridx = 0;
    c.gridy = 10;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(ISEPathLabel, c);
    c.gridx = 0;
    c.gridy = 11;
    c.gridwidth = 2;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(ISEPathField, c);
    c.gridx = 2;
    c.gridy = 11;
    c.fill = GridBagConstraints.NONE;
    add(ISEPathButton, c);

    c.gridx = 0;
    c.gridy = 12;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), c);

    c.gridx = 0;
    c.gridy = 13;
    c.gridwidth = 4;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(VivadoPathLabel, c);
    c.gridx = 0;
    c.gridy = 14;
    c.gridwidth = 2;
    c.weightx = 1.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(VivadoPathField, c);
    c.gridx = 2;
    c.gridy = 14;
    c.fill = GridBagConstraints.NONE;
    add(VivadoPathButton, c);

    AutoUpdateCheckbox.setSelected(AppPreferences.AutomaticUpdateCheck.getBoolean());

    questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION.getBoolean());

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
    return S.get("softwaresHelp");
  }

  @Override
  public String getTitle() {
    return S.get("softwaresTitle");
  }

  @Override
  public void localeChanged() {
    AutoUpdateCheckbox.setText(S.get("AutoUpdateLabel"));
    questaValidationCheckBox.setText(S.get("softwaresQuestaValidationLabel"));
    questaPathButton.setText(S.get("softwaresQuestaPathButton"));
    questaPathLabel.setText(S.get("softwaresQuestaPathLabel"));
    QuartusPathButton.setText(S.get("softwaresQuestaPathButton"));
    QuartusPathLabel.setText(S.get("QuartusToolPath"));
    ISEPathButton.setText(S.get("softwaresQuestaPathButton"));
    ISEPathLabel.setText(S.get("ISEToolPath"));
    VivadoPathButton.setText(S.get("softwaresQuestaPathButton"));
    VivadoPathLabel.setText(S.get("VivadoToolPath"));
  }
}
