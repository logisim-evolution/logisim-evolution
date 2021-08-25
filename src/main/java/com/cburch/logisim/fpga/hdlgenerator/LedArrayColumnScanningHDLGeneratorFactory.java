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
    final var contents =
        new LineBuffer(
            new LineBuffer.Pairs() {
              {
                add("columnAddress", LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress);
                add("clock", TickComponentHDLGeneratorFactory.FPGAClock);
                add("counterBits", scanningCounterBitsString);
                add("counterValue", scanningCounterValueString);
              }
            });
    if (HDL.isVHDL()) {
      contents.add(
          "",
          "{{columnAddress}} <= s_columnCounterReg;",
          "",
          "s_tickNext <= '1' WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{counterBits}})) ELSE '0';",
          "",
          "s_scanningCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation",
          "                         std_logic_vector(to_unsigned({{counterValue}}-1, {{counterBits}}))",
          "                            WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{counterBits}})) ELSE ",
          "                         std_logic_vector(unsigned(s_scanningCounterReg)-1);",
          "",
          "s_columnCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation",
          "                       s_columnCounterReg WHEN s_tickReg = '0' ELSE",
          "                       std_logic_vector(to_unsigned(nrOfColumns-1,nrOfcolumnAddressBits))",
          "                          WHEN s_columnCounterReg = std_logic_vector(to_unsigned(0,nrOfColumnAddressBits)) ELSE",
          "                       std_logic_vector(unsigned(s_columnCounterReg)-1);",
          "",
          "makeFlops : PROCESS ({{clock}}) IS",
          "BEGIN",
          "   IF (rising_edge({{clock}})) THEN",
          "      s_columnCounterReg   <= s_columnCounterNext;",
          "      s_scanningCounterReg <= s_scanningCounterNext;",
          "      s_tickReg            <= s_tickNext;",
          "   END IF;",
          "END PROCESS makeFlops;",
          "");
    } else {
      contents
          .add(
              "",
              "assign columnAddress = s_columnCounterReg;",
              "",
              "assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;",
              "assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{counterValue}} : s_scanningCounterReg - 1;",
              "assign s_columnCounterNext = (s_tickReg == 1'b0) ? s_columnCounterReg : ",
              "                             (s_rowCounterReg == 0) ? nrOfColumns-1 : s_columnCounterReg-1;",
              "")
          .addRemarkBlock("Here the simulation only initial is defined")
          .add(
              "initial",
              "begin",
              "   s_columnCounterReg   = 0;",
              "   s_scanningCounterReg = 0;",
              "   s_tickReg            = 1'b0;",
              "end",
              "",
              "always @(posedge {{clock}})",
              "begin",
              "    s_columnCounterReg   = s_columnCounterNext;",
              "    s_scanningCounterReg = s_scanningCounterNext;",
              "    s_tickReg            = s_tickNext;",
              "end");
    }
    return contents.getWithIndent();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    contents
        .withPairs()
        .add("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
        .add("outs", LedArrayGenericHDLGeneratorFactory.LedArrayRowOutputs)
        .add("nrOfLeds", nrOfLedsString)
        .add("nrOfRows", nrOfRowsString)
        .add("activeLow", activeLowString);

    contents.add(getColumnCounterCode());
    if (HDL.isVHDL()) {
      contents
          .add("makeVirtualInputs : PROCESS ( internalLeds ) IS",
          "BEGIN",
          "   s_maxLedInputs <= (OTHERS => '0');",
          "   IF ({{activeLow}} = 1) THEN",
          "      s_maxLedInputs( {{nrOfLeds}}-1 DOWNTO 0) <= NOT {{ins}};",
          "   ELSE",
          "      s_maxLedInputs( {{nrOfLeds}}-1 DOWNTO 0) <= {{ins}};",
          "   END IF;",
          "END PROCESS makeVirtualInputs;",
          "",
          "GenOutputs : FOR n IN {{nrOfRows}}-1 DOWNTO 0 GENERATE",
          "   {{outs}}(n) <= s_maxLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);",
          "END GENERATE GenOutputs;");
    } else {
      contents.add(
          "",
          "genvar i;",
          "generate",
          "   for (i = 0; i < {{nrOfRows}}; i = i + 1) begin",
          "      assign {{outs}}[i] = (activeLow == 1)",
          "          ? ~{{ins}}[i*nrOfColumns+s_columnCounterReg]",
          "          : {{ins}}[i*nrOfColumns+s_columnCounterReg];",
          "   end",
          "endgenerate");
    }
    return contents.getWithIndent();
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
