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
    final var map = new ArrayList<String>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress 
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress 
          + id
          + ",");
      map.add("                 "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + " => "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + " => "
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
          + TickComponentHDLGeneratorFactory.FPGAClock
          + "("
          + TickComponentHDLGeneratorFactory.FPGAClock
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
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.addAll(getRowCounterCode());
    if (HDL.isVHDL()) {
      contents.add("   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS");
      contents.add("   BEGIN");
      contents.add("      s_maxRedLedInputs <= (OTHERS => '0');");
      contents.add("      s_maxGreenLedInputs <= (OTHERS => '0');");
      contents.add("      s_maxBlueLedInputs <= (OTHERS => '0');");
      contents.add("      IF (" + activeLowString + " = 1) THEN");
      contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      contents.add("                                       ELSE");
      contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      contents.add("      END IF;");
      contents.add("   END PROCESS makeVirtualInputs;");
      contents.add("");
      contents.add("   GenOutputs : FOR n IN " + nrOfColumnsString + "-1 DOWNTO 0 GENERATE");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + "(n)   <= s_maxRedLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + "(n) <= s_maxGreenLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + "(n)  <= s_maxBlueLedInputs(" + nrOfColumnsString + "*to_integer(unsigned(s_rowCounterReg)) + n);");
      contents.add("   END GENERATE GenOutputs;");
    } else {
      contents.add("");
      contents.add("   genvar i;");
      contents.add("   generate");
      contents.add("      for (i = 0; i < " + nrOfColumnsString + "; i = i + 1) begin");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[" + nrOfColumnsString + "*s_rowCounterReg + i];");
      contents.add("      end");
      contents.add("   endgenerate");
    }
    return contents;
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }
  
}
