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

public class RGBArrayRowScanningHDLGeneratorFactory extends LedArrayRowScanningHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayRowScanning";

  public static ArrayList<String> getPortMap(int id) {
    ArrayList<String> map = new ArrayList<>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress 
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress 
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
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
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress
          + id
          + "),");
      map.add("       ." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + id
          + "),");
      map.add("       ." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + id
          + "),");
      map.add("       ." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "(s_"
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
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress, nrOfRowAddressBitsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs, nrOfColumsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs, nrOfColumsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs, nrOfColumsGeneric);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.putAll(super.GetWireList(attrs, Nets));
    Wires.put("s_maxRedLedInputs", maxNrLedsGeneric);
    Wires.put("s_maxBlueLedInputs", maxNrLedsGeneric);
    Wires.put("s_maxGreenLedInputs", maxNrLedsGeneric);
    return Wires;
  }
  
  
  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    Contents.addAll(getRowCounterCode());
    if (HDL.isVHDL()) {
      Contents.add("   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS");
      Contents.add("   BEGIN");
      Contents.add("      s_maxRedLedInputs <= (OTHERS => '0');");
      Contents.add("      s_maxGreenLedInputs <= (OTHERS => '0');");
      Contents.add("      s_maxBlueLedInputs <= (OTHERS => '0');");
      Contents.add("      IF (" + activeLowString + " = 1) THEN");
      Contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      Contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      Contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      Contents.add("                                       ELSE");
      Contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      Contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      Contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS makeVirtualInputs;");
      Contents.add("");
      Contents.add("   GenOutputs : FOR n IN " + nrOfColumnsString + "-1 DOWNTO 0 GENERATE");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + "(n)   <= s_maxRedLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + "(n) <= s_maxGreenLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + "(n)  <= s_maxBlueLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      Contents.add("   END GENERATE GenOutputs;");
    } else {
      Contents.add("");
      Contents.add("   genvar i;");
      Contents.add("   generate");
      Contents.add("      for (i = 0; i < " + nrOfColumnsString + "; i = i + 1) begin");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
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
