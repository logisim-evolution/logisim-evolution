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

import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;

public class RGBArrayLedDefaultHDLGeneratorFactory extends LedArrayLedDefaultHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayLedDefault";

  public static ArrayList<String> getPortMap(int id) {
    final var map = new ArrayList<String>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + id
          + ");");
    } else {
      map.add("      (."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "( s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "( s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + id
          + "));");
    }
    return map;
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
    final var redIn = LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs;
    final var greenIn = LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs;
    final var blueIn = LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs;

    final var redOut = LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs;
    final var greenOut = LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs;
    final var blueOut = LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs;

    final var contents = new ContentBuilder();
    if (HDL.isVHDL()) {
      contents
          .add("   genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE")
          .add("      %s(n) <= NOT(%s(n)) WHEN activeLow = 1 ELSE %s(n);", redOut, redIn, redIn)
          .add("      %s(n) <= NOT(%s(n)) WHEN activeLow = 1 ELSE %s(n);", greenOut, greenIn, greenIn)
          .add("      %s(n) <= NOT(%s(n)) WHEN activeLow = 1 ELSE %s(n);", blueOut, blueIn, blueIn)
          .add("   END GENERATE;");
    } else {
      contents
          .add("   genvar i;")
          .add("   generate")
          .add("      for (i = 0; i < nrOfLeds; i = i + 1) begin")
          .add("         assign %s[i] = (activeLow == 1) ? ~%s[n] : %s[n];", redOut, redIn, redIn)
          .add("         assign %s[i] = (activeLow == 1) ? ~%s[n] : %s[n];", greenOut, greenIn, greenIn)
          .add("         assign %s[i] = (activeLow == 1) ? ~%s[n] : %s[n];", blueOut, blueIn, blueIn)
          .add("      end")
          .add("   endgenerate");
    }
    return contents.get();
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }

}
