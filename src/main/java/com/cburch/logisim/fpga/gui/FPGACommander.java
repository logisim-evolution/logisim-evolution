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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.fpga.file.BoardReaderClass;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ProjectAddIcon;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.StringGetter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class FPGACommander
    implements ActionListener,
        LibraryListener,
        ProjectListener,
        SimulatorListener,
        CircuitListener,
        WindowListener,
        LocaleListener,
        PreferenceChangeListener {

  public static final int FONT_SIZE = 12;
  private JFrame panel;
  private JLabel textMainCircuit = new JLabel();
  private JLabel boardPic = new JLabel();
  private BoardIcon boardIcon = null;
  private JButton annotateButton = new JButton();
  private JButton validateButton = new JButton();
  private JComboBox<String> circuitsList = new JComboBox<>();
  private JComboBox<StringGetter> annotationList = new JComboBox<>();
  private JComboBox<StringGetter> actionCommands = new JComboBox<>();
  private JButton ToolPath = new JButton();
  private JButton Settings = new JButton();
  private JButton StopButton = new JButton();
  private JProgressBar Progress = new JProgressBar();
  private FPGAReportTabbedPane ReporterGui;
  private Download Downloader;
  public static final String StopRequested = "stop";
  private JPanel BoardSelectionPanel = new JPanel();
  private FPGAClockPanel FrequencyPanel;
  private Project MyProject;
  private FPGAReport MyReporter;
  private BoardInformation MyBoardInformation = null;

  
  @Override
  public void preferenceChange(PreferenceChangeEvent pce) {
    String property = pce.getKey();
    if (property.equals(AppPreferences.SelectedBoard.getIdentifier())) {
      MyBoardInformation =
          new BoardReaderClass(AppPreferences.Boards.GetSelectedBoardFileName())
              .GetBoardInformation();
      MyBoardInformation.setBoardName(AppPreferences.SelectedBoard.get());
      boardIcon = new BoardIcon(MyBoardInformation.GetImage());
      boardPic.setIcon(boardIcon);
      boardPic.repaint();
      FrequencyPanel.setFpgaClockFrequency(MyBoardInformation.fpga.getClockFrequency());;
      HandleHDLOnly();
    }
  }

  @Override
  public void libraryChanged(LibraryEvent event) {
    if (event.getAction() == LibraryEvent.ADD_TOOL
        || event.getAction() == LibraryEvent.REMOVE_TOOL) {
      RebuildCircuitSelection();
    }
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    if (event.getAction() == ProjectEvent.ACTION_SET_CURRENT) {
      Circuit circ = event.getCircuit();
      if (circ != null) SetCurrentSheet(circ.getName());
    } else if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
      RebuildCircuitSelection();
    }
  }

  @Override
  public void propagationCompleted(SimulatorEvent e) {}

  @Override
  public void simulatorStateChanged(SimulatorEvent e) {
  FrequencyPanel.setSelectedFrequency();
  }

  @Override
  public void tickCompleted(SimulatorEvent e) {}

  @Override
  public void circuitChanged(CircuitEvent event) {
    int act = event.getAction();

    if (act == CircuitEvent.ACTION_SET_NAME) {
      RebuildCircuitSelection();
    }
    ReporterGui.clearDRCTrace();
  }

  private void rebuildBoardSelectionPanel() {
    BoardSelectionPanel.removeAll();
    BoardSelectionPanel.setLayout(new GridBagLayout());
    BoardSelectionPanel.setBorder(BorderFactory.createTitledBorder(
    BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaGuiBoardSelect")));
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    MyBoardInformation =
        new BoardReaderClass(AppPreferences.Boards.GetSelectedBoardFileName())
            .GetBoardInformation();
    MyBoardInformation.setBoardName(AppPreferences.SelectedBoard.get());
    boardIcon = new BoardIcon(MyBoardInformation.GetImage());
    JComboBox<String> selector = AppPreferences.Boards.BoardSelector();
    selector.setPreferredSize(new Dimension(boardIcon.getIconWidth(),AppPreferences.getScaled(20)));
    BoardSelectionPanel.add(selector, c);
    c.gridy++;
    // set board image on panel creation
    boardPic.setIcon(boardIcon);
    BoardSelectionPanel.add(boardPic, c);
    if (MyBoardInformation!= null && !VendorSoftware.toolsPresent(
            MyBoardInformation.fpga.getVendor(),
            VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor()))) {
      /* add the select toolpath button */
      c.gridy++;
      BoardSelectionPanel.add(ToolPath,c);
    }
    FrequencyPanel.setFpgaClockFrequency(MyBoardInformation.fpga.getClockFrequency());
  }
  
  private void setBoardSelectionEnabled( boolean enabled ) {
    AppPreferences.Boards.BoardSelector().setEnabled(enabled);
    ToolPath.setEnabled(enabled);
  }
  
  private JPanel getProgressBar() {
    JPanel pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaGuiProgress")));
    Progress.setStringPainted(true);
    pan.add(Progress,BorderLayout.CENTER);
    StopButton.setEnabled(false);
    StopButton.setActionCommand(StopRequested);
    StopButton.addActionListener(this);
    ProjectAddIcon bi = new ProjectAddIcon(true);
    bi.setDeselect(true);
    StopButton.setIcon(bi);
    pan.add(StopButton,BorderLayout.EAST);
    return pan;
  }
  
  private JPanel getAnnotationWindow() {
    JPanel pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaGuiAnnotationMethod")));
    annotationList.addItem(S.getter("FpgaGuiRelabelAll"));
    annotationList.addItem(S.getter("FpgaGuiRelabelEmpty"));
    annotationList.setSelectedIndex(1);
    pan.add(annotationList, BorderLayout.NORTH);
    annotateButton.setActionCommand("annotate");
    annotateButton.addActionListener(this);
    pan.add(annotateButton, BorderLayout.CENTER);
    return pan;
  }
  
  private void setAnnotationWindowEnabled( boolean enabled ) {
    annotationList.setEnabled(enabled);
    annotateButton.setEnabled(enabled);
  }
  
  private JPanel getExecuteWindow() {
    JPanel pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createStrokeBorder(new BasicStroke(2)), S.get("FpgaGuiExecution")));
    JPanel pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    pan1.add(textMainCircuit, BorderLayout.WEST);
    circuitsList.setActionCommand("mainCircuit");
    RebuildCircuitSelection();
    MyProject.addProjectListener(this);
    MyProject.getLogisimFile().addLibraryListener(this);
    circuitsList.setActionCommand("Circuit");
    circuitsList.addActionListener(this);
    pan1.add(circuitsList, BorderLayout.CENTER);
    pan.add(pan1, BorderLayout.NORTH);
    validateButton.setActionCommand("Download");
    validateButton.addActionListener(this);
    pan.add(actionCommands,BorderLayout.CENTER);
    pan.add(validateButton,BorderLayout.SOUTH);
    return pan;
  }
  
  private void setExecuteWindowEnabled(boolean enabled) {
    circuitsList.setEnabled(enabled);
    textMainCircuit.setEnabled(enabled);
    actionCommands.setEnabled(enabled);
    validateButton.setEnabled(enabled);
  }

  public FPGACommander(Project Main) {
    MyProject = Main;
    FrequencyPanel = new FPGAClockPanel(Main);
    rebuildBoardSelectionPanel();
    ToolPath.setActionCommand("ToolPath");
    ToolPath.addActionListener(this);
    MyProject.getSimulator().addSimulatorListener(this);
    MyProject.getFrame().addWindowListener(this);

    panel = new JFrame();
    panel.setResizable(false);
    panel.setAlwaysOnTop(false);
    panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    panel.addWindowListener(this);

    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(new GridBagLayout());

    // select annotation level
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(FrequencyPanel, c);
    c.gridy++;
    panel.add(getAnnotationWindow(), c);
    
    // Here the action window is placed
    c.gridy++;
    panel.add(getExecuteWindow(),c);

    /* Read the selected board information to retrieve board picture */
    JPanel pan1 = new JPanel();
    pan1.setLayout(new BorderLayout());
    c.gridx = 1;
    c.gridy = 0;
    c.gridheight = 3;
    pan1.add(BoardSelectionPanel, BorderLayout.SOUTH);
    // Settings
    Settings.setActionCommand("Settings");
    Settings.addActionListener(this);
    pan1.add(Settings, BorderLayout.NORTH);
    panel.add(pan1, c);
    c.gridheight = 1;
    

    // Progress bar
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 2;
    panel.add(getProgressBar(), c);

    // FPGAReporter GUI
    ReporterGui = new FPGAReportTabbedPane(MyProject);
    c.gridy = 4;
    panel.add(ReporterGui, c);
    panel.setLocationRelativeTo(null);
    panel.setVisible(false);

    AppPreferences.getPrefs().addPreferenceChangeListener(this);
    MyReporter = new FPGAReport(this,Progress);
    localeChanged();
  }

  public FPGAReportTabbedPane getReporterGui() {
    return ReporterGui;
  }

  private void HandleHDLOnly() {
    rebuildBoardSelectionPanel();
    int sel = actionCommands.getItemCount() == 0 ? 1 : actionCommands.getSelectedIndex();
    int nrItems = 1;
    actionCommands.removeAllItems();
    actionCommands.addItem(S.getter("FpgaGuiHdlOnly"));
    ToolPath.setText(S.fmt("FpgaGuiToolpath",
          VendorSoftware.getVendorString(MyBoardInformation.fpga.getVendor())));
    if (MyBoardInformation!= null && VendorSoftware.toolsPresent(
        MyBoardInformation.fpga.getVendor(),
        VendorSoftware.GetToolPath(MyBoardInformation.fpga.getVendor()))) {
      actionCommands.addItem(S.getter("FpgaGuiSyntAndD"));
      nrItems++;
      actionCommands.addItem(S.getter("FpgaGuiDownload"));
      nrItems++;
      if (MyBoardInformation.fpga.isFlashDefined()) {
        actionCommands.addItem(S.getter("FpgaGuiWriteFlash"));
      }
    } 
    if (sel == 0 && nrItems > 1) sel = 1;
    if (sel < nrItems) actionCommands.setSelectedIndex(sel);
    else actionCommands.setSelectedIndex(0);
    panel.pack();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("annotate")) {
      Annotate(annotationList.getSelectedIndex() == 0);
    } else if (e.getActionCommand().equals("Settings")) {
      PreferencesFrame.showFPGAPreferences();
    } else if (e.getActionCommand().equals("ToolPath")) {
      selectToolPath(MyBoardInformation.fpga.getVendor());
      HandleHDLOnly();
    } else if (e.getActionCommand().equals(StopRequested)) {
      if (Downloader != null)
        Downloader.stop();
      ((ProjectAddIcon)StopButton.getIcon()).setDeselect(true);
      StopButton.setEnabled(false);
    } else if (e.getActionCommand().equals("Download")) {
      setExecuteWindowEnabled(false);
      setAnnotationWindowEnabled(false);
      setBoardSelectionEnabled(false);
      FrequencyPanel.setEnabled(false);
      ((ProjectAddIcon)StopButton.getIcon()).setDeselect(false);
      StopButton.setEnabled(true);
      ReporterGui.clearAllMessages();
      boolean writeFlash = actionCommands.getSelectedIndex() == 3;
      boolean HdlOnly = actionCommands.getSelectedIndex() == 0;
      boolean DownloadOnly = actionCommands.getSelectedIndex() >= 2;
      Downloader =
          new Download(
              MyProject,
              circuitsList.getSelectedItem().toString(),
              FrequencyPanel.GetTickfrequency(),
              MyReporter,
              MyBoardInformation,
              "",
              writeFlash,
              DownloadOnly,
              HdlOnly,
              Progress,
              panel);
      Downloader.AddListener(this);
      Downloader.DoDownload();
    } else if (e.getSource() instanceof Download) {
      setExecuteWindowEnabled(true);
      setAnnotationWindowEnabled(true);
      setBoardSelectionEnabled(true);
      FrequencyPanel.setEnabled(true);
      ((ProjectAddIcon)StopButton.getIcon()).setDeselect(true);
      StopButton.setEnabled(false);
      Progress.setString(S.get("FpgaGuiIdle"));
      Progress.setValue(0);
    }
  }

  private void Annotate(boolean ClearExistingLabels) {
    ReporterGui.clearAllMessages();
    String CircuitName = circuitsList.getSelectedItem().toString();
    Circuit root = MyProject.getLogisimFile().getCircuit(CircuitName);
    if (root != null) {
      if (ClearExistingLabels) {
        root.ClearAnnotationLevel();
      }
      root.Annotate(ClearExistingLabels, MyReporter, false);
      MyReporter.AddInfo(S.get("FpgaGuiAnnotationDone"));
      /* TODO: Dirty hack, see Circuit.java function Annotate for details */
      MyProject.repaintCanvas();
      MyProject.getLogisimFile().setDirty(true);
    }
  }

  private void RebuildCircuitSelection() {
    circuitsList.removeAllItems();
    localeChanged();
    int i = 0;
    for (Circuit thisone : MyProject.getLogisimFile().getCircuits()) {
      circuitsList.addItem(thisone.getName());
      thisone.removeCircuitListener(this);
      thisone.addCircuitListener(this);
      if (MyProject.getCurrentCircuit() != null
          && thisone.getName().equals(MyProject.getCurrentCircuit().getName())) {
        circuitsList.setSelectedIndex(i);
      }
      i++;
    }
  }

  public static void selectToolPath(char vendor) {
    String ToolPath = VendorSoftware.GetToolPath(vendor);
    if (ToolPath == null) return;
    JFileChooser fc = new JFileChooser(ToolPath);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    File test = new File(ToolPath);
    if (test.exists()) {
      fc.setSelectedFile(test);
    }
    fc.setDialogTitle(VendorSoftware.Vendors[vendor] + " " + S.get("FpgaGuiSoftwareSelect"));
    int retval;
    boolean ok = false;
    do {
      retval = fc.showOpenDialog(null);
      if (retval == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        ToolPath = file.getPath();
        if (!ToolPath.endsWith(File.separator)) {
          ToolPath += File.separator;
        }
        if (VendorSoftware.setToolPath(vendor, ToolPath)) {
          ok = true;
        } else {
          OptionPane.showMessageDialog(
              null,
              S.fmt("FpgaToolsNotFound", ToolPath),
              S.get("FpgaGuiSoftwareSelect"),
              OptionPane.ERROR_MESSAGE);
        }
      } else ok = true;
    } while (!ok);
  }

  private void SetCurrentSheet(String Name) {
    for (int i = 0; i < circuitsList.getItemCount(); i++) {
      if (circuitsList.getItemAt(i).equals(Name)) {
        circuitsList.setSelectedIndex(i);
        circuitsList.repaint();
        return;
      }
    }
  }

  public void ShowGui() {
    if (!panel.isVisible()) {
      panel.setVisible(true);
    } else {
      panel.setVisible(false);
      panel.setVisible(true);
    }
  }

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) {
    ReporterGui.CloseOpenWindows();
    if (e.getSource().equals(MyProject.getFrame())&&panel.isVisible()) {
      panel.setVisible(false);
    }
  }

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowActivated(WindowEvent e) {}

  @Override
  public void windowDeactivated(WindowEvent e) {}

  @Override
  public void localeChanged() {
    textMainCircuit.setText(S.get("FpgaGuiMainCircuit"));
    panel.setTitle(S.get("FpgaGuiTitle")+" " + MyProject.getLogisimFile().getName());
    annotationList.repaint();
    validateButton.setText(S.get("FpgaGuiExecute"));
    annotateButton.setText(S.get("FpgaGuiAnnotate"));
    Settings.setText(S.get("FpgaGuiSettings"));
    Progress.setString(S.get("FpgaGuiIdle"));
    HandleHDLOnly();
  }
}
