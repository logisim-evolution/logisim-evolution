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

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;

public class RGBArrayLedDefaultHDLGeneratorFactory extends LedArrayLedDefaultHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayLedDefault";

  private static final LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs() {
        {
          add("clock", TickComponentHDLGeneratorFactory.FPGAClock);
          add("redOuts", LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs);
          add("greenOuts", LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs);
          add("blueOuts", LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs);
          add("redIns", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs);
          add("greenIns", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs);
          add("blueIns", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs);
        }
      };

  public static ArrayList<String> getPortMap(int id) {
    final var contents = new LineBuffer(sharedPairs);
    contents.add("id", id);

    if (HDL.isVHDL()) {
      contents.add(
          "PORT MAP ( {{redOuts   }} => {{redOuts}}{{id}},",
          "            {{greenOuts}} => {{greenOuts}}{{id}},",
          "            {{blueOuts }} => {{blueOuts}}{{id}},",
          "            {{redIns   }} => s_{{redIns}}{{id}},",
          "            {{greenIns }} => s_{{greenIns}}{{id}},",
          "            {{blueIns  }} => s_{{blueIns}}{{id}} );");
    } else {
      contents.add(
          "(.{{redOuts  }}({{redOuts}}{{id}}),",
          " .{{greenOuts}}({{greenOuts}}{{id}}),",
          " .{{blueOuts }}({{blueOuts}}{{id}}),",
          " .{{redIns   }}(s_{{redIns}}{{id}}),",
          " .{{greenIns }}(s_{{greenIns}}{{id}}),",
          " .{{blueIns  }}(s_{{blueIns}}{{id}}));");
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
      contents.add(
          "genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE",
          "   {{redOuts  }}(n) <= NOT({{redIns  }}(n)) WHEN activeLow = 1 ELSE {{redIns  }}(n);",
          "   {{greenOuts}}(n) <= NOT({{greenIns}}(n)) WHEN activeLow = 1 ELSE {{greenIns}}(n);",
          "   {{blueOuts }}(n) <= NOT({{blueIns }}(n)) WHEN activeLow = 1 ELSE {{blueIns }}(n);",
          "END GENERATE;");
    } else {
      contents.add(
          "genvar i;",
          "generate",
          "   for (i = 0; i < nrOfLeds; i = i + 1) begin",
          "      assign {{redOuts  }}[i] = (activeLow == 1) ? ~{{redIns}}[n] : {{redIns}}[n];",
          "      assign {{greenOuts}}[i] = (activeLow == 1) ? ~{{greenIns}}[n] : {{greenIns}}[n];",
          "      assign {{blueOuts }}[i] = (activeLow == 1) ? ~{{blueIns}}[n] : {{blueIns}}[n];",
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
