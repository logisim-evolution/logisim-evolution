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

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ColorIcon;
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FPGAOptions extends OptionsPanel {

  private class MyListener implements ActionListener, PreferenceChangeListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      Object source = ae.getSource();
      if (source == WorkSpaceButton) {
        selectWorkSpace(frame);
      }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent pce) {
      String property = pce.getKey();
      if (property.equals(AppPreferences.FPGA_Workspace.getIdentifier())) {
        WorkSpacePath.setText(AppPreferences.FPGA_Workspace.get());
      }
    }
  }

  private class MyColorListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() instanceof JButton) {
        JButton but = (JButton) e.getSource();
        if (but.getIcon() instanceof ColorIcon) {
          ColorIcon i = (ColorIcon) but.getIcon();
          i.update(frame);
        }
      }
    }
  }
  private MyListener myListener = new MyListener();
  private MyColorListener mcol = new MyColorListener();
  private JLabel WorkspaceLabel = new JLabel();
  private JTextField WorkSpacePath;
  private JButton WorkSpaceButton;
  private JLabel EditSelectLabel = new JLabel();
  private JButton EditSelectColor = new JButton();
  private JLabel EditHighligtLabel = new JLabel();
  private JButton EditHighligtColor = new JButton();
  private JLabel EditMoveLabel = new JLabel();
  private JButton EditMoveColor = new JButton();
  private JLabel EditResizeLabel = new JLabel();
  private JButton EditResizeColor = new JButton();
  private JPanel editPan;
  private PreferencesFrame frame;
  private PrefOptionList HDL_Used;

  public FPGAOptions(PreferencesFrame frame) {
    super(frame);
    this.frame = frame;
    AppPreferences.getPrefs().addPreferenceChangeListener(myListener);

    WorkSpacePath = new JTextField(32);
    WorkSpacePath.setText(AppPreferences.FPGA_Workspace.get());
    WorkSpacePath.setEditable(false);
    WorkSpaceButton = new JButton();
    WorkSpaceButton.addActionListener(myListener);
    HDL_Used =
        new PrefOptionList(
            AppPreferences.HDL_Type,
            S.getter("HDLLanguageUsed"),
            new PrefOption[] {
              new PrefOption(HDLGeneratorFactory.VHDL, S.getter("VHDL")),
              new PrefOption(HDLGeneratorFactory.VERILOG, S.getter("Verilog"))
            });

    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    setLayout(layout);

    c.insets = new Insets(2, 4, 4, 2);
    c.anchor = GridBagConstraints.BASELINE_LEADING;

    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.NONE;
    add(WorkspaceLabel, c);
    c.gridx = 2;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(WorkSpaceButton, c);
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 1.0;
    add(WorkSpacePath, c);
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    add(HDL_Used.getJLabel(), c);
    c.gridx = 2;
    c.gridy = 1;
    c.gridwidth = 1;
    add(HDL_Used.getJComboBox(), c);
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 3;
    add(AppPreferences.Boards.AddRemovePanel(), c);
    c.gridy = 4;
    add(getEditCols(),c);
    localeChanged();
  }
  
  private JPanel getEditCols() {
    editPan = new JPanel();
    editPan.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    editPan.add(EditSelectLabel,c);
    c.gridx++;
    EditSelectColor.addActionListener(mcol);
    EditSelectColor.setIcon(new ColorIcon(AppPreferences.FPGA_DEFINE_COLOR));
    editPan.add(EditSelectColor,c);
    c.gridx++;
    editPan.add(EditHighligtLabel,c);
    EditHighligtColor.addActionListener(mcol);
    EditHighligtColor.setIcon(new ColorIcon(AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR));
    c.gridx++;
    editPan.add(EditHighligtColor,c);
    c.gridy++;
    c.gridx=0;
    editPan.add(EditMoveLabel,c);
    EditMoveColor.addActionListener(mcol);
    EditMoveColor.setIcon(new ColorIcon(AppPreferences.FPGA_DEFINE_MOVE_COLOR));
    c.gridx++;
    editPan.add(EditMoveColor,c);
    c.gridx++;
    editPan.add(EditResizeLabel,c);
    EditResizeColor.addActionListener(mcol);
    EditResizeColor.setIcon(new ColorIcon(AppPreferences.FPGA_DEFINE_RESIZE_COLOR));
    c.gridx++;
    editPan.add(EditResizeColor,c);
    return editPan;
  }
  
  @Override
  public String getHelpText() {
    return S.get("FPGAHelp");
  }

  @Override
  public String getTitle() {
    return S.get("FPGATitle");
  }

  @Override
  public void localeChanged() {
    WorkspaceLabel.setText(S.get("FPGAWorkSpace"));
    WorkSpaceButton.setText(S.get("Browse"));
    EditSelectLabel.setText(S.get("EditColSel"));
    EditHighligtLabel.setText(S.get("EditColHighlight"));
    EditMoveLabel.setText(S.get("EditColMove"));
    EditResizeLabel.setText(S.get("EditColResize"));
    editPan.setBorder(BorderFactory.createTitledBorder(S.get("EditColors")));
  }
  
  private void selectWorkSpace(Component parentComponent) {
    JFileChooser fc = new JFileChooser(AppPreferences.FPGA_Workspace.get());
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    File test = new File(AppPreferences.FPGA_Workspace.get());
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle(S.get("FpgaGuiWorkspacePath"));
    boolean ValidWorkpath = false;
    while (!ValidWorkpath) {
      int retval = fc.showOpenDialog(null);
      if (retval != JFileChooser.APPROVE_OPTION) return;
      if (fc.getSelectedFile().getAbsolutePath().contains(" ")) {
        OptionPane.showMessageDialog(
            parentComponent,
            S.get("FpgaGuiWorkspaceError"),
            S.get("FpgaGuiWorkspacePath"),
            OptionPane.ERROR_MESSAGE);
      } else {
        ValidWorkpath = true;
      }
    }
    File file = fc.getSelectedFile();
    if (file.getPath().endsWith(File.separator)) {
      AppPreferences.FPGA_Workspace.set(file.getPath());
    } else {
      AppPreferences.FPGA_Workspace.set(file.getPath() + File.separator);
    }
  }

}
