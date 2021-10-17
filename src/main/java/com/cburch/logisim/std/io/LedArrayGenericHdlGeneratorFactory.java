/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;

public class LedArrayGenericHdlGeneratorFactory {
  public static String LedArrayOutputs = "externalLeds";
  public static String LedArrayRedOutputs = "externalRedLeds";
  public static String LedArrayGreenOutputs = "externalGreenLeds";
  public static String LedArrayBlueOutputs = "externalBlueLeds";
  public static String LedArrayRowOutputs = "rowLeds";
  public static String LedArrayRowRedOutputs = "rowRedLeds";
  public static String LedArrayRowGreenOutputs = "rowGreenLeds";
  public static String LedArrayRowBlueOutputs = "rowBlueLeds";
  public static String LedArrayRowAddress = "rowAddress";
  public static String LedArrayColumnOutputs = "columnLeds";
  public static String LedArrayColumnRedOutputs = "columnRedLeds";
  public static String LedArrayColumnGreenOutputs = "columnGreenLeds";
  public static String LedArrayColumnBlueOutputs = "columnBlueLeds";
  public static String LedArrayColumnAddress = "columnAddress";
  public static String LedArrayInputs = "internalLeds";
  public static String LedArrayRedInputs = "internalRedLeds";
  public static String LedArrayGreenInputs = "internalGreenLeds";
  public static String LedArrayBlueInputs = "internalBlueLeds";

  public static AbstractHdlGeneratorFactory getSpecificHDLGenerator(String type) {
    final var typeId = LedArrayDriving.getId(type);
    return switch (typeId) {
      case LedArrayDriving.LED_DEFAULT -> new LedArrayLedDefaultHdlGeneratorFactory();
      case LedArrayDriving.LED_ROW_SCANNING -> new LedArrayRowScanningHdlGeneratorFactory();
      case LedArrayDriving.LED_COLUMN_SCANNING -> new LedArrayColumnScanningHdlGeneratorFactory();
      case LedArrayDriving.RGB_DEFAULT -> new RgbArrayLedDefaultHdlGeneratorFactory();
      case LedArrayDriving.RGB_ROW_SCANNING -> new RgbArrayRowScanningHdlGeneratorFactory();
      case LedArrayDriving.RGB_COLUMN_SCANNING -> new RgbArrayColumnScanningHdlGeneratorFactory();
      default -> null;
    };
  }

  public static String getSpecificHDLName(char typeId) {
    return switch (typeId) {
      case LedArrayDriving.LED_DEFAULT -> LedArrayLedDefaultHdlGeneratorFactory.HDL_IDENTIFIER;
      case LedArrayDriving.LED_ROW_SCANNING -> LedArrayRowScanningHdlGeneratorFactory.HDL_IDENTIFIER;
      case LedArrayDriving.LED_COLUMN_SCANNING -> LedArrayColumnScanningHdlGeneratorFactory.HDL_IDENTIFIER;
      case LedArrayDriving.RGB_DEFAULT -> RgbArrayLedDefaultHdlGeneratorFactory.HDL_IDENTIFIER;
      case LedArrayDriving.RGB_ROW_SCANNING -> RgbArrayRowScanningHdlGeneratorFactory.HDL_IDENTIFIER;
      case LedArrayDriving.RGB_COLUMN_SCANNING -> RgbArrayColumnScanningHdlGeneratorFactory.HDL_IDENTIFIER;
      default -> null;
    };
  }

  public static String getSpecificHDLName(String type) {
    return getSpecificHDLName(LedArrayDriving.getId(type));
  }

  public static int getNrOfBitsRequired(int value) {
    final var nrBitsDouble = Math.log(value) / Math.log(2.0);
    return (int) Math.ceil(nrBitsDouble);
  }

  public static String getExternalSignalName(char typeId, int nrOfRows, int nrOfColumns, int identifier, int pinNr) {
    final var nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    final var nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT:
        return LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayOutputs, identifier, pinNr);
      case LedArrayDriving.LED_ROW_SCANNING:
        return (pinNr < nrRowAddressBits)
          ? LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowAddress, identifier, pinNr)
          : LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnOutputs, identifier, pinNr - nrRowAddressBits);
      case LedArrayDriving.LED_COLUMN_SCANNING:
        return (pinNr < nrColumnAddressBits)
            ? LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnAddress, identifier, pinNr)
            : LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowOutputs, identifier, pinNr - nrColumnAddressBits);
      case LedArrayDriving.RGB_DEFAULT: {
        final var index = pinNr % 3;
        final var col = pinNr / 3;
        return switch (col) {
          case 0 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRedOutputs, identifier, index);
          case 1 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayGreenOutputs, identifier, index);
          case 2 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayBlueOutputs, identifier, index);
          default -> "";
        };
      }
      case LedArrayDriving.RGB_ROW_SCANNING: {
        final var index = (pinNr - nrRowAddressBits) % nrOfColumns;
        final var col = (pinNr - nrRowAddressBits) / nrOfColumns;
        if (pinNr < nrRowAddressBits)
          return LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowAddress, identifier, pinNr);
        return switch (col) {
          case 0 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnRedOutputs, identifier, index);
          case 1 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnGreenOutputs, identifier, index);
          case 2 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnBlueOutputs, identifier, index);
          default -> "";
        };
      }
      case LedArrayDriving.RGB_COLUMN_SCANNING: {
        final var index = (pinNr - nrColumnAddressBits) % nrOfRows;
        final var col = (pinNr - nrColumnAddressBits) / nrOfRows;
        if (pinNr < nrColumnAddressBits)
          return LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayColumnAddress, identifier, pinNr);
        return switch (col) {
          case 0 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowRedOutputs, identifier, index);
          case 1 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowGreenOutputs, identifier, index);
          case 2 -> LineBuffer.format("{{1}}{{2}}[{{3}}]", LedArrayRowBlueOutputs, identifier, index);
          default -> "";
        };
      }
    }
    return "";
  }

  public static boolean requiresClock(char typeId) {
    return (typeId != LedArrayDriving.LED_DEFAULT) && (typeId != LedArrayDriving.RGB_DEFAULT);
  }

  public static SortedMap<String, Integer> getExternalSignals(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier) {
    final var externals = new TreeMap<String, Integer>();
    final var nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    final var nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT: {
        externals.put(String.format("%s%d", LedArrayOutputs, identifier), nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING: {
        externals.put(String.format("%s%d", LedArrayRowAddress, identifier), nrRowAddressBits);
        externals.put(String.format("%s%d", LedArrayColumnOutputs, identifier), nrOfColumns);
        break;
      }
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        externals.put(String.format("%s%d", LedArrayColumnAddress, identifier), nrColumnAddressBits);
        externals.put(String.format("%s%d", LedArrayRowOutputs, identifier), nrOfRows);
        break;
      }
      case LedArrayDriving.RGB_DEFAULT: {
        externals.put(String.format("%s%d", LedArrayRedOutputs, identifier), nrOfRows * nrOfColumns);
        externals.put(String.format("%s%d", LedArrayGreenOutputs, identifier), nrOfRows * nrOfColumns);
        externals.put(String.format("%s%d", LedArrayBlueOutputs, identifier), nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.RGB_ROW_SCANNING: {
        externals.put(String.format("%s%d", LedArrayRowAddress, identifier), nrRowAddressBits);
        externals.put(String.format("%s%d", LedArrayColumnRedOutputs, identifier), nrOfColumns);
        externals.put(String.format("%s%d", LedArrayColumnGreenOutputs, identifier), nrOfColumns);
        externals.put(String.format("%s%d", LedArrayColumnBlueOutputs, identifier), nrOfColumns);
        break;
      }
      case LedArrayDriving.RGB_COLUMN_SCANNING: {
        externals.put(String.format("%s%d", LedArrayColumnAddress, identifier), nrColumnAddressBits);
        externals.put(String.format("%s%d", LedArrayRowRedOutputs, identifier), nrOfRows);
        externals.put(String.format("%s%d", LedArrayRowGreenOutputs, identifier), nrOfRows);
        externals.put(String.format("%s%d", LedArrayRowBlueOutputs, identifier), nrOfRows);
        break;
      }
    }
    return externals;
  }

  public static Map<String, Integer> getInternalSignals(char typeId, int nrOfRows, int nrOfColumns, int identifier) {
    final var wires = new TreeMap<String, Integer>();
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT:
      case LedArrayDriving.LED_ROW_SCANNING:
      case LedArrayDriving.LED_COLUMN_SCANNING:
        wires.put(String.format("s_%s%d", LedArrayInputs, identifier), nrOfRows * nrOfColumns);
        break;
      case LedArrayDriving.RGB_DEFAULT:
      case LedArrayDriving.RGB_ROW_SCANNING:
      case LedArrayDriving.RGB_COLUMN_SCANNING:
        wires.put(String.format("s_%s%d", LedArrayRedInputs, identifier), nrOfRows * nrOfColumns);
        wires.put(String.format("s_%s%d", LedArrayGreenInputs, identifier), nrOfRows * nrOfColumns);
        wires.put(String.format("s_%s%d", LedArrayBlueInputs, identifier), nrOfRows * nrOfColumns);
        break;
    }
    return wires;
  }

  public static List<String> getComponentMap(char typeId, int nrOfRows, int nrOfColumns, int identifier, long FpgaClockFrequency, boolean isActiveLow) {
    final var componentMap = LineBuffer.getBuffer()
            .add(Hdl.isVhdl()
                ? LineBuffer.format("array{{1}} : {{2}}", identifier, getSpecificHDLName(typeId))
                : getSpecificHDLName(typeId));
    switch (typeId) {
      case LedArrayDriving.RGB_DEFAULT:
      case LedArrayDriving.LED_DEFAULT:
        componentMap.add(LedArrayLedDefaultHdlGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow).getWithIndent());
        break;
      case LedArrayDriving.RGB_COLUMN_SCANNING:
      case LedArrayDriving.LED_COLUMN_SCANNING:
        componentMap.add(LedArrayColumnScanningHdlGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow).getWithIndent());
        break;
      case LedArrayDriving.RGB_ROW_SCANNING:
      case LedArrayDriving.LED_ROW_SCANNING:
        componentMap.add(LedArrayRowScanningHdlGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow).getWithIndent());
        break;
    }
    if (Hdl.isVerilog()) componentMap.add("   array{{1}}", identifier);
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT: {
        componentMap.add(LedArrayLedDefaultHdlGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RGB_DEFAULT: {
        componentMap.add(RgbArrayLedDefaultHdlGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING: {
        componentMap.add(LedArrayRowScanningHdlGeneratorFactory.getPortMap(identifier).getWithIndent());
        break;
      }
      case LedArrayDriving.RGB_ROW_SCANNING: {
        componentMap.add(RgbArrayRowScanningHdlGeneratorFactory.getPortMap(identifier).getWithIndent());
        break;
      }
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        componentMap.add(LedArrayColumnScanningHdlGeneratorFactory.getPortMap(identifier).getWithIndent());
        break;
      }
      case LedArrayDriving.RGB_COLUMN_SCANNING: {
        componentMap.add(RgbArrayColumnScanningHdlGeneratorFactory.getPortMap(identifier).getWithIndent());
        break;
      }
    }
    return componentMap.empty().get();
  }

  public static List<String> getArrayConnections(FpgaIoInformationContainer array, int id) {
    final var connections = new ArrayList<String>();
    connections.addAll(
        switch (array.getArrayDriveMode()) {
          case LedArrayDriving.LED_DEFAULT, LedArrayDriving.LED_ROW_SCANNING, LedArrayDriving.LED_COLUMN_SCANNING -> getLedArrayConnections(array, id);
          case LedArrayDriving.RGB_DEFAULT, LedArrayDriving.RGB_ROW_SCANNING, LedArrayDriving.RGB_COLUMN_SCANNING -> getRGBArrayConnections(array, id);
          default -> throw new IllegalStateException("Unexpected value: " + array.getArrayDriveMode());
        });
    connections.add("");
    return connections;
  }

  public static List<String> getLedArrayConnections(FpgaIoInformationContainer info, int id) {
    final var connections = LineBuffer.getHdlBuffer();
    final var wires = new HashMap<String, String>();
    for (var pin = 0; pin < info.getNrOfPins(); pin++) {
      final var led = LineBuffer.formatHdl("s_{{1}}{{2}}{{<}}{{3}}{{>}}", LedArrayInputs, id, pin);
      if (!info.isPinMapped(pin)) {
        wires.put(led, Hdl.zeroBit());
      } else {
        wires.put(led, info.getPinMap(pin).getHdlSignalName(info.getMapPin(pin)));
      }
    }
    Hdl.addAllWiresSorted(connections, wires);
    return connections.get();
  }

  public static List<String> getRGBArrayConnections(FpgaIoInformationContainer array, int id) {
    final var wires = new HashMap<String, String>();
    final var connections =
        LineBuffer.getHdlBuffer()
           .pair("id", id)
           .pair("insR", LedArrayRedInputs)
           .pair("insG", LedArrayGreenInputs)
           .pair("insB", LedArrayBlueInputs);

    for (var pin = 0; pin < array.getNrOfPins(); pin++) {
      final var red = LineBuffer.formatHdl("s_{{1}}{{2}}{{<}}{{3}}{{>}}", LedArrayRedInputs, id, pin);
      final var green = LineBuffer.formatHdl("s_{{1}}{{2}}{{<}}{{3}}{{>}}", LedArrayGreenInputs, id, pin);
      final var blue = LineBuffer.formatHdl("s_{{1}}{{2}}{{<}}{{3}}{{>}}", LedArrayBlueInputs, id, pin);
      if (!array.isPinMapped(pin)) {
        wires.put(red, Hdl.zeroBit());
        wires.put(green, Hdl.zeroBit());
        wires.put(blue, Hdl.zeroBit());
      } else {
        final var map = array.getPinMap(pin);
        if (map.getComponentFactory() instanceof RgbLed) {
          wires.put(red, map.getHdlSignalName(RgbLed.RED));
          wires.put(green, map.getHdlSignalName(RgbLed.GREEN));
          wires.put(blue, map.getHdlSignalName(RgbLed.BLUE));
        } else if (map.getAttributeSet().containsAttribute(IoLibrary.ATTR_ON_COLOR)
            && map.getAttributeSet().containsAttribute(IoLibrary.ATTR_OFF_COLOR)) {

          final var onColor = map.getAttributeSet().getValue(IoLibrary.ATTR_ON_COLOR);
          final var offColor = map.getAttributeSet().getValue(IoLibrary.ATTR_OFF_COLOR);
          final var rOn = onColor.getRed();
          final var gOn = onColor.getGreen();
          final var bOn = onColor.getBlue();
          final var rOff = offColor.getRed();
          final var gOff = offColor.getGreen();
          final var bOff = offColor.getBlue();
          final var pinName = map.getHdlSignalName(array.getMapPin(pin));
          wires.putAll(getColorMap(red, rOn, rOff, pinName));
          wires.putAll(getColorMap(green, gOn, gOff, pinName));
          wires.putAll(getColorMap(blue, bOn, bOff, pinName));
        } else {
          final var pinName = map.getHdlSignalName(array.getMapPin(pin));
          wires.put(red, pinName);
          wires.put(green, pinName);
          wires.put(blue, pinName);
        }
      }
    }
    Hdl.addAllWiresSorted(connections, wires);
    return connections.get();
  }

  private static Map<String, String> getColorMap(String dest, int onColor, int offColor, String source) {
    final var onBit = (onColor > 128);
    final var offBit = (offColor > 128);
    final var result = new HashMap<String, String>();
    if (onBit == offBit) result.put(dest, onBit ? Hdl.oneBit() : Hdl.zeroBit());
    else if (onBit) result.put(dest, source);
    else result.put(dest, LineBuffer.format("{{1}}{{2}}", Hdl.notOperator(), source));
    return result;
  }

  public static LineBuffer getGenericPortMapAlligned(Map<String, String> generics, boolean isGeneric) {
    var preamble = Hdl.isVhdl() ? LineBuffer.formatVhdl("{{port}} {{map}} ( ") : "( ";
    if (isGeneric) preamble = Hdl.isVhdl() ? LineBuffer.formatVhdl("{{generic}} {{map}} ( ") : "#( ";
    final var contents = LineBuffer.getHdlBuffer();
    var maxNameLength = 0;
    var first = true;
    var nrOfGenerics = 0;
    for (final var generic : generics.keySet()) {
      maxNameLength = Math.max(maxNameLength, generic.length());
      nrOfGenerics++;
    }
    for (final var generic : generics.keySet()) {
      nrOfGenerics--;
      final var intro = first ? preamble : " ".repeat(preamble.length());
      final var map = Hdl.isVhdl() ? LineBuffer.formatHdl("{{1}}{{2}} => {{3}}", generic,
          " ".repeat(maxNameLength - generic.length()), generics.get(generic))
          : LineBuffer.formatHdl(".{{1}}({{2}})", generic, generics.get(generic));
      final var end = (nrOfGenerics == 0) ? isGeneric ? " )" : " );" : ",";
      contents.add(LineBuffer.format("{{1}}{{2}}{{3}}", intro, map, end));
      first = false;
    }
    return contents;
  }

}
