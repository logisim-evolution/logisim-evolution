/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.prefs;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.FpgaCommander;
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
      final var source = ae.getSource();

      if (source == questaPathButton) {
        Softwares.setQuestaPath(getPreferencesFrame());
      } else if (source == questaValidationCheckBox) {
        AppPreferences.QUESTA_VALIDATION.setBoolean(questaValidationCheckBox.isSelected());
      } else if (source == quartusPathButton) {
        FpgaCommander.selectToolPath(VendorSoftware.VENDOR_ALTERA);
      } else if (source == isePathButton) {
        FpgaCommander.selectToolPath(VendorSoftware.VENDOR_XILINX);
      } else if (source == vivadoPathButton) {
        FpgaCommander.selectToolPath(VendorSoftware.VENDOR_VIVADO);
      } else if (source == openfpgaPathButton) {
        FpgaCommander.selectToolPath(VendorSoftware.VENDOR_OPENFPGA);
      }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent pce) {
      String property = pce.getKey();

      if (property.equals(AppPreferences.QUESTA_PATH.getIdentifier())) {
        questaPathField.setText(AppPreferences.QUESTA_PATH.get());
      } else if (property.equals(AppPreferences.QUESTA_VALIDATION.getIdentifier())) {
        questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION.getBoolean());
      } else if (property.equals(AppPreferences.QuartusToolPath.getIdentifier())) {
        quartusPathField.setText(AppPreferences.QuartusToolPath.get());
      } else if (property.equals(AppPreferences.ISEToolPath.getIdentifier())) {
        isePathField.setText(AppPreferences.ISEToolPath.get());
      } else if (property.equals(AppPreferences.VivadoToolPath.getIdentifier())) {
        vivadoPathField.setText(AppPreferences.VivadoToolPath.get());
      } else if (property.equals(AppPreferences.OpenFpgaToolPath.getIdentifier())) {
        openfpgaPathField.setText(AppPreferences.OpenFpgaToolPath.get());
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private final MyListener myListener = new MyListener();

  private final JCheckBox questaValidationCheckBox = new JCheckBox();
  private final JLabel questaPathLabel = new JLabel();
  private final JTextField questaPathField = new JTextField(40);
  private final JButton questaPathButton = new JButton();
  private final JLabel quartusPathLabel = new JLabel();
  private final JTextField quartusPathField = new JTextField(40);
  private final JButton quartusPathButton = new JButton();
  private final JLabel isePathLabel = new JLabel();
  private final JTextField isePathField = new JTextField(40);
  private final JButton isePathButton = new JButton();
  private final JLabel vivadoPathLabel = new JLabel();
  private final JTextField vivadoPathField = new JTextField(40);
  private final JButton vivadoPathButton = new JButton();
  private final JLabel openfpgaPathLabel = new JLabel();
  private final JTextField openfpgaPathField = new JTextField(40);
  private final JButton openfpgaPathButton = new JButton();

  public SoftwaresOptions(PreferencesFrame window) {
    super(window);

    questaValidationCheckBox.addActionListener(myListener);
    questaPathButton.addActionListener(myListener);
    quartusPathButton.addActionListener(myListener);
    isePathButton.addActionListener(myListener);
    vivadoPathButton.addActionListener(myListener);
    openfpgaPathButton.addActionListener(myListener);
    AppPreferences.getPrefs().addPreferenceChangeListener(myListener);

    final var sep = new JSeparator(JSeparator.HORIZONTAL);
    final var gbl = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gbl);

    gbc.insets = new Insets(2, 4, 4, 2);
    gbc.anchor = GridBagConstraints.BASELINE_LEADING;

    var gridY = 0;
    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.NONE;
    add(questaValidationCheckBox, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(sep, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.NONE;
    add(questaPathLabel, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(questaPathField, gbc);
    gbc.gridx = 2;
    gbc.gridy = gridY++;
    gbc.fill = GridBagConstraints.NONE;
    add(questaPathButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(quartusPathLabel, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(quartusPathField, gbc);
    gbc.gridx = 2;
    gbc.gridy = gridY++;
    gbc.fill = GridBagConstraints.NONE;
    add(quartusPathButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    add(isePathLabel, gbc);
    gbc.gridx = 0;
    gbc.gridy = gridY;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(isePathField, gbc);
    gbc.gridx = 2;
    gbc.gridy = gridY++;
    gbc.fill = GridBagConstraints.NONE;
    add(isePathButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(vivadoPathLabel, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(vivadoPathField, gbc);

    gbc.gridx = 2;
    gbc.gridy = gridY++;
    gbc.fill = GridBagConstraints.NONE;
    add(vivadoPathButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JSeparator(JSeparator.HORIZONTAL), gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY++;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(openfpgaPathLabel, gbc);

    gbc.gridx = 0;
    gbc.gridy = gridY;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(openfpgaPathField, gbc);

    gbc.gridx = 2;
    gbc.gridy = gridY++;
    gbc.fill = GridBagConstraints.NONE;
    add(openfpgaPathButton, gbc);

    questaValidationCheckBox.setSelected(AppPreferences.QUESTA_VALIDATION.getBoolean());

    quartusPathField.setText(AppPreferences.QuartusToolPath.get());
    quartusPathField.setEditable(false);
    isePathField.setText(AppPreferences.ISEToolPath.get());
    isePathField.setEditable(false);
    vivadoPathField.setText(AppPreferences.VivadoToolPath.get());
    vivadoPathField.setEditable(false);
    questaPathField.setText(AppPreferences.QUESTA_PATH.get());
    questaPathField.setEditable(false);
    openfpgaPathField.setText(AppPreferences.OpenFpgaToolPath.get());
    openfpgaPathField.setEditable(false);
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
    questaValidationCheckBox.setText(S.get("softwaresQuestaValidationLabel"));
    questaPathButton.setText(S.get("softwaresQuestaPathButton"));
    questaPathLabel.setText(S.get("softwaresQuestaPathLabel"));
    quartusPathButton.setText(S.get("softwaresQuestaPathButton"));
    quartusPathLabel.setText(S.get("QuartusToolPath"));
    isePathButton.setText(S.get("softwaresQuestaPathButton"));
    isePathLabel.setText(S.get("ISEToolPath"));
    vivadoPathButton.setText(S.get("softwaresQuestaPathButton"));
    vivadoPathLabel.setText(S.get("VivadoToolPath"));
    openfpgaPathButton.setText(S.get("softwaresQuestaPathButton"));
    openfpgaPathLabel.setText(S.get("openfpgaToolPath"));
  }
}
