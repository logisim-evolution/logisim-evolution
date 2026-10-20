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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

import java.util.HashMap;

public class RgbArrayLedDefaultHdlGeneratorFactory extends LedArrayLedDefaultHdlGeneratorFactory {

  public static final String HDL_IDENTIFIER = "RGBArrayLedDefault";

  public RgbArrayLedDefaultHdlGeneratorFactory() {
    super();
    myPorts.removePorts(); // remove the ports from the super class
    myPorts
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, NR_OF_LEDS_ID, 0)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, NR_OF_LEDS_ID, 1)
        .add(Port.INPUT, LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, NR_OF_LEDS_ID, 2)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayRedOutputs, NR_OF_LEDS_ID, 3)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayGreenOutputs, NR_OF_LEDS_ID, 4)
        .add(Port.OUTPUT, LedArrayGenericHdlGeneratorFactory.LedArrayBlueOutputs, NR_OF_LEDS_ID, 5);
  }

  public static LineBuffer getPortMap(int id) {
    final var ports = new HashMap<String, String>();
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRedOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRedOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayGreenOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayGreenOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayBlueOutputs, String.format("%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayBlueOutputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs, id));
    ports.put(LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, String.format("s_%s%d", LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs, id));
    return LedArrayGenericHdlGeneratorFactory.getGenericPortMapAlligned(ports, false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("outsR", LedArrayGenericHdlGeneratorFactory.LedArrayRedOutputs)
        .pair("outsG", LedArrayGenericHdlGeneratorFactory.LedArrayGreenOutputs)
        .pair("outsB", LedArrayGenericHdlGeneratorFactory.LedArrayBlueOutputs)
        .pair("insR", LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs)
        .pair("insG", LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs)
        .pair("insB", LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs)
        .pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK);

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          genLeds : {{for}} n {{in}} (nrOfLeds-1) {{downto}} 0 {{generate}}
             {{outsR}}(n) <= {{not}}({{insR}}(n)) {{when}} activeLow = 1 {{else}} {{insR}}(n);
             {{outsG}}(n) <= {{not}}({{insG}}(n)) {{when}} activeLow = 1 {{else}} {{insG}}(n);
             {{outsB}}(n) <= {{not}}({{insB}}(n)) {{when}} activeLow = 1 {{else}} {{insB}}(n);
          {{end}} {{generate}};
          """).empty();
    } else {
      contents.add("""
          genvar i;
          generate
             for (i = 0; i < nrOfLeds; i = i + 1)
             begin:outputs
                assign {{outsR}}[i] = (activeLow == 1) ? ~{{insR}}[n] : {{insR}}[n];
                assign {{outsG}}[i] = (activeLow == 1) ? ~{{insG}}[n] : {{insG}}[n];
                assign {{outsB}}[i] = (activeLow == 1) ? ~{{insB}}[n] : {{insB}}[n];
             end
          endgenerate
          """).empty();
    }
    return contents;
  }
}
