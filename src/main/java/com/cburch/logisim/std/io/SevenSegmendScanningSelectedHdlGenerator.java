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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class SevenSegmendScanningSelectedHdlGenerator extends SevenSegmentScanningDecodedHdlGeneratorFactory {

  public static final String HDL_IDENTIFIER =  "SevenSegmentScanning";
    

  public static final int SELECT_DIGIT_INVERTED_ID = -7;
  public static final String SELECT_DIGIT_INVERTED_STRING = "activeLowSelectPins";

  public SevenSegmendScanningSelectedHdlGenerator() {
    super();
    myParametersList.add(SELECT_DIGIT_INVERTED_STRING, SELECT_DIGIT_INVERTED_ID);
    myPorts.removePorts();
    myPorts
        .add(Port.INPUT, SevenSegmentScanningGenericHdlGenerator.SevenSegmentSegmenInputs, NR_OF_SEGMENTS_ID, 9)
        .add(Port.OUTPUT, SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput, NR_OF_DIGITS_ID, 10)
        .add(Port.INPUT, TickComponentHdlGeneratorFactory.FPGA_CLOCK, 1, 11);
    var id = 0;
    for (final var segName : SevenSegment.getLabels()) {
      myPorts.add(Port.OUTPUT, segName, 1, id++);
    }
  }
  
  public static LineBuffer getGenericMap(
          int nrOfRows, int nrOfColumns, long fpgaClockFrequency, boolean activeLow, boolean selectActiveLow) {
    final var scanningReload = (int) (fpgaClockFrequency / 1000);
    final var nrOfScanningBitsCount = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(scanningReload);
    final var nrOfControl = nrOfControlBits(nrOfRows, nrOfColumns);
    final var generics = new HashMap<String, String>();
    generics.put(NR_OF_SEGMENTS_STRING, Integer.toString(nrOfRows * 8));
    generics.put(NR_OF_DIGITS_STRING, Integer.toString(nrOfRows));
    generics.put(NR_OF_CONTROL_STRING, Integer.toString(nrOfControl));
    generics.put(ACTIVE_LOW_STRING, activeLow ? "1" : "0");
    generics.put(SELECT_DIGIT_INVERTED_STRING, selectActiveLow ? "1" : "0");
    generics.put(SCANNING_COUNTER_BITS_STRING, Integer.toString(nrOfScanningBitsCount));
    generics.put(SCANNING_COUNTER_VALUE_STRING, Integer.toString(scanningReload - 1));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(generics, true);
  }
  
  
  public static List<String> getDecodedCode() {
    final var contents =
        LineBuffer.getHdlBuffer()
        .pair("controlOutput", SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput)
        .pair("nMax", NR_OF_DIGITS_STRING)
        .pair("sVal", SELECT_DIGIT_INVERTED_STRING)
        .pair("nrBits", NR_OF_CONTROL_STRING);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          genSels : {{for}} n {{in}} {{nMax}} - 1 {{downto}} 0 {{generate}}
            {{controlOutput}}(n) <= '1' 
               {{when}} (s_columnCounterReg = std_logic_vector(to_unsigned(n, {{nrBits}})) {{and}} {{sVal}} = 0) {{or}}
                    (s_columnCounterReg /= std_logic_vector(to_unsigned(n, {{nrBits}})) {{and}} {{sVal}} = 1) {{else}} '0';
          {{end}} {{generate}};
          
          """).empty();
    } else {
      contents.add(
          """
          genvar n;
          
          generate
            for (n = 0; n < {{nMax}}; n = n + 1)
              assign {{controlOutput}}[n] = s_columnCounterReg == n ? ~{{sVal}} : {{sVal}};
          endgenerate
          
          """).empty();
    }
    return contents.get();
  }

  
  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .add(getTickCounterCode())
        .add(getDecodedCode());
    return contents;
  }
}
