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

package com.cburch.logisim.fpga.fpgaboardeditor;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.LocaleListener;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

public class BoardDialog implements ActionListener, ComponentListener, LocaleListener {

  private static class XMLFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(XML_EXTENSION);
    }

    @Override
    public String getDescription() {
      return S.get("XMLFileFilter");
    }
  }

  private class ZoomChange implements ChangeListener {

    private BoardPanel parent;

    public ZoomChange(BoardPanel parent) {
      this.parent = parent;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      JSlider source = (JSlider) e.getSource();
      if (!source.getValueIsAdjusting()) {
        int value = (int) source.getValue();
        if (value > MaxZoom) {
          source.setValue(MaxZoom);
          value = MaxZoom;
        }
        parent.SetScale((float) value / (float) 100.0);
      }
    }
  }

  private JFrame panel;
  public LinkedList<BoardRectangle> defined_components = new LinkedList<BoardRectangle>();
  private String action_id;
  boolean abort;
  private BoardInformation TheBoard = new BoardInformation();
  private JTextField BoardNameInput;
  private JButton saveButton = new JButton();
  private JButton loadButton = new JButton();
  private JButton importButton = new JButton();
  private JButton cancelButton = new JButton();
  private JButton fpgaButton = new JButton();
  private JLabel LocText = new JLabel();
  private BoardPanel picturepanel;
  private ZoomSlider zoomslide;
  public static final String XML_EXTENSION = ".xml";
  public static final FileFilter XML_FILTER = new XMLFileFilter();
  private static final String CancelStr = "cancel";
  private static final String FPGAStr = "fpgainfo";
  private int DefaultStandard = 0;
  private int DefaultDriveStrength = 0;
  private int DefaultPullSelection = 0;

  private int DefaultActivity = 0;
  private int MaxZoom;
  public BoardRectangle highlight = null;

  public BoardDialog() {
    GridBagConstraints gbc = new GridBagConstraints();

    panel = new JFrame();
    panel.setResizable(false);
    panel.addComponentListener(this);
    panel.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    GridBagLayout thisLayout = new GridBagLayout();
    panel.setLayout(thisLayout);

    // Set an empty board picture
    picturepanel = new BoardPanel(this);
    picturepanel.addComponentListener(this);

    JPanel ButtonPanel = new JPanel();
    GridBagLayout ButtonLayout = new GridBagLayout();
    ButtonPanel.setLayout(ButtonLayout);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(LocText, gbc);

    BoardNameInput = new JTextField(22);
    BoardNameInput.setEnabled(false);
    gbc.gridx = 2;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(BoardNameInput, gbc);

    gbc.gridx = 4;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    cancelButton.setActionCommand(CancelStr);
    cancelButton.addActionListener(this);
    ButtonPanel.add(cancelButton, gbc);
    
    gbc.gridx = 1;
    gbc.gridy = 1;
    fpgaButton.setActionCommand(FPGAStr);
    fpgaButton.addActionListener(this);
    fpgaButton.setEnabled(false);
    ButtonPanel.add(fpgaButton,gbc);

    zoomslide = new ZoomSlider();
    zoomslide.addChangeListener(new ZoomChange(picturepanel));
    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ButtonPanel.add(zoomslide, gbc);

    gbc.gridx = 3;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    loadButton.setActionCommand("load");
    loadButton.addActionListener(this);
    loadButton.setEnabled(true);
    ButtonPanel.add(loadButton, gbc);
    
    gbc.gridx = 4;
    importButton.setActionCommand("internal");
    importButton.addActionListener(this);
    ButtonPanel.add(importButton, gbc);

    gbc.gridx = 3;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    saveButton.setActionCommand("save");
    saveButton.addActionListener(this);
    saveButton.setEnabled(false);
    ButtonPanel.add(saveButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(ButtonPanel, gbc);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(picturepanel, gbc);

    panel.setLocationRelativeTo(null);
    panel.setVisible(true);
    int ScreenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int ScreenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    int ImageWidth = picturepanel.getWidth();
    int ImageHeight = picturepanel.getHeight();
    int ImageXBorder = panel.getWidth() - ImageWidth;
    int ImageYBorder = panel.getHeight() - ImageHeight;
    ScreenWidth -= ImageXBorder;
    ScreenHeight -= (ImageYBorder + (ImageYBorder >> 1));
    int zoomX = (ScreenWidth * 100) / ImageWidth;
    int zoomY = (ScreenHeight * 100) / ImageHeight;
    MaxZoom = (zoomY > zoomX) ? zoomX : zoomY;
    localeChanged();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(CancelStr)) {
      this.clear();
    } else if (e.getActionCommand().equals("save")) {
      panel.setVisible(false);
      TheBoard.setBoardName(BoardNameInput.getText());
      String filename = getDirName("", S.get("FpgaBoardSaveDir"));
      filename += TheBoard.getBoardName() + ".xml";
      BoardWriterClass xmlwriter =
          new BoardWriterClass(
              TheBoard,
              picturepanel.getScaledImage(
                  picturepanel.getImageWidth(), picturepanel.getImageHeight()));
      xmlwriter.PrintXml(filename);
      this.clear();
    } else if (e.getActionCommand().equals("load")) {
      JFileChooser fc = new JFileChooser(S.get("FpgaBoardLoadFile"));
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fc.setFileFilter(XML_FILTER);
      fc.setAcceptAllFileFilterUsed(false);
      int retval = fc.showOpenDialog(null);
      if (retval == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        SetBoardName(file.getName());
        String FileName = file.getPath();
        BoardReaderClass reader = new BoardReaderClass(FileName);
        UpdateInfo(reader);
      }
    } else if (e.getActionCommand().equals(FPGAStr)) {
      getFpgaInformation();
      if ((TheBoard.GetNrOfDefinedComponents() > 0) && TheBoard.fpga.FpgaInfoPresent())
          saveButton.setEnabled(true);
    } else if (e.getActionCommand().equals("internal")) {
      String Board = getInternalBoardName();
      if (Board != null) {
        SetBoardName(Board);
        BoardReaderClass reader = new BoardReaderClass(AppPreferences.Boards.GetBoardFilePath(Board));
        UpdateInfo(reader);
      }
    }
  }
  
  private void UpdateInfo(BoardReaderClass reader) {
    defined_components.clear();
    TheBoard = reader.GetBoardInformation();
    picturepanel.SetImage(TheBoard.GetImage());
    for (FPGAIOInformationContainer comp : TheBoard.GetAllComponents())
      defined_components.add(comp.GetRectangle());
    if ((TheBoard.GetNrOfDefinedComponents() > 0) && TheBoard.fpga.FpgaInfoPresent())
      saveButton.setEnabled(true);
    picturepanel.repaint();
  }
  
  private String getInternalBoardName() {
    ArrayList<String> boards = AppPreferences.Boards.GetBoardNames();
    return (String)OptionPane.showInputDialog(panel,S.get("FpgaBoardSelect"),
        S.get("FpgaBoardLoadInternal"), OptionPane.PLAIN_MESSAGE, null,
        boards.toArray(),boards.get(0));
  }

  private String checkIfEndsWithSlash(String path) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    return (path);
  }

  public void clear() {
    if (panel.isVisible()) panel.setVisible(false);
    picturepanel.clear();
    defined_components.clear();
    TheBoard.clear();
    BoardNameInput.setText("");
    saveButton.setEnabled(false);
    fpgaButton.setEnabled(false);
    loadButton.setEnabled(true);
    importButton.setEnabled(true);
  }

  @Override
  public void componentHidden(ComponentEvent e) {}

  @Override
  public void componentMoved(ComponentEvent e) {}

  @Override
  public void componentResized(ComponentEvent e) {
    panel.pack();
  }

  @Override
  public void componentShown(ComponentEvent e) {}

  public int GetDefaultActivity() {
    return DefaultActivity;
  }

  public int GetDefaultDriveStrength() {
    return DefaultDriveStrength;
  }

  public int GetDefaultPullSelection() {
    return DefaultPullSelection;
  }

  public int GetDefaultStandard() {
    return DefaultStandard;
  }

  private String getDirName(String old, String window_name) {
    JFileChooser fc = new JFileChooser(old);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setDialogTitle(window_name);
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      old = checkIfEndsWithSlash(file.getPath());
    }
    return old;
  }
  
  private void getFpgaInformation() {
    final JDialog selWindow = new JDialog(panel, S.get("FpgaBoardFpgaProp"));
    /* here the action listener is defined */
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(CancelStr)) {
              abort = true;
            }
            selWindow.setVisible(false);
          }
        };
    GridBagConstraints c = new GridBagConstraints();
    /* Here the clock related settings are defined */    
    JPanel ClockPanel = new JPanel();
    ClockPanel.setLayout(new GridBagLayout());
    ClockPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardClkProp")));

    JLabel FreqText = new JLabel(S.get("FpgaBoardClkFreq"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    ClockPanel.add(FreqText, c);

    JPanel FreqPanel = new JPanel();
    GridBagLayout FreqLayout = new GridBagLayout();
    FreqPanel.setLayout(FreqLayout);

    JTextField FreqInput = new JTextField(10);
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    if (TheBoard.fpga.FpgaInfoPresent())
      FreqInput.setText(Integer.toString(getFrequencyValue(TheBoard.fpga.getClockFrequency())));
    FreqPanel.add(FreqInput, c);

    String[] freqStrs = {"Hz", "kHz", "MHz"};
    JComboBox<String> StandardInput = new JComboBox<>(freqStrs);
    StandardInput.setSelectedIndex(2);
    c.gridx = 1;
    if (TheBoard.fpga.FpgaInfoPresent())
      StandardInput.setSelectedIndex(getFrequencyIndex(TheBoard.fpga.getClockFrequency()));
    FreqPanel.add(StandardInput, c);

    ClockPanel.add(FreqPanel, c);

    JLabel LocText = new JLabel(S.get("FpgaBoardClkLoc"));
    c.gridy = 1;
    c.gridx = 0;
    ClockPanel.add(LocText, c);

    JTextField LocInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) LocInput.setText(TheBoard.fpga.getClockPinLocation());
    c.gridx = 1;
    ClockPanel.add(LocInput, c);

    JLabel PullText = new JLabel(S.get("FpgaBoardClkPul"));
    c.gridy = 2;
    c.gridx = 0;
    ClockPanel.add(PullText, c);

    JComboBox<String> PullInput = new JComboBox<>(PullBehaviors.Behavior_strings);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      PullInput.setSelectedIndex(TheBoard.fpga.getClockPull());
    } else PullInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(PullInput, c);

    JLabel StandardText = new JLabel(S.get("FpgaBoardClkStd"));
    c.gridy = 3;
    c.gridx = 0;
    ClockPanel.add(StandardText, c);

    JComboBox<String> StdInput = new JComboBox<>(IoStandards.Behavior_strings);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      StdInput.setSelectedIndex(TheBoard.fpga.getClockStandard());
    } else StdInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(StdInput, c);

    /* Here the FPGA related settings are defined */
    JPanel FPGAPanel = new JPanel();
    FPGAPanel.setLayout(new GridBagLayout());
    FPGAPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardFpgaProp")));

    JLabel VendorText = new JLabel(S.get("FpgaBoardFpgaVend"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    FPGAPanel.add(VendorText, c);

    JComboBox<String> VendorInput = new JComboBox<>(VendorSoftware.Vendors);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      VendorInput.setSelectedIndex(TheBoard.fpga.getVendor());
    } else VendorInput.setSelectedIndex(0);
    c.gridx = 1;
    FPGAPanel.add(VendorInput, c);

    JLabel FamilyText = new JLabel(S.get("FpgaBoardFpgaFam"));
    c.gridy = 1;
    c.gridx = 0;
    FPGAPanel.add(FamilyText, c);

    JTextField FamilyInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) FamilyInput.setText(TheBoard.fpga.getTechnology());
    c.gridx = 1;
    FPGAPanel.add(FamilyInput, c);

    JLabel PartText = new JLabel(S.get("FpgaBoardFpgaPart"));
    c.gridy = 2;
    c.gridx = 0;
    FPGAPanel.add(PartText, c);

    JTextField PartInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) PartInput.setText(TheBoard.fpga.getPart());
    c.gridx = 1;
    FPGAPanel.add(PartInput, c);

    JLabel BoxText = new JLabel(S.get("FpgaBoardFpgaPack"));
    c.gridy = 3;
    c.gridx = 0;
    FPGAPanel.add(BoxText, c);

    JTextField BoxInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) BoxInput.setText(TheBoard.fpga.getPackage());
    c.gridx = 1;
    FPGAPanel.add(BoxInput, c);

    JLabel SpeedText = new JLabel(S.get("FpgaBoardFpgaSG"));
    c.gridy = 4;
    c.gridx = 0;
    FPGAPanel.add(SpeedText, c);

    JTextField SpeedInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) SpeedInput.setText(TheBoard.fpga.getSpeedGrade());
    c.gridx = 1;
    FPGAPanel.add(SpeedInput, c);

    JLabel UnusedPinsText = new JLabel(S.get("FpgaBoardPinUnused"));
    c.gridy = 5;
    c.gridx = 0;
    FPGAPanel.add(UnusedPinsText, c);

    JComboBox<String> UnusedPinsInput = new JComboBox<>(PullBehaviors.Behavior_strings);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      UnusedPinsInput.setSelectedIndex(TheBoard.fpga.getUnusedPinsBehavior());
    } else UnusedPinsInput.setSelectedIndex(0);
    c.gridx = 1;
    FPGAPanel.add(UnusedPinsInput, c);

    /* JTAG related Settings */
    JPanel JTAGPanel = new JPanel();
    JTAGPanel.setLayout(new GridBagLayout());
    JTAGPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardJtagProp")));

    JLabel PosText = new JLabel(S.get("FpgaBoardJtagLoc"));
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    JTAGPanel.add(PosText, c);

    JTextField PosInput = new JTextField(5);
    PosInput.setText("1");
    if (TheBoard.fpga.FpgaInfoPresent())
      PosInput.setText(Integer.toString(TheBoard.fpga.getFpgaJTAGChainPosition()));
    c.gridx = 1;
    JTAGPanel.add(PosInput, c);

    JLabel FlashPosText = new JLabel(S.get("FpgaBoardFlashLoc"));
    c.gridy = 1;
    c.gridx = 0;
    JTAGPanel.add(FlashPosText, c);
    
    JTextField FlashPosInput = new JTextField(5);
    FlashPosInput.setText("2");
    if (TheBoard.fpga.FpgaInfoPresent())
      FlashPosInput.setText(Integer.toString(TheBoard.fpga.getFlashJTAGChainPosition()));
    c.gridx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    JTAGPanel.add(FlashPosInput, c);

    /* misc settings */
    JPanel MiscPanel = new JPanel();
    MiscPanel.setLayout(new GridBagLayout());
    MiscPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardMiscProp")));

    JLabel FlashName = new JLabel(S.get("FpgaBoardFlashType"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    MiscPanel.add(FlashName, c);
    
    JTextField FlashNameInput = new JTextField("");
    if (TheBoard.fpga.FpgaInfoPresent()) FlashNameInput.setText(TheBoard.fpga.getFlashName());
    c.gridx = 1;
    MiscPanel.add(FlashNameInput, c);

    JCheckBox UsbTmc = new JCheckBox(S.get("FpgaBoardUSBTMC"));
    UsbTmc.setSelected(false);
    if (TheBoard.fpga.FpgaInfoPresent()) UsbTmc.setSelected(TheBoard.fpga.USBTMCDownloadRequired());
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    MiscPanel.add(UsbTmc, c);

    GridBagLayout dialogLayout = new GridBagLayout();
    selWindow.setLayout(dialogLayout);
    abort = false;
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(ClockPanel, c);
    c.gridx = 1;
    selWindow.add(FPGAPanel, c);
    c.gridx = 0;
    c.gridy = 1;
    selWindow.add(JTAGPanel,c);
    c.gridx = 1;
    selWindow.add(MiscPanel, c);

    JButton CancelButton = new JButton(S.get("FpgaBoardCancel"));
    CancelButton.addActionListener(actionListener);
    CancelButton.setActionCommand(CancelStr);
    c.gridx = 0;
    c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(CancelButton, c);

    JButton SaveButton = new JButton(S.get("FpgaBoardDone"));
    SaveButton.addActionListener(actionListener);
    SaveButton.setActionCommand("save");
    c.gridx = 1;
    c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(SaveButton, c);

    selWindow.pack();
    selWindow.setModal(true);
    selWindow.setResizable(false);
    selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    selWindow.setAlwaysOnTop(true);
    selWindow.setLocationRelativeTo(panel);
    boolean save_settings = false;
    while ((!abort) && (!save_settings)) {
      selWindow.setVisible(true);
      if (!abort) {
        save_settings = true;
        switch ((int)getFrequency(FreqInput.getText(), StandardInput.getSelectedItem().toString())) {
          case -2:
            save_settings = false;
            showDialogNotification( selWindow, "Error", S.get("FpgaBoardFreqError"));
            break;
          case -1:
            save_settings = false;
            showDialogNotification(selWindow, "Error", S.get("FpgaBoardFracError"));
            break;
          case 0:
            save_settings = false;
            showDialogNotification(selWindow, "Error", S.get("FpgaBoardClkReq"));
            break;
          default:
            break;
        }
        if (save_settings && LocInput.getText().isEmpty()) {
          save_settings = false;
          showDialogNotification(selWindow, "Error", S.get("FpgaBoardClkPin"));
        }
        if (save_settings && FamilyInput.getText().isEmpty()) {
          save_settings = false;
          showDialogNotification(selWindow, "Error", S.get("FpgaBoardFpgaFamMis"));
        }
        if (save_settings && PartInput.getText().isEmpty()) {
          save_settings = false;
          showDialogNotification(selWindow, "Error", S.get("FpgaBoardFpgaPartMis"));
        }
        if (save_settings && BoxInput.getText().isEmpty()) {
          save_settings = false;
          showDialogNotification(selWindow, "Error", S.get("FpgaBoardFpgaPacMis"));
        }
        if (save_settings && SpeedInput.getText().isEmpty()) {
          save_settings = false;
          showDialogNotification(selWindow, "Error", S.get("FpgaBoardFpgaSpeedMis"));
        }
        if (save_settings) {
          TheBoard.fpga.Set(
              getFrequency(FreqInput.getText(), StandardInput.getSelectedItem().toString()),
              LocInput.getText(),
              PullInput.getSelectedItem().toString(),
              StdInput.getSelectedItem().toString(),
              FamilyInput.getText(),
              PartInput.getText(),
              BoxInput.getText(),
              SpeedInput.getText(),
              VendorInput.getSelectedItem().toString(),
              UnusedPinsInput.getSelectedItem().toString(),
              UsbTmc.isSelected(),
              PosInput.getText(),
              FlashNameInput.getText(),
              FlashPosInput.getText());
        }
      }
    }
    selWindow.dispose();
  }

  private int getFrequencyValue(long freq) {
    if ((freq % 1000) != 0) return (int) freq;
    if ((freq % 1000000) != 0) return (int) freq / 1000;
    return (int) freq / 1000000;
  }

  private int getFrequencyIndex(long freq) {
    if ((freq % 1000) != 0) return 0;
    if ((freq % 1000000) != 0) return 1;
    return 2;
  }

  private long getFrequency(String chars, String speed) {
    long result = 0;
    long multiplier = 1;
    boolean dec_mult = false;

    if (speed.equals("kHz")) multiplier = 1000;
    if (speed.equals("MHz")) multiplier = 1000000;
    for (int i = 0; i < chars.length(); i++) {
      if (chars.charAt(i) >= '0' && chars.charAt(i) <= '9') {
        result *= 10;
        result += (chars.charAt(i) - '0');
        if (dec_mult) {
          multiplier /= 10;
          if (multiplier == 0) return -1;
        }
      } else {
        if (chars.charAt(i) == '.') {
          dec_mult = true;
        } else {
          return -2;
        }
      }
    }
    result *= multiplier;

    return result;
  }

  public JFrame GetPanel() {
    return panel;
  }

  public boolean isActive() {
    return panel.isVisible();
  }
  
  public boolean isDefinedComponent(int xpos, int ypos) {
    for (BoardRectangle rect : defined_components)
      if (rect.PointInside(xpos, ypos)) {
    if (highlight== null || !highlight.equals(rect)) {
          highlight = rect;
          picturepanel.repaint();
    }
        return true;
      }
    if (highlight != null) {
      highlight = null;
      picturepanel.repaint();
    }
    return false;
  }
  
  public void EditDialog() {
    if (highlight == null) return;
    FPGAIOInformationContainer comp = TheBoard.GetComponent(highlight);
    if (!comp.IsKnownComponent()) return;
    try {
      FPGAIOInformationContainer edit = (FPGAIOInformationContainer) comp.clone();
      edit.edit(true,this);
      if (edit.IsKnownComponent()) TheBoard.ReplaceComponent(comp, edit);
      if (edit.isToBeDeleted()) {
    	TheBoard.deleteComponent(comp);
    	defined_components.remove(highlight);
    	highlight = null;
    	picturepanel.repaint();
      }
    } catch (CloneNotSupportedException e) {}
  }
  
  public void resizeRect(int deltax , int deltay) {
    if (highlight == null || (deltax == 0 && deltay == 0)) return;
    Rectangle update = new Rectangle(highlight.getXpos(),
                                     highlight.getYpos(),
                                     highlight.getWidth()+deltax,
                                     highlight.getHeight()+deltay);
    Boolean overlap = false;
    for (BoardRectangle test : defined_components)
      if (!test.equals(highlight)) overlap |= test.Overlap(update);
    if (overlap) return;
    if (update.getWidth() < 4) return;
    if (update.getHeight() < 4) return;
    if ((update.getX()+update.getWidth()) >= BoardPanel.image_width) return;
    if ((update.getY()+update.getHeight()) >= BoardPanel.image_height) return;
    highlight.updateRectangle(update);
  }
  
  public void moveRect(int deltax , int deltay) {
    if (highlight == null || (deltax == 0 && deltay == 0)) return;
    Rectangle update = new Rectangle(highlight.getXpos()+deltax,
                                     highlight.getYpos()+deltay,
                                     highlight.getWidth(),
                                     highlight.getHeight());
    Boolean overlap = false;
    for (BoardRectangle test : defined_components)
      if (!test.equals(highlight)) overlap |= test.Overlap(update);
    if (overlap) return;
    if (update.getX() < 0) return;
    if (update.getY() < 0) return;
    if ((update.getX()+update.getWidth()) >= BoardPanel.image_width) return;
    if ((update.getY()+update.getHeight()) >= BoardPanel.image_height) return;
    highlight.updateRectangle(update);
  }
  
  public boolean hasOverlap(BoardRectangle orig , BoardRectangle update) {
    boolean overlap = false;
    for (BoardRectangle test : defined_components)
        if (!test.equals(orig)) overlap |= test.Overlap(update);
    return overlap;
  }

  public void SelectDialog(BoardRectangle rect) {

    /*
     * Before doing anything we have to check that this region does not
     * overlap with an already defined region. If we detect an overlap we
     * abort the action.
     */
    Iterator<BoardRectangle> iter = defined_components.iterator();
    Boolean overlap = false;
    while (iter.hasNext()) {
      overlap |= iter.next().Overlap(rect);
    }
    if (overlap) {
      showDialogNotification(this, "Error", S.get("FpgaBoardOverlap"));
      return;
    }
    String res = ShowItemSelectWindow();
    if (res.equals(CancelStr)) return;
    FPGAIOInformationContainer comp =
        new FPGAIOInformationContainer(IOComponentTypes.valueOf(res), rect, this);
    if (comp.IsKnownComponent()) {
      TheBoard.AddComponent(comp);
      defined_components.add(rect);
      if ((TheBoard.GetNrOfDefinedComponents() > 0) && TheBoard.fpga.FpgaInfoPresent())
        saveButton.setEnabled(true);
    }
  }

  public void setActive() {
    this.clear();
    panel.setVisible(true);
  }

  public void SetBoardName(String name) {
    String comps = name.toUpperCase();
    comps = comps.replaceAll(".PNG", "");
    comps = comps.replaceAll(".XML", "");
    BoardNameInput.setEnabled(true);
    BoardNameInput.setText(comps);
    TheBoard.setBoardName(comps);
    loadButton.setEnabled(false);
    importButton.setEnabled(false);
    fpgaButton.setEnabled(true);
  }

  public void SetDefaultActivity(int value) {
    DefaultActivity = value;
  }

  public void SetDefaultDriveStrength(int value) {
    DefaultDriveStrength = value;
  }

  public void SetDefaultPullSelection(int value) {
    DefaultPullSelection = value;
  }

  public void SetDefaultStandard(int value) {
    DefaultStandard = value;
  }

  private void showDialogNotification(JDialog parent, String type, String string) {
    final JDialog dialog = new JDialog(parent, type);
    JLabel pic = new JLabel();
    if (type.equals("Warning")) {
      pic.setIcon(new WarningIcon());
    } else {
      pic.setIcon(new ErrorIcon());
    }
    GridBagLayout dialogLayout = new GridBagLayout();
    dialog.setLayout(dialogLayout);
    GridBagConstraints c = new GridBagConstraints();
    JLabel message = new JLabel(string);
    JButton close = new JButton(S.get("FpgaBoardClose"));
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dialog.dispose();
          }
        };
    close.addActionListener(actionListener);

    c.gridx = 0;
    c.gridy = 0;
    c.ipadx = 20;
    dialog.add(pic, c);

    c.gridx = 1;
    c.gridy = 0;
    dialog.add(message, c);

    c.gridx = 1;
    c.gridy = 1;
    dialog.add(close, c);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
  }

  public static void showDialogNotification(Object parrent, String type, String string) {
    final JFrame dialog = new JFrame(type);
    JLabel pic = new JLabel();
    if (type.equals("Warning")) {
      pic.setIcon(new WarningIcon());
    } else {
      pic.setIcon(new ErrorIcon());
    }
    GridBagLayout dialogLayout = new GridBagLayout();
    dialog.setLayout(dialogLayout);
    GridBagConstraints c = new GridBagConstraints();
    JLabel message = new JLabel(string);
    JButton close = new JButton(S.get("FpgaBoardClose"));
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            dialog.dispose();
          }
        };
    close.addActionListener(actionListener);

    c.gridx = 0;
    c.gridy = 0;
    c.ipadx = 20;
    dialog.add(pic, c);

    c.gridx = 1;
    c.gridy = 0;
    dialog.add(message, c);

    c.gridx = 1;
    c.gridy = 1;
    dialog.add(close, c);
    dialog.pack();
    dialog.setLocation(
        Toolkit.getDefaultToolkit().getScreenSize().width >> 2,
        Toolkit.getDefaultToolkit().getScreenSize().height >> 2);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
  }

  private String ShowItemSelectWindow() {
    action_id = CancelStr;
    final JDialog selWindow = new JDialog(panel, S.get("FpgaBoardIOResources"));
    /* here the action listener is defined */
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            action_id = e.getActionCommand();
            selWindow.setVisible(false);
          }
        };
    GridBagLayout dialogLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    selWindow.setLayout(dialogLayout);
    JButton button;
    for (String comp : FPGAIOInformationContainer.GetComponentTypes()) {
      button = new JButton(S.fmt("FpgaBoardDefine", comp));
      button.setActionCommand(comp);
      button.addActionListener(actionListener);
      c.gridy++;
      selWindow.add(button, c);
    }
    JButton cancel = new JButton(S.get("FpgaBoardCancel"));
    cancel.setActionCommand(CancelStr);
    cancel.addActionListener(actionListener);
    c.gridy++;
    selWindow.add(cancel, c);
    selWindow.pack();
    selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(), selWindow.getHeight()));
    selWindow.setModal(true);
    selWindow.setResizable(false);
    selWindow.setAlwaysOnTop(true);
    selWindow.setVisible(true);
    selWindow.dispose();
    return action_id;
  }

  @Override
  public void localeChanged() {
    panel.setTitle(S.get("FPGABoardEditor"));
    LocText.setText(S.get("FpgaBoardName"));
    cancelButton.setText(S.get("FpgaBoardCancel"));
    loadButton.setText(S.get("FpgaBoardLoadExternal"));
    importButton.setText(S.get("FpgaBoardLoadInternal"));
    saveButton.setText(S.get("FpgaBoardSave"));
    fpgaButton.setText(S.get("FpgaBoardFpgaParam"));
    panel.pack();
  }
}
