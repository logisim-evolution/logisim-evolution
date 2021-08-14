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

public class LedArrayColumnScanningHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static int nrOfLedsGeneric = -1;
  public static int nrOfRowsGeneric = -2;
  public static int nrOfColumsGeneric = -3;
  public static int nrOfColumnAddressBitsGeneric = -4;
  public static int activeLowGeneric = -5;
  public static int scanningCounterBitsGeneric = -6;
  public static int maxNrLedsGeneric = -7;
  public static int scanningCounterValueGeneric = -8;
  public static String nrOfRowsString = "nrOfRows";
  public static String nrOfColumnsString = "nrOfColumns";
  public static String nrOfLedsString = "nrOfLeds";
  public static String nrOfColumnAddressBitsString = "nrOfColumnAddressBits";
  public static String scanningCounterBitsString = "nrOfScanningCounterBits";
  public static String scanningCounterValueString = "scanningCounterReloadValue";
  public static String maxNrLedsString = "maxNrLedsAddrColumns";
  public static String activeLowString = "activeLow";
  public static String LedArrayName = "LedArrayColumnScanning";
  
  public static ArrayList<String> getGenericMap(int nrOfRows,
      int nrOfColumns,
      long FpgaClockFrequency,
      boolean activeLow) {
    var map = new ArrayList<String>();
    final var nrColAddrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfColumns);
    final var scanningReload = (int) (FpgaClockFrequency / (long) 1000);
    final var nrOfScanningBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(scanningReload);
    final var maxNrLeds = ((int) Math.pow(2.0, (double) nrColAddrBits)) * nrOfRows;
    if (HDL.isVHDL()) {
      map.add("      GENERIC MAP ( " + nrOfLedsString + " => " + (nrOfRows * nrOfColumns) + ",");
      map.add("                    " + nrOfRowsString + " => " + nrOfRows + ",");
      map.add("                    " + nrOfColumnsString + " => " + nrOfColumns + ",");
      map.add("                    " + nrOfColumnAddressBitsString + " => " + nrColAddrBits + ",");
      map.add("                    " + scanningCounterBitsString + " => " + nrOfScanningBits + ",");
      map.add("                    " + scanningCounterValueString + " => " + (scanningReload - 1) + ",");
      map.add("                    " + maxNrLedsString + " => " + maxNrLeds + ",");
      map.add("                    " + activeLowString + " => " + ((activeLow) ? "1" : "0") + ")");
    } else {
      map.add("      #( ." + nrOfLedsString + "(" + (nrOfRows * nrOfColumns) + "),");
      map.add("         ." + nrOfRowsString + "(" + nrOfRows + "),");
      map.add("         ." + nrOfColumnsString + "(" + nrOfColumns + "),");
      map.add("         ." + nrOfColumnAddressBitsString + "(" + nrColAddrBits + "),");
      map.add("         ." + scanningCounterBitsString + "(" + nrOfScanningBits + "),");
      map.add("         ." + scanningCounterValueString + "(" + (scanningReload - 1) + "),");
      map.add("         ." + maxNrLedsString + "(" + maxNrLeds + "),");
      map.add("         ." + activeLowString + "(" + ((activeLow) ? "1" : "0") + "))");
    }
    return map;
  }
  
  public static ArrayList<String> getPortMap(int id) {
    var map = new ArrayList<String>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress 
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress 
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + id
          + ",");
      map.add("                 "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + " => "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + id
          + ");");
    } else {
      map.add("      (." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + id
          + "),");
      map.add("       ." 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + id
          + "),");
      map.add("       ." 
          + TickComponentHDLGeneratorFactory.FPGAClock
          + "("
          + TickComponentHDLGeneratorFactory.FPGAClock
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
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress, nrOfColumnAddressBitsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs, nrOfRowsGeneric);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayInputs, nrOfLedsGeneric);
    return inputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var generics = new TreeMap<Integer, String>();
    generics.put(nrOfLedsGeneric, nrOfLedsString);
    generics.put(nrOfRowsGeneric, nrOfRowsString);
    generics.put(nrOfColumsGeneric, nrOfColumnsString);
    generics.put(nrOfColumnAddressBitsGeneric, nrOfColumnAddressBitsString);
    generics.put(scanningCounterBitsGeneric, scanningCounterBitsString);
    generics.put(scanningCounterValueGeneric, scanningCounterValueString);
    generics.put(maxNrLedsGeneric, maxNrLedsString);
    generics.put(activeLowGeneric, activeLowString);
    return generics;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_columnCounterNext", nrOfColumnAddressBitsGeneric);
    wires.put("s_scanningCounterNext", scanningCounterBitsGeneric);
    wires.put("s_tickNext", 1);
    wires.put("s_maxLedInputs", maxNrLedsGeneric);
    return wires;
  }
  
  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var regs = new TreeMap<String, Integer>();
    regs.put("s_columnCounterReg", nrOfColumnAddressBitsGeneric);
    regs.put("s_scanningCounterReg", scanningCounterBitsGeneric);
    regs.put("s_tickReg", 1);
    return regs;
  }

  public ArrayList<String> getColumnCounterCode() {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add("");
      contents.add("   " + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress + " <= s_columnCounterReg;");
      contents.add("");
      contents.add("   s_tickNext <= '1' WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0,"
              + scanningCounterBitsString
              + ")) ELSE '0';");
      contents.add("");
      contents.add("   s_scanningCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation");
      contents.add("                            std_logic_vector(to_unsigned("
              + scanningCounterValueString
              + "-1, " 
              + scanningCounterBitsString
              + "))");
      contents.add("                               WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0,"
              + scanningCounterBitsString
              + ")) ELSE ");
      contents.add("                            std_logic_vector(unsigned(s_scanningCounterReg)-1);");
      contents.add("");
      contents.add("   s_columnCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation");
      contents.add("                          s_columnCounterReg WHEN s_tickReg = '0' ELSE");
      contents.add("                          std_logic_vector(to_unsigned(nrOfColumns-1,nrOfcolumnAddressBits))");
      contents.add("                             WHEN s_columnCounterReg = std_logic_vector(to_unsigned(0,nrOfColumnAddressBits)) ELSE");
      contents.add("                          std_logic_vector(unsigned(s_columnCounterReg)-1);");
      contents.add("");
      contents.add("   makeFlops : PROCESS (" + TickComponentHDLGeneratorFactory.FPGAClock + ") IS");
      contents.add("   BEGIN");
      contents.add("      IF (rising_edge("  + TickComponentHDLGeneratorFactory.FPGAClock + ")) THEN");
      contents.add("         s_columnCounterReg   <= s_columnCounterNext;");
      contents.add("         s_scanningCounterReg <= s_scanningCounterNext;");
      contents.add("         s_tickReg            <= s_tickNext;");
      contents.add("      END IF;");
      contents.add("   END PROCESS makeFlops;");
      contents.add("");
    } else {
      contents.add("");
      contents.add("   assign columnAddress = s_columnCounterReg;");
      contents.add("");
      contents.add("   assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;");
      contents.add("   assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? "
              + scanningCounterValueString 
              + " : s_scanningCounterReg - 1;");
      contents.add("   assign s_columnCounterNext = (s_tickReg == 1'b0) ? s_columnCounterReg : ");
      contents.add("                                (s_rowCounterReg == 0) ? nrOfColumns-1 : s_columnCounterReg-1;");
      contents.add("");
      contents.addAll(MakeRemarkBlock("Here the simulation only initial is defined", 3));
      contents.add("   initial");
      contents.add("   begin");
      contents.add("      s_columnCounterReg   = 0;");
      contents.add("      s_scanningCounterReg = 0;");
      contents.add("      s_tickReg            = 1'b0;");
      contents.add("   end");
      contents.add("");
      contents.add("   always @(posedge " + TickComponentHDLGeneratorFactory.FPGAClock + ")");
      contents.add("   begin");
      contents.add("       s_columnCounterReg   = s_columnCounterNext;");
      contents.add("       s_scanningCounterReg = s_scanningCounterNext;");
      contents.add("       s_tickReg            = s_tickNext;");
      contents.add("   end");

    }
    return contents;
  }
  
  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.addAll(getColumnCounterCode());
    if (HDL.isVHDL()) {
      contents.add("   makeVirtualInputs : PROCESS ( internalLeds ) IS");
      contents.add("   BEGIN");
      contents.add("      s_maxLedInputs <= (OTHERS => '0');");
      contents.add("      IF (" + activeLowString + " = 1) THEN");
      contents.add("         s_maxLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs 
          + ";");
      contents.add("                                       ELSE");
      contents.add("         s_maxLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + ";");
      contents.add("      END IF;");
      contents.add("   END PROCESS makeVirtualInputs;");
      contents.add("");
      contents.add("   GenOutputs : FOR n IN " + nrOfRowsString + "-1 DOWNTO 0 GENERATE");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + "(n) <= s_maxLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      contents.add("   END GENERATE GenOutputs;");
    } else {
      contents.add("");
      contents.add("   genvar i;");
      contents.add("   generate");
      contents.add("      for (i = 0; i < " + nrOfColumnsString + "; i = i + 1) begin");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs
          + "[i] = (activeLow == 1) ? ~"
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      contents.add("                                                 " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayInputs
          + "[i*nrOfColumns+s_columnCounterReg];");
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
