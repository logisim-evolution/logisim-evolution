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

import java.util.HashMap;

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

  public static LineBuffer getGenericMap(int nrOfRows, int nrOfColumns, long fpgaClockFrequency, boolean activeLow) {
    final var generics = new HashMap<String, String>();
    generics.put(NR_OF_LEDS_STRING, Integer.toString(nrOfRows * nrOfColumns));
    generics.put(ACTIVE_LOW_STRING, activeLow ? "1" : "0");
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(generics, true);
  }

  public static LineBuffer getPortMap(int id) {
    final var ports = new HashMap<String, String>();
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayInputs, id));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(ports, false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("ins", LedArrayGenericHdlGeneratorFactory.LedArrayInputs)
        .pair("outs", LedArrayGenericHdlGeneratorFactory.LedArrayOutputs);

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          genLeds : {{for}} n {{in}} (nrOfLeds-1) {{downto}} 0 {{generate}}
             {{outs}}(n) <= {{not}}({{ins}}(n)) {{when}} activeLow = 1 {{else}} {{ins}}(n);
          {{end}} {{generate}};
          """).empty();
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < nrOfLeds; i = i + 1)
             begin:outputs
                assign {{outs}}[i] = (activeLow == 1) ? ~{{ins}}[i] : {{ins}}[i];
             end
          endgenerate
          """).empty();
    }
    return contents;
  }
}
