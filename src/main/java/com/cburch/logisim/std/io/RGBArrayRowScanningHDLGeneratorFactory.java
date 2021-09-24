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
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RGBArrayRowScanningHDLGeneratorFactory extends LedArrayRowScanningHDLGeneratorFactory {

  public static final String HDL_IDENTIFIER = "RGBArrayRowScanning";

  public RGBArrayRowScanningHDLGeneratorFactory() {
    super();
    myWires
        .addWire("s_maxRedLedInputs", MAX_NR_LEDS_ID)
        .addWire("s_maxBlueLedInputs", MAX_NR_LEDS_ID)
        .addWire("s_maxGreenLedInputs", MAX_NR_LEDS_ID);
  }

  static final LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs()
          .pair("insR", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs)
          .pair("insG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs)
          .pair("insB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs)
          .pair("outsR", LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs)
          .pair("outsG", LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs)
          .pair("outsB", LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs);

  public static ArrayList<String> getPortMap(int id) {
    final var contents =
        (new LineBuffer(sharedPairs))
            .pair("addr", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK)
            .pair("id", id);

    if (HDL.isVHDL()) {
      contents.add("""
          PORT MAP ( {{addr }} => {{addr}}{{id}}
                     {{clock}} => {{clock}},
                     {{outsR}} => {{outsR}}{{id}},
                     {{outsG}} => {{outsG}}{{id}},
                     {{outsB}} => {{outsB}}{{id}},
                     {{insR }} => s_{{insR}}{{id}},
                     {{insG }} => s_{{insG}}{{id}},
                     {{insB }} => s_{{insB}}{{id}} );
          """);
    } else {
      contents.add("""
          ( .{{addr }}({{addr}}{{id}}),
            .{{clock}}({{clock}}),
            .{{outsR}}({{outsR}}{{id}}),
            .{{outsG}}({{outsG}}{{id}}),
            .{{outsB}}({{outsB}}{{id}}),
            .{{insR }}(s_{{insR}}{{id}}),
            .{{insG }}(s_{{insG}}{{id}}),
            .{{insB }}(s_{{insB}}{{id}}) );
          """);
    }
    return contents.getWithIndent(6);
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress, NR_OF_ROW_ADDRESS_BITS_ID);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs, NR_OF_COLUMS_ID);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs, NR_OF_COLUMS_ID);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs, NR_OF_COLUMS_ID);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, NR_OF_LEDS_ID);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, NR_OF_LEDS_ID);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, NR_OF_LEDS_ID);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        (new LineBuffer(sharedPairs))
            .pair("activeLow", ACTIVE_LOW_STRING)
            .pair("nrOfLeds", NR_OF_LEDS_STRING)
            .pair("nrOfColumns", NR_OF_COLUMS_STRING);

    contents.add(getRowCounterCode());
    if (HDL.isVHDL()) {
      contents.add("""
          
          makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS
          BEGIN
             s_maxRedLedInputs <= (OTHERS => '0');
             s_maxGreenLedInputs <= (OTHERS => '0');
             s_maxBlueLedInputs <= (OTHERS => '0');
             IF ({{activeLow}} = 1) THEN
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insR}};
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insG}};
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insB}};
             ELSE
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insR}};
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insG}};
                s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insB}};
             END IF;
          END PROCESS makeVirtualInputs;
          
          GenOutputs : FOR n IN {{nrOfColumns}}-1 DOWNTO 0 GENERATE
             {{outsR}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);
             {{outsG}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);
             {{outsB}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);
          END GENERATE GenOutputs;
          """);
    } else {
      contents.add("""
          
          genvar i;
          generate
             for (i = 0; i < {{nrOfColumns}}; i = i + 1)
             begin:outputs
                assign {{outsR}}[i] = (activeLow == 1)
                   ? ~{{insR}}[{{nrOfColumns}} * s_rowCounterReg + i]
                   :  {{insR}}[{{nrOfColumns}} * s_rowCounterReg + i];
                assign {{outsG}}[i] = (activeLow == 1)
                   ? ~{{insG}}[{{nrOfColumns}} * s_rowCounterReg + i]
                   :  {{insG}}[{{nrOfColumns}} * s_rowCounterReg + i];
                assign {{outsB}}[i] = (activeLow == 1)
                   ? ~{{insB}}[{{nrOfColumns}} * s_rowCounterReg + i]
                   :  {{insB}}[{{nrOfColumns}} * s_rowCounterReg + i];
             end
          endgenerate" +
          """);
    }
    return contents.getWithIndent();
  }
}
