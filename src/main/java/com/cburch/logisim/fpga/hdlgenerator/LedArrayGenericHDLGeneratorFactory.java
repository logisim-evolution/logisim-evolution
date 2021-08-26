package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.std.io.RgbLed;

public class LedArrayGenericHDLGeneratorFactory {
  public static String LedArrayOutputs = "externalLeds";
  public static String LedArrayRedOutputs = "externalLeds";
  public static String LedArrayGreenOutputs = "externalLeds";
  public static String LedArrayBlueOutputs = "externalLeds";
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

  public static AbstractHDLGeneratorFactory getSpecificHDLGenerator(String type) {
    final var typeId = LedArrayDriving.getId(type);
    return switch (typeId) {
      case LedArrayDriving.LED_DEFAULT -> new LedArrayLedDefaultHDLGeneratorFactory();
      case LedArrayDriving.LED_ROW_SCANNING -> new LedArrayRowScanningHDLGeneratorFactory();
      case LedArrayDriving.LED_COLUMN_SCANNING -> new LedArrayColumnScanningHDLGeneratorFactory();
      case LedArrayDriving.RgbDefault  -> new RGBArrayLedDefaultHDLGeneratorFactory();
      case LedArrayDriving.RgbRowScanning  -> new RGBArrayRowScanningHDLGeneratorFactory();
      case LedArrayDriving.RgbColumnScanning  -> new RGBArrayColumnScanningHDLGeneratorFactory();
      default -> null;
    };
  }

  public static String getSpecificHDLName(char typeId) {
    return switch (typeId) {
      case LedArrayDriving.LED_DEFAULT -> LedArrayLedDefaultHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.LED_ROW_SCANNING -> LedArrayRowScanningHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.LED_COLUMN_SCANNING -> LedArrayColumnScanningHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.RgbDefault -> RGBArrayLedDefaultHDLGeneratorFactory.RGBArrayName;
      case LedArrayDriving.RgbRowScanning -> RGBArrayRowScanningHDLGeneratorFactory.RGBArrayName;
      case LedArrayDriving.RgbColumnScanning -> RGBArrayColumnScanningHDLGeneratorFactory.RGBArrayName;
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
        return LedArrayOutputs + identifier + "[" + pinNr + "]";
      case LedArrayDriving.LED_ROW_SCANNING:
        return (pinNr < nrRowAddressBits)
          ? LedArrayRowAddress + identifier + "[" + pinNr + "]"
          : LedArrayColumnOutputs + identifier + "[" + (pinNr - nrRowAddressBits) + "]";
      case LedArrayDriving.LED_COLUMN_SCANNING:
        return (pinNr < nrColumnAddressBits)
            ? LedArrayColumnAddress + identifier + "[" + pinNr + "]"
            : LedArrayRowOutputs + identifier + "[" + (pinNr - nrColumnAddressBits) + "]";
      case LedArrayDriving.RgbDefault : {
        final var index = pinNr % 3;
        final var col = pinNr / 3;
        return switch (col) {
          case 0 -> LedArrayRedOutputs + identifier + "[" + index + "]";
          case 1 -> LedArrayGreenOutputs + identifier + "[" + index + "]";
          case 2 -> LedArrayBlueOutputs + identifier + "[" + index + "]";
          default -> "";
        };
      }
      case LedArrayDriving.RgbRowScanning : {
        final var index = (pinNr - nrRowAddressBits) % nrOfColumns;
        final var col = (pinNr - nrRowAddressBits) / nrOfColumns;
        if (pinNr < nrRowAddressBits)
          return LedArrayRowAddress + identifier + "[" + pinNr + "]";
        return switch (col) {
          case 0 -> LedArrayColumnRedOutputs + identifier + "[" + index + "]";
          case 1 -> LedArrayColumnGreenOutputs + identifier + "[" + index + "]";
          case 2 -> LedArrayColumnBlueOutputs + identifier + "[" + index + "]";
          default -> "";
        };
      }
      case LedArrayDriving.RgbColumnScanning : {
        final var index = (pinNr - nrColumnAddressBits) % nrOfRows;
        final var col = (pinNr - nrColumnAddressBits) / nrOfRows;
        if (pinNr < nrColumnAddressBits)
          return LedArrayColumnAddress + identifier + "[" + pinNr + "]";
        return switch (col) {
          case 0 -> LedArrayRowRedOutputs + identifier + "[" + index + "]";
          case 1 -> LedArrayRowGreenOutputs + identifier + "[" + index + "]";
          case 2 -> LedArrayRowBlueOutputs + identifier + "[" + index + "]";
          default -> "";
        };
      }
    }
    return "";
  }

  public static boolean requiresClock(char typeId) {
    return (typeId != LedArrayDriving.LED_DEFAULT) && (typeId != LedArrayDriving.RgbDefault);
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
        externals.put(LedArrayOutputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING: {
        externals.put(LedArrayRowAddress + identifier, nrRowAddressBits);
        externals.put(LedArrayColumnOutputs + identifier, nrOfColumns);
        break;
      }
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        externals.put(LedArrayColumnAddress + identifier, nrColumnAddressBits);
        externals.put(LedArrayRowOutputs + identifier, nrOfRows);
        break;
      }
      case LedArrayDriving.RgbDefault : {
        externals.put(LedArrayRedOutputs + identifier, nrOfRows * nrOfColumns);
        externals.put(LedArrayGreenOutputs + identifier, nrOfRows * nrOfColumns);
        externals.put(LedArrayBlueOutputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbRowScanning : {
        externals.put(LedArrayRowAddress + identifier, nrRowAddressBits);
        externals.put(LedArrayColumnRedOutputs + identifier, nrOfColumns);
        externals.put(LedArrayColumnGreenOutputs + identifier, nrOfColumns);
        externals.put(LedArrayColumnBlueOutputs + identifier, nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbColumnScanning : {
        externals.put(LedArrayColumnAddress + identifier, nrColumnAddressBits);
        externals.put(LedArrayRowRedOutputs + identifier, nrOfRows);
        externals.put(LedArrayRowGreenOutputs + identifier, nrOfRows);
        externals.put(LedArrayRowBlueOutputs + identifier, nrOfRows);
        break;
      }
    }
    return externals;
  }

  public static SortedMap<String, Integer> getInternalSignals(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier) {
    final var wires = new TreeMap<String, Integer>();
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT:
      case LedArrayDriving.LED_ROW_SCANNING:
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        wires.put("s_" + LedArrayInputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbDefault :
      case LedArrayDriving.RgbRowScanning :
      case LedArrayDriving.RgbColumnScanning : {
        wires.put("s_" + LedArrayRedInputs + identifier, nrOfRows * nrOfColumns);
        wires.put("s_" + LedArrayGreenInputs + identifier, nrOfRows * nrOfColumns);
        wires.put("s_" + LedArrayBlueInputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
    }
    return wires;
  }

  public static ArrayList<String> GetComponentMap(char typeId, int nrOfRows, int nrOfColumns, int identifier, long FpgaClockFrequency, boolean isActiveLow) {
    final var componentMap = new LineBuffer();
    componentMap.
            add(HDL.isVHDL()
      ? "   array" + identifier + " : " + getSpecificHDLName(typeId)
      : "   " + getSpecificHDLName(typeId));
    switch (typeId) {
      case LedArrayDriving.RgbDefault :
      case LedArrayDriving.LED_DEFAULT:
        componentMap.add(LedArrayLedDefaultHDLGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow));
        break;
      case LedArrayDriving.RgbColumnScanning :
      case LedArrayDriving.LED_COLUMN_SCANNING:
        componentMap.add(LedArrayColumnScanningHDLGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow));
        break;
      case LedArrayDriving.RgbRowScanning :
      case LedArrayDriving.LED_ROW_SCANNING:
        componentMap.add(LedArrayRowScanningHDLGeneratorFactory.getGenericMap(
            nrOfRows,
            nrOfColumns,
            FpgaClockFrequency,
            isActiveLow));
        break;
    }
    if (HDL.isVerilog()) componentMap.add("      array" + identifier);
    switch (typeId) {
      case LedArrayDriving.LED_DEFAULT: {
        componentMap.add(LedArrayLedDefaultHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbDefault : {
        componentMap.add(RGBArrayLedDefaultHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.LED_ROW_SCANNING: {
        componentMap.add(LedArrayRowScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbRowScanning : {
        componentMap.add(RGBArrayRowScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.LED_COLUMN_SCANNING: {
        componentMap.add(LedArrayColumnScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbColumnScanning : {
        componentMap.add(RGBArrayColumnScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
    }
    componentMap.add("");
    return componentMap.get();
  }

  public static ArrayList<String> getArrayConnections(FPGAIOInformationContainer array, int id) {
    final var connections = new ArrayList<String>();
    connections.addAll(
      switch (array.getArrayDriveMode()) {
        case LedArrayDriving.LED_DEFAULT, LedArrayDriving.LED_ROW_SCANNING, LedArrayDriving.LED_COLUMN_SCANNING -> getLedArrayConnections(array, id);
        case LedArrayDriving.RgbDefault, LedArrayDriving.RgbRowScanning, LedArrayDriving.RgbColumnScanning -> getRGBArrayConnections(array, id);
        default -> throw new IllegalStateException("Unexpected value: " + array.getArrayDriveMode());
      });
    connections.add("");
    return connections;
  }

  public static ArrayList<String> getLedArrayConnections(FPGAIOInformationContainer info, int id) {
    final var connections = (new LineBuffer()).withHdlPairs();
    connections.addPair("id", id).addPair("ins", LedArrayInputs);
    for (var pin = 0; pin < info.getNrOfPins(); pin++) {
      connections.addPair("pin", pin);
      if (!info.pinIsMapped(pin)) {
        connections.add("{{assign}} s_{{ins}}{{id}{{<}}{{pin}}{{>}} {{=}} {{0b}};");
      } else {
        connections.add("{{assign}} s_{{ins}}{{id}}{{<}}{{pin}}{{>}} {{=}} %s;", info.getPinMap(pin).getHdlSignalName(info.getMapPin(pin)));
      }
    }
    return connections.getWithIndent();
  }

  public static ArrayList<String> getRGBArrayConnections(FPGAIOInformationContainer array, int id) {
    final var connections =
        (new LineBuffer())
            .withHdlPairs()
            .addPair("id", id)
            .addPair("insR", LedArrayRedInputs)
            .addPair("insG", LedArrayGreenInputs)
            .addPair("insB", LedArrayBlueInputs);

    for (var pin = 0; pin < array.getNrOfPins(); pin++) {
      connections.addPair("pin", pin);
      if (!array.pinIsMapped(pin)) {
        connections.add(
            "{{assign}} s_{{insR}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{0b}};",
            "{{assign}} s_{{insG}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{0b}};",
            "{{assign}} s_{{insB}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{0b}};");
      } else {
        final var map = array.getPinMap(pin);
        if (map.getComponentFactory() instanceof RgbLed) {
          connections
              .addPair("mapR", map.getHdlSignalName(RgbLed.RED))
              .addPair("mapG", map.getHdlSignalName(RgbLed.GREEN))
              .addPair("mapB", map.getHdlSignalName(RgbLed.BLUE))
              .add(
                  "{{assign}} s_{{insR}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{mapR}};",
                  "{{assign}} s_{{insG}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{mapG}};",
                  "{{assign}} s_{{insB}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{mapB}};");
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

          connections.add(getColorMap("s_"
              + LedArrayRedInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose(),
              rOn,
              rOff,
              pinName));
          connections.add(getColorMap("s_"
              + LedArrayGreenInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose(),
              gOn,
              gOff,
              pinName));
          connections.add(getColorMap("s_"
              + LedArrayBlueInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose(),
              bOn,
              bOff,
              pinName));
        } else {
          final var pinName = map.getHdlSignalName(array.getMapPin(pin));
          connections.add("   "
              + HDL.assignPreamble()
              + "s_"
              + LedArrayRedInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose()
              + HDL.assignOperator()
              + pinName
              + ";");
          connections.add("   "
              + HDL.assignPreamble()
              + "s_"
              + LedArrayGreenInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose()
              + HDL.assignOperator()
              + pinName
              + ";");
          connections.add("   "
              + HDL.assignPreamble()
              + "s_"
              + LedArrayBlueInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose()
              + HDL.assignOperator()
              + pinName
              + ";");
        }
      }
    }
    return connections.getWithIndent();
  }

  private static String getColorMap(String dest, int onColor, int offColor, String source) {
    final var onBit = (onColor > 128) ? HDL.oneBit() : HDL.zeroBit();
    final var offBit = (offColor > 128) ? HDL.oneBit() : HDL.zeroBit();
    return
      HDL.isVHDL()
        ?  dest + HDL.assignOperator() + onBit + " WHEN " + source + " = '1' ELSE " + offBit + ";"
        : "assign " + dest + " = (" + source + " == " + HDL.oneBit() + ") ? " + onBit + " : " + offBit + ";";
  }
}
