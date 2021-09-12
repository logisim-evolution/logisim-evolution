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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;

public class LedArrayLedDefaultHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static final int nrOfLedsGeneric = -1;
  public static final int activeLowGeneric = -2;
  public static final String nrOfLedsString = "nrOfLeds";
  public static final String activeLowString = "activeLow";
  public static String LedArrayName = "LedArrayLedDefault"; 
  
  public LedArrayLedDefaultHDLGeneratorFactory() {
    super(LedArrayName);
  }

  public LedArrayLedDefaultHDLGeneratorFactory(String id) {
    super(id);
  }

  public static ArrayList<String> getGenericMap(int nrOfRows, int nrOfColumns, long fpgaClockFrequency, boolean activeLow) {
    final var contents =
        (new LineBuffer())
            .pair("nrOfLeds", nrOfLedsString)
            .pair("ledsCount", nrOfRows * nrOfColumns)
            .pair("rows", nrOfRows)
            .pair("cols", nrOfColumns)
            .pair("activeLow", activeLowString)
            .pair("activeLowVal", activeLow ? "1" : "0");

    if (HDL.isVHDL()) {
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

  public static ArrayList<String> getPortMap(int id) {
    final var map =
        (new LineBuffer())
            .pair("id", id)
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHDLGeneratorFactory.LedArrayOutputs);
    if (HDL.isVHDL()) {
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
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayOutputs, nrOfLedsGeneric);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayInputs, nrOfLedsGeneric);
    return inputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var generics = new TreeMap<Integer, String>();
    generics.put(nrOfLedsGeneric, nrOfLedsString);
    generics.put(activeLowGeneric, activeLowString);
    return generics;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        (new LineBuffer())
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHDLGeneratorFactory.LedArrayOutputs);

    if (HDL.isVHDL()) {
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
    return contents.getWithIndent();
  }
}
