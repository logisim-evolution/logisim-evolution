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

public class LedArrayColumnScanningHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_LEDS_ID = -1;
  public static final int NR_OF_ROWS_ID = -2;
  public static final int NR_OF_COLUMNS_ID = -3;
  public static final int NR_OF_COLUMN_ADDRESS_BITS_ID = -4;
  public static final int ACTIVE_LOW_ID = -5;
  public static final int SCANNING_COUNTER_BITS_ID = -6;
  public static final int MAX_NR_LEDS_ID = -7;
  public static final int SCANNING_COUNTER_VALUE_ID = -8;
  public static final String NR_OF_ROWS_STRING = "nrOfRows";
  public static final String NR_OF_COLUMNS_STRING = "nrOfColumns";
  public static final String NR_OF_LEDS_STRING = "nrOfLeds";
  public static final String NR_OF_COLUMN_ADDRESS_BITS_STRING = "nrOfColumnAddressBits";
  public static final String SCANNING_COUNTER_BITS_STRING = "nrOfScanningCounterBits";
  public static final String SCANNING_COUNTER_VALUE_STRING = "scanningCounterReloadValue";
  public static final String MAX_NR_LEDS_STRING = "maxNrLedsAddrColumns";
  public static final String ACTIVE_LOW_STRING = "activeLow";
  public static final String HDL_IDENTIFIER = "LedArrayColumnScanning";

  public LedArrayColumnScanningHdlGeneratorFactory() {
    super();
    myParametersList
        .add(ACTIVE_LOW_STRING, ACTIVE_LOW_ID)
        .add(MAX_NR_LEDS_STRING, MAX_NR_LEDS_ID)
        .add(NR_OF_COLUMNS_STRING, NR_OF_COLUMNS_ID)
        .add(NR_OF_COLUMN_ADDRESS_BITS_STRING, NR_OF_COLUMN_ADDRESS_BITS_ID)
        .add(NR_OF_LEDS_STRING, NR_OF_LEDS_ID)
        .add(NR_OF_ROWS_STRING, NR_OF_ROWS_ID)
        .add(SCANNING_COUNTER_BITS_STRING, SCANNING_COUNTER_BITS_ID)
        .add(SCANNING_COUNTER_VALUE_STRING, SCANNING_COUNTER_VALUE_ID);
    myWires
        .addWire("s_columnCounterNext", NR_OF_COLUMN_ADDRESS_BITS_ID)
        .addWire("s_scanningCounterNext", SCANNING_COUNTER_BITS_ID)
        .addWire("s_tickNext", 1)
        .addWire("s_maxLedInputs", MAX_NR_LEDS_ID)
        .addRegister("s_columnCounterReg", NR_OF_COLUMN_ADDRESS_BITS_ID)
        .addRegister("s_scanningCounterReg", SCANNING_COUNTER_BITS_ID)
        .addRegister("s_tickReg", 1);
    myPorts
        .add(Port.INPUT, TickComponentHdlGeneratorFactory.FPGA_CLOCK, 1, 0)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayInputs, NR_OF_LEDS_ID, 1)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress, NR_OF_COLUMN_ADDRESS_BITS_ID, 2)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRowOutputs, NR_OF_ROWS_ID, 3);
  }

  public static List<String> getGenericMap(int nrOfRows, int nrOfColumns, long fpgaClockFrequency, boolean activeLow) {
    final var nrColAddrBits = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
    final var scanningReload = (int) (fpgaClockFrequency / 1000);
    final var nrOfScanningBitsCount = LedArrayGenericHdlGeneratorFactory.getNrOfBitsRequired(scanningReload);
    final var maxNrLeds = ((int) Math.pow(2.0, (double) nrColAddrBits)) * nrOfRows;

    final var contents =
        LineBuffer.getBuffer()
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("ledsCount", nrOfRows * nrOfColumns)
            .pair("maxNrLeds", MAX_NR_LEDS_STRING)
            .pair("maxNrLedsCount", maxNrLeds)
            .pair("nrOfRows", NR_OF_ROWS_STRING)
            .pair("nrOfRowsCount", nrOfRows)
            .pair("nrOfColumns", NR_OF_COLUMNS_STRING)
            .pair("nrOfColumnsCount", nrOfColumns)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("activeLowValue", activeLow ? "1" : "0")
            .pair("nrColAddrBits", NR_OF_COLUMN_ADDRESS_BITS_STRING)
            .pair("nrColAddrBitsCount", nrColAddrBits)
            .pair("scanningCounterBits", SCANNING_COUNTER_BITS_STRING)
            .pair("nrOfScanningBitsCount", nrOfScanningBitsCount)
            .pair("scanningCounter", SCANNING_COUNTER_VALUE_STRING)
            .pair("scanningValue", (scanningReload - 1));

    if (Hdl.isVhdl()) {
      contents.add("""
          GENERIC MAP ( {{nrOfLeds}} => {{ledsCount}},
                        {{nrOfRows}} => {{nrOfRowsCount}},
                        {{nrOfColumns}} => {{nrOfColumnsCount}},
                        {{nrColAddrBits}} => {{nrColAddrBitsCount}},
                        {{scanningCounterBits}} => {{nrOfScanningBitsCount}},
                        {{scanningCounter}} => {{scanningValue}},
                        {{maxNrLeds}} => {{maxNrLedsCount}},
                        {{activeLow}} => {{activeLowValue}} )
          """);
    } else {
      contents.add("""
          #( .{{nrOfLeds}}({{ledsCount}}),
             .{{nrOfRows}}({{nrOfRowsCount}}),
             .{{nrOfColumns}}({{nrOfColumnsCount}}),
             .{{nrColAddrBits}}({{nrColAddrBitsCount}}),
             .{{scanningCounterBits}}({{nrOfScanningBitsCount}}),
             .{{scanningCounter}}({{scanningValue}}),
             .{{maxNrLeds}}({{maxNrLedsCount}}),
             .{{activeLow}}({{activeLowValue}}) )
             """);
    }
    return contents.getWithIndent(6);
  }

  public static List<String> getPortMap(int id) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("columnAddress", LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress)
            .pair("outs", LedArrayGenericHdlGeneratorFactory.LedArrayRowOutputs)
            .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
            .pair("ins", LedArrayGenericHdlGeneratorFactory.LedArrayInputs)
            .pair("id", id);

    if (Hdl.isVhdl()) {
      contents.add("""
          PORT MAP ( {{columnAddress}} => {{columnAddress}}{{id}},
                     {{outs}} => {{outs}}{{id}},
                     {{clock}} => {{clock}},
                     {{ins}} => s_{{ins}}{{id}} );
          """);
    } else {
      contents.add("""
          ( .{{columnAddress}}({{columnAddress}}{{id}}),
            .{{outs}}({{outs}}{{id}}),
            .{{clock}}({{clock}}),
            .{{ins}}({{s_{{ins}}{{id}}) );
          """);
    }
    return contents.getWithIndent(6);
  }

  public List<String> getColumnCounterCode() {
    final var contents =
        LineBuffer.getBuffer()
            .pair("columnAddress", LedArrayGenericHdlGeneratorFactory.LedArrayColumnAddress)
            .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK)
            .pair("counterBits", SCANNING_COUNTER_BITS_STRING)
            .pair("counterValue", SCANNING_COUNTER_VALUE_STRING);

    if (Hdl.isVhdl()) {
      contents.add(
          """

          {{columnAddress}} <= s_columnCounterReg;

          s_tickNext <= '1' WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{counterBits}})) ELSE '0';

          s_scanningCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation
                                   std_logic_vector(to_unsigned({{counterValue}}-1, {{counterBits}}))
                                      WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{counterBits}})) ELSE
                                   std_logic_vector(unsigned(s_scanningCounterReg)-1);

          s_columnCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation
                                 s_columnCounterReg WHEN s_tickReg = '0' ELSE
                                 std_logic_vector(to_unsigned(nrOfColumns-1,nrOfcolumnAddressBits))
                                    WHEN s_columnCounterReg = std_logic_vector(to_unsigned(0,nrOfColumnAddressBits)) ELSE
                                 std_logic_vector(unsigned(s_columnCounterReg)-1);

          makeFlops : PROCESS ({{clock}}) IS
          BEGIN
             IF (rising_edge({{clock}})) THEN
                s_columnCounterReg   <= s_columnCounterNext;
                s_scanningCounterReg <= s_scanningCounterNext;
                s_tickReg            <= s_tickNext;
             END IF;
          END PROCESS makeFlops;
          """);
    } else {
      contents
          .add("""

              assign columnAddress = s_columnCounterReg;

              assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;
              assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{counterValue}} : s_scanningCounterReg - 1;
              assign s_columnCounterNext = (s_tickReg == 1'b0) ? s_columnCounterReg :
                                           (s_columnCounterReg == 0) ? nrOfColumns-1 : s_columnCounterReg-1;
              """)
          .addRemarkBlock("Here the simulation only initial is defined")
          .add("""
              initial
              begin
                 s_columnCounterReg   = 0;
                 s_scanningCounterReg = 0;
                 s_tickReg            = 1'b0;
              end

              always @(posedge {{clock}})
              begin
                  s_columnCounterReg   = s_columnCounterNext;
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
            .pair("ins", LedArrayGenericHdlGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHdlGeneratorFactory.LedArrayRowOutputs)
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("nrOfRows", NR_OF_ROWS_STRING)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .add(getColumnCounterCode());

    if (Hdl.isVhdl()) {
      contents.add("""
          makeVirtualInputs : PROCESS ( internalLeds ) IS
          BEGIN
             s_maxLedInputs <= (OTHERS => '0');
             IF ({{activeLow}} = 1) THEN
                s_maxLedInputs( {{nrOfLeds}}-1 DOWNTO 0) <= NOT {{ins}};
             ELSE
                s_maxLedInputs( {{nrOfLeds}}-1 DOWNTO 0) <= {{ins}};
             END IF;
          END PROCESS makeVirtualInputs;

          GenOutputs : FOR n IN {{nrOfRows}}-1 DOWNTO 0 GENERATE
             {{outs}}(n) <= s_maxLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);
          END GENERATE GenOutputs;
          """);
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < {{nrOfRows}}; i = i + 1)
             begin: outputs
                assign {{outs}}[i] = (activeLow == 1)
                    ? ~{{ins}}[i * nrOfColumns + s_columnCounterReg]
                    :  {{ins}}[i * nrOfColumns + s_columnCounterReg];
             end
          endgenerate
          """);
    }
    return contents;
  }
}
