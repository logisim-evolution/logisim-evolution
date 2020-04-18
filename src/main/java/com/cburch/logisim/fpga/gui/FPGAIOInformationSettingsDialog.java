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

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.IOComponentsInformation;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.PinActivity;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;

public class FPGAIOInformationSettingsDialog {

  private static void buildPinTable(FPGAIOInformationContainer info, JPanel pinPanel, 
      ArrayList<JTextField> LocInputs, ArrayList<String> PinLabels) {
    PinLabels.clear();
    PinLabels.addAll(FPGAIOInformationContainer.getPinLabels(info));
    GridBagConstraints c = new GridBagConstraints();
    pinPanel.removeAll();
    LocInputs.clear();
    int offset = 0;
    int oldY = 0;
    int maxY = -1;
    for (int i = 0; i < info.GetType().getNbPins(); i++) {
      if (i % 16 == 0) {
        offset = (i / 16) * 2;
        c.gridy = oldY;
      }
      JLabel LocText = new JLabel(S.fmt("FpgaIoLocation", PinLabels.get(i)));
      c.gridx = 0 + offset;
      c.gridy++;
      pinPanel.add(LocText, c);
      JTextField txt = new JTextField(6);
      if (info.defined()) {
        txt.setText(info.getPinLocation(i));
      }
      LocInputs.add(txt);
      c.gridx = 1 + offset;
      pinPanel.add(LocInputs.get(i), c);
      maxY = c.gridy > maxY ? c.gridy : maxY;
    }
  }
  
  private static JPanel getRectPanel(ArrayList<JTextField> rectLocations) {
    JPanel rectPanel = new JPanel();
    rectPanel.setLayout(new GridBagLayout());
    rectPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoRecProp")));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridy = 0;
    c.gridx = 0;
    rectPanel.add(new JLabel(S.get("FpgaIoXpos")), c);
    c.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoYpos")), c);
    c.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoWidth")), c);
    c.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoHeight")), c);
    c.gridx = 1;
    for (c.gridy = 0; c.gridy < 4 ; c.gridy++)
      rectPanel.add(rectLocations.get(c.gridy), c);
    return rectPanel;
  }

  public static void GetSimpleInformationDialog(Boolean deleteButton, 
        IOComponentsInformation IOcomps, FPGAIOInformationContainer info) {
    IOComponentTypes MyType = info.GetType();
    BoardRectangle MyRectangle = info.GetRectangle();
    MyType.setNbPins(info.getNrOfPins() == 0 ? IOComponentTypes.GetNrOfFPGAPins(MyType) : info.getNrOfPins());
    final JDialog selWindow = new JDialog(IOcomps.getParrentFram(), MyType + " " + S.get("FpgaIoProperties"));
    JComboBox<String> DriveInput = new JComboBox<>(DriveStrength.Behavior_strings);
    JComboBox<String> PullInput = new JComboBox<>(PullBehaviors.Behavior_strings);
    JComboBox<String> ActiveInput = new JComboBox<>(PinActivity.Behavior_strings);
    JComboBox<Integer> size = new JComboBox<>();
    ArrayList<JTextField> LocInputs = new ArrayList<JTextField>();
    ArrayList<String> PinLabels = new ArrayList<String>();
    JPanel pinPanel = new JPanel();
    Boolean abort = false;
    ArrayList<JTextField> rectLocations = new ArrayList<JTextField>();
    ActionListener actionListener =
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("size")) {
              int nr = (int) size.getSelectedItem();
              MyType.setNbPins(nr);
              buildPinTable(info,pinPanel,LocInputs,PinLabels);
              selWindow.pack();
              return;
            } else  if (e.getActionCommand().equals("cancel")) {
              info.setType(IOComponentTypes.Unknown);
            } else if (e.getActionCommand().equals("delete")) {
              info.setToBeDeleted();
            }
            selWindow.setVisible(false);
            selWindow.dispose();
          }
        };
    selWindow.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = -1;
    if (MyRectangle != null) {
      JTextField tf = new JTextField(5);
      tf.setText(Integer.toString(MyRectangle.getXpos()));
      rectLocations.add(tf);
      tf = new JTextField(5);
      tf.setText(Integer.toString(MyRectangle.getYpos()));
      rectLocations.add(tf);
      tf = new JTextField(5);
      tf.setText(Integer.toString(MyRectangle.getWidth()));
      rectLocations.add(tf);
      tf = new JTextField(5);
      tf.setText(Integer.toString(MyRectangle.getHeight()));
      rectLocations.add(tf);
      c.fill = GridBagConstraints.NORTH;
      c.gridy++;
      c.gridwidth = 2;
      selWindow.add(getRectPanel(rectLocations),c);
      c.gridwidth = 1;
    }
    c.fill = GridBagConstraints.HORIZONTAL;
    if (MyType.equals(IOComponentTypes.DIPSwitch) || MyType.equals(IOComponentTypes.PortIO)) {
      int min = 1;
      int max = 1;
      String text = "null";
      switch (MyType) {
        case DIPSwitch:
          min = DipSwitch.MIN_SWITCH;
          max = DipSwitch.MAX_SWITCH;
          text = "switches";
          break;
        case PortIO:
          min = PortIO.MIN_IO;
          max = PortIO.MAX_IO;
          text = "pins";
          break;
        default:
          break;
      }
      for (int i = min; i <= max; i++) {
        size.addItem(i);
      }
      size.setSelectedItem(MyType.getNbPins());
      c.gridy++;
      c.gridx = 0;
      JLabel sizeText = new JLabel(S.fmt("FpgaIoNrElem", text));
      selWindow.add(sizeText,c);
      c.gridx = 1;
      selWindow.add(size,c);
      c.gridx = 0;
      size.addActionListener(actionListener);
      size.setActionCommand("size");
    }

    pinPanel.setLayout(new GridBagLayout());
    buildPinTable(info,pinPanel,LocInputs,PinLabels);

    c.gridy++;
    c.gridwidth = 2;
    selWindow.add(pinPanel,c);
    c.gridwidth = 1;

    JLabel LabText = new JLabel(S.get("FpgaIoLabel"));
    c.gridy++;
    c.gridx = 0;
    selWindow.add(LabText, c);
    JTextField LabelInput = new JTextField(6);
    LabelInput.setText(info.GetLabel());
    c.gridx = 1;
    selWindow.add(LabelInput, c);

    JLabel StandardText = new JLabel(S.get("FpgaIoStandard"));
    c.gridy++;
    c.gridx = 0;
    selWindow.add(StandardText, c);
    JComboBox<String> StandardInput = new JComboBox<>(IoStandards.Behavior_strings);
    if (info.defined()) StandardInput.setSelectedIndex(info.GetIOStandard());
    else StandardInput.setSelectedIndex(IOcomps.GetDefaultStandard());
    c.gridx = 1;
    selWindow.add(StandardInput, c);

    if (IOComponentTypes.OutputComponentSet.contains(MyType)) {
      JLabel DriveText = new JLabel(S.get("FpgaIoStrength"));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(DriveText, c);
      if (info.defined()) DriveInput.setSelectedIndex(info.GetDrive());
      else DriveInput.setSelectedIndex(IOcomps.GetDefaultDriveStrength());
      c.gridx = 1;
      selWindow.add(DriveInput, c);
    }

    if (IOComponentTypes.InputComponentSet.contains(MyType)) {
      JLabel PullText = new JLabel(S.get("FpgaIoPull"));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(PullText, c);
      if (info.defined()) PullInput.setSelectedIndex(info.GetPullBehavior());
      else PullInput.setSelectedIndex(IOcomps.GetDefaultPullSelection());
      c.gridx = 1;
      selWindow.add(PullInput, c);
    }

    if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
      JLabel ActiveText = new JLabel(S.fmt("FpgaIoActivity", MyType));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(ActiveText, c);
      if (info.defined()) ActiveInput.setSelectedIndex(info.GetActivityLevel());
      else ActiveInput.setSelectedIndex(IOcomps.GetDefaultActivity());
      c.gridx = 1;
      selWindow.add(ActiveInput, c);
    }
    if (deleteButton) {
      JButton delButton = new JButton();
      delButton.setActionCommand("delete");
      delButton.addActionListener(actionListener);
      delButton.setText(S.get("FpgaIoDelete"));
      c.gridwidth = 2;
      c.gridx = 0;
      c.gridy++;
      selWindow.add(delButton,c);
      c.gridwidth = 1;
    }
    JButton OkayButton = new JButton(S.get("FpgaBoardDone"));
    OkayButton.setActionCommand("done");
    OkayButton.addActionListener(actionListener);
    c.gridx = 0;
    c.gridy++;
    selWindow.add(OkayButton, c);

    JButton CancelButton = new JButton(S.get("FpgaBoardCancel"));
    CancelButton.setActionCommand("cancel");
    CancelButton.addActionListener(actionListener);
    c.gridx = 1;
    selWindow.add(CancelButton, c);
    selWindow.pack();
    selWindow.setLocationRelativeTo(IOcomps.getParrentFram());
    selWindow.setModal(true);
    selWindow.setResizable(false);
    selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    selWindow.setAlwaysOnTop(true);
    abort = false;
    while (!abort) {
      selWindow.setVisible(true);
      abort |= info.GetType().equals(IOComponentTypes.Unknown);
      if (!abort) {
        boolean correct = true;
        for (int i = 0; i < MyType.getNbPins(); i++) {
          if (LocInputs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.fmt("FpgaIoPinLoc", PinLabels.get(i)));
            continue;
          }
        }
        if (correct) {
          if (!rectLocations.isEmpty()) {
            int[] values = new int[4];
            for (int i = 0 ; i < 4 ; i++) {
              try {
                values[i] = Integer.parseUnsignedInt(rectLocations.get(i).getText());
              } catch (NumberFormatException e) {
                correct=false;
                switch (i) {
                  case 0 : DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", 
                               S.fmt("FpgaIoIntError", S.get("FpgaIoXpos"),rectLocations.get(i).getText()));
                           break;
                  case 1 : DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", 
                              S.fmt("FpgaIoIntError", S.get("FpgaIoYpos"),rectLocations.get(i).getText()));
                           break;
                  case 2 : DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", 
                              S.fmt("FpgaIoIntError", S.get("FpgaIoWidth"),rectLocations.get(i).getText()));
                            break;
                  default : DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", 
                              S.fmt("FpgaIoIntError", S.get("FpgaIoHeight"),rectLocations.get(i).getText()));
                            break;
                }
              }
            }
            if (!correct) continue;
            if (values[0] != MyRectangle.getXpos() ||
                values[1] != MyRectangle.getYpos() ||
                values[2] != MyRectangle.getWidth() ||
                values[3] != MyRectangle.getHeight()) {
              Rectangle update = new Rectangle(values[0],values[1],values[2],values[3]);
              if (IOcomps.hasOverlap(MyRectangle, new BoardRectangle(update))) {
              DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.get("FpgaIoRectError"));
                continue;
              } else if (update.getX()+update.getWidth() >= BoardManipulator.IMAGE_WIDTH) {
                DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.get("FpgaIoRectTWide"));
                continue;
              } else if (update.getY()+update.getHeight() >= BoardManipulator.IMAGE_HEIGHT) {
                DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.get("FpgaIoRectTHeigt"));
                continue;
              } else if (update.getWidth() < 2) {
                DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.get("FpgaIoRectWNLE"));
                continue;
              } else if (update.getHeight() < 2) {
                DialogNotification.showDialogNotification(IOcomps.getParrentFram(), "Error", S.get("FpgaIoRectHNLE"));
                continue;
              } else {
                MyRectangle.updateRectangle(update);
              }
            }
          }
          IOcomps.SetDefaultStandard(StandardInput.getSelectedIndex());
          int NrOfPins = MyType.getNbPins();
          info.setNrOfPins(NrOfPins);
          for (int i = 0; i < NrOfPins; i++) {
            info.setPinLocation(i, LocInputs.get(i).getText());
          }
          if (LabelInput.getText() != null && LabelInput.getText().length() != 0)
            info.setLabel(LabelInput.getText());
          else info.setLabel(null);
          info.setIOStandard(IoStandards.getId(StandardInput.getSelectedItem().toString()));
          if (IOComponentTypes.OutputComponentSet.contains(MyType)) {
            IOcomps.SetDefaultDriveStrength(DriveInput.getSelectedIndex());
            info.setDrive(DriveStrength.getId(DriveInput.getSelectedItem().toString()));
          }
          if (IOComponentTypes.InputComponentSet.contains(MyType)) {
            IOcomps.SetDefaultPullSelection(PullInput.getSelectedIndex());
            info.setPullBehavior(PullBehaviors.getId(PullInput.getSelectedItem().toString()));
          }
          if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
            IOcomps.SetDefaultActivity(ActiveInput.getSelectedIndex());
            info.setActivityLevel(PinActivity.getId(ActiveInput.getSelectedItem().toString()));
          }
          abort = true;
        }
      }
    }
    selWindow.dispose();
  }
  
  private static boolean abort;
  
  public static void getFpgaInformation(Frame panel, BoardInformation TheBoard) {
    final JDialog selWindow = new JDialog(panel, S.get("FpgaBoardFpgaProp"));
    /* here the action listener is defined */
    abort = false;
    ActionListener actionListener =
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("cancel")) {
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
    CancelButton.setActionCommand("cancel");
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
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFreqError"));
            break;
          case -1:
            save_settings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFracError"));
            break;
          case 0:
            save_settings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardClkReq"));
            break;
          default:
            break;
        }
        if (save_settings && LocInput.getText().isEmpty()) {
          save_settings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardClkPin"));
        }
        if (save_settings && FamilyInput.getText().isEmpty()) {
          save_settings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaFamMis"));
        }
        if (save_settings && PartInput.getText().isEmpty()) {
          save_settings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaPartMis"));
        }
        if (save_settings && BoxInput.getText().isEmpty()) {
          save_settings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaPacMis"));
        }
        if (save_settings && SpeedInput.getText().isEmpty()) {
          save_settings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaSpeedMis"));
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

  private static int getFrequencyValue(long freq) {
    if ((freq % 1000) != 0) return (int) freq;
    if ((freq % 1000000) != 0) return (int) freq / 1000;
    return (int) freq / 1000000;
  }

  private static int getFrequencyIndex(long freq) {
    if ((freq % 1000) != 0) return 0;
    if ((freq % 1000000) != 0) return 1;
    return 2;
  }

  private static long getFrequency(String chars, String speed) {
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

  
}
