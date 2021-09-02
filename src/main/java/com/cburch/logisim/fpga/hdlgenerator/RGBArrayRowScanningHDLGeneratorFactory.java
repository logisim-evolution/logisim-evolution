/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RGBArrayRowScanningHDLGeneratorFactory extends LedArrayRowScanningHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayRowScanning";

  static final LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs()
          .add("insR", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs)
          .add("insG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs)
          .add("insB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs)
          .add("outsR", LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs)
          .add("outsG", LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs)
          .add("outsB", LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs);

  public static ArrayList<String> getPortMap(int id) {
    final var contents =
        (new LineBuffer(sharedPairs))
            .pair("address", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK)
            .pair("id", id);

    if (HDL.isVHDL()) {
      contents.addLines(
          "PORT MAP ( {{address}} => {{address}}{{id}}",
          "           {{clock     }} => {{clock}},",
          "           {{outsR     }} => {{outsR}}{{id}},",
          "           {{outsG     }} => {{outsG}}{{id}},",
          "           {{outsB     }} => {{outsB}}{{id}},",
          "           {{insR      }} => s_{{insR}}{{id}},",
          "           {{insG      }} => s_{{insG}}{{id}},",
          "           {{insB      }} => s_{{insB}}{{id}});");
    } else {
      contents.addLines(
          "( .{{address}}({{address}}{{id}}),",
          "  .{{clock     }}({{clock}}),",
          "  .{{outsR   }}({{outsR}}{{id}}),",
          "  .{{outsG }}({{outsG}}{{id}}),",
          "  .{{outsB  }}({{outsB}}{{id}}),",
          "  .{{insR    }}(s_{{insR}}{{id}}),",
          "  .{{insG  }}(s_{{insG}}{{id}}),",
          "  .{{insB   }}(s_{{insB}}{{id}}) ); ");
    }
    return contents.getWithIndent(6);
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress, nrOfRowAddressBitsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs, nrOfColumsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs, nrOfColumsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs, nrOfColumsGeneric);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.putAll(super.GetWireList(attrs, Nets));
    wires.put("s_maxRedLedInputs", maxNrLedsGeneric);
    wires.put("s_maxBlueLedInputs", maxNrLedsGeneric);
    wires.put("s_maxGreenLedInputs", maxNrLedsGeneric);
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        (new LineBuffer(sharedPairs))
            .pair("activeLow", activeLowString)
            .pair("nrOfLeds", nrOfLedsString)
            .pair("nrOfColumns", nrOfColumnsString);

    contents.add(getRowCounterCode());
    if (HDL.isVHDL()) {
      contents.addLines(
          "",
          "makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS",
          "BEGIN",
          "   s_maxRedLedInputs <= (OTHERS => '0');",
          "   s_maxGreenLedInputs <= (OTHERS => '0');",
          "   s_maxBlueLedInputs <= (OTHERS => '0');",
          "   IF ({{activeLow}} = 1) THEN",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insR}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insG}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{insB}};",
          "   ELSE",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insR}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insG}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{insB}};",
          "   END IF;",
          "END PROCESS makeVirtualInputs;",
          "",
          "GenOutputs : FOR n IN {{nrOfColumns}}-1 DOWNTO 0 GENERATE",
          "   {{outsR}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "   {{outsG}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "   {{outsB}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "END GENERATE GenOutputs;");
    } else {
      contents.addLines(
          "",
          "genvar i;",
          "generate",
          "   for (i = 0; i < {{nrOfColumns}}; i = i + 1)",
          "   begin:outputs",
          "      assign {{outsR}}[i] = (activeLow == 1)",
          "         ? ~{{insR}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "         : {{insR}}[nrOfColumns * s_rowCounterReg + i];",
          "      assign {{outsG}}[i] = (activeLow == 1)",
          "         ? ~{{insG}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "          : {{insG}}[{{nrOfColumns}} * s_rowCounterReg + i];",
          "      assign {{outsB}}[i] = (activeLow == 1)",
          "          ? ~{{insB}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "         : {{insB}}[{{nrOfColumns}} * s_rowCounterReg + i];",
          "   end",
          "endgenerate");
    }
    return contents.getWithIndent();
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }

}
