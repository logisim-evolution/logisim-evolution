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
          add("outsR", LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs);
          add("outsG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs);
          add("outsB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs);
          add("insR", LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs);
          add("insG", LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs);
          add("insB", LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs);
          add("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);
        }
      };

  public static ArrayList<String> getPortMap(int id) {
    final var contents = new LineBuffer(sharedPairs);
    contents.add("id", id);

    if (HDL.isVHDL()) {
      contents.addLines(
          "PORT MAP ( {{outsR}} => {{outsR}}{{id}},",
          "           {{outsG}} => {{outsG}}{{id}},",
          "           {{outsB}} => {{outsB}}{{id}},",
          "           {{insR }} => s_{{insR}}{{id}},",
          "           {{insG }} => s_{{insG}}{{id}},",
          "           {{insB }} => s_{{insB}}{{id}} );");
    } else {
      contents.addLines(
          "( .{{outsR}}({{outsR}}{{id}}),",
          "  .{{outsG}}({{outsG}}{{id}}),",
          "  .{{outsB}}({{outsB}}{{id}}),",
          "  .{{insR}}(s_{{insR}}{{id}}),",
          "  .{{insG}}(s_{{insG}}{{id}}),",
          "  .{{insB}}(s_{{insB}}{{id}}) );");
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
      contents.addLines(
          "genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE",
          "   {{outsR}}(n) <= NOT({{insR}}(n)) WHEN activeLow = 1 ELSE {{insR}}(n);",
          "   {{outsG}}(n) <= NOT({{insG}}(n)) WHEN activeLow = 1 ELSE {{insG}}(n);",
          "   {{outsB}}(n) <= NOT({{insB}}(n)) WHEN activeLow = 1 ELSE {{insB}}(n);",
          "END GENERATE;");
    } else {
      contents.addLines(
          "genvar i;",
          "generate",
          "   for (i = 0; i < nrOfLeds; i = i + 1)",
          "   begin:outputs",
          "      assign {{outsR}}[i] = (activeLow == 1) ? ~{{insR}}[n] : {{insR}}[n];",
          "      assign {{outsG}}[i] = (activeLow == 1) ? ~{{insG}}[n] : {{insG}}[n];",
          "      assign {{outsB}}[i] = (activeLow == 1) ? ~{{insB}}[n] : {{insB}}[n];",
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
