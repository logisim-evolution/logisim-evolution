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

public class LedArrayLedDefaultHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static int nrOfLedsGeneric = -1;
  public static int activeLowGeneric = -2;
  public static String nrOfLedsString = "nrOfLeds";
  public static String activeLowString = "activeLow";
  public static String LedArrayName = "LedArrayLedDefault";
  
  public static ArrayList<String> getGenericMap(int nrOfRows,
      int nrOfColumns,
      long FpgaClockFrequency,
      boolean activeLow) {
    ArrayList<String> map = new ArrayList<>();
    if (HDL.isVHDL()) {
      map.add("      GENERIC MAP ( " + nrOfLedsString + " => " + (nrOfRows * nrOfColumns) + ",");
      map.add("                    " + activeLowString + " => " + ((activeLow) ? "1" : "0") + ")");
    } else {
      map.add("      #( ." + nrOfLedsString + "(" + (nrOfRows * nrOfColumns) + "),");
      map.add("         ." + activeLowString + "(" + ((activeLow) ? "1" : "0") + "))");
    }
    return map;
  }
  
  public static ArrayList<String> getPortMap(int id) {
    final var map = new ArrayList<String>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs 
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs 
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + id
          + ");");
    } else {
      map.add("      (." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + id
          + "));");
    }
    return map;
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
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add("   genLeds : FOR n in (nrOfLeds-1) DOWNTO 0 GENERATE");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs
          + "(n) <= NOT("
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "(n)) WHEN activeLow = 1 ELSE " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "(n);");
      contents.add("   END GENERATE;");
    } else {
      contents.add("   genvar i;");
      contents.add("   generate");
      contents.add("      for (i = 0; i < nrOfLeds; i = i + 1) begin");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayOutputs
          + "[i] = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "[n] : " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "[n];");
      contents.add("      end");
      contents.add("   endgenerate");
    }
    return contents;
  }


  @Override
  public String getComponentStringIdentifier() {
    return LedArrayName;
  }
  
  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "ledarrays";
  }

}
