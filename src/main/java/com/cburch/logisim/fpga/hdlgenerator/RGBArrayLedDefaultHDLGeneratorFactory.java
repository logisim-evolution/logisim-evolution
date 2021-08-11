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

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;

public class RGBArrayLedDefaultHDLGeneratorFactory extends LedArrayLedDefaultHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayLedDefault";

  public static ArrayList<String> getPortMap(int id) {
    ArrayList<String> map = new ArrayList<>();
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
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs, nrOfLedsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs, nrOfLedsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs, nrOfLedsGeneric);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    if (HDL.isVHDL()) {
      Contents.add("   genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + "(n)   <= NOT(" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "(n)) WHEN activeLow = 1 ELSE " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "(n);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + "(n)   <= NOT(" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "(n)) WHEN activeLow = 1 ELSE " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "(n);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + "(n)   <= NOT(" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "(n)) WHEN activeLow = 1 ELSE " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "(n);");
      Contents.add("   END GENERATE;");
    } else {
      Contents.add("   genvar i;");
      Contents.add("   generate");
      Contents.add("      for (i = 0; i < nrOfLeds; i = i + 1) begin");
      Contents.add("         assign "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs
          + "[i]   = (activeLow == 1) ? ~"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[n] : " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[n];");
      Contents.add("         assign "
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs
          + "[i]   = (activeLow == 1) ? ~"
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[n] : " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[n];");
      Contents.add("         assign "
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs
          + "[i]   = (activeLow == 1) ? ~"
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[n] : " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[n];");
      Contents.add("      end");
      Contents.add("   endgenerate");
    }
    return Contents;
  }


  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }
  
}
