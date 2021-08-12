package com.cburch.logisim.fpga.hdlgenerator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.LedArrayDriving;
import com.cburch.logisim.fpga.data.MapComponent;
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
    char typeId = LedArrayDriving.getId(type);
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
    double nrBitsDouble = Math.log(value) / Math.log(2.0);
    return (int) Math.ceil(nrBitsDouble);
  }
  
  public static String getExternalSignalName(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier,
      int pinNr) {
    int nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    int nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
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
        int index = pinNr % 3;
        int col = pinNr / 3;
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
        int index = (pinNr - nrRowAddressBits) % nrOfColumns;
        int col = (pinNr - nrRowAddressBits) / nrOfColumns;
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
        int index = (pinNr - nrColumnAddressBits) % nrOfRows;
        int col = (pinNr - nrColumnAddressBits) / nrOfRows;
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
    SortedMap<String, Integer> Externals = new TreeMap<>();
    int nrRowAddressBits = getNrOfBitsRequired(nrOfRows);
    int nrColumnAddressBits = getNrOfBitsRequired(nrOfColumns);
    switch (typeId) {
      case LedArrayDriving.LedDefault : {
        Externals.put(LedArrayOutputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.LedRowScanning : {
        Externals.put(LedArrayRowAddress + identifier, nrRowAddressBits);
        Externals.put(LedArrayColumnOutputs + identifier, nrOfColumns);
        break;
      }
      case LedArrayDriving.LedColumnScanning : {
        Externals.put(LedArrayColumnAddress + identifier, nrColumnAddressBits);
        Externals.put(LedArrayRowOutputs + identifier, nrOfRows);
        break;
      }
      case LedArrayDriving.RgbDefault : {
        Externals.put(LedArrayRedOutputs + identifier, nrOfRows * nrOfColumns);
        Externals.put(LedArrayGreenOutputs + identifier, nrOfRows * nrOfColumns);
        Externals.put(LedArrayBlueOutputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbRowScanning : {
        Externals.put(LedArrayRowAddress + identifier, nrRowAddressBits);
        Externals.put(LedArrayColumnRedOutputs + identifier, nrOfColumns);
        Externals.put(LedArrayColumnGreenOutputs + identifier, nrOfColumns);
        Externals.put(LedArrayColumnBlueOutputs + identifier, nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbColumnScanning : {
        Externals.put(LedArrayColumnAddress + identifier, nrColumnAddressBits);
        Externals.put(LedArrayRowRedOutputs + identifier, nrOfRows);
        Externals.put(LedArrayRowGreenOutputs + identifier, nrOfRows);
        Externals.put(LedArrayRowBlueOutputs + identifier, nrOfRows);
        break;
      }
    }
    return Externals;
  }
  
  public static SortedMap<String, Integer> getInternalSignals(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    switch (typeId) {
      case LedArrayDriving.LedDefault :
      case LedArrayDriving.LedRowScanning :
      case LedArrayDriving.LedColumnScanning : {
        Wires.put("s_" + LedArrayInputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
      case LedArrayDriving.RgbDefault :
      case LedArrayDriving.RgbRowScanning :
      case LedArrayDriving.RgbColumnScanning : {
        Wires.put("s_" + LedArrayRedInputs + identifier, nrOfRows * nrOfColumns);
        Wires.put("s_" + LedArrayGreenInputs + identifier, nrOfRows * nrOfColumns);
        Wires.put("s_" + LedArrayBlueInputs + identifier, nrOfRows * nrOfColumns);
        break;
      }
    }
    return Wires;
  }
  
  public static ArrayList<String> GetComponentMap(char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier,
      long FpgaClockFrequency,
      boolean isActiveLow) {
    ArrayList<String> componentMap = new ArrayList<>();
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
    ArrayList<String> connections = new ArrayList<>();
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
    ArrayList<String> connections = new ArrayList<>();
    for (int pin = 0; pin < array.getNrOfPins(); pin++) {
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
    ArrayList<String> connections = new ArrayList<>();
    for (int pin = 0; pin < array.getNrOfPins(); pin++) {
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
        MapComponent map = array.getPinMap(pin);
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
          Color onColor = map.getAttributeSet().getValue(IoLibrary.ATTR_ON_COLOR);
          Color offColor = map.getAttributeSet().getValue(IoLibrary.ATTR_OFF_COLOR);
          int rOn = onColor.getRed();
          int gOn = onColor.getGreen();
          int bOn = onColor.getBlue();
          int rOff = offColor.getRed();
          int gOff = offColor.getGreen();
          int bOff = offColor.getBlue();
          String pinName = map.getHdlSignalName(array.getMapPin(pin));
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
          String pinName = map.getHdlSignalName(array.getMapPin(pin));
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
    String onBit = (onColor > 128) ? HDL.oneBit() : HDL.zeroBit() ;
    String offBit = (offColor > 128) ? HDL.oneBit() : HDL.zeroBit();
    if (HDL.isVHDL()) {
      return "   " + dest + HDL.assignOperator() + onBit + " WHEN " + source + " = '1' ELSE " + offBit + ";";
    } else {
      return "   assign " + dest + " = (" + source + " == " + HDL.oneBit() + ") ? " + onBit + " : " + offBit + ";";
    }
  }
}
