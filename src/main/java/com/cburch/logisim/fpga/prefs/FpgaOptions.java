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

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.prefs.ColorChooserButton;
import com.cburch.logisim.gui.prefs.OptionsPanel;
import com.cburch.logisim.gui.prefs.PrefOption;
import com.cburch.logisim.gui.prefs.PrefOptionList;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorBoolean;
import java.awt.Component;
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
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FpgaOptions extends OptionsPanel {

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
      } else if (property.equals(AppPreferences.HdlType.getIdentifier())) {
        final var isVhdl = AppPreferences.HdlType.get().equals(HdlGeneratorFactory.VHDL);
        vhdlPan.setEnabled(isVhdl);
        vhdlKeywordUpperCase.setEnabled(isVhdl);
      }
    }
  }

  private final MyListener myListener = new MyListener();
  private final JLabel WorkspaceLabel = new JLabel();
  private final JTextField WorkSpacePath;
  private final JButton WorkSpaceButton;
  private final JLabel EditSelectLabel = new JLabel();
  private ColorChooserButton EditSelectColor;
  private final JLabel EditHighligtLabel = new JLabel();
  private ColorChooserButton EditHighligtColor;
  private final JLabel EditMoveLabel = new JLabel();
  private ColorChooserButton EditMoveColor;
  private final JLabel EditResizeLabel = new JLabel();
  private ColorChooserButton EditResizeColor;
  private final JLabel MappedLabel = new JLabel();
  private ColorChooserButton MappedColor;
  private final JLabel SelMapLabel = new JLabel();
  private ColorChooserButton SelMapColor;
  private final JLabel SelectMapLabel = new JLabel();
  private ColorChooserButton SelectMapColor;
  private final JLabel SelectLabel = new JLabel();
  private ColorChooserButton SelectColor;
  private JPanel editPan;
  private JPanel mapPan;
  private JPanel ReportPan;
  private JPanel vhdlPan;
  private JCheckBox SupressGated;
  private JCheckBox SupressOpen;
  private JCheckBox vhdlKeywordUpperCase;
  private final PreferencesFrame frame;
  private final PrefOptionList HDL_Used;

  public FpgaOptions(PreferencesFrame frame) {
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
            AppPreferences.HdlType,
            S.getter("HDLLanguageUsed"),
            new PrefOption[] {
              new PrefOption(HdlGeneratorFactory.VHDL, S.getter("VHDL")),
              new PrefOption(HdlGeneratorFactory.VERILOG, S.getter("Verilog"))
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
    add(getVhdlOptions(), c);
    c.gridy++;
    add(AppPreferences.Boards.addRemovePanel(), c);
    c.gridy++;
    add(getReporterOptions(), c);
    c.gridy++;
    add(getEditCols(), c);
    c.gridy++;
    add(getMapCols(), c);
    localeChanged();
  }
  
  private JPanel getVhdlOptions() {
    final var isVhdl = AppPreferences.HdlType.get().equals(HdlGeneratorFactory.VHDL);
    vhdlPan = new JPanel();
    vhdlPan.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    vhdlKeywordUpperCase = ((PrefMonitorBoolean) AppPreferences.VhdlKeywordsUpperCase).getCheckBox();
    vhdlPan.add(vhdlKeywordUpperCase, gbc);
    vhdlPan.setEnabled(isVhdl);
    vhdlKeywordUpperCase.setEnabled(isVhdl);
    return vhdlPan;
  }

  private JPanel getReporterOptions() {
    ReportPan = new JPanel();
    ReportPan.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    SupressGated = ((PrefMonitorBoolean) AppPreferences.SupressGatedClockWarnings).getCheckBox();
    ReportPan.add(SupressGated, c);
    c.gridy++;
    SupressOpen = ((PrefMonitorBoolean) AppPreferences.SupressOpenPinWarnings).getCheckBox();
    ReportPan.add(SupressOpen, c);
    return ReportPan;
  }

  private JPanel getEditCols() {
    editPan = new JPanel();
    editPan.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    editPan.add(EditSelectLabel, c);
    c.gridx++;
    EditSelectColor = new ColorChooserButton(frame, AppPreferences.FPGA_DEFINE_COLOR);
    editPan.add(EditSelectColor, c);
    c.gridx++;
    editPan.add(EditHighligtLabel, c);
    EditHighligtColor = new ColorChooserButton(frame, AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR);
    c.gridx++;
    editPan.add(EditHighligtColor, c);
    c.gridy++;
    c.gridx = 0;
    editPan.add(EditMoveLabel, c);
    EditMoveColor = new ColorChooserButton(frame, AppPreferences.FPGA_DEFINE_MOVE_COLOR);
    c.gridx++;
    editPan.add(EditMoveColor, c);
    c.gridx++;
    editPan.add(EditResizeLabel, c);
    EditResizeColor = new ColorChooserButton(frame, AppPreferences.FPGA_DEFINE_RESIZE_COLOR);
    c.gridx++;
    editPan.add(EditResizeColor, c);
    return editPan;
  }

  private JPanel getMapCols() {
    mapPan = new JPanel();
    mapPan.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    mapPan.add(MappedLabel);
    c.gridx++;
    MappedColor = new ColorChooserButton(frame, AppPreferences.FPGA_MAPPED_COLOR);
    mapPan.add(MappedColor);
    c.gridx++;
    mapPan.add(SelMapLabel);
    c.gridx++;
    SelMapColor = new ColorChooserButton(frame, AppPreferences.FPGA_SELECTED_MAPPED_COLOR);
    mapPan.add(SelMapColor);
    c.gridx = 0;
    c.gridy++;
    mapPan.add(SelectMapLabel, c);
    c.gridx++;
    SelectMapColor = new ColorChooserButton(frame, AppPreferences.FPGA_SELECTABLE_MAPPED_COLOR);
    mapPan.add(SelectMapColor, c);
    c.gridx++;
    mapPan.add(SelectLabel, c);
    c.gridx++;
    SelectColor = new ColorChooserButton(frame, AppPreferences.FPGA_SELECT_COLOR);
    mapPan.add(SelectColor, c);
    return mapPan;
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
    MappedLabel.setText(S.get("MapColor"));
    SelMapLabel.setText(S.get("SelMapCol"));
    SelectMapLabel.setText(S.get("SelectMapCol"));
    SelectLabel.setText(S.get("SelectCol"));
    SupressGated.setText(S.get("SupressGatedClock"));
    SupressOpen.setText(S.get("SupressOpenInput"));
    vhdlKeywordUpperCase.setText(S.get("VhdlKeywordUpperCase"));
    editPan.setBorder(BorderFactory.createTitledBorder(S.get("EditColors")));
    mapPan.setBorder(BorderFactory.createTitledBorder(S.get("MapColors")));
    ReportPan.setBorder(BorderFactory.createTitledBorder(S.get("ReporterOptions")));
    vhdlPan.setBorder(BorderFactory.createTitledBorder(S.get("VhdlOptions")));
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
