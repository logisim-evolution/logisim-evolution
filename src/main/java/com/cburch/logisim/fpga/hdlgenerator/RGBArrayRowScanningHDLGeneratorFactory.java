/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
    public static ArrayList<String> getPortMap(int id) {
      final var contents =
              new LineBuffer(
                      new LineBuffer.Pairs() {
                        {
                          add("redIns", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs);
                          add("greenIns", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs);
                          add("blueIns", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs);
                          add("redOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs);
                          add("greenOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs);
                          add("blueOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs);
                          add("rowAddress", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress);
                          add("clock", TickComponentHDLGeneratorFactory.FPGAClock);
                          add("id", id);
                        }
                      });

    if (HDL.isVHDL()) {
      contents.add(
          "PORT MAP ( {{rowAddress}} => {{rowAddress}}{{id}}",
          "           {{clock     }} => {{clock}},",
          "           {{redOuts   }} => {{redOuts}}{{id}},",
          "           {{greenOuts }} => {{greenOuts}}{{id}},",
          "           {{blueOuts  }} => {{blueOuts}}{{id}},",
          "           {{redIns    }} => s_{{redIns}}{{id}},",
          "           {{greenIns  }} => s_{{greenIns}}{{id}},",
          "           {{blueIns   }} => s_{{blueIns}}{{id}});");
    } else {
      contents.add(
          "(.{{rowAddress}}({{rowAddress}}{{id}}),",
          " .{{clock     }}({{clock}}),",
          " .{{redOuts   }}({{redOuts}}{{id}}),",
          " .{{greenOuts }}({{greenOuts}}{{id}}),",
          " .{{blueOuts  }}({{blueOuts}}{{id}}),",
          " .{{redIns    }}(s_{{redIns}}{{id}}),",
          " .{{greenIns  }}(s_{{greenIns}}{{id}}),",
          " .{{blueIns   }}(s_{{blueIns}}{{id}})); ");
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
    inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
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
        new LineBuffer(
            new LineBuffer.Pairs() {
              {
                add("redIns", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs);
                add("greenIns", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs);
                add("blueIns", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs);
                add("redOuts", LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs);
                add("greenOuts", LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs);
                add("blueOuts", LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs);
                add("activeLow", activeLowString);
                add("nrOfLeds", nrOfLedsString);
                add("nrOfColumns", nrOfColumnsString);
              }
            });
    contents.add(getRowCounterCode());
    if (HDL.isVHDL()) {
      contents.add(
          "makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS",
          "BEGIN",
          "   s_maxRedLedInputs <= (OTHERS => '0');",
          "   s_maxGreenLedInputs <= (OTHERS => '0');",
          "   s_maxBlueLedInputs <= (OTHERS => '0');",
          "   IF ({{activeLow}} = 1) THEN",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{redIns}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{greenIns}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{blueIns}};",
          "   ELSE",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{redIns}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{greenIns}};",
          "      s_maxRedLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{blueIns}};",
          "   END IF;",
          "END PROCESS makeVirtualInputs;",
          "",
          "GenOutputs : FOR n IN {{nrOfColumns}}-1 DOWNTO 0 GENERATE",
          "   {{redOuts}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "   {{greenOuts}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "   {{blueOuts}}(n) <= s_maxRedLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "END GENERATE GenOutputs;");
    } else {
      contents.add(
          "",
          "genvar i;",
          "generate",
          "   for (i = 0; i < {{nrOfColumns}}; i = i + 1) begin",
          "      assign {{redOuts}}[i] = (activeLow == 1)",
          "         ? ~{{redIns}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "         : {{redIns}}[nrOfColumns * s_rowCounterReg + i];",
          "      assign {{greenOuts}}[i] = (activeLow == 1)",
          "         ? ~{{greenIns}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "          : {{greenIns}}[{{nrOfColumns}} * s_rowCounterReg + i];",
          "      assign {{blueOuts}}[i] = (activeLow == 1)",
          "          ? ~{{blueIns}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "         : {{blueIns}}[{{nrOfColumns}} * s_rowCounterReg + i];",
          "   end",
          "endgenerate");
    }
    return contents.getWithIndent(3);
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }

}
