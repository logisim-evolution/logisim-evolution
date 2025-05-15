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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class SevenSegmentScanningDecodedHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_SEGMENTS_ID = -1;
  public static final int NR_OF_DIGITS_ID = -2;
  public static final int NR_OF_CONTROL_ID = -3;
  public static final int ACTIVE_LOW_ID = -4;
  public static final int SCANNING_COUNTER_BITS_ID = -5;
  public static final int SCANNING_COUNTER_VALUE_ID = -6;
  public static final String NR_OF_SEGMENTS_STRING = "nrOfSegments";
  public static final String NR_OF_DIGITS_STRING = "nrOfDigits";
  public static final String NR_OF_CONTROL_STRING = "nrOfControl";
  public static final String HDL_IDENTIFIER = "SevenSegmentScanningDecoded";
  public static final String ACTIVE_LOW_STRING = "activeLow";
  public static final String SCANNING_COUNTER_BITS_STRING = "nrOfScanningCounterBits";
  public static final String SCANNING_COUNTER_VALUE_STRING = "scanningCounterReloadValue";

  public SevenSegmentScanningDecodedHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_SEGMENTS_STRING, NR_OF_SEGMENTS_ID)
        .add(NR_OF_DIGITS_STRING, NR_OF_DIGITS_ID)
        .add(NR_OF_CONTROL_STRING, NR_OF_CONTROL_ID)
        .add(ACTIVE_LOW_STRING, ACTIVE_LOW_ID)
        .add(SCANNING_COUNTER_BITS_STRING, SCANNING_COUNTER_BITS_ID)
        .add(SCANNING_COUNTER_VALUE_STRING, SCANNING_COUNTER_VALUE_ID);
    myWires
        .addWire("s_columnCounterNext", NR_OF_CONTROL_ID)
        .addWire("s_scanningCounterNext", SCANNING_COUNTER_BITS_ID)
        .addWire("s_tickNext", 1)
        .addRegister("s_columnCounterReg", NR_OF_CONTROL_ID)
        .addRegister("s_scanningCounterReg", SCANNING_COUNTER_BITS_ID)
        .addRegister("s_tickReg", 1);
    myPorts
        .add(Port.INPUT, SevenSegmentScanningGenericHdlGenerator.SevenSegmentSegmenInputs, NR_OF_SEGMENTS_ID, 9)
        .add(Port.OUTPUT, SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput, NR_OF_CONTROL_ID, 10)
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
    final var generics = new HashMap<String, String>();
    final var nrOfControl = nrOfControlBits(nrOfRows, nrOfColumns);
    generics.put(NR_OF_SEGMENTS_STRING, Integer.toString(nrOfRows * 8));
    generics.put(NR_OF_DIGITS_STRING, Integer.toString(nrOfRows));
    generics.put(NR_OF_CONTROL_STRING, Integer.toString(nrOfControl));
    generics.put(ACTIVE_LOW_STRING, activeLow ? "1" : "0");
    generics.put(SCANNING_COUNTER_BITS_STRING, Integer.toString(nrOfScanningBitsCount));
    generics.put(SCANNING_COUNTER_VALUE_STRING, Integer.toString(scanningReload - 1));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(generics, true);
  }

  public static int nrOfControlBits(int nrOfDigits, int nrOfDecodedBits) {
    return Math.max((int) Math.ceil(Math.log(nrOfDigits) / Math.log(2.0)), nrOfDecodedBits);
  }

  public static LineBuffer getPortMap(int id) {
    final var ports = new HashMap<String, String>();
    ports.put(SevenSegmentScanningGenericHdlGenerator.SevenSegmentSegmenInputs,
            String.format("s_%s%d", SevenSegmentScanningGenericHdlGenerator.InternalSignalName, id));
    ports.put(TickComponentHdlGeneratorFactory.FPGA_CLOCK, TickComponentHdlGeneratorFactory.FPGA_CLOCK);
    ports.put(SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput, String.format("Displ%dSelect", id));
    for (final var segName : SevenSegment.getLabels())
        ports.put(segName, String.format("Displ%d_%s", id, segName));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(ports, false);
  }

  public static List<String> getTickCounterCode() {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
            .pair("counterBits", SCANNING_COUNTER_BITS_STRING)
            .pair("counterValue", SCANNING_COUNTER_VALUE_STRING)
            .pair("digitReload", NR_OF_DIGITS_STRING)
            .pair("nrControlBits", NR_OF_CONTROL_STRING)
            .pair("nrOfRows", NR_OF_DIGITS_STRING)
            .pair("controlOutput", SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput)
            .pair("seg_a", SevenSegment.getOutputLabel(SevenSegment.Segment_A))
            .pair("seg_b", SevenSegment.getOutputLabel(SevenSegment.Segment_B))
            .pair("seg_c", SevenSegment.getOutputLabel(SevenSegment.Segment_C))
            .pair("seg_d", SevenSegment.getOutputLabel(SevenSegment.Segment_D))
            .pair("seg_e", SevenSegment.getOutputLabel(SevenSegment.Segment_E))
            .pair("seg_f", SevenSegment.getOutputLabel(SevenSegment.Segment_F))
            .pair("seg_g", SevenSegment.getOutputLabel(SevenSegment.Segment_G))
            .pair("dp", SevenSegment.getOutputLabel(SevenSegment.DP))
            .pair("seg_a_id", SevenSegment.Segment_A)
            .pair("seg_b_id", SevenSegment.Segment_B)
            .pair("seg_c_id", SevenSegment.Segment_C)
            .pair("seg_d_id", SevenSegment.Segment_D)
            .pair("seg_e_id", SevenSegment.Segment_E)
            .pair("seg_f_id", SevenSegment.Segment_F)
            .pair("seg_g_id", SevenSegment.Segment_G)
            .pair("dp_id", SevenSegment.DP)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("segmentInputs", SevenSegmentScanningGenericHdlGenerator.SevenSegmentSegmenInputs);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_tickNext <= '1' {{when}} s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{counterBits}})) {{else}} '0';

          s_scanningCounterNext <= ({{others}} => '0') {{when}} s_tickReg /= '0' {{and}} s_tickReg /= '1' {{else}} -- for simulation
                                   std_logic_vector(to_unsigned({{counterValue}}-1, {{counterBits}}))
                                      {{when}} s_tickNext = '1' {{else}}
                                   std_logic_vector(unsigned(s_scanningCounterReg)-1);

          s_columnCounterNext <= ({{others}} => '0') {{when}} s_tickReg /= '0' {{and}} s_tickReg /= '1' {{else}} -- for simulation
                                 s_columnCounterReg {{when}} s_tickReg = '0' {{else}}
                                 std_logic_vector(to_unsigned({{digitReload}} - 1, {{nrControlBits}}))
                                   {{when}} s_columnCounterReg = std_logic_vector(to_unsigned(0, {{nrControlBits}})) {{else}}
                                 std_logic_vector(unsigned(s_columnCounterReg)-1);

          makeFlops : {{process}} ({{clock}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{clock}})) {{then}}
                s_scanningCounterReg <= s_scanningCounterNext;
                s_columnCounterReg   <= s_columnCounterNext;
                s_tickReg            <= s_tickNext;
             {{end}} {{if}};
          {{end}} {{process}} makeFlops;

          {{seg_a}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_a_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_a_id}});
          {{seg_b}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_b_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_b_id}});
          {{seg_c}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_c_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_c_id}});
          {{seg_d}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_d_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_d_id}});
          {{seg_e}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_e_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_e_id}});
          {{seg_f}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_f_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_f_id}});
          {{seg_g}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_g_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{seg_g_id}});
          {{dp}} <= {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{dp_id}}) {{when}} {{activeLow}} = 0 {{else}} {{not}} {{segmentInputs}}(to_integer(unsigned(s_columnCounterReg))*8 + {{dp_id}});
          """).empty();
    } else {
      contents.add(
          """

          assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;
          assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{counterValue}} : s_scanningCounterReg - 1;
          assign s_columnCounterNext =  (s_tickReg == 1'b0) ? s_columnCounterReg : (s_columnCounterReg == 0) ? {{digitReload}} - 1 : s_columnCounterReg - 1;

          assign {{seg_a}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_a_id}}] ^ activeLow;
          assign {{seg_b}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_b_id}}] ^ activeLow;
          assign {{seg_c}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_c_id}}] ^ activeLow;
          assign {{seg_d}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_d_id}}] ^ activeLow;
          assign {{seg_e}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_e_id}}] ^ activeLow;
          assign {{seg_f}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_f_id}}] ^ activeLow;
          assign {{seg_g}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{seg_g_id}}] ^ activeLow;
          assign {{dp}} = {{segmentInputs}}[s_columnCounterReg * 8 + {{dp_id}}] ^ activeLow;
          """)
        .addRemarkBlock("Here the simulation only initial is defined")
        .add("""
            initial
            begin
               s_scanningCounterReg = 0;
               s_columnCounterReg   = 0;
               s_tickReg            = 1'b0;
            end

            always @(posedge {{clock}})
            begin
                s_scanningCounterReg = s_scanningCounterNext;
                s_columnCounterReg   = s_columnCounterNext;
                s_tickReg            = s_tickNext;
            end
            """).empty();
    }
    return contents.get();
  }

  public static List<String> getDecoderCounterCode() {
    final var contents =
        LineBuffer.getHdlBuffer()
        .pair("controlOutput", SevenSegmentScanningGenericHdlGenerator.SevenSegmentControlOutput);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          {{controlOutput}} <= s_columnCounterReg;

          """).empty();
    } else {
      contents.add(
          """
          assign {{controlOutput}} = s_columnCounterReg;

          """).empty();
    }
    return contents.get();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .add(getTickCounterCode())
        .add(getDecoderCounterCode());
    return contents;
  }
}
