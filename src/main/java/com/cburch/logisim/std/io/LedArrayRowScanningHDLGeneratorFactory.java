/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.List;

public class LedArrayRowScanningHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_LEDS_ID = -1;
  public static final int NR_OF_ROWS_ID = -2;
  public static final int NR_OF_COLUMS_ID = -3;
  public static final int NR_OF_ROW_ADDRESS_BITS_ID = -4;
  public static final int ACTIVE_LOW_ID = -5;
  public static final int SCANNING_COUNTER_BITS_ID = -6;
  public static final int MAX_NR_LEDS_ID = -7;
  public static final int SCANNING_COUNTER_VALUE_ID = -8;
  public static final String NR_OF_ROWS_STRING = "nrOfRows";
  public static final String NR_OF_COLUMS_STRING = "nrOfColumns";
  public static final String NR_OF_LEDS_STRING = "nrOfLeds";
  public static final String NR_OF_ROW_ADDRESS_BITS_STRING = "nrOfRowAddressBits";
  public static final String ACTIVE_LOW_STRING = "activeLow";
  public static final String SCANNING_COUNTER_BITS_STRING = "nrOfScanningCounterBits";
  public static final String SCANNING_COUNTER_VALUE_STRING = "scanningCounterReloadValue";
  public static final String MAX_NR_LEDS_STRING = "maxNrLedsAddrColumns";
  public static final String HDL_IDENTIFIER = "LedArrayRowScanning";

  public LedArrayRowScanningHDLGeneratorFactory() {
    super();
    myParametersList
        .add(ACTIVE_LOW_STRING, ACTIVE_LOW_ID)
        .add(MAX_NR_LEDS_STRING, MAX_NR_LEDS_ID)
        .add(NR_OF_COLUMS_STRING, NR_OF_COLUMS_ID)
        .add(NR_OF_LEDS_STRING, NR_OF_LEDS_ID)
        .add(NR_OF_ROWS_STRING, NR_OF_ROWS_ID)
        .add(NR_OF_ROW_ADDRESS_BITS_STRING, NR_OF_ROW_ADDRESS_BITS_ID)
        .add(SCANNING_COUNTER_BITS_STRING, SCANNING_COUNTER_BITS_ID)
        .add(SCANNING_COUNTER_VALUE_STRING, SCANNING_COUNTER_VALUE_ID);
    myWires
        .addWire("s_rowCounterNext", NR_OF_ROW_ADDRESS_BITS_ID)
        .addWire("s_scanningCounterNext", SCANNING_COUNTER_BITS_ID)
        .addWire("s_tickNext", 1)
        .addWire("s_maxLedInputs", MAX_NR_LEDS_ID)
        .addRegister("s_rowCounterReg", NR_OF_ROW_ADDRESS_BITS_ID)
        .addRegister("s_scanningCounterReg", SCANNING_COUNTER_BITS_ID)
        .addRegister("s_tickReg", 1);
    myPorts
        .add(Port.INPUT, TickComponentHdlGeneratorFactory.FPGA_CLOCK, 1, 0)
        .add(Port.INPUT, LedArrayGenericHDLGeneratorFactory.LedArrayInputs, NR_OF_LEDS_ID, 1)
        .add(Port.OUTPUT, LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress, NR_OF_ROW_ADDRESS_BITS_ID, 2)
        .add(Port.OUTPUT, LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs, NR_OF_COLUMS_ID, 3);
  }

  public static List<String> getGenericMap(int nrOfRows, int nrOfColumns, long FpgaClockFrequency, boolean activeLow) {
    final var nrRowAddrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfRows);
    final var scanningReload = (int) (FpgaClockFrequency / 1000);
    final var nrOfScanningBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(scanningReload);
    final var maxNrLeds = ((int) Math.pow(2.0, (double) nrRowAddrBits)) * nrOfRows;

    final var contents =
        LineBuffer.getBuffer()
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("nrOfLedsVal", nrOfRows * nrOfColumns)
            .pair("nrOfRows", NR_OF_ROWS_STRING)
            .pair("nrOfRowsVal", nrOfRows)
            .pair("nrOfColumns", NR_OF_COLUMS_STRING)
            .pair("nrOfColumnsVal", nrOfColumns)
            .pair("nrOfRowAddressBits", NR_OF_ROW_ADDRESS_BITS_STRING)
            .pair("nrOfRowAddressBitsVal", nrRowAddrBits)
            .pair("scanningCounterBits", SCANNING_COUNTER_BITS_STRING)
            .pair("scanningCounterBitsVal", nrOfScanningBits)
            .pair("scanningCounterValue", SCANNING_COUNTER_VALUE_STRING)
            .pair("scanningCounterValueVal", scanningReload - 1)
            .pair("maxNrLeds", MAX_NR_LEDS_STRING)
            .pair("maxNrLedsVal", maxNrLeds)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("activeLowVal", activeLow ? "1" : "0");

    if (Hdl.isVhdl()) {
      contents.add("""
          GENERIC MAP ( {{nrOfLeds}} => {{nrOfLedsVal}},
                        {{nrOfRows}} => {{nrOfRowsVal}},
                        {{nrOfColumns}} => {{nrOfColumnsVal}},
                        {{nrOfRowAddressBits}} => {{nrOfRowAddressBitsVal}},
                        {{scanningCounterBits}} => {{scanningCounterBitsVal}},
                        {{scanningCounterValue}} => {{scanningCounterValueVal}},
                        {{maxNrLeds}} => {{maxNrLedsVal}},
                        {{activeLow}} => {{activeLowVal}} )
          """);
    } else {
      contents.add("""
          #( .{{nrOfLeds}}({{nrOfLedsVal}}),
             .{{nrOfRows}}({{nrOfRowsVal}}),
             .{{nrOfColumns}}({{nrOfColumns}}),
             .{{nrOfRowAddressBits}}({{nrOfRowAddressBitsVal}}),
             .{{scanningCounterBits}}({{scanningCounterBitsVal}}),
             .{{scanningCounterValue}}({{scanningCounterValueVal}}),
             .{{maxNrLeds}}({{maxNrLedsVal}}),
             .{{activeLow}}({{activeLowVal}}) )
          """);
    }
    return contents.getWithIndent(6);
  }

  public static List<String> getPortMap(int id) {
    final var map =
        LineBuffer.getBuffer()
            .pair("rowAddr", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("colOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
                .pair("id", id);
    if (Hdl.isVhdl()) {
      map.add("""
          PORT MAP ( {{rowAddr}} => {{rowAddr}}{{id}},
                     {{outs}} => {{outs}}{{id}},
                     {{clock}} => {{clock}},
                     {{ins}} => => s_{{ins}}{{id}} );
          """);
    } else {
      map.add("""
          ( .{{rowAddr}}({{rowAddr}}{{id}}),
            .{{outs}}({{outs}}{{id}}),
            .{{clock}}({{clock}}),
            .{{ins}}(s_{{ins}}{{id}}) );
          """);
    }
    return map.getWithIndent(6);
  }

  public List<String> getRowCounterCode() {
    final var contents =
        LineBuffer.getBuffer()
            .pair("rowAddress", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("bits", SCANNING_COUNTER_BITS_STRING)
            .pair("value", SCANNING_COUNTER_VALUE_STRING)
            .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK);
    if (Hdl.isVhdl()) {
      contents.add("""

          {{rowAddress}} <= s_rowCounterReg;

          s_tickNext <= '1' WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{bits}})) ELSE '0';

          s_scanningCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation
                                   std_logic_vector(to_unsigned({{value}}-1, {{bits}})) WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{bits}})) ELSE
                                   std_logic_vector(unsigned(s_scanningCounterReg)-1);

          s_rowCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation
                              s_rowCounterReg WHEN s_tickReg = '0' ELSE
                              std_logic_vector(to_unsigned(nrOfRows-1,nrOfRowAddressBits))
                                 WHEN s_rowCounterReg = std_logic_vector(to_unsigned(0,nrOfRowAddressBits)) ELSE
                              std_logic_vector(unsigned(s_rowCounterReg)-1);

          makeFlops : PROCESS ({{clock}}) IS
          BEGIN
             IF (rising_edge({{clock}})) THEN
                s_rowCounterReg      <= s_rowCounterNext;
                s_scanningCounterReg <= s_scanningCounterNext;
                s_tickReg            <= s_tickNext;
             END IF;
          END PROCESS makeFlops;
          """);
    } else {
      contents.add("""

          assign rowAddress = s_rowCounterReg;

          assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;
          assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{value}} : s_scanningCounterReg - 1;
          assign s_rowCounterNext = (s_tickReg == 1'b0) ? s_rowCounterReg :
                                    (s_rowCounterReg == 0) ? nrOfRows-1 : s_rowCounterReg-1;
          """)
          .addRemarkBlock("Here the simulation only initial is defined")
          .add("""
               initial
               begin
                  s_rowCounterReg      = 0;
                  s_scanningCounterReg = 0;
                  s_tickReg            = 1'b0;
               end

               always @(posedge {{clock}})
               begin
                   s_rowCounterReg      = s_rowCounterNext;
                   s_scanningCounterReg = s_scanningCounterNext;
                   s_tickReg            = s_tickNext;
               end
               """);
    }
    return contents.getWithIndent();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("nrOfColumns", NR_OF_COLUMS_STRING)
            .add(getRowCounterCode());

    if (Hdl.isVhdl()) {
      contents.add("""
          makeVirtualInputs : PROCESS ( internalLeds ) IS
          BEGIN
             s_maxLedInputs <= (OTHERS => '0');
             IF ({{activeLow}} = 1) THEN
                s_maxLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{ins}};
             ELSE
                s_maxLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{ins}};
             END IF;
          END PROCESS makeVirtualInputs;

          GenOutputs : FOR n IN {{nrOfColumns}}-1 DOWNTO 0 GENERATE
             {{outs}}(n) <= s_maxLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);
          END GENERATE GenOutputs;
          """);
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < {{nrOfColumns}}; i = i + 1)
             begin:outputs
                assign {{outs}}[i] = (activeLow == 1)
                   ? ~{{ins}}[{{nrOfColumns}} * s_rowCounterReg + i]
                   :  {{ins}}[{{nrOfColumns}} * s_rowCounterReg + i];
             end
          endgenerate
          """);
    }
    return contents;
  }

}
