/*
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

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LoaderException;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.Template;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

class TemplateOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final MyListener myListener = new MyListener();
  private final JRadioButton plain = new JRadioButton();
  private final JRadioButton empty = new JRadioButton();
  private final JRadioButton custom = new JRadioButton();
  private final JTextField templateField = new JTextField(40);
  private final JButton templateButton = new JButton();
  public TemplateOptions(PreferencesFrame window) {
    super(window);

    final var bgroup = new ButtonGroup();
    bgroup.add(plain);
    bgroup.add(empty);
    bgroup.add(custom);

    plain.addActionListener(myListener);
    empty.addActionListener(myListener);
    custom.addActionListener(myListener);
    templateField.setEditable(false);
    templateButton.addActionListener(myListener);
    myListener.computeEnabled();

    final var gridbag = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    setLayout(gridbag);
    gbc.weightx = 1.0;
    gbc.gridx = 0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    gbc.gridwidth = 3;
    gbc.anchor = GridBagConstraints.LINE_START;
    gridbag.setConstraints(plain, gbc);
    add(plain);
    gridbag.setConstraints(empty, gbc);
    add(empty);
    gridbag.setConstraints(custom, gbc);
    add(custom);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 1;
    gbc.gridy = 3;
    gbc.gridx = GridBagConstraints.RELATIVE;
    final var strut = new JPanel();
    strut.setMinimumSize(new Dimension(50, 1));
    strut.setPreferredSize(new Dimension(50, 1));
    gbc.weightx = 0.0;
    gridbag.setConstraints(strut, gbc);
    add(strut);
    gbc.weightx = 1.0;
    gridbag.setConstraints(templateField, gbc);
    add(templateField);
    gbc.weightx = 0.0;
    gridbag.setConstraints(templateButton, gbc);
    add(templateButton);

    AppPreferences.addPropertyChangeListener(AppPreferences.TEMPLATE_TYPE, myListener);
    AppPreferences.addPropertyChangeListener(AppPreferences.TEMPLATE_FILE, myListener);
    switch (AppPreferences.getTemplateType()) {
      case AppPreferences.TEMPLATE_PLAIN:
        plain.setSelected(true);
        break;
      case AppPreferences.TEMPLATE_EMPTY:
        empty.setSelected(true);
        break;
      case AppPreferences.TEMPLATE_CUSTOM:
        custom.setSelected(true);
        break;
    }
    myListener.setTemplateField(AppPreferences.getTemplateFile());
  }

  @Override
  public String getHelpText() {
    return S.get("templateHelp");
  }

  @Override
  public String getTitle() {
    return S.get("templateTitle");
  }

  @Override
  public void localeChanged() {
    plain.setText(S.get("templatePlainOption"));
    empty.setText(S.get("templateEmptyOption"));
    custom.setText(S.get("templateCustomOption"));
    templateButton.setText(S.get("templateSelectButton"));
  }

  private class MyListener implements ActionListener, PropertyChangeListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      if (src == templateButton) {
        final var chooser = JFileChoosers.create();
        chooser.setDialogTitle(S.get("selectDialogTitle"));
        chooser.setApproveButtonText(S.get("selectDialogButton"));
        int action = chooser.showOpenDialog(getPreferencesFrame());
        if (action == JFileChooser.APPROVE_OPTION) {
          final var file = chooser.getSelectedFile();
          FileInputStream reader = null;
          InputStream reader2 = null;
          try {
            final var loader = new Loader(getPreferencesFrame());
            reader = new FileInputStream(file);
            final var template = Template.create(reader);
            reader2 = template.createStream();
            LogisimFile.load(reader2, loader); // to see if OK
            AppPreferences.setTemplateFile(file, template);
            AppPreferences.setTemplateType(AppPreferences.TEMPLATE_CUSTOM);
          } catch (LoaderException ignored) {
          } catch (IOException ex) {
            OptionPane.showMessageDialog(
                getPreferencesFrame(),
                S.get("templateErrorMessage", ex.toString()),
                S.get("templateErrorTitle"),
                OptionPane.ERROR_MESSAGE);
          } finally {
            try {
              if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
            try {
              if (reader != null) reader2.close();
            } catch (IOException ignored) {
            }
          }
        }
      } else {
        var value = AppPreferences.TEMPLATE_UNKNOWN;
        if (plain.isSelected()) value = AppPreferences.TEMPLATE_PLAIN;
        else if (empty.isSelected()) value = AppPreferences.TEMPLATE_EMPTY;
        else if (custom.isSelected()) value = AppPreferences.TEMPLATE_CUSTOM;
        AppPreferences.setTemplateType(value);
      }
      computeEnabled();
    }

    private void computeEnabled() {
      custom.setEnabled(!templateField.getText().equals(""));
      templateField.setEnabled(custom.isSelected());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      final var prop = event.getPropertyName();
      if (prop.equals(AppPreferences.TEMPLATE_TYPE)) {
        final var value = AppPreferences.getTemplateType();
        plain.setSelected(value == AppPreferences.TEMPLATE_PLAIN);
        empty.setSelected(value == AppPreferences.TEMPLATE_EMPTY);
        custom.setSelected(value == AppPreferences.TEMPLATE_CUSTOM);
      } else if (prop.equals(AppPreferences.TEMPLATE_FILE)) {
        setTemplateField((File) event.getNewValue());
      }
    }

    private void setTemplateField(File f) {
      try {
        templateField.setText(f == null ? "" : f.getCanonicalPath());
      } catch (IOException e) {
        templateField.setText(f.getName());
      }
      computeEnabled();
    }
  }
}
