/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.DriveStrength;
import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.IoComponentTypes;
import com.cburch.logisim.fpga.data.IoComponentsInformation;
import com.cburch.logisim.fpga.data.IoStandards;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.PinActivity;
import com.cburch.logisim.fpga.data.PullBehaviors;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.std.io.LedArrayGenericHdlGeneratorFactory;
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

public class FpgaIoInformationSettingsDialog {

  private static final int INPUT_ID = 0;
  private static final int OUTPUT_ID = 1;
  private static final int IO_ID = 2;

  private static boolean abort;

  private static void buildPinTable(
      int nr,
      IoComponentTypes type,
      JPanel pinPanel,
      ArrayList<JTextField> LocInputs,
      ArrayList<String> PinLabels,
      ArrayList<String> oldLocations) {
    var c = new GridBagConstraints();
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
        final var nrBits = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(nrOfRows);
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
        final var nrBits = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
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
          preamble = switch (rgb) {
            case 0 -> "Red_";
            case 1 -> "Green_";
            default -> "Blue_";
          };
          for (var row = 0; row < nrOfRows; row++) {
            for (var col = 0; col < nrOfColumns; col++)
              pinLabels.add(preamble + "Row_" + row + "_Col_" + col);
          }
        }
        break;
      }
      case LedArrayDriving.RGB_ROW_SCANNING: {
        final var nrBits = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(nrOfRows);
        nrOfPins = nrBits + 3 * nrOfColumns;
        var preamble = "";
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("RowAddress_" + i);
          } else {
            final var id = i - nrBits;
            final var rgb = id / nrOfColumns;
            final var col = id % nrOfColumns;
            preamble = switch (rgb) {
              case 0 -> "Red_";
              case 1 -> "Green_";
              default -> "Blue_";
            };
            pinLabels.add(preamble + "Col_" + col);
          }
        }
        break;
      }
      case LedArrayDriving.RGB_COLUMN_SCANNING: {
        final var nrBits = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
        nrOfPins = nrBits + 3 * nrOfRows;
        var preamble = "";
        for (var i = 0; i < nrOfPins; i++) {
          if (i < nrBits) {
            pinLabels.add("ColumnAddress_" + i);
          } else {
            final var id = i - nrBits;
            final var rgb = id / nrOfRows;
            final var col = id % nrOfRows;
            preamble = switch (rgb) {
              case 0 -> "Red_";
              case 1 -> "Green_";
              default -> "Blue_";
            };
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
        IoComponentTypes.LedArray,
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

  public static void getSimpleInformationDialog(Boolean deleteButton, IoComponentsInformation IOcomps, FpgaIoInformationContainer info) {
    final var nrOfPins = new HashMap<Integer, Integer>();
    final var selWindow = new JDialog(IOcomps.getParentFrame(), info.getType() + " " + S.get("FpgaIoProperties"));
    final var contents = new JPanel();
    final var driveInput = new JComboBox<String>(DriveStrength.BEHAVIOR_STRINGS);
    final var pullInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    final var activeInput = new JComboBox<String>(PinActivity.BEHAVIOR_STRINGS);
    final var inputSize = new JComboBox<Integer>();
    final var outputSize = new JComboBox<Integer>();
    final var ioSize = new JComboBox<Integer>();
    final var rowSize = new JComboBox<Integer>();
    final var colSize = new JComboBox<Integer>();
    final var eEncoding = new JComboBox<String>();
    final var mapRotation = new JComboBox<String>();
    final var locInputs = new ArrayList<JTextField>();
    final var locOutputs = new ArrayList<JTextField>();
    final var locIos = new ArrayList<JTextField>();
    final var pinLabels = new ArrayList<String>();
    final var arrayPanel = new JPanel();
    final var inputsPanel = new JPanel();
    final var outputsPanel = new JPanel();
    final var ioPanel = new JPanel();
    var abort = false;
    final var rectLocations = new ArrayList<JTextField>();
    final var oldInputLocations = new ArrayList<String>();
    final var oldOutputLocations = new ArrayList<String>();
    final var oldIoLocations = new ArrayList<String>();
    final var myType = info.getType();
    final var myRotation = info.getMapRotation();
    final var MyRectangle = info.getRectangle();
    if (info.getNrOfPins() == 0) {
      nrOfPins.put(INPUT_ID, IoComponentTypes.getFpgaInputRequirement(myType));
      nrOfPins.put(OUTPUT_ID, IoComponentTypes.getFpgaOutputRequirement(myType));
      nrOfPins.put(IO_ID, IoComponentTypes.getFpgaInOutRequirement(myType));
    } else {
      nrOfPins.put(INPUT_ID, info.getNrOfInputPins());
      nrOfPins.put(OUTPUT_ID, info.getNrOfOutputPins());
      nrOfPins.put(IO_ID, info.getNrOfIoPins());
    }
    for (var cnt = 0; cnt < info.getNrOfPins(); cnt++) {
      if (cnt < nrOfPins.get(INPUT_ID)) oldInputLocations.add(info.getPinLocation(cnt));
      else if (cnt < nrOfPins.get(INPUT_ID) + nrOfPins.get(OUTPUT_ID))
        oldOutputLocations.add(info.getPinLocation(cnt));
      else
        oldIoLocations.add(info.getPinLocation(cnt));
    }
    ActionListener actionListener =
        e -> {
          switch (e.getActionCommand()) {
            case "inputSize": {
              var nr = (int) inputSize.getSelectedItem();
              nrOfPins.put(INPUT_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getInputLabel(nr, i, myType));
              buildPinTable(nr, myType, inputsPanel, locInputs, pinLabels, oldInputLocations);
              selWindow.pack();
              return;
            }
            case "outputSize": {
              var nr = (int) outputSize.getSelectedItem();
              nrOfPins.put(OUTPUT_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
              buildPinTable(nr, myType, outputsPanel, locOutputs, pinLabels, oldOutputLocations);
              selWindow.pack();
              return;
            }
            case "ioSize": {
              var nr = (int) ioSize.getSelectedItem();
              nrOfPins.put(IO_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getIoLabel(nr, i, myType));
              buildPinTable(nr, myType, ioPanel, locIos, pinLabels, oldIoLocations);
              selWindow.pack();
              return;
            }
            case "LedArray": {
              info.setNrOfRows(rowSize.getSelectedIndex() + 1);
              info.setNrOfColumns(colSize.getSelectedIndex() + 1);
              info.setArrayDriveMode((char) eEncoding.getSelectedIndex());
              updateLedArrayRequirements(info.getNrOfRows(),
                  info.getNrOfColumns(),
                  info.getArrayDriveMode(),
                  outputsPanel,
                  locOutputs,
                  oldOutputLocations,
                  nrOfPins);
              selWindow.pack();
              return;
            }
            case "cancel":
              info.setType(IoComponentTypes.Unknown);
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
    if (IoComponentTypes.hasRotationAttribute(myType)) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
          BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaRotationDefinition")));
      mapRotation.addItem(S.get(IoComponentTypes.getRotationString(myType, IoComponentTypes.ROTATION_ZERO)));
      mapRotation.addItem(S.get(IoComponentTypes.getRotationString(myType, IoComponentTypes.ROTATION_CW_90)));
      mapRotation.addItem(S.get(IoComponentTypes.getRotationString(myType, IoComponentTypes.ROTATION_CCW_90)));
      panel.add(mapRotation, BorderLayout.CENTER);
      switch (myRotation) {
        case IoComponentTypes.ROTATION_CW_90: {
          mapRotation.setSelectedIndex(1);
          break;
        }
        case IoComponentTypes.ROTATION_CCW_90: {
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
    if (myType.equals(IoComponentTypes.LedArray)) {
      final var panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaArrayDefinition")));
      arrayPanel.setLayout(new GridBagLayout());
      rowSize.removeAll();
      colSize.removeAll();
      for (var i = 1; i < 33; i++) {
        rowSize.addItem(i);
        colSize.addItem(i);
      }
      rowSize.setSelectedIndex(info.getNrOfRows() - 1);
      colSize.setSelectedIndex(info.getNrOfColumns() - 1);
      eEncoding.removeAll();
      for (var val : LedArrayDriving.getDisplayStrings())
        eEncoding.addItem(val);
      eEncoding.setSelectedIndex(info.getArrayDriveMode());
      rowSize.setActionCommand("LedArray");
      rowSize.addActionListener(actionListener);
      colSize.setActionCommand("LedArray");
      colSize.addActionListener(actionListener);
      eEncoding.setActionCommand("LedArray");
      eEncoding.addActionListener(actionListener);
      final var arr = new GridBagConstraints();
      arr.gridx = 0;
      arr.gridy = 0;
      arr.gridwidth = 2;
      panel.add(new JLabel(S.get("FpgaArrayDriving")), arr);
      arr.gridy++;
      panel.add(eEncoding, arr);
      arr.gridwidth = 1;
      arr.gridy++;
      panel.add(new JLabel(S.get("FpgaArrayRows")), arr);
      arr.gridx++;
      panel.add(rowSize, arr);
      arr.gridy++;
      panel.add(colSize, arr);
      arr.gridx--;
      panel.add(new JLabel(S.get("FpgaArrayCols")), arr);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (nrOfPins.get(INPUT_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoInpPins")));
      inputsPanel.setLayout(new GridBagLayout());
      if (IoComponentTypes.nrOfInputPinsConfigurable(myType)) {
        inputSize.removeAllItems();
        for (var i = 1; i < 129; i++) inputSize.addItem(i);
        inputSize.setSelectedItem(nrOfPins.get(INPUT_ID));
        inputSize.addActionListener(actionListener);
        inputSize.setActionCommand("inputSize");
        panel.add(inputSize, BorderLayout.NORTH);
      }
      pinLabels.clear();
      var nr = nrOfPins.get(INPUT_ID);
      for (var i = 0; i < nr; i++) pinLabels.add(IoComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
      buildPinTable(nrOfPins.get(INPUT_ID), myType, inputsPanel, locInputs, pinLabels, oldInputLocations);
      panel.add(inputsPanel, BorderLayout.CENTER);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (nrOfPins.get(OUTPUT_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoOutpPins")));
      outputsPanel.setLayout(new GridBagLayout());
      if (IoComponentTypes.nrOfOutputPinsConfigurable(myType)) {
        outputSize.removeAllItems();
        for (var i = 1; i < 129; i++) outputSize.addItem(i);
        outputSize.setSelectedItem(nrOfPins.get(OUTPUT_ID));
        outputSize.addActionListener(actionListener);
        outputSize.setActionCommand("outputSize");
        panel.add(outputSize, BorderLayout.NORTH);
      }
      if (myType != IoComponentTypes.LedArray) {
        pinLabels.clear();
        final var nr = nrOfPins.get(OUTPUT_ID);
        for (var i = 0; i < nr; i++) pinLabels.add(IoComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
        buildPinTable(nrOfPins.get(OUTPUT_ID), myType, outputsPanel, locOutputs, pinLabels, oldOutputLocations);
      } else {
        updateLedArrayRequirements(
            info.getNrOfRows(),
            info.getNrOfColumns(),
            info.getArrayDriveMode(),
            outputsPanel,
            locOutputs,
            oldOutputLocations,
            nrOfPins);
      }
      panel.add(outputsPanel, BorderLayout.CENTER);
      c.gridy++;
      c.gridwidth = 2;
      contents.add(panel, c);
      c.gridwidth = 1;
    }
    if (nrOfPins.get(IO_ID) > 0) {
      final var panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaIoIOPins")));
      ioPanel.setLayout(new GridBagLayout());
      if (IoComponentTypes.nrOfIoPinsConfigurable(myType)) {
        ioSize.removeAllItems();
        for (var i = 1; i < 129; i++) ioSize.addItem(i);
        ioSize.setSelectedItem(nrOfPins.get(IO_ID));
        ioSize.addActionListener(actionListener);
        ioSize.setActionCommand("ioSize");
        panel.add(ioSize, BorderLayout.NORTH);
      }
      pinLabels.clear();
      final var nr = nrOfPins.get(IO_ID);
      for (var i = 0; i < nr; i++) pinLabels.add(IoComponentTypes.getIoLabel(nr, i, myType));
      buildPinTable(nrOfPins.get(IO_ID), myType, ioPanel, locIos, pinLabels, oldIoLocations);
      panel.add(ioPanel, BorderLayout.CENTER);
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
    LabelInput.setText(info.getLabel());
    c.gridx = 1;
    contents.add(LabelInput, c);

    final var StandardText = new JLabel(S.get("FpgaIoStandard"));
    c.gridy++;
    c.gridx = 0;
    contents.add(StandardText, c);
    final var StandardInput = new JComboBox<String>(IoStandards.BEHAVIOR_STRINGS);
    if (info.getIoStandard() != IoStandards.UNKNOWN)
      StandardInput.setSelectedIndex(info.getIoStandard());
    else StandardInput.setSelectedIndex(IOcomps.getDefaultStandard());
    c.gridx = 1;
    contents.add(StandardInput, c);

    if (IoComponentTypes.OUTPUT_COMPONENT_SET.contains(myType)) {
      final var DriveText = new JLabel(S.get("FpgaIoStrength"));
      c.gridy++;
      c.gridx = 0;
      contents.add(DriveText, c);
      if (info.getDrive() != DriveStrength.UNKNOWN) driveInput.setSelectedIndex(info.getDrive());
      else driveInput.setSelectedIndex(IOcomps.getDefaultDriveStrength());
      c.gridx = 1;
      contents.add(driveInput, c);
    }

    if (IoComponentTypes.INPUT_COMPONENT_SET.contains(myType)) {
      final var PullText = new JLabel(S.get("FpgaIoPull"));
      c.gridy++;
      c.gridx = 0;
      contents.add(PullText, c);
      if (info.getPullBehavior() != PullBehaviors.UNKNOWN)
        pullInput.setSelectedIndex(info.getPullBehavior());
      else pullInput.setSelectedIndex(IOcomps.getDefaultPullSelection());
      c.gridx = 1;
      contents.add(pullInput, c);
    }

    if (!IoComponentTypes.IN_OUT_COMPONENT_SET.contains(myType)) {
      final var ActiveText = new JLabel(S.get("FpgaIoActivity", myType));
      c.gridy++;
      c.gridx = 0;
      contents.add(ActiveText, c);
      if (info.getActivityLevel() != PinActivity.Unknown)
        activeInput.setSelectedIndex(info.getActivityLevel());
      else activeInput.setSelectedIndex(IOcomps.getDefaultActivity());
      c.gridx = 1;
      contents.add(activeInput, c);
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
      abort |= info.getType().equals(IoComponentTypes.Unknown);
      if (!abort) {
        final var NrPins = nrOfPins.get(INPUT_ID) + nrOfPins.get(OUTPUT_ID) + nrOfPins.get(IO_ID);
        var correct = true;
        for (var i = 0; i < nrOfPins.get(INPUT_ID); i++) {
          if (locInputs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc", IoComponentTypes.getInputLabel(nrOfPins.get(INPUT_ID), i, myType)));
            break;
          }
        }
        if (!correct) continue;
        for (var i = 0; i < nrOfPins.get(OUTPUT_ID); i++) {
          if (locOutputs.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc",
                    IoComponentTypes.getOutputLabel(nrOfPins.get(INPUT_ID), 0, 0, i, myType)));
            break;
          }
        }
        if (!correct) continue;
        for (var i = 0; i < nrOfPins.get(IO_ID); i++) {
          if (locIos.get(i).getText().isEmpty()) {
            correct = false;
            DialogNotification.showDialogNotification(
                IOcomps.getParentFrame(),
                "Error",
                S.get("FpgaIoPinLoc",
                    IoComponentTypes.getIoLabel(nrOfPins.get(INPUT_ID), i, myType)));
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
          IOcomps.setDefaultStandard(StandardInput.getSelectedIndex());
          info.setNrOfPins(NrPins);
          var idx = 0;
          for (var i = 0; i < nrOfPins.get(INPUT_ID); i++)
            info.setInputPinLocation(idx++, locInputs.get(i).getText());
          for (var i = 0; i < nrOfPins.get(OUTPUT_ID); i++)
            info.setOutputPinLocation(idx++, locOutputs.get(i).getText());
          for (var i = 0; i < nrOfPins.get(IO_ID); i++)
            info.setIOPinLocation(idx++, locIos.get(i).getText());
          if (LabelInput.getText() != null && LabelInput.getText().length() != 0)
            info.setLabel(LabelInput.getText());
          else info.setLabel(null);
          info.setIOStandard(IoStandards.getId(StandardInput.getSelectedItem().toString()));
          if (IoComponentTypes.OUTPUT_COMPONENT_SET.contains(myType)) {
            IOcomps.setDefaultDriveStrength(driveInput.getSelectedIndex());
            info.setDrive(DriveStrength.getId(driveInput.getSelectedItem().toString()));
          }
          if (IoComponentTypes.INPUT_COMPONENT_SET.contains(myType)) {
            IOcomps.setDefaultPullSelection(pullInput.getSelectedIndex());
            info.setPullBehavior(PullBehaviors.getId(pullInput.getSelectedItem().toString()));
          }
          if (!IoComponentTypes.IN_OUT_COMPONENT_SET.contains(myType)) {
            IOcomps.setDefaultActivity(activeInput.getSelectedIndex());
            info.setActivityLevel(PinActivity.getId(activeInput.getSelectedItem().toString()));
          }
          final var rotation = switch (mapRotation.getSelectedIndex()) {
            case 1 -> IoComponentTypes.ROTATION_CW_90;
            case 2 -> IoComponentTypes.ROTATION_CCW_90;
            default -> IoComponentTypes.ROTATION_ZERO;
          };
          info.setMapRotation(rotation);
          abort = true;
        }
      }
    }
    selWindow.dispose();
  }

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
    if (TheBoard.fpga.isFpgaInfoPresent())
      FreqInput.setText(Integer.toString(getFrequencyValue(TheBoard.fpga.getClockFrequency())));
    FreqPanel.add(FreqInput, c);

    String[] freqStrs = {"Hz", "kHz", "MHz"};
    final var StandardInput = new JComboBox<String>(freqStrs);
    StandardInput.setSelectedIndex(2);
    c.gridx = 1;
    if (TheBoard.fpga.isFpgaInfoPresent())
      StandardInput.setSelectedIndex(getFrequencyIndex(TheBoard.fpga.getClockFrequency()));
    FreqPanel.add(StandardInput, c);

    ClockPanel.add(FreqPanel, c);

    final var LocText = new JLabel(S.get("FpgaBoardClkLoc"));
    c.gridy = 1;
    c.gridx = 0;
    ClockPanel.add(LocText, c);

    final var LocInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) LocInput.setText(TheBoard.fpga.getClockPinLocation());
    c.gridx = 1;
    ClockPanel.add(LocInput, c);

    final var PullText = new JLabel(S.get("FpgaBoardClkPul"));
    c.gridy = 2;
    c.gridx = 0;
    ClockPanel.add(PullText, c);

    final var PullInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      PullInput.setSelectedIndex(TheBoard.fpga.getClockPull());
    } else PullInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(PullInput, c);

    final var StandardText = new JLabel(S.get("FpgaBoardClkStd"));
    c.gridy = 3;
    c.gridx = 0;
    ClockPanel.add(StandardText, c);

    final var StdInput = new JComboBox<String>(IoStandards.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      StdInput.setSelectedIndex(TheBoard.fpga.getClockStandard());
    } else StdInput.setSelectedIndex(0);
    c.gridx = 1;
    ClockPanel.add(StdInput, c);

    /* Here the FPGA related settings are defined */
    final var fpgaPanel = new JPanel();
    fpgaPanel.setLayout(new GridBagLayout());
    fpgaPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardFpgaProp")));

    final var VendorText = new JLabel(S.get("FpgaBoardFpgaVend"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    fpgaPanel.add(VendorText, c);

    final var vendorInput = new JComboBox<String>(VendorSoftware.VENDORS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      vendorInput.setSelectedIndex(TheBoard.fpga.getVendor());
    } else vendorInput.setSelectedIndex(0);
    c.gridx = 1;
    fpgaPanel.add(vendorInput, c);

    final var familyText = new JLabel(S.get("FpgaBoardFpgaFam"));
    c.gridy = 1;
    c.gridx = 0;
    fpgaPanel.add(familyText, c);

    final var familyInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) familyInput.setText(TheBoard.fpga.getTechnology());
    c.gridx = 1;
    fpgaPanel.add(familyInput, c);

    final var PartText = new JLabel(S.get("FpgaBoardFpgaPart"));
    c.gridy = 2;
    c.gridx = 0;
    fpgaPanel.add(PartText, c);

    final var partInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) partInput.setText(TheBoard.fpga.getPart());
    c.gridx = 1;
    fpgaPanel.add(partInput, c);

    final var BoxText = new JLabel(S.get("FpgaBoardFpgaPack"));
    c.gridy = 3;
    c.gridx = 0;
    fpgaPanel.add(BoxText, c);

    final var boxInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) boxInput.setText(TheBoard.fpga.getPackage());
    c.gridx = 1;
    fpgaPanel.add(boxInput, c);

    final var speedText = new JLabel(S.get("FpgaBoardFpgaSG"));
    c.gridy = 4;
    c.gridx = 0;
    fpgaPanel.add(speedText, c);

    final var speedInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) speedInput.setText(TheBoard.fpga.getSpeedGrade());
    c.gridx = 1;
    fpgaPanel.add(speedInput, c);

    final var unusedPinsText = new JLabel(S.get("FpgaBoardPinUnused"));
    c.gridy = 5;
    c.gridx = 0;
    fpgaPanel.add(unusedPinsText, c);

    final var unusedPinsInput = new JComboBox<String>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      unusedPinsInput.setSelectedIndex(TheBoard.fpga.getUnusedPinsBehavior());
    } else unusedPinsInput.setSelectedIndex(0);
    c.gridx = 1;
    fpgaPanel.add(unusedPinsInput, c);

    /* JTAG related Settings */
    final var jtagPanel = new JPanel();
    jtagPanel.setLayout(new GridBagLayout());
    jtagPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardJtagProp")));

    final var posText = new JLabel(S.get("FpgaBoardJtagLoc"));
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    jtagPanel.add(posText, c);

    final var PosInput = new JTextField(5);
    PosInput.setText("1");
    if (TheBoard.fpga.isFpgaInfoPresent())
      PosInput.setText(Integer.toString(TheBoard.fpga.getFpgaJTAGChainPosition()));
    c.gridx = 1;
    jtagPanel.add(PosInput, c);

    final var FlashPosText = new JLabel(S.get("FpgaBoardFlashLoc"));
    c.gridy = 1;
    c.gridx = 0;
    jtagPanel.add(FlashPosText, c);

    final var flashPosInput = new JTextField(5);
    flashPosInput.setText("2");
    if (TheBoard.fpga.isFpgaInfoPresent())
      flashPosInput.setText(Integer.toString(TheBoard.fpga.getFlashJTAGChainPosition()));
    c.gridx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    jtagPanel.add(flashPosInput, c);

    /* misc settings */
    final var miscPanel = new JPanel();
    miscPanel.setLayout(new GridBagLayout());
    miscPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardMiscProp")));

    final var flashName = new JLabel(S.get("FpgaBoardFlashType"));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    miscPanel.add(flashName, c);

    final var flashNameInput = new JTextField("");
    if (TheBoard.fpga.isFpgaInfoPresent()) flashNameInput.setText(TheBoard.fpga.getFlashName());
    c.gridx = 1;
    miscPanel.add(flashNameInput, c);

    final var usbTmc = new JCheckBox(S.get("FpgaBoardUSBTMC"));
    usbTmc.setSelected(false);
    if (TheBoard.fpga.isFpgaInfoPresent()) usbTmc.setSelected(TheBoard.fpga.isUsbTmcDownloadRequired());
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    miscPanel.add(usbTmc, c);

    final var dialogLayout = new GridBagLayout();
    selWindow.setLayout(dialogLayout);
    abort = false;
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(ClockPanel, c);
    c.gridx = 1;
    selWindow.add(fpgaPanel, c);
    c.gridx = 0;
    c.gridy = 1;
    selWindow.add(jtagPanel, c);
    c.gridx = 1;
    selWindow.add(miscPanel, c);

    final var cancelButton = new JButton(S.get("FpgaBoardCancel"));
    cancelButton.addActionListener(actionListener);
    cancelButton.setActionCommand("cancel");
    c.gridx = 0;
    c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(cancelButton, c);

    final var saveButton = new JButton(S.get("FpgaBoardDone"));
    saveButton.addActionListener(actionListener);
    saveButton.setActionCommand("save");
    c.gridx = 1;
    c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(saveButton, c);

    selWindow.pack();
    selWindow.setModal(true);
    selWindow.setResizable(false);
    selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    selWindow.setAlwaysOnTop(true);
    selWindow.setLocationRelativeTo(panel);
    var saveSettings = false;
    while ((!abort) && (!saveSettings)) {
      selWindow.setVisible(true);
      if (!abort) {
        saveSettings = true;
        switch ((int)
            getFrequency(FreqInput.getText(), StandardInput.getSelectedItem().toString())) {
          case -2:
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFreqError"));
            break;
          case -1:
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFracError"));
            break;
          case 0:
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardClkReq"));
            break;
          default:
            break;
        }
        if (saveSettings && LocInput.getText().isEmpty()) {
          saveSettings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardClkPin"));
        }
        if (saveSettings && familyInput.getText().isEmpty()) {
          saveSettings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaFamMis"));
        }
        if (saveSettings && partInput.getText().isEmpty()) {
          saveSettings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaPartMis"));
        }
        if (saveSettings && boxInput.getText().isEmpty()) {
          saveSettings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaPacMis"));
        }
        if (saveSettings && speedInput.getText().isEmpty()) {
          saveSettings = false;
          DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFpgaSpeedMis"));
        }
        if (saveSettings) {
          TheBoard.fpga.set(
              getFrequency(FreqInput.getText(), StandardInput.getSelectedItem().toString()),
              LocInput.getText(),
              PullInput.getSelectedItem().toString(),
              StdInput.getSelectedItem().toString(),
              familyInput.getText(),
              partInput.getText(),
              boxInput.getText(),
              speedInput.getText(),
              vendorInput.getSelectedItem().toString(),
              unusedPinsInput.getSelectedItem().toString(),
              usbTmc.isSelected(),
              PosInput.getText(),
              flashNameInput.getText(),
              flashPosInput.getText());
        }
      }
    }
    selWindow.dispose();
  }

  private static int getFrequencyValue(long freq) {
    if ((freq % 1_000) != 0) return (int) freq;
    if ((freq % 1_000_000) != 0) return (int) freq / 1_000;
    return (int) freq / 1_000_000;
  }

  private static int getFrequencyIndex(long freq) {
    if ((freq % 1_000) != 0) return 0;
    if ((freq % 1_000_000) != 0) return 1;
    return 2;
  }

  private static long getFrequency(String chars, String speed) {
    var result = 0L;
    var multiplier = 1L;
    var dec_mult = false;

    if ("kHz".equals(speed)) multiplier = 1_000L;
    if ("MHz".equals(speed)) multiplier = 1_000_000L;
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
