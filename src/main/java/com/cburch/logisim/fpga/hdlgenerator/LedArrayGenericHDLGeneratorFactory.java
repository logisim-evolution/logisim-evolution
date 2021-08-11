package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.fpga.data.LedArrayDriving;

public class LedArrayGenericHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  
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
}
