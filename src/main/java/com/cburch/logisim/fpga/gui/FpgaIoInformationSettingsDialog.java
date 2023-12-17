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
import com.cburch.logisim.fpga.data.SevenSegmentScanningDriving;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.std.io.LedArrayGenericHdlGeneratorFactory;
import com.cburch.logisim.std.io.SevenSegment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    var gbc = new GridBagConstraints();
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
        gbc.gridy = oldY;
      }
      final var LocText = new JLabel(S.get("FpgaIoLocation", PinLabels.get(i)));
      gbc.gridx = 0 + offset;
      gbc.gridy++;
      pinPanel.add(LocText, gbc);
      gbc.gridx = 1 + offset;
      pinPanel.add(LocInputs.get(i), gbc);
      maxY = Math.max(gbc.gridy, maxY);
    }
  }
  
  private static void updateScanningRequirements(
      int nrOfDigits,
      int nrOfDecodedBits,
      char driveMode,
      FpgaIoInformationContainer info,
      JPanel pinPanel,
      ArrayList<JTextField> LocInputs,
      ArrayList<String> oldLocations,
      HashMap<Integer, Integer> NrOfPins) {
    var pinLabels = new ArrayList<String>();
    final var isDecoded = driveMode == SevenSegmentScanningDriving.SEVEN_SEG_DECODED;
    decodedBits.setVisible(isDecoded);
    decodedString.setVisible(isDecoded);
    if (isDecoded) {
      final var nrOfDecodeBits = Math.max((int) Math.ceil(Math.log(nrOfDigits) / Math.log(2.0)), nrOfDecodedBits);
      if (nrOfDecodedBits < nrOfDecodeBits) {
        decodedBits.setSelectedIndex(nrOfDecodeBits - 1);
        info.setNrOfColumns(nrOfDecodeBits);
      }
    }
    NrOfPins.clear();
    NrOfPins.put(INPUT_ID, 0);
    NrOfPins.put(IO_ID, 0);
    var nrOfPins = 8;
    pinLabels.addAll(SevenSegment.getLabels());
    final var nrOfControlPins = (driveMode == SevenSegmentScanningDriving.SEVEN_SEG_DECODED) 
        ? Math.max((int) Math.ceil(Math.log(nrOfDigits) / Math.log(2.0)), nrOfDecodedBits) : nrOfDigits;
    nrOfPins += nrOfControlPins;
    final var pinName = (driveMode == SevenSegmentScanningDriving.SEVEN_SEG_DECODED) ? "A" : "Seg";
    for (var contrPin = 0; contrPin < nrOfControlPins; contrPin++) {
      pinLabels.add(String.format("%s%d", pinName, contrPin));
    }
    NrOfPins.put(OUTPUT_ID, nrOfPins);
    buildPinTable(nrOfPins,
        IoComponentTypes.SevenSegmentScanning,
        pinPanel,
        LocInputs,
        pinLabels,
        oldLocations);
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
      case LedArrayDriving.LED_DEFAULT -> {
        nrOfPins = nrOfRows * nrOfColumns;
        for (var row = 0; row < nrOfRows; row++) {
          for (var col = 0; col < nrOfColumns; col++)
            pinLabels.add("Row_" + row + "_Col_" + col);
        }
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING -> {
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
      case LedArrayDriving.LED_COLUMN_SCANNING -> {
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
      case LedArrayDriving.RGB_DEFAULT -> {
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
      case LedArrayDriving.RGB_ROW_SCANNING -> {
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
      case LedArrayDriving.RGB_COLUMN_SCANNING -> {
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
      default -> {
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
    var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 0;
    gbc.gridx = 0;
    rectPanel.add(new JLabel(S.get("FpgaIoXpos")), gbc);
    gbc.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoYpos")), gbc);
    gbc.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoWidth")), gbc);
    gbc.gridy++;
    rectPanel.add(new JLabel(S.get("FpgaIoHeight")), gbc);
    gbc.gridx = 1;
    for (gbc.gridy = 0; gbc.gridy < 4; gbc.gridy++)
      rectPanel.add(rectLocations.get(gbc.gridy), gbc);
    return rectPanel;
  }
  
  private static JComboBox<Integer> decodedBits;
  private static JLabel decodedString;

  public static void getSimpleInformationDialog(Boolean deleteButton, IoComponentsInformation IOcomps, FpgaIoInformationContainer info) {
    final var nrOfPins = new HashMap<Integer, Integer>();
    final var selWindow = new JDialog(IOcomps.getParentFrame(), info.getType() + " " + S.get("FpgaIoProperties"));
    final var contents = new JPanel();
    final var driveInput = new JComboBox<>(DriveStrength.BEHAVIOR_STRINGS);
    final var pullInput = new JComboBox<>(PullBehaviors.BEHAVIOR_STRINGS);
    final var activeInput = new JComboBox<>(PinActivity.BEHAVIOR_STRINGS);
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
    final var scanningPanel = new JPanel();
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
            case "inputSize" -> {
              var nr = (int) inputSize.getSelectedItem();
              nrOfPins.put(INPUT_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getInputLabel(nr, i, myType));
              buildPinTable(nr, myType, inputsPanel, locInputs, pinLabels, oldInputLocations);
              selWindow.pack();
              return;
            }
            case "outputSize" -> {
              var nr = (int) outputSize.getSelectedItem();
              nrOfPins.put(OUTPUT_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
              buildPinTable(nr, myType, outputsPanel, locOutputs, pinLabels, oldOutputLocations);
              selWindow.pack();
              return;
            }
            case "ioSize" -> {
              var nr = (int) ioSize.getSelectedItem();
              nrOfPins.put(IO_ID, nr);
              pinLabels.clear();
              for (var i = 0; i < nr; i++)
                pinLabels.add(IoComponentTypes.getIoLabel(nr, i, myType));
              buildPinTable(nr, myType, ioPanel, locIos, pinLabels, oldIoLocations);
              selWindow.pack();
              return;
            }
            case "LedArray" -> {
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
            case "ScanningArray" -> {
              final var driveMode = (char) eEncoding.getSelectedIndex();
              info.setNrOfRows(rowSize.getSelectedIndex() + 2);
              info.setArrayDriveMode(driveMode);
              info.setNrOfColumns(driveMode == SevenSegmentScanningDriving.SEVEN_SEG_DECODED ? decodedBits.getSelectedIndex() + 1 : -1);
              updateScanningRequirements(
                      info.getNrOfRows(),
                      info.getNrOfColumns(),
                      info.getArrayDriveMode(),
                      info,
                      outputsPanel,
                      locOutputs,
                      oldOutputLocations,
                      nrOfPins);
              selWindow.pack();
              return;
            }
            case "cancel" -> info.setType(IoComponentTypes.Unknown);
            case "delete" -> info.setToBeDeleted();
          }
          selWindow.setVisible(false);
          selWindow.dispose();
        };
    contents.setLayout(new GridBagLayout());
    final var gbc = new GridBagConstraints();
    gbc.gridy = -1;
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
      gbc.fill = GridBagConstraints.NORTH;
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(getRectPanel(rectLocations), gbc);
      gbc.gridwidth = 1;
    }
    gbc.fill = GridBagConstraints.HORIZONTAL;
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
        case IoComponentTypes.ROTATION_CW_90 -> {
          mapRotation.setSelectedIndex(1);
          break;
        }
        case IoComponentTypes.ROTATION_CCW_90 -> {
          mapRotation.setSelectedIndex(2);
          break;
        }
        default -> {
          mapRotation.setSelectedIndex(0);
          break;
        }
      }
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
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
      final var arr = new GridBagConstraints();   // FIXME: should be gbc (duplicate name)
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
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
    }
    if (myType.equals(IoComponentTypes.SevenSegmentScanning)) {
      final var panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.setBorder(BorderFactory.createTitledBorder(
              BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaScanningDefinition")));
      scanningPanel.setLayout(new GridBagLayout());
      rowSize.removeAll();
      for (var nrOfSegments = 2; nrOfSegments < 16; nrOfSegments++) 
        rowSize.addItem(nrOfSegments);
      rowSize.setSelectedIndex(info.getNrOfRows() - 2);
      eEncoding.removeAll();
      for (var val : SevenSegmentScanningDriving.getDisplayStrings()) {
        eEncoding.addItem(val);
      }
      eEncoding.setSelectedIndex(info.getArrayDriveMode());
      decodedBits = new JComboBox<Integer>();
      for (var nrOfDecodedBits = 1; nrOfDecodedBits < 6; nrOfDecodedBits++) {
        decodedBits.addItem(nrOfDecodedBits);
      }
      if (info.getArrayDriveMode() != SevenSegmentScanningDriving.SEVEN_SEG_DECODED) {
        decodedBits.setVisible(false);
      } else {
        decodedBits.setSelectedIndex(info.getNrOfColumns() - 1);
      }
      decodedString = new JLabel(S.get("FpgaNrOfDecodeBits"));
      decodedBits.setActionCommand("ScanningArray");
      decodedBits.addActionListener(actionListener);
      rowSize.setActionCommand("ScanningArray");
      rowSize.addActionListener(actionListener);
      eEncoding.setActionCommand("ScanningArray");
      eEncoding.addActionListener(actionListener);
      final var arr = new GridBagConstraints();   // FIXME: should be gbc (duplicate name)
      arr.gridx = 0;
      arr.gridy = 0;
      panel.add(new JLabel(S.get("FpgaScanningDriving")), arr);
      arr.gridx++;
      panel.add(eEncoding, arr);
      arr.gridy++;
      panel.add(rowSize, arr);
      arr.gridx--;
      panel.add(new JLabel(S.get("FpgaNrOfSegments")), arr);
      arr.gridy++;
      panel.add(decodedString, arr);
      arr.gridx++;
      panel.add(decodedBits, arr);
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
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
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
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
      if (myType != IoComponentTypes.LedArray && myType != IoComponentTypes.SevenSegmentScanning) {
        pinLabels.clear();
        final var nr = nrOfPins.get(OUTPUT_ID);
        for (var i = 0; i < nr; i++) pinLabels.add(IoComponentTypes.getOutputLabel(nr, 0, 0, i, myType));
        buildPinTable(nrOfPins.get(OUTPUT_ID), myType, outputsPanel, locOutputs, pinLabels, oldOutputLocations);
      } else if (myType == IoComponentTypes.LedArray) {
        updateLedArrayRequirements(
            info.getNrOfRows(),
            info.getNrOfColumns(),
            info.getArrayDriveMode(),
            outputsPanel,
            locOutputs,
            oldOutputLocations,
            nrOfPins);
      } else {
        updateScanningRequirements(
            info.getNrOfRows(),
            info.getNrOfColumns(),
            info.getArrayDriveMode(),
            info,
            outputsPanel,
            locOutputs,
            oldOutputLocations,
            nrOfPins);
      }
      panel.add(outputsPanel, BorderLayout.CENTER);
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
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
      gbc.gridy++;
      gbc.gridwidth = 2;
      contents.add(panel, gbc);
      gbc.gridwidth = 1;
    }

    final var LabText = new JLabel(S.get("FpgaIoLabel"));
    gbc.gridy++;
    gbc.gridx = 0;
    contents.add(LabText, gbc);
    var LabelInput = new JTextField(6);
    LabelInput.setText(info.getLabel());
    gbc.gridx = 1;
    contents.add(LabelInput, gbc);

    final var StandardText = new JLabel(S.get("FpgaIoStandard"));
    gbc.gridy++;
    gbc.gridx = 0;
    contents.add(StandardText, gbc);
    final var StandardInput = new JComboBox<>(IoStandards.BEHAVIOR_STRINGS);
    if (info.getIoStandard() != IoStandards.UNKNOWN)
      StandardInput.setSelectedIndex(info.getIoStandard());
    else StandardInput.setSelectedIndex(IOcomps.getDefaultStandard());
    gbc.gridx = 1;
    contents.add(StandardInput, gbc);

    if (IoComponentTypes.OUTPUT_COMPONENT_SET.contains(myType)) {
      final var DriveText = new JLabel(S.get("FpgaIoStrength"));
      gbc.gridy++;
      gbc.gridx = 0;
      contents.add(DriveText, gbc);
      if (info.getDrive() != DriveStrength.UNKNOWN) driveInput.setSelectedIndex(info.getDrive());
      else driveInput.setSelectedIndex(IOcomps.getDefaultDriveStrength());
      gbc.gridx = 1;
      contents.add(driveInput, gbc);
    }

    if (IoComponentTypes.INPUT_COMPONENT_SET.contains(myType)) {
      final var PullText = new JLabel(S.get("FpgaIoPull"));
      gbc.gridy++;
      gbc.gridx = 0;
      contents.add(PullText, gbc);
      if (info.getPullBehavior() != PullBehaviors.UNKNOWN)
        pullInput.setSelectedIndex(info.getPullBehavior());
      else pullInput.setSelectedIndex(IOcomps.getDefaultPullSelection());
      gbc.gridx = 1;
      contents.add(pullInput, gbc);
    }

    if (!IoComponentTypes.IN_OUT_COMPONENT_SET.contains(myType)) {
      final var ActiveText = new JLabel(S.get("FpgaIoActivity", myType));
      gbc.gridy++;
      gbc.gridx = 0;
      contents.add(ActiveText, gbc);
      if (info.getActivityLevel() != PinActivity.Unknown)
        activeInput.setSelectedIndex(info.getActivityLevel());
      else activeInput.setSelectedIndex(IOcomps.getDefaultActivity());
      gbc.gridx = 1;
      contents.add(activeInput, gbc);
    }
    if (deleteButton) {
      final var delButton = new JButton();
      delButton.setActionCommand("delete");
      delButton.addActionListener(actionListener);
      delButton.setText(S.get("FpgaIoDelete"));
      gbc.gridwidth = 2;
      gbc.gridx = 0;
      gbc.gridy++;
      contents.add(delButton, gbc);
      gbc.gridwidth = 1;
    }
    final var OkayButton = new JButton(S.get("FpgaBoardDone"));
    OkayButton.setActionCommand("done");
    OkayButton.addActionListener(actionListener);
    gbc.gridx = 1;
    gbc.gridy++;
    contents.add(OkayButton, gbc);

    final var CancelButton = new JButton(S.get("FpgaBoardCancel"));
    CancelButton.setActionCommand("cancel");
    CancelButton.addActionListener(actionListener);
    gbc.gridx = 0;
    contents.add(CancelButton, gbc);
    // FIXME: Find better solution to handle window close button
    selWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        CancelButton.doClick();
      }
    });
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
    final var gbc = new GridBagConstraints();
    /* Here the clock related settings are defined */
    final var ClockPanel = new JPanel();
    ClockPanel.setLayout(new GridBagLayout());
    ClockPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardClkProp")));

    final var FreqText = new JLabel(S.get("FpgaBoardClkFreq"));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    ClockPanel.add(FreqText, gbc);

    final var FreqPanel = new JPanel();
    final var FreqLayout = new GridBagLayout();
    FreqPanel.setLayout(FreqLayout);

    final var FreqInput = new JTextField(10);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    if (TheBoard.fpga.isFpgaInfoPresent())
      FreqInput.setText(Integer.toString(getFrequencyValue(TheBoard.fpga.getClockFrequency())));
    FreqPanel.add(FreqInput, gbc);

    String[] freqStrs = {"Hz", "kHz", "MHz"};
    final var StandardInput = new JComboBox<>(freqStrs);
    StandardInput.setSelectedIndex(2);
    gbc.gridx = 1;
    if (TheBoard.fpga.isFpgaInfoPresent())
      StandardInput.setSelectedIndex(getFrequencyIndex(TheBoard.fpga.getClockFrequency()));
    FreqPanel.add(StandardInput, gbc);

    ClockPanel.add(FreqPanel, gbc);

    final var LocText = new JLabel(S.get("FpgaBoardClkLoc"));
    gbc.gridy = 1;
    gbc.gridx = 0;
    ClockPanel.add(LocText, gbc);

    final var LocInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) LocInput.setText(TheBoard.fpga.getClockPinLocation());
    gbc.gridx = 1;
    ClockPanel.add(LocInput, gbc);

    final var PullText = new JLabel(S.get("FpgaBoardClkPul"));
    gbc.gridy = 2;
    gbc.gridx = 0;
    ClockPanel.add(PullText, gbc);

    final var PullInput = new JComboBox<>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      PullInput.setSelectedIndex(TheBoard.fpga.getClockPull());
    } else PullInput.setSelectedIndex(0);
    gbc.gridx = 1;
    ClockPanel.add(PullInput, gbc);

    final var StandardText = new JLabel(S.get("FpgaBoardClkStd"));
    gbc.gridy = 3;
    gbc.gridx = 0;
    ClockPanel.add(StandardText, gbc);

    final var StdInput = new JComboBox<>(IoStandards.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      StdInput.setSelectedIndex(TheBoard.fpga.getClockStandard());
    } else StdInput.setSelectedIndex(0);
    gbc.gridx = 1;
    ClockPanel.add(StdInput, gbc);

    /* Here the FPGA related settings are defined */
    final var fpgaPanel = new JPanel();
    fpgaPanel.setLayout(new GridBagLayout());
    fpgaPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardFpgaProp")));

    final var VendorText = new JLabel(S.get("FpgaBoardFpgaVend"));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    fpgaPanel.add(VendorText, gbc);

    final var vendorInput = new JComboBox<>(VendorSoftware.VENDORS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      vendorInput.setSelectedIndex(TheBoard.fpga.getVendor());
    } else vendorInput.setSelectedIndex(0);
    gbc.gridx = 1;
    fpgaPanel.add(vendorInput, gbc);

    final var familyText = new JLabel(S.get("FpgaBoardFpgaFam"));
    gbc.gridy = 1;
    gbc.gridx = 0;
    fpgaPanel.add(familyText, gbc);

    final var familyInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) familyInput.setText(TheBoard.fpga.getTechnology());
    gbc.gridx = 1;
    fpgaPanel.add(familyInput, gbc);

    final var PartText = new JLabel(S.get("FpgaBoardFpgaPart"));
    gbc.gridy = 2;
    gbc.gridx = 0;
    fpgaPanel.add(PartText, gbc);

    final var partInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) partInput.setText(TheBoard.fpga.getPart());
    gbc.gridx = 1;
    fpgaPanel.add(partInput, gbc);

    final var BoxText = new JLabel(S.get("FpgaBoardFpgaPack"));
    gbc.gridy = 3;
    gbc.gridx = 0;
    fpgaPanel.add(BoxText, gbc);

    final var boxInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) boxInput.setText(TheBoard.fpga.getPackage());
    gbc.gridx = 1;
    fpgaPanel.add(boxInput, gbc);

    final var speedText = new JLabel(S.get("FpgaBoardFpgaSG"));
    gbc.gridy = 4;
    gbc.gridx = 0;
    fpgaPanel.add(speedText, gbc);

    final var speedInput = new JTextField();
    if (TheBoard.fpga.isFpgaInfoPresent()) speedInput.setText(TheBoard.fpga.getSpeedGrade());
    gbc.gridx = 1;
    fpgaPanel.add(speedInput, gbc);

    final var unusedPinsText = new JLabel(S.get("FpgaBoardPinUnused"));
    gbc.gridy = 5;
    gbc.gridx = 0;
    fpgaPanel.add(unusedPinsText, gbc);

    final var unusedPinsInput = new JComboBox<>(PullBehaviors.BEHAVIOR_STRINGS);
    if (TheBoard.fpga.isFpgaInfoPresent()) {
      unusedPinsInput.setSelectedIndex(TheBoard.fpga.getUnusedPinsBehavior());
    } else unusedPinsInput.setSelectedIndex(0);
    gbc.gridx = 1;
    fpgaPanel.add(unusedPinsInput, gbc);

    /* JTAG related Settings */
    final var jtagPanel = new JPanel();
    jtagPanel.setLayout(new GridBagLayout());
    jtagPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardJtagProp")));

    final var posText = new JLabel(S.get("FpgaBoardJtagLoc"));
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    jtagPanel.add(posText, gbc);

    final var PosInput = new JTextField(5);
    PosInput.setText("1");
    if (TheBoard.fpga.isFpgaInfoPresent())
      PosInput.setText(Integer.toString(TheBoard.fpga.getFpgaJTAGChainPosition()));
    gbc.gridx = 1;
    jtagPanel.add(PosInput, gbc);

    final var FlashPosText = new JLabel(S.get("FpgaBoardFlashLoc"));
    gbc.gridy = 1;
    gbc.gridx = 0;
    jtagPanel.add(FlashPosText, gbc);

    final var flashPosInput = new JTextField(5);
    flashPosInput.setText("2");
    if (TheBoard.fpga.isFpgaInfoPresent())
      flashPosInput.setText(Integer.toString(TheBoard.fpga.getFlashJTAGChainPosition()));
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    jtagPanel.add(flashPosInput, gbc);

    /* misc settings */
    final var miscPanel = new JPanel();
    miscPanel.setLayout(new GridBagLayout());
    miscPanel.setBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2, true), S.get("FpgaBoardMiscProp")));

    final var flashName = new JLabel(S.get("FpgaBoardFlashType"));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    miscPanel.add(flashName, gbc);

    final var flashNameInput = new JTextField("");
    if (TheBoard.fpga.isFpgaInfoPresent()) flashNameInput.setText(TheBoard.fpga.getFlashName());
    gbc.gridx = 1;
    miscPanel.add(flashNameInput, gbc);

    final var usbTmc = new JCheckBox(S.get("FpgaBoardUSBTMC"));
    usbTmc.setSelected(false);
    if (TheBoard.fpga.isFpgaInfoPresent()) usbTmc.setSelected(TheBoard.fpga.isUsbTmcDownloadRequired());
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    miscPanel.add(usbTmc, gbc);

    final var dialogLayout = new GridBagLayout();
    selWindow.setLayout(dialogLayout);
    abort = false;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(ClockPanel, gbc);
    gbc.gridx = 1;
    selWindow.add(fpgaPanel, gbc);
    gbc.gridx = 0;
    gbc.gridy = 1;
    selWindow.add(jtagPanel, gbc);
    gbc.gridx = 1;
    selWindow.add(miscPanel, gbc);

    final var cancelButton = new JButton(S.get("FpgaBoardCancel"));
    cancelButton.addActionListener(actionListener);
    cancelButton.setActionCommand("cancel");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(cancelButton, gbc);
    // FIXME: Find better solution to handle window close button
    selWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        cancelButton.doClick();
      }
    });

    final var saveButton = new JButton(S.get("FpgaBoardDone"));
    saveButton.addActionListener(actionListener);
    saveButton.setActionCommand("save");
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    selWindow.add(saveButton, gbc);

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
          case -2 -> {
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFreqError"));
          }
          case -1 -> {
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardFracError"));
          }
          case 0 -> {
            saveSettings = false;
            DialogNotification.showDialogNotification(panel, "Error", S.get("FpgaBoardClkReq"));
          }
          default -> {
          }
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
