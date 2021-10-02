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
import java.util.List;

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

  private static final LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs() {
        {
          pair("outsR", LedArrayGenericHdlGeneratorFactory.LedArrayRedOutputs);
          pair("outsG", LedArrayGenericHdlGeneratorFactory.LedArrayGreenOutputs);
          pair("outsB", LedArrayGenericHdlGeneratorFactory.LedArrayBlueOutputs);
          pair("insR", LedArrayGenericHdlGeneratorFactory.LedArrayRedInputs);
          pair("insG", LedArrayGenericHdlGeneratorFactory.LedArrayGreenInputs);
          pair("insB", LedArrayGenericHdlGeneratorFactory.LedArrayBlueInputs);
          pair("clock", TickComponentHdlGeneratorFactory.FPGA_CLOCK);
        }
      };

  public static List<String> getPortMap(int id) {
    final var contents = new LineBuffer(sharedPairs);
    contents.add("id", id);

    if (Hdl.isVhdl()) {
      contents.add("""
          PORT MAP ( {{outsR}} => {{outsR}}{{id}},
                     {{outsG}} => {{outsG}}{{id}},
                     {{outsB}} => {{outsB}}{{id}},
                     {{insR }} => s_{{insR}}{{id}},
                     {{insG }} => s_{{insG}}{{id}},
                     {{insB }} => s_{{insB}}{{id}} );
          """);
    } else {
      contents.add("""
          ( .{{outsR}}({{outsR}}{{id}}),
            .{{outsG}}({{outsG}}{{id}}),
            .{{outsB}}({{outsB}}{{id}}),
            .{{insR}}(s_{{insR}}{{id}}),
            .{{insG}}(s_{{insG}}{{id}}),
            .{{insB}}(s_{{insB}}{{id}}) );
          """);
    }
    return contents.getWithIndent(6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer(sharedPairs);

    if (Hdl.isVhdl()) {
      contents.add("""
          genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE
             {{outsR}}(n) <= NOT({{insR}}(n)) WHEN activeLow = 1 ELSE {{insR}}(n);
             {{outsG}}(n) <= NOT({{insG}}(n)) WHEN activeLow = 1 ELSE {{insG}}(n);
             {{outsB}}(n) <= NOT({{insB}}(n)) WHEN activeLow = 1 ELSE {{insB}}(n);
          END GENERATE;
          """);
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
          """);
    }
    return contents;
  }
}
