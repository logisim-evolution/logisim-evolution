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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardDialog;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardPanel;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardRectangle;
import com.cburch.logisim.fpga.fpgaboardeditor.DriveStrength;
import com.cburch.logisim.fpga.fpgaboardeditor.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.fpgaboardeditor.IoStandards;
import com.cburch.logisim.fpga.fpgaboardeditor.PinActivity;
import com.cburch.logisim.fpga.fpgaboardeditor.PullBehaviors;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import com.cburch.logisim.proj.Projects;
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

  private static void showDialogNotification(JDialog parent, String type, String string) {
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
    JButton close = new JButton("close");
    ActionListener actionListener =
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            // panel.setAlwaysOnTop(true);
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

  public static void GetSimpleInformationDialog(Boolean deleteButton, BoardDialog parent, FPGAIOInformationContainer info) {
    IOComponentTypes MyType = info.GetType();
    BoardRectangle MyRectangle = info.GetRectangle();
    MyType.setNbPins(info.getNrOfPins() == 0 ? IOComponentTypes.GetNrOfFPGAPins(MyType) : info.getNrOfPins());
    final JDialog selWindow = new JDialog(parent.GetPanel(), MyType + " " + S.get("FpgaIoProperties"));
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
    else StandardInput.setSelectedIndex(parent.GetDefaultStandard());
    c.gridx = 1;
    selWindow.add(StandardInput, c);

    if (IOComponentTypes.OutputComponentSet.contains(MyType)) {
      JLabel DriveText = new JLabel(S.get("FpgaIoStrength"));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(DriveText, c);
      if (info.defined()) DriveInput.setSelectedIndex(info.GetDrive());
      else DriveInput.setSelectedIndex(parent.GetDefaultDriveStrength());
      c.gridx = 1;
      selWindow.add(DriveInput, c);
    }

    if (IOComponentTypes.InputComponentSet.contains(MyType)) {
      JLabel PullText = new JLabel(S.get("FpgaIoPull"));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(PullText, c);
      if (info.defined()) PullInput.setSelectedIndex(info.GetPullBehavior());
      else PullInput.setSelectedIndex(parent.GetDefaultPullSelection());
      c.gridx = 1;
      selWindow.add(PullInput, c);
    }

    if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
      JLabel ActiveText = new JLabel(S.fmt("FpgaIoActivity", MyType));
      c.gridy++;
      c.gridx = 0;
      selWindow.add(ActiveText, c);
      if (info.defined()) ActiveInput.setSelectedIndex(info.GetActivityLevel());
      else ActiveInput.setSelectedIndex(parent.GetDefaultActivity());
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
    selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(), selWindow.getHeight()));
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
            showDialogNotification(selWindow, "Error", S.fmt("FpgaIoPinLoc", PinLabels.get(i)));
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
                  case 0 : showDialogNotification(selWindow, "Error", 
                               S.fmt("FpgaIoIntError", S.get("FpgaIoXpos"),rectLocations.get(i).getText()));
                           break;
                  case 1 : showDialogNotification(selWindow, "Error", 
                              S.fmt("FpgaIoIntError", S.get("FpgaIoYpos"),rectLocations.get(i).getText()));
                           break;
                  case 2 : showDialogNotification(selWindow, "Error", 
                              S.fmt("FpgaIoIntError", S.get("FpgaIoWidth"),rectLocations.get(i).getText()));
                            break;
                  default : showDialogNotification(selWindow, "Error", 
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
              if (parent.hasOverlap(MyRectangle, new BoardRectangle(update))) {
                showDialogNotification(selWindow, "Error", S.get("FpgaIoRectError"));
                continue;
              } else if (update.getX()+update.getWidth() >= BoardPanel.image_width) {
                showDialogNotification(selWindow, "Error", S.get("FpgaIoRectTWide"));
                continue;
              } else if (update.getY()+update.getHeight() >= BoardPanel.image_height) {
                showDialogNotification(selWindow, "Error", S.get("FpgaIoRectTHeigt"));
                continue;
              } else if (update.getWidth() < 2) {
                showDialogNotification(selWindow, "Error", S.get("FpgaIoRectWNLE"));
                continue;
              } else if (update.getHeight() < 2) {
                showDialogNotification(selWindow, "Error", S.get("FpgaIoRectHNLE"));
                continue;
              } else {
                MyRectangle.updateRectangle(update);
              }
            }
          }
          parent.SetDefaultStandard(StandardInput.getSelectedIndex());
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
            parent.SetDefaultDriveStrength(DriveInput.getSelectedIndex());
            info.setDrive(DriveStrength.getId(DriveInput.getSelectedItem().toString()));
          }
          if (IOComponentTypes.InputComponentSet.contains(MyType)) {
            parent.SetDefaultPullSelection(PullInput.getSelectedIndex());
            info.setPullBehavior(PullBehaviors.getId(PullInput.getSelectedItem().toString()));
          }
          if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
            parent.SetDefaultActivity(ActiveInput.getSelectedIndex());
            info.setActivityLevel(PinActivity.getId(ActiveInput.getSelectedItem().toString()));
          }
          abort = true;
        }
      }
    }
    selWindow.dispose();
  }
  
}
