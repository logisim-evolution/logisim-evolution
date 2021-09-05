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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.IOComponentsInformation;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.PinActivity;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.hdlgenerator.LedArrayGenericHDLGeneratorFactory;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class FPGAIOInformationSettingsDialog {

  private static void buildPinTable(
      int nr,
      IOComponentTypes type,
      JPanel pinPanel,
      ArrayList<JTextField> LocInputs,
      ArrayList<String> PinLabels,
      ArrayList<String> oldLocations) {
    GridBagConstraints c = new GridBagConstraints();
    pinPanel.removeAll();
    if (LocInputs.size() == 0) {
      for (var i = 0; i < nr; i++) {
        final var txt = new JTextField(6);
        if (i < oldLocations.size())
          txt.setText(oldLocations.get(i));
        LocInputs.add(txt);
      }
    }
    while (LocInputs.size() < nr) {
      final var txt = new JTextField(6);
      LocInputs.add(txt);
      final var idx = (LocInputs.indexOf(txt));
      if (idx < oldLocations.size()) txt.setText(oldLocations.get(idx));
    }
    while (LocInputs.size() > nr) LocInputs.remove(LocInputs.size() - 1);
    var offset = 0;
    var oldY = 0;
    var maxY = -1;
    for (var i = 0; i < nr; i++) {
      if (i % 16 == 0) {
        offset = (i / 16) * 2;
        c.gridy = oldY;
      }
      final var LocText = new JLabel(S.get("FpgaIoLocation", PinLabels.get(i)));
      c.gridx = 0 + offset;
      c.gridy++;
      pinPanel.add(LocText, c);
      c.gridx = 1 + offset;
      pinPanel.add(LocInputs.get(i), c);
      maxY = Math.max(c.gridy, maxY);
    }
  }

  private static void updateLedArrayRequirements(
      int nrOfRows,
      int nrOfColumns,
      char driveMode,
      JPanel pinPanel,
      ArrayList<JTextField> LocInputs,
      ArrayList<String> oldLocations,
      HashMap<Integer, Integer> NrOfPins) {
    var pinLabels = new ArrayList<String>();
    NrOfPins.clear();
    NrOfPins.put(INPUT_ID, 0);
    NrOfPins.put(IO_ID, 0);
    var nrOfPins = 0;
    switch (driveMode) {
      case LedArrayDriving.LED_DEFAULT: {
        nrOfPins = nrOfRows * nrOfColumns;
        for (var row = 0; row < nrOfRows; row++) {
          for (var col = 0; col < nrOfColumns; col++)
            pinLabels.add("Row_" + row + "_Col_" + col);
        }
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING: {
        final var nrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfRows);
        nrOfPins = nrBits + nrOfColumns;
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("RowAddress_" + i);
          } else {
            pinLabels.add("Col_" + (i - nrBits));
          }
        }
        break;
      }
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        final var nrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
        nrOfPins = nrBits + nrOfRows;
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("ColumnAddress_" + i);
          } else {
            pinLabels.add("Row_" + (i - nrBits));
          }
        }
        break;
      }
      case LedArrayDriving.RGB_DEFAULT: {
        nrOfPins = nrOfRows * nrOfColumns * 3;
        var preamble = "";
        for (var rgb = 0; rgb < 3; rgb++) {
          switch (rgb) {
            case 0: {
              preamble = "Red_";
              break;
            }
            case 1: {
              preamble = "Green_";
              break;
            }
            default: {
              preamble = "Blue_";
              break;
            }
          }
          for (var row = 0; row < nrOfRows; row++) {
            for (var col = 0; col < nrOfColumns; col++)
              pinLabels.add(preamble + "Row_" + row + "_Col_" + col);
          }
        }
        break;
      }
      case LedArrayDriving.RGB_ROW_SCANNING: {
        final var nrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfRows);
        nrOfPins = nrBits + 3 * nrOfColumns;
        var preamble = "";
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("RowAddress_" + i);
          } else {
            final var id = i - nrBits;
            final var rgb = id / nrOfColumns;
            final var col = id % nrOfColumns;
            switch (rgb) {
              case 0: {
                preamble = "Red_";
                break;
              }
              case 1: {
                preamble = "Green_";
                break;
              }
              default: {
                preamble = "Blue_";
                break;
              }
            }
            pinLabels.add(preamble + "Col_" + col);
          }
        }
        break;
      }
      case LedArrayDriving.RGB_COLUMN_SCANNING: {
        final var nrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
        nrOfPins = nrBits + 3 * nrOfRows;
        var preamble = "";
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("ColumnAddress_" + i);
          } else {
            final var id = i - nrBits;
            final var rgb = id / nrOfRows;
            final var col = id % nrOfRows;
            switch (rgb) {
              case 0: {
                preamble = "Red_";
                break;
              }
              case 1: {
                preamble = "Green_";
                break;
              }
              default: {
                preamble = "Blue_";
                break;
              }
            }
            pinLabels.add(preamble + "Row_" + col);
          }
        }
        break;
      }
      default: {
        nrOfPins = 0;
      }
    }
    NrOfPins.put(OUTPUT_ID, nrOfPins);
    buildPinTable(nrOfPins,
        IOComponentTypes.LEDArray,
        pinPanel,
        LocInputs,
        pinLabels,
        oldLocations);
  }

  private static JPanel getRectPanel(ArrayList<JTextField> rectLocations) {
    final var rectPanel = new JPanel();
    rectPanel.setLayout(new GridBagLayout());
    rectPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoRecProp")));
    var c = new GridBagConstraints();
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
    for (c.gridy = 0; c.gridy < 4; c.gridy++)
      rectPanel.add(rectLocations.get(c.gridy), c);
    return rectPanel;
  }


  private static final int INPUT_ID = 0;
  private static final int OUTPUT_ID = 1;
  private static final int IO_ID = 2;

  public static void GetSimpleInformationDialog(Boolean deleteButton,
        IOComponentsInformation IOcomps, FPGAIOInformationContainer info) {
    HashMap<Integer, Integer> NrOfPins = new HashMap<>();
    final var selWindow = new JDialog(IOcomps.getParentFrame(), info.GetType() + " " + S.get("FpgaIoProperties"));
    final var contents = new JPanel();
    final var DriveInput = new JComboBox<String>(DriveStrength.BEHAVIOR_STRINGS);
    final var PullInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    final var ActiveInput = new JComboBox<String>(PinActivity.BEHAVIOR_STRINGS);
    final var Inputsize = new JComboBox<Integer>();
    final var Outputsize = new JComboBox<Integer>();
    final var IOsize = new JComboBox<Integer>();
    final var RowSize = new JComboBox<Integer>();
    final var ColSize = new JComboBox<Integer>();
    final var Encoding = new JComboBox<String>();
    final var mapRotation = new JComboBox<String>();
    final var LocInputs = new ArrayList<JTextField>();
    final var LocOutputs = new ArrayList<JTextField>();
    final var LocIOs = new ArrayList<JTextField>();
    final var PinLabels = new ArrayList<String>();
    final var ArrayPanel = new JPanel();
    final var InputsPanel = new JPanel();
    final var OutputsPanel = new JPanel();
    final var IOPanel = new JPanel();
    var abort = false;
    final var rectLocations = new ArrayList<JTextField>();
    final var oldInputLocations = new ArrayList<String>();
    final var oldOutputLocations = new ArrayList<String>();
    final var oldIOLocations = new ArrayList<String>();
    final var myType = info.GetType();
    final var myRotation = info.getMapRotation();
    final var MyRectangle = info.GetRectangle();
    if (info.getNrOfPins() == 0) {
      NrOfPins.put(INPUT_ID, IOComponentTypes.GetFPGAInputRequirement(myType));
      NrOfPins.put(OUTPUT_ID, IOComponentTypes.GetFPGAOutputRequirement(myType));
      NrOfPins.put(IO_ID, IOComponentTypes.GetFPGAInOutRequirement(myType));
    } else {
      NrOfPins.put(INPUT_ID, info.getNrOfInputPins());
      NrOfPins.put(OUTPUT_ID, info.getNrOfOutputPins());
      NrOfPins.put(IO_ID, info.getNrOfIOPins());
    }
    for (var cnt = 0; cnt < info.getNrOfPins(); cnt++) {
      if (cnt < NrOfPins.get(INPUT_ID)) oldInputLocations.add(info.getPinLocation(cnt));
      else if (cnt < NrOfPins.get(INPUT_ID) + NrOfPins.get(OUTPUT_ID))
        oldOutputLocations.add(info.getPinLocation(cnt));
      else
        oldIOLocations.add(info.getPinLocation(cnt));
    }
    ActionListener actionListener =
        e -> {
          switch (e.getActionCommand()) {
            case "inputSize": {
              var nr = (int) Inputsize.getSelectedItem();
              NrOfPins.put(INPUT_ID, nr);
              PinLabels.clear();
              for (var i = 0; i < nr; i++)
                PinLabels.add(IOComponentTypes.getInputLabel(nr, i, myType));
              buildPinTable(nr, myType, InputsPanel, LocInputs, PinLabels, oldInputLocations);
              selWindow.pack();
              return;
            }
            case "outputSize": {
              var nr = (int) Outputsize.getSelectedItem();
              NrOfPins.put(OUTPUT_ID, nr);
              PinLabels.clear();
              for (var i = 0; i < nr; i++)
                PinLabels.add(IOComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
              buildPinTable(nr, myType, OutputsPanel, LocOutputs, PinLabels, oldOutputLocations);
              selWindow.pack();
              return;
            }
            case "ioSize": {
              var nr = (int) IOsize.getSelectedItem();
              NrOfPins.put(IO_ID, nr);
              PinLabels.clear();
              for (var i = 0; i < nr; i++)
                PinLabels.add(IOComponentTypes.getIOLabel(nr, i, myType));
              buildPinTable(nr, myType, IOPanel, LocIOs, PinLabels, oldIOLocations);
              selWindow.pack();
              return;
            }
            case "LedArray": {
              info.setNrOfRows(RowSize.getSelectedIndex() + 1);
              info.setNrOfColumns(ColSize.getSelectedIndex() + 1);
              info.setArrayDriveMode((char) Encoding.getSelectedIndex());
              updateLedArrayRequirements(info.getNrOfRows(),
                  info.getNrOfColumns(),
                  info.getArrayDriveMode(),
                  OutputsPanel,
                  LocOutputs,
                  oldOutputLocations,
                  NrOfPins);
              selWindow.pack();
              return;
            }
            case "cancel":
              info.setType(IOComponentTypes.Unknown);
              break;
            case "delete":
              info.setToBeDeleted();
              break;
          }
          selWindow.setVisible(false);
          selWindow.dispose();
        };
    contents.setLayout(new GridBagLayout());
    final var c = new GridBagConstraints();
    c.gridy = -1;
    if (MyRectangle != null) {
      var tf = new JTextField(5);
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
      contents.add(getRectPanel(rectLocations), c);
      c.gridwidth = 1;
    }
    c.fill = GridBagConstraints.HORIZONTAL;
    if (IOComponentTypes.hasRotationAttribute(myType)) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
          BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaRotationDefinition")));
      mapRotation.addItem(S.get(IOComponentTypes.getRotationString(myType, IOComponentTypes.ROTATION_ZERO)));
      mapRotation.addItem(S.get(IOComponentTypes.getRotationString(myType, IOComponentTypes.ROTATION_CW_90)));
      mapRotation.addItem(S.get(IOComponentTypes.getRotationString(myType, IOComponentTypes.ROTATION_CCW_90)));
      panel.add(mapRotation, BorderLayout.CENTER);
      switch (myRotation) {
        case IOComponentTypes.ROTATION_CW_90: {
          mapRotation.setSelectedIndex(1);
          break;
        }
        case IOComponentTypes.ROTATION_CCW_90: {
          mapRotation.setSelectedIndex(2);
          break;
        }
        default: {
          mapRotation.setSelectedIndex(0);
          break;
        }
      }
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (myType.equals(IOComponentTypes.LEDArray)) {
      final var panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaArrayDefinition")));
      ArrayPanel.setLayout(new GridBagLayout());
      RowSize.removeAll();
      ColSize.removeAll();
      for (var i = 1; i < 33; i++) {
        RowSize.addItem(i);
        ColSize.addItem(i);
      }
      RowSize.setSelectedIndex(info.getNrOfRows() - 1);
      ColSize.setSelectedIndex(info.getNrOfColumns() - 1);
      Encoding.removeAll();
      for (var val : LedArrayDriving.getDisplayStrings())
        Encoding.addItem(val);
      Encoding.setSelectedIndex(info.getArrayDriveMode());
      RowSize.setActionCommand("LedArray");
      RowSize.addActionListener(actionListener);
      ColSize.setActionCommand("LedArray");
      ColSize.addActionListener(actionListener);
      Encoding.setActionCommand("LedArray");
      Encoding.addActionListener(actionListener);
      final var arr = new GridBagConstraints();
      arr.gridx = 0;
      arr.gridy = 0;
      arr.gridwidth = 2;
      panel.add(new JLabel(S.get("FpgaArrayDriving")), arr);
      arr.gridy++;
      panel.add(Encoding, arr);
      arr.gridwidth = 1;
      arr.gridy++;
      panel.add(new JLabel(S.get("FpgaArrayRows")), arr);
      arr.gridx++;
      panel.add(RowSize, arr);
      arr.gridy++;
      panel.add(ColSize, arr);
      arr.gridx--;
      panel.add(new JLabel(S.get("FpgaArrayCols")), arr);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (NrOfPins.get(INPUT_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoInpPins")));
      InputsPanel.setLayout(new GridBagLayout());
      if (IOComponentTypes.nrOfInputPinsConfigurable(myType)) {
        Inputsize.removeAllItems();
        for (var i = 1; i < 129; i++) Inputsize.addItem(i);
        Inputsize.setSelectedItem(NrOfPins.get(INPUT_ID));
        Inputsize.addActionListener(actionListener);
        Inputsize.setActionCommand("inputSize");
        panel.add(Inputsize, BorderLayout.NORTH);
      }
      PinLabels.clear();
      var nr = NrOfPins.get(INPUT_ID);
      for (var i = 0; i < nr; i++) PinLabels.add(IOComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
      buildPinTable(NrOfPins.get(INPUT_ID), myType, InputsPanel, LocInputs, PinLabels, oldInputLocations);
      panel.add(InputsPanel, BorderLayout.CENTER);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (NrOfPins.get(OUTPUT_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoOutpPins")));
      OutputsPanel.setLayout(new GridBagLayout());
      if (IOComponentTypes.nrOfOutputPinsConfigurable(myType)) {
        Outputsize.removeAllItems();
        for (var i = 1; i < 129; i++) Outputsize.addItem(i);
        Outputsize.setSelectedItem(NrOfPins.get(OUTPUT_ID));
        Outputsize.addActionListener(actionListener);
        Outputsize.setActionCommand("outputSize");
        panel.add(Outputsize, BorderLayout.NORTH);
      }
      if (myType != IOComponentTypes.LEDArray) {
        PinLabels.clear();
        final var nr = NrOfPins.get(OUTPUT_ID);
        for (var i = 0; i < nr; i++) PinLabels.add(IOComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
        buildPinTable(NrOfPins.get(OUTPUT_ID), myType, OutputsPanel, LocOutputs, PinLabels, oldOutputLocations);
      } else {
        updateLedArrayRequirements(
            info.getNrOfRows(),
            info.getNrOfColumns(),
            info.getArrayDriveMode(),
            OutputsPanel,
            LocOutputs,
            oldOutputLocations,
            NrOfPins);
      }
      panel.add(OutputsPanel, BorderLayout.CENTER);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (NrOfPins.get(IO_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
          BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoIOPins")));
      IOPanel.setLayout(new GridBagLayout());
      if (IOComponentTypes.nrOfIOPinsConfigurable(myType)) {
        IOsize.removeAllItems();
        for (var i = 1; i < 129; i++) IOsize.addItem(i);
        IOsize.setSelectedItem(NrOfPins.get(IO_ID));
        IOsize.addActionListener(actionListener);
        IOsize.setActionCommand("ioSize");
        panel.add(IOsize, BorderLayout.NORTH);
      }
      PinLabels.clear();
      final var nr = NrOfPins.get(IO_ID);
      for (var i = 0; i < nr; i++) PinLabels.add(IOComponentTypes.getIOLabel(nr, i, myType));
      buildPinTable(NrOfPins.get(IO_ID), myType, IOPanel, LocIOs, PinLabels, oldIOLocations);
      panel.add(IOPanel, BorderLayout.CENTER);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }

    final var LabText = new JLabel(S.get("FpgaIoLabel"));
    c.gridy++;
    c.gridx = 0;
    contents.add(LabText, c);
    var LabelInput = new JTextField(6);
    LabelInput.setText(info.GetLabel());
    c.gridx = 1;
    contents.add(LabelInput, c);

    final var StandardText = new JLabel(S.get("FpgaIoStandard"));
    c.gridy++;
    c.gridx = 0;
    contents.add(StandardText, c);
    final var StandardInput = new JComboBox<String>(IoStandards.Behavior_strings);
    if (info.GetIOStandard() != IoStandards.UNKNOWN)
      StandardInput.setSelectedIndex(info.GetIOStandard());
    else StandardInput.setSelectedIndex(IOcomps.GetDefaultStandard());
    c.gridx = 1;
    contents.add(StandardInput, c);

    if (IOComponentTypes.OutputComponentSet.contains(myType)) {
      final var DriveText = new JLabel(S.get("FpgaIoStrength"));
      c.gridy++;
      c.gridx = 0;
      contents.add(DriveText, c);
      if (info.GetDrive() != DriveStrength.UNKNOWN) DriveInput.setSelectedIndex(info.GetDrive());
      else DriveInput.setSelectedIndex(IOcomps.GetDefaultDriveStrength());
      c.gridx = 1;
      contents.add(DriveInput, c);
    }

    if (IOComponentTypes.InputComponentSet.contains(myType)) {
      final var PullText = new JLabel(S.get("FpgaIoPull"));
      c.gridy++;
      c.gridx = 0;
      contents.add(PullText, c);
      if (info.GetPullBehavior() != PullBehaviors.UNKNOWN)
        PullInput.setSelectedIndex(info.GetPullBehavior());
      else PullInput.setSelectedIndex(IOcomps.GetDefaultPullSelection());
      c.gridx = 1;
      contents.add(PullInput, c);
    }

    if (!IOComponentTypes.InOutComponentSet.contains(myType)) {
      final var ActiveText = new JLabel(S.get("FpgaIoActivity", myType));
      c.gridy++;
      c.gridx = 0;
      contents.add(ActiveText, c);
      if (info.GetActivityLevel() != PinActivity.Unknown)
        ActiveInput.setSelectedIndex(info.GetActivityLevel());
      else ActiveInput.setSelectedIndex(IOcomps.GetDefaultActivity());
      c.gridx = 1;
      contents.add(ActiveInput, c);
    }
    if (deleteButton) {
      final var delButton = new JButton();
      delButton.setActionCommand("delete");
      delButton.addActionListener(actionListener);
      delButton.setText(S.get("FpgaIoDelete"));
      c.gridwidth = 2;
      c.gridx = 0;
      c.gridy++;
      contents.add(delButton, c);
      c.gridwidth = 1;
    }
    final var OkayButton = new JButton(S.get("FpgaBoardDone"));
    OkayButton.setActionCommand("done");
    OkayButton.addActionListener(actionListener);
    c.gridx = 1;
    c.gridy++;
    contents.add(OkayButton, c);

    final var CancelButton = new JButton(S.get("FpgaBoardCancel"));
    CancelButton.setActionCommand("cancel");
    CancelButton.addActionListener(actionListener);
    c.gridx = 0;
    contents.add(CancelButton, c);
    selWindow.add(new JScrollPane(contents));
    selWindow.pack();
    selWindow.setLocationRelativeTo(IOcomps.getParentFrame());
    selWindow.setModal(true);
    selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    selWindow.setAlwaysOnTop(true);
    abort = false;
    while (!abort) {
      selWindow.setVisible(true);
      abort |= info.GetType().equals(IOComponentTypes.Unknown);
      if (!abort) {
        final var NrPins = NrOfPins.get(INPUT_ID) + NrOfPins.get(OUTPUT_ID) + NrOfPins.get(IO_ID);
        var correct = true;
        for (var i = 0; i < NrOfPins.get(INPUT_ID); i++) {
          if (LocInputs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc", IOComponentTypes.getInputLabel(NrOfPins.get(INPUT_ID), i, myType)));
            break;
          }
        }
        if (!correct) continue;
        for (var i = 0; i < NrOfPins.get(OUTPUT_ID); i++) {
          if (LocOutputs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc",
                    IOComponentTypes.getOutputLabel(NrOfPins.get(INPUT_ID), 0, 0, i, myType)));
            break;
          }
        }
        if (!correct) continue;
        for (var i = 0; i < NrOfPins.get(IO_ID); i++) {
          if (LocIOs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc",
                    IOComponentTypes.getIOLabel(NrOfPins.get(INPUT_ID), i, myType)));
            break;
          }
        }
        if (correct) {
          if (!rectLocations.isEmpty()) {
            var values = new int[4];
            for (var i = 0; i < 4; i++) {
              try {
                values[i] = Integer.parseUnsignedInt(rectLocations.get(i).getText());
              } catch (NumberFormatException e) {
                correct = false;
                final var msgKey = switch (i) {
                  case 0 -> "FpgaIoXpos";
                  case 1 -> "FpgaIoYpos";
                  case 2 -> "FpgaIoWidth";
                  default -> "FpgaIoHeight";
                };
                DialogNotification.showDialogNotification(
                        // FIXME: hardcoded string
                        IOcomps.getParentFrame(), "Error", S.get("FpgaIoIntError", S.get(msgKey), rectLocations.get(i).getText()));
              }
            }
            if (!correct) continue;
            if (values[0] != MyRectangle.getXpos()
                || values[1] != MyRectangle.getYpos()
                || values[2] != MyRectangle.getWidth()
                || values[3] != MyRectangle.getHeight()) {
              final var update = new Rectangle(values[0], values[1], values[2], values[3]);
              if (IOcomps.hasOverlap(MyRectangle, new BoardRectangle(update))) {
                // FIXME: hardcoded string
                DialogNotification.showDialogNotification(
                    IOcomps.getParentFrame(), "Error", S.get("FpgaIoRectError"));
                continue;
              } else if (update.getX() + update.getWidth() >= BoardManipulator.IMAGE_WIDTH) {
                // FIXME: hardcoded string
                DialogNotification.showDialogNotification(
                    IOcomps.getParentFrame(), "Error", S.get("FpgaIoRectTWide"));
                continue;
              } else if (update.getY() + update.getHeight() >= BoardManipulator.IMAGE_HEIGHT) {
                // FIXME: hardcoded string
                DialogNotification.showDialogNotification(
                    IOcomps.getParentFrame(), "Error", S.get("FpgaIoRectTHeigt"));
                continue;
              } else if (update.getWidth() < 2) {
                // FIXME: hardcoded string
                DialogNotification.showDialogNotification(
                    IOcomps.getParentFrame(), "Error", S.get("FpgaIoRectWNLE"));
                continue;
              } else if (update.getHeight() < 2) {
                // FIXME: hardcoded string
                DialogNotification.showDialogNotification(
                    IOcomps.getParentFrame(), "Error", S.get("FpgaIoRectHNLE"));
                continue;
              } else {
                MyRectangle.updateRectangle(update);
              }
            }
          }
          IOcomps.SetDefaultStandard(StandardInput.getSelectedIndex());
          info.setNrOfPins(NrPins);
          var idx = 0;
          for (var i = 0; i < NrOfPins.get(INPUT_ID); i++)
            info.setInputPinLocation(idx++, LocInputs.get(i).getText());
          for (var i = 0; i < NrOfPins.get(OUTPUT_ID); i++)
            info.setOutputPinLocation(idx++, LocOutputs.get(i).getText());
          for (var i = 0; i < NrOfPins.get(IO_ID); i++)
            info.setIOPinLocation(idx++, LocIOs.get(i).getText());
          if (LabelInput.getText() != null && LabelInput.getText().length() != 0)
            info.setLabel(LabelInput.getText());
          else info.setLabel(null);
          info.setIOStandard(IoStandards.getId(StandardInput.getSelectedItem().toString()));
          if (IOComponentTypes.OutputComponentSet.contains(myType)) {
            IOcomps.SetDefaultDriveStrength(DriveInput.getSelectedIndex());
            info.setDrive(DriveStrength.getId(DriveInput.getSelectedItem().toString()));
          }
          if (IOComponentTypes.InputComponentSet.contains(myType)) {
            IOcomps.SetDefaultPullSelection(PullInput.getSelectedIndex());
            info.setPullBehavior(PullBehaviors.getId(PullInput.getSelectedItem().toString()));
          }
          if (!IOComponentTypes.InOutComponentSet.contains(myType)) {
            IOcomps.SetDefaultActivity(ActiveInput.getSelectedIndex());
            info.setActivityLevel(PinActivity.getId(ActiveInput.getSelectedItem().toString()));
          }
          final var rotation = switch (mapRotation.getSelectedIndex()) {
            case 1 -> IOComponentTypes.ROTATION_CW_90;
            case 2 -> IOComponentTypes.ROTATION_CCW_90;
            default -> IOComponentTypes.ROTATION_ZERO;
          };
          info.setMapRotation(rotation);
          abort = true;
        }
      }
    }
    selWindow.dispose();
  }

  private static boolean abort;

  public static void getFpgaInformation(Frame panel, BoardInformation TheBoard) {
    final var selWindow = new JDialog(panel, S.get("FpgaBoardFpgaProp"));
    /* here the action listener is defined */
    abort = false;
    ActionListener actionListener =
        e -> {
          if (e.getActionCommand().equals("cancel")) {
            abort = true;
          }
          selWindow.setVisible(false);
        };
    final var c = new GridBagConstraints();
    /* Here the clock related settings are defined */
    final var ClockPanel = new JPanel();
    ClockPanel.setLayout(new GridBagLayout());
    ClockPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardClkProp")));

    final var FreqText = new JLabel(S.get("FpgaBoardClkFreq"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    ClockPanel.add(FreqText, c);

    final var FreqPanel = new JPanel();
    final var FreqLayout = new GridBagLayout();
    FreqPanel.setLayout(FreqLayout);

    final var FreqInput = new JTextField(10);
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    if (TheBoard.fpga.FpgaInfoPresent())
      FreqInput.setText(Integer.toString(getFrequencyValue(TheBoard.fpga.getClockFrequency())));
    FreqPanel.add(FreqInput, c);

    String[] freqStrs = {"Hz", "kHz", "MHz"};
    final var StandardInput = new JComboBox<String>(freqStrs);
    StandardInput.setSelectedIndex(2);
    c.gridx = 1;
    if (TheBoard.fpga.FpgaInfoPresent())
      StandardInput.setSelectedIndex(getFrequencyIndex(TheBoard.fpga.getClockFrequency()));
    FreqPanel.add(StandardInput, c);

    ClockPanel.add(FreqPanel, c);

    final var LocText = new JLabel(S.get("FpgaBoardClkLoc"));
    c.gridy = 1;
    c.gridx = 0;
    ClockPanel.add(LocText, c);

    final var LocInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) LocInput.setText(TheBoard.fpga.getClockPinLocation());
    c.gridx = 1;
    ClockPanel.add(LocInput, c);

    final var PullText = new JLabel(S.get("FpgaBoardClkPul"));
    c.gridy = 2;
    c.gridx = 0;
    ClockPanel.add(PullText, c);

    final var PullInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      PullInput.setSelectedIndex(TheBoard.fpga.getClockPull());
    } else PullInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(PullInput, c);

    final var StandardText = new JLabel(S.get("FpgaBoardClkStd"));
    c.gridy = 3;
    c.gridx = 0;
    ClockPanel.add(StandardText, c);

    final var StdInput = new JComboBox<String>(IoStandards.Behavior_strings);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      StdInput.setSelectedIndex(TheBoard.fpga.getClockStandard());
    } else StdInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(StdInput, c);

    /* Here the FPGA related settings are defined */
    final var FPGAPanel = new JPanel();
    FPGAPanel.setLayout(new GridBagLayout());
    FPGAPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardFpgaProp")));

    final var VendorText = new JLabel(S.get("FpgaBoardFpgaVend"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    FPGAPanel.add(VendorText, c);

    final var VendorInput = new JComboBox<String>(VendorSoftware.VENDORS);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      VendorInput.setSelectedIndex(TheBoard.fpga.getVendor());
    } else VendorInput.setSelectedIndex(0);
    c.gridx = 1;
    FPGAPanel.add(VendorInput, c);

    final var FamilyText = new JLabel(S.get("FpgaBoardFpgaFam"));
    c.gridy = 1;
    c.gridx = 0;
    FPGAPanel.add(FamilyText, c);

    final var FamilyInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) FamilyInput.setText(TheBoard.fpga.getTechnology());
    c.gridx = 1;
    FPGAPanel.add(FamilyInput, c);

    final var PartText = new JLabel(S.get("FpgaBoardFpgaPart"));
    c.gridy = 2;
    c.gridx = 0;
    FPGAPanel.add(PartText, c);

    final var PartInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) PartInput.setText(TheBoard.fpga.getPart());
    c.gridx = 1;
    FPGAPanel.add(PartInput, c);

    final var BoxText = new JLabel(S.get("FpgaBoardFpgaPack"));
    c.gridy = 3;
    c.gridx = 0;
    FPGAPanel.add(BoxText, c);

    final var BoxInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) BoxInput.setText(TheBoard.fpga.getPackage());
    c.gridx = 1;
    FPGAPanel.add(BoxInput, c);

    final var SpeedText = new JLabel(S.get("FpgaBoardFpgaSG"));
    c.gridy = 4;
    c.gridx = 0;
    FPGAPanel.add(SpeedText, c);

    final var SpeedInput = new JTextField();
    if (TheBoard.fpga.FpgaInfoPresent()) SpeedInput.setText(TheBoard.fpga.getSpeedGrade());
    c.gridx = 1;
    FPGAPanel.add(SpeedInput, c);

    final var UnusedPinsText = new JLabel(S.get("FpgaBoardPinUnused"));
    c.gridy = 5;
    c.gridx = 0;
    FPGAPanel.add(UnusedPinsText, c);

    final var UnusedPinsInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.FpgaInfoPresent()) {
      UnusedPinsInput.setSelectedIndex(TheBoard.fpga.getUnusedPinsBehavior());
    } else UnusedPinsInput.setSelectedIndex(0);
    c.gridx = 1;
    FPGAPanel.add(UnusedPinsInput, c);

    /* JTAG related Settings */
    final var JTAGPanel = new JPanel();
    JTAGPanel.setLayout(new GridBagLayout());
    JTAGPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardJtagProp")));

    final var PosText = new JLabel(S.get("FpgaBoardJtagLoc"));
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    JTAGPanel.add(PosText, c);

    final var PosInput = new JTextField(5);
    PosInput.setText("1");
    if (TheBoard.fpga.FpgaInfoPresent())
      PosInput.setText(Integer.toString(TheBoard.fpga.getFpgaJTAGChainPosition()));
    c.gridx = 1;
    JTAGPanel.add(PosInput, c);

    final var FlashPosText = new JLabel(S.get("FpgaBoardFlashLoc"));
    c.gridy = 1;
    c.gridx = 0;
    JTAGPanel.add(FlashPosText, c);

    final var FlashPosInput = new JTextField(5);
    FlashPosInput.setText("2");
    if (TheBoard.fpga.FpgaInfoPresent())
      FlashPosInput.setText(Integer.toString(TheBoard.fpga.getFlashJTAGChainPosition()));
    c.gridx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    JTAGPanel.add(FlashPosInput, c);

    /* misc settings */
    final var MiscPanel = new JPanel();
    MiscPanel.setLayout(new GridBagLayout());
    MiscPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardMiscProp")));

    final var FlashName = new JLabel(S.get("FpgaBoardFlashType"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    MiscPanel.add(FlashName, c);

    final var FlashNameInput = new JTextField("");
    if (TheBoard.fpga.FpgaInfoPresent()) FlashNameInput.setText(TheBoard.fpga.getFlashName());
    c.gridx = 1;
    MiscPanel.add(FlashNameInput, c);

    final var UsbTmc = new JCheckBox(S.get("FpgaBoardUSBTMC"));
    UsbTmc.setSelected(false);
    if (TheBoard.fpga.FpgaInfoPresent()) UsbTmc.setSelected(TheBoard.fpga.USBTMCDownloadRequired());
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    MiscPanel.add(UsbTmc, c);

    final var dialogLayout = new GridBagLayout();
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
    selWindow.add(JTAGPanel, c);
    c.gridx = 1;
    selWindow.add(MiscPanel, c);

    final var CancelButton = new JButton(S.get("FpgaBoardCancel"));
    CancelButton.addActionListener(actionListener);
    CancelButton.setActionCommand("cancel");
    c.gridx = 0;
    c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(CancelButton, c);

    final var SaveButton = new JButton(S.get("FpgaBoardDone"));
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
    var save_settings = false;
    while ((!abort) && (!save_settings)) {
      selWindow.setVisible(true);
      if (!abort) {
        save_settings = true;
        switch ((int)
            getFrequency(FreqInput.getText(), StandardInput.getSelectedItem().toString())) {
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
    var result = 0L;
    var multiplier = 1L;
    var dec_mult = false;

    if (speed.equals("kHz")) multiplier = 1000L;
    if (speed.equals("MHz")) multiplier = 1000000L;
    for (var i = 0; i < chars.length(); i++) {
      if (chars.charAt(i) >= '0' && chars.charAt(i) <= '9') {
        result *= 10L;
        result += (chars.charAt(i) - '0');
        if (dec_mult) {
          multiplier /= 10L;
          if (multiplier == 0L) return -1;
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
