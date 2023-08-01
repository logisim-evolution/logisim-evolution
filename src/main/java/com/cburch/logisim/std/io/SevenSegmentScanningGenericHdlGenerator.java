/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.SevenSegmentScanningDriving;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.util.LineBuffer;

public class SevenSegmentScanningGenericHdlGenerator {
  
  public static String InternalSignalName = "scanningSevenSegSegments";
  public static String SevenSegmentSegmenInputs = "allSegments";
  public static String SevenSegmentControlOutput = "digitDecodedSelect";
  public static String SevenSegmentSegmentOutput = "scannedSegments";
  
  public static AbstractHdlGeneratorFactory getSpecificHDLGenerator(String type) {
    final var typeId = SevenSegmentScanningDriving.getId(type);
    return switch (typeId) {
      case SevenSegmentScanningDriving.SEVEN_SEG_DECODED -> new SevenSegmentScanningDecodedHdlGeneratorFactory();
      case SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_HI,
          SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW -> new SevenSegmendScanningSelectedHdlGenerator();
      default -> null;
    };
  }

  public static String getSpecificHDLName(char typeId) {
    return switch (typeId) {
      case SevenSegmentScanningDriving.SEVEN_SEG_DECODED -> SevenSegmentScanningDecodedHdlGeneratorFactory.HDL_IDENTIFIER;
      case SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_HI,
          SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW -> SevenSegmendScanningSelectedHdlGenerator.HDL_IDENTIFIER;
      default -> null;
    };
  }
  
  public static String getSpecificHDLName(String type) {
    return getSpecificHDLName(SevenSegmentScanningDriving.getId(type));
  }

  public static Map<String, Integer> getInternalSignals(int nrOfRows, int identifier) {
    final var wires = new TreeMap<String, Integer>();
    wires.put(String.format("s_%s%d", InternalSignalName, identifier), nrOfRows * 8);
    return wires;
  }
  
  public static SortedMap<String, Integer> getExternalSignals(
      char typeId,
      int nrOfRows,
      int nrOfColumns,
      int identifier) {
    final var externals = new TreeMap<String, Integer>();
    final var nrOfControlSignals = (typeId == SevenSegmentScanningDriving.SEVEN_SEG_DECODED)
        ? Math.max((int) Math.ceil(Math.log(nrOfRows) / Math.log(2.0)), nrOfColumns) : nrOfRows;
    for (final var segmentName : SevenSegment.getLabels()) {
      externals.put(String.format("Displ%d_%s", identifier, segmentName), 1);
    }
    externals.put(String.format("Displ%dSelect", identifier), nrOfControlSignals);
    return externals;
  }

  public static List<String> getComponentMap(char typeId, int nrOfRows, int nrOfColumns, int identifier, long FpgaClockFrequency, boolean isActiveLow) {
    final var componentMap = LineBuffer.getBuffer()
            .add(Hdl.isVhdl()
                ? LineBuffer.format("sevenSegScan{{1}} : {{2}}", identifier, getSpecificHDLName(typeId))
                : getSpecificHDLName(typeId));
    switch (typeId) {
      case SevenSegmentScanningDriving.SEVEN_SEG_DECODED: componentMap.add(
          SevenSegmentScanningDecodedHdlGeneratorFactory.getGenericMap(nrOfRows, nrOfColumns, FpgaClockFrequency, isActiveLow, false)
              .getWithIndent());
        break;
      case SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_HI,
        SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW: componentMap.add(
            SevenSegmendScanningSelectedHdlGenerator.getGenericMap(nrOfRows, nrOfColumns, FpgaClockFrequency, isActiveLow, 
                typeId == SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW)).getWithIndent();
        break;
    }
    if (Hdl.isVerilog()) componentMap.add("   sevenSegScan{{1}}", identifier);
    switch (typeId) {
      case SevenSegmentScanningDriving.SEVEN_SEG_DECODED,
        SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_HI,
        SevenSegmentScanningDriving.SEVEN_SEG_SCANNING_ACTIVE_LOW: componentMap.add(
            SevenSegmendScanningSelectedHdlGenerator.getPortMap(identifier).getWithIndent());
    }
    return componentMap.empty().get();
  }
  
  public static List<String> getSegmentConnections(FpgaIoInformationContainer segment, int id) {
    final var connections = LineBuffer.getHdlBuffer();
    final var wires = new HashMap<String, String>();
    for (var pin = 0; pin < segment.getNrOfPins(); pin++) {
      final var seg = LineBuffer.formatHdl("s_{{1}}{{2}}{{<}}{{3}}{{>}}", InternalSignalName, id, pin);
      if (!segment.isPinMapped(pin)) {
        wires.put(seg, Hdl.zeroBit());
      } else {
        wires.put(seg, segment.getPinMap(pin).getHdlSignalName(segment.getMapPin(pin)));
      }
    }
    Hdl.addAllWiresSorted(connections, wires);
    return connections.get();
  }
  
  public static String getExternalSignalName(int nrOfRows, int identifier, int pinNr) {
    if (pinNr < 8) return String.format("Displ%d_%s", identifier, SevenSegment.getOutputLabel(pinNr));
    return String.format("Displ%dSelect[%d]", identifier, pinNr - 8);
  }
}
