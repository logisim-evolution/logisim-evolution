/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;

public class RGBArrayLedDefaultHDLGeneratorFactory extends LedArrayLedDefaultHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayLedDefault";
  
  public RGBArrayLedDefaultHDLGeneratorFactory() {
    super(RGBArrayName);
  }

  private static final LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs() {
        {
          pair("outsR", LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs);
          pair("outsG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs);
          pair("outsB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs);
          pair("insR", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs);
          pair("insG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs);
          pair("insB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs);
          pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);
        }
      };

  public static ArrayList<String> getPortMap(int id) {
    final var contents = new LineBuffer(sharedPairs);
    contents.add("id", id);

    if (HDL.isVHDL()) {
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
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs, nrOfLedsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs, nrOfLedsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs, nrOfLedsGeneric);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer(sharedPairs);

    if (HDL.isVHDL()) {
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
    return contents.getWithIndent(3);
  }
}
