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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.List;

public class LedArrayLedDefaultHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_LEDS_ID = -1;
  public static final int ACTIVE_LOW_ID = -2;
  public static final String NR_OF_LEDS_STRING = "nrOfLeds";
  public static final String ACTIVE_LOW_STRING = "activeLow";
  public static final String HDL_IDENTIFIER = "LedArrayLedDefault";

  public LedArrayLedDefaultHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_LEDS_STRING, NR_OF_LEDS_ID)
        .add(ACTIVE_LOW_STRING, ACTIVE_LOW_ID);
    myPorts
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayInputs, NR_OF_LEDS_ID, 0)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayOutputs, NR_OF_LEDS_ID, 1);
  }

  public static List<String> getGenericMap(int nrOfRows, int nrOfColumns, long fpgaClockFrequency, boolean activeLow) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("ledsCount", nrOfRows * nrOfColumns)
            .pair("rows", nrOfRows)
            .pair("cols", nrOfColumns)
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("activeLowVal", activeLow ? "1" : "0");

    if (Hdl.isVhdl()) {
      contents.add("""
          GENERIC MAP ( {{nrOfLeds}} => {{ledsCount}},
                        {{activeLow}} => {{activeLowVal}} )
          """);
    } else {
      contents.add("""
          #( .{{nrOfLeds}}({{ledsCount}}),
             .{{activeLow}}({{activeLowVal}}) )
          """);
    }
    return contents.getWithIndent(6);
  }

  public static List<String> getPortMap(int id) {
    final var map =
        LineBuffer.getBuffer()
            .pair("id", id)
            .pair("ins", LedArrayGenericHdlGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHdlGeneratorFactory.LedArrayOutputs);
    if (Hdl.isVhdl()) {
      map.add("""
          PORT MAP ( {{outs}} => {{outs}}{{id}},
                     {{ins }} => s_{{ins}}{{id}} );
          """);
    } else {
      map.add("""
          ( .{{outs}}({{outs}}{{id}}),
            .{{ins}}(s_{{ins}}{{id}}) );
          """);
    }
    return map.getWithIndent(6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("ins", LedArrayGenericHdlGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHdlGeneratorFactory.LedArrayOutputs);

    if (Hdl.isVhdl()) {
      contents.add("""
          genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE
             {{outs}}(n) <= NOT({{ins}}(n)) WHEN activeLow = 1 ELSE {{ins}}(n);
          END GENERATE;
          """);
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < nrOfLeds; i = i + 1)
             begin:outputs
                assign {{outs}}[i] = (activeLow == 1) ? ~{{ins}}[i] : {{ins}}[i];
             end
          endgenerate
          """);
    }
    return contents;
  }
}