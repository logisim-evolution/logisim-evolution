package com.cburch.logisim.fpga.hdlgenerator;

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
    switch (typeId) {
      case LedArrayDriving.LedDefault : return new LedArrayLedDefaultHDLGeneratorFactory();
      case LedArrayDriving.LedRowScanning : return new LedArrayRowScanningHDLGeneratorFactory();
      case LedArrayDriving.LedColumnScanning : return new LedArrayColumnScanningHDLGeneratorFactory();
      case LedArrayDriving.RgbDefault : return new RGBArrayLedDefaultHDLGeneratorFactory();
      case LedArrayDriving.RgbRowScanning : return new RGBArrayRowScanningHDLGeneratorFactory();
      case LedArrayDriving.RgbColumnScanning : return new RGBArrayColumnScanningHDLGeneratorFactory();
      default : return null;
    }
  }
  
  public static String getSpecificHDLName(char typeId) {
    switch (typeId) {
      case LedArrayDriving.LedDefault : return LedArrayLedDefaultHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.LedRowScanning : return LedArrayRowScanningHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.LedColumnScanning : return LedArrayColumnScanningHDLGeneratorFactory.LedArrayName;
      case LedArrayDriving.RgbDefault : return RGBArrayLedDefaultHDLGeneratorFactory.RGBArrayName;
      case LedArrayDriving.RgbRowScanning : return RGBArrayRowScanningHDLGeneratorFactory.RGBArrayName;
      case LedArrayDriving.RgbColumnScanning : return RGBArrayColumnScanningHDLGeneratorFactory.RGBArrayName;
      default : return null;
    }
  }
  
  public static String getSpecificHDLName(String type) {
    return getSpecificHDLName(LedArrayDriving.getId(type));
  }
  
  public static int getNrOfBitsRequired(int value) {
    final var nrBitsDouble = Math.log(value) / Math.log(2.0);
    return (int) Math.ceil(nrBitsDouble);
  }
  
  public static String getExternalSignalName(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier,
      int pinNr) {
    final var nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    final var nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
    switch (typeId) {
      case LedArrayDriving.LedDefault : {
        return LedArrayOutputs + identifier + "[" + pinNr + "]";
      }
      case LedArrayDriving.LedRowScanning : {
        if (pinNr < nrRowAddressBits) {
          return LedArrayRowAddress + identifier + "[" + pinNr + "]";
        }
        return LedArrayColumnOutputs + identifier + "[" + (pinNr - nrRowAddressBits) + "]";
      }
      case LedArrayDriving.LedColumnScanning : {
        if (pinNr < nrColumnAddressBits) {
          return LedArrayColumnAddress + identifier + "[" + pinNr + "]";
        }
        return LedArrayRowOutputs + identifier + "[" + (pinNr - nrColumnAddressBits) + "]";
      }
      case LedArrayDriving.RgbDefault : {
        final var index = pinNr % 3;
        final var col = pinNr / 3;
        switch (col) {
          case 0 : {
            return LedArrayRedOutputs + identifier + "[" + index + "]";
          }
          case 1 : {
            return LedArrayGreenOutputs + identifier + "[" + index + "]";
          }
          case 2 : {
            return LedArrayBlueOutputs + identifier + "[" + index + "]";
          }
          default : return "";
        }
      }
      case LedArrayDriving.RgbRowScanning : {
        final var index = (pinNr - nrRowAddressBits) % nrOfColumns;
        final var col = (pinNr - nrRowAddressBits) / nrOfColumns;
        if (pinNr < nrRowAddressBits) {
          return LedArrayRowAddress + identifier + "[" + pinNr + "]";
        } else {
          switch (col) {
            case 0 : {
              return LedArrayColumnRedOutputs + identifier + "[" + index + "]";
            }
            case 1 : {
              return LedArrayColumnGreenOutputs + identifier + "[" + index + "]";
            }
            case 2 : {
              return LedArrayColumnBlueOutputs + identifier + "[" + index + "]";
            }
            default : return "";
          }
        }
      }
      case LedArrayDriving.RgbColumnScanning : {
        final var index = (pinNr - nrColumnAddressBits) % nrOfRows;
        final var col = (pinNr - nrColumnAddressBits) / nrOfRows;
        if (pinNr < nrColumnAddressBits) {
          return LedArrayColumnAddress + identifier + "[" + pinNr + "]";
        } else {
          switch (col) {
            case 0 : {
              return LedArrayRowRedOutputs + identifier + "[" + index + "]";
            }
            case 1 : {
              return LedArrayRowGreenOutputs + identifier + "[" + index + "]";
            }
            case 2 : {
              return LedArrayRowBlueOutputs + identifier + "[" + index + "]";
            }
            default : return "";
          }
        }
      }
    }
    return "";
  }
  
  public static boolean requiresClock(char typeId) {
    return (typeId != LedArrayDriving.LedDefault) && (typeId != LedArrayDriving.RgbDefault);
  }
  
  public static SortedMap<String, Integer> getExternalSignals(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier) {
    final var externals = new TreeMap<String, Integer>();
    final var nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    final var nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
    switch (typeId) {
      case LedArrayDriving.LedDefault : {
        externals.put(LedArrayOutputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.LedRowScanning : {
        externals.put(LedArrayRowAddress + identifier, nrRowAddressBits);
        externals.put(LedArrayColumnOutputs + identifier, nrOfColumns);
        break;
      }
      case LedArrayDriving.LedColumnScanning : {
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
      case LedArrayDriving.LedDefault :
      case LedArrayDriving.LedRowScanning :
      case LedArrayDriving.LedColumnScanning : {
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
  
  public static ArrayList<String> GetComponentMap(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier,
      long FpgaClockFrequency,
      boolean isActiveLow) {
    final var componentMap = new ArrayList<String>();
    if (HDL.isVHDL())
      componentMap.add("   array" + identifier + " : " + getSpecificHDLName(typeId));
    else
      componentMap.add("   " + getSpecificHDLName(typeId));
    switch (typeId) {
      case LedArrayDriving.RgbDefault : 
      case LedArrayDriving.LedDefault : {
        componentMap.addAll(LedArrayLedDefaultHDLGeneratorFactory.getGenericMap(
            nrOfRows, 
            nrOfColumns, 
            FpgaClockFrequency, 
            isActiveLow));
        break;
      }
      case LedArrayDriving.RgbColumnScanning :
      case LedArrayDriving.LedColumnScanning : {
        componentMap.addAll(LedArrayColumnScanningHDLGeneratorFactory.getGenericMap(
            nrOfRows, 
            nrOfColumns, 
            FpgaClockFrequency, 
            isActiveLow));
        break;
      }
      case LedArrayDriving.RgbRowScanning :
      case LedArrayDriving.LedRowScanning : {
        componentMap.addAll(LedArrayRowScanningHDLGeneratorFactory.getGenericMap(
            nrOfRows, 
            nrOfColumns, 
            FpgaClockFrequency, 
            isActiveLow));
        break;
      }
    }
    if (HDL.isVerilog()) componentMap.add("      array" + identifier);
    switch (typeId) {
      case LedArrayDriving.LedDefault : {
        componentMap.addAll(LedArrayLedDefaultHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbDefault : {
        componentMap.addAll(RGBArrayLedDefaultHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.LedRowScanning : {
        componentMap.addAll(LedArrayRowScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbRowScanning : {
        componentMap.addAll(RGBArrayRowScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.LedColumnScanning : {
        componentMap.addAll(LedArrayColumnScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
      case LedArrayDriving.RgbColumnScanning : {
        componentMap.addAll(RGBArrayColumnScanningHDLGeneratorFactory.getPortMap(identifier));
        break;
      }
    }
    componentMap.add("");
    return componentMap;
  }
  
  public static ArrayList<String> getArrayConnections(FPGAIOInformationContainer array, int id) {
    final var connections = new ArrayList<String>();
    switch (array.getArrayDriveMode()) {
      case LedArrayDriving.LedDefault :
      case LedArrayDriving.LedRowScanning :
      case LedArrayDriving.LedColumnScanning : {
        connections.addAll(getLedArrayConnections(array, id));
        connections.add("");
        break;
      }
      case LedArrayDriving.RgbDefault :
      case LedArrayDriving.RgbRowScanning :
      case LedArrayDriving.RgbColumnScanning : {
        connections.addAll(getRGBArrayConnections(array, id));
        connections.add("");
        break;
      }
    }
    return connections;
  }

  public static ArrayList<String> getLedArrayConnections(FPGAIOInformationContainer array, int id) {
    final var connections = new ArrayList<String>();
    for (var pin = 0; pin < array.getNrOfPins(); pin++) {
      if (!array.pinIsMapped(pin)) {
        connections.add("   " 
            + HDL.assignPreamble()
            + "s_"
            + LedArrayInputs
            + id
            + HDL.BracketOpen()
            + pin
            + HDL.BracketClose()
            + HDL.assignOperator()
            + HDL.zeroBit()
            + ";");
      } else {
        connections.add("   " 
            + HDL.assignPreamble()
            + "s_"
            + LedArrayInputs
            + id
            + HDL.BracketOpen()
            + pin
            + HDL.BracketClose()
            + HDL.assignOperator()
            + array.getPinMap(pin).getHdlSignalName(array.getMapPin(pin))
            + ";");
      }
    }
    return connections;
  }

  public static ArrayList<String> getRGBArrayConnections(FPGAIOInformationContainer array, int id) {
    final var connections = new ArrayList<String>();
    for (var pin = 0; pin < array.getNrOfPins(); pin++) {
      if (!array.pinIsMapped(pin)) {
        connections.add("   " 
            + HDL.assignPreamble()
            + "s_"
            + LedArrayRedInputs
            + id
            + HDL.BracketOpen()
            + pin
            + HDL.BracketClose()
            + HDL.assignOperator()
            + HDL.zeroBit()
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
            + HDL.zeroBit()
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
            + HDL.zeroBit()
            + ";");
      } else {
        final var map = array.getPinMap(pin);
        if (map.getComponentFactory() instanceof RgbLed) {
          connections.add("   " 
              + HDL.assignPreamble()
              + "s_"
              + LedArrayRedInputs
              + id
              + HDL.BracketOpen()
              + pin
              + HDL.BracketClose()
              + HDL.assignOperator()
              + map.getHdlSignalName(RgbLed.RED)
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
              + map.getHdlSignalName(RgbLed.GREEN)
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
              + map.getHdlSignalName(RgbLed.BLUE)
              + ";");
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
    return connections;
  }
  
  private static String getColorMap(String dest, int onColor, int offColor, String source) {
    final var onBit = (onColor > 128) ? HDL.oneBit() : HDL.zeroBit();
    final var offBit = (offColor > 128) ? HDL.oneBit() : HDL.zeroBit();
    if (HDL.isVHDL()) {
      return "   " + dest + HDL.assignOperator() + onBit + " WHEN " + source + " = '1' ELSE " + offBit + ";";
    } else {
      return "   assign " + dest + " = (" + source + " == " + HDL.oneBit() + ") ? " + onBit + " : " + offBit + ";";
    }
  }
}
