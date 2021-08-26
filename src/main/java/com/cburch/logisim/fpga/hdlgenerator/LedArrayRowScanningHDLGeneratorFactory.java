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
import javax.sound.sampled.Line;

public class LedArrayRowScanningHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static int nrOfLedsGeneric = -1;
  public static int nrOfRowsGeneric = -2;
  public static int nrOfColumsGeneric = -3;
  public static int nrOfRowAddressBitsGeneric = -4;
  public static int activeLowGeneric = -5;
  public static int scanningCounterBitsGeneric = -6;
  public static int maxNrLedsGeneric = -7;
  public static int scanningCounterValueGeneric = -8;
  public static String nrOfRowsString = "nrOfRows";
  public static String nrOfColumnsString = "nrOfColumns";
  public static String nrOfLedsString = "nrOfLeds";
  public static String nrOfRowAddressBitsString = "nrOfRowAddressBits";
  public static String activeLowString = "activeLow";
  public static String LedArrayName = "LedArrayRowScanning";
  public static String scanningCounterBitsString = "nrOfScanningCounterBits";
  public static String scanningCounterValueString = "scanningCounterReloadValue";
  public static String maxNrLedsString = "maxNrLedsAddrColumns";

  public static ArrayList<String> getGenericMap(int nrOfRows, int nrOfColumns, long FpgaClockFrequency, boolean activeLow) {
    final var nrRowAddrBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(nrOfRows);
    final var scanningReload = (int) (FpgaClockFrequency / (long) 1000);
    final var nrOfScanningBits = LedArrayGenericHDLGeneratorFactory.getNrOfBitsRequired(scanningReload);
    final var maxNrLeds = ((int) Math.pow(2.0, (double) nrRowAddrBits)) * nrOfRows;

    final var contents =
        (new LineBuffer())
            .addPair("nrOfLeds", nrOfLedsString)
            .addPair("nrOfLedsVal", nrOfRows * nrOfColumns)
            .addPair("nrOfRows", nrOfRowsString)
            .addPair("nrOfRowsVal", nrOfRows)
            .addPair("nrOfColumns", nrOfColumnsString)
            .addPair("nrOfColumnsVal", nrOfColumns)
            .addPair("nrOfRowAddressBits", nrOfRowAddressBitsString)
            .addPair("nrOfRowAddressBitsVal", nrRowAddrBits)
            .addPair("scanningCounterBits", scanningCounterBitsString)
            .addPair("scanningCounterBitsVal", nrOfScanningBits)
            .addPair("scanningCounterValue", scanningCounterValueString)
            .addPair("scanningCounterValueVal", scanningReload - 1)
            .addPair("maxNrLeds", maxNrLedsString)
            .addPair("maxNrLedsVal", maxNrLeds)
            .addPair("activeLow", activeLowString)
            .addPair("activeLowVal", activeLow ? "1" : "0");

    if (HDL.isVHDL()) {
      contents.add(
          "GENERIC MAP ( {{nrOfLeds}} => {{nrOfLedsVal}},",
          "              {{nrOfRows}} => {{nrOfRowsVal}},",
          "              {{nrOfColumns}} => {{nrOfColumnsVal}},",
          "              {{nrOfRowAddressBits}} => {{nrOfRowAddressBitsVal}},",
          "              {{scanningCounterBits}} => {{scanningCounterBitsVal}},",
          "              {{scanningCounterValue}} => {{scanningCounterValueVal}},",
          "              {{maxNrLeds}} => {{maxNrLedsVal}},",
          "              {{activeLow}} => {{activeLowVal}} )");
    } else {
      contents.add(
          "#( .{{nrOfLeds}}({{nrOfLedsVal}}),",
          "   .{{nrOfRows}}({{nrOfRowsVal}}),",
          "   .{{nrOfColumns}}({{nrOfColumns}}),",
          "   .{{nrOfRowAddressBits}}({{nrOfRowAddressBitsVal}}),",
          "   .{{scanningCounterBits}}({{scanningCounterBitsVal}}),",
          "   .{{scanningCounterValue}}({{scanningCounterValueVal}}),",
          "   .{{maxNrLeds}}({{maxNrLedsVal}}),",
          "   .{{activeLow}}({{activeLowVal}}) )");
    }
    return contents.getWithIndent(6);
  }

  public static ArrayList<String> getPortMap(int id) {
    final var map =
        (new LineBuffer())
            .addPair("rowAddr", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .addPair("colOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .addPair("clock", TickComponentHDLGeneratorFactory.FPGAClock)
            .addPair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
                .addPair("id", id);
    if (HDL.isVHDL()) {
      map.add(
          "PORT MAP ( {{rowAddr}} => {{rowAddr}}{{id}},",
          "           {{outs}} => {{outs}}{{id}},",
          "           {{clock}} => {{clock}},",
          "           {{ins}} => => s_{{ins}}{{id}} );");
    } else {
      map.add(
          "( .{{rowAddr}}({{rowAddr}}{{id}}),",
          "  .{{outs}}({{outs}}{{id}}),",
          "  .{{clock}}({{clock}}),",
          "  .{{ins}}(s_{{ins}}{{id}}) );");
    }
    return map.getWithIndent(6);
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress, nrOfRowAddressBitsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs, nrOfColumsGeneric);
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
    generics.put(nrOfRowAddressBitsGeneric, nrOfRowAddressBitsString);
    generics.put(scanningCounterBitsGeneric, scanningCounterBitsString);
    generics.put(scanningCounterValueGeneric, scanningCounterValueString);
    generics.put(maxNrLedsGeneric, maxNrLedsString);
    generics.put(activeLowGeneric, activeLowString);
    return generics;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_rowCounterNext", nrOfRowAddressBitsGeneric);
    wires.put("s_scanningCounterNext", scanningCounterBitsGeneric);
    wires.put("s_tickNext", 1);
    wires.put("s_maxLedInputs", maxNrLedsGeneric);
    return wires;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var regs = new TreeMap<String, Integer>();
    regs.put("s_rowCounterReg", nrOfRowAddressBitsGeneric);
    regs.put("s_scanningCounterReg", scanningCounterBitsGeneric);
    regs.put("s_tickReg", 1);
    return regs;
  }

  public ArrayList<String> getRowCounterCode() {
    final var contents =
        (new LineBuffer())
            .addPair("rowAddress", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .addPair("bits", scanningCounterBitsString)
            .addPair("value", scanningCounterValueString)
            .addPair("clock", TickComponentHDLGeneratorFactory.FPGAClock);
    if (HDL.isVHDL()) {
      contents.add(
          "",
          "{{rowAddress}} <= s_rowCounterReg;",
          "",
          "s_tickNext <= '1' WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{bits}})) ELSE '0';",
          "",
          "s_scanningCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation",
          "                         std_logic_vector(to_unsigned({{value}}-1, {{bits}})) WHEN s_scanningCounterReg = std_logic_vector(to_unsigned(0, {{bits}})) ELSE ",
          "                         std_logic_vector(unsigned(s_scanningCounterReg)-1);",
          "",
          "s_rowCounterNext <= (OTHERS => '0') WHEN s_tickReg /= '0' AND s_tickReg /= '1' ELSE -- for simulation",
          "                    s_rowCounterReg WHEN s_tickReg = '0' ELSE",
          "                    std_logic_vector(to_unsigned(nrOfRows-1,nrOfRowAddressBits))",
          "                       WHEN s_rowCounterReg = std_logic_vector(to_unsigned(0,nrOfRowAddressBits)) ELSE",
          "                    std_logic_vector(unsigned(s_rowCounterReg)-1);",
          "",
          "makeFlops : PROCESS ({{clock}}) IS",
          "BEGIN",
          "   IF (rising_edge({{clock}})) THEN",
          "      s_rowCounterReg      <= s_rowCounterNext;",
          "      s_scanningCounterReg <= s_scanningCounterNext;",
          "      s_tickReg            <= s_tickNext;",
          "   END IF;",
          "END PROCESS makeFlops;",
          "");
    } else {
      contents
          .add(
              "",
              "assign rowAddress = s_rowCounterReg;",
              "",
              "assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;",
              "assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{value}} : s_scanningCounterReg - 1;",
              "assign s_rowCounterNext = (s_tickReg == 1'b0) ? s_rowCounterReg : ",
              "                          (s_rowCounterReg == 0) ? nrOfRows-1 : s_rowCounterReg-1;",
              "")
          .addRemarkBlock("Here the simulation only initial is defined")
          .add(
              "initial",
              "begin",
              "   s_rowCounterReg      = 0;",
              "   s_scanningCounterReg = 0;",
              "   s_tickReg            = 1'b0;",
              "end",
              "",
              "always @(posedge {{clock}})",
              "begin",
              "    s_rowCounterReg      = s_rowCounterNext;",
              "    s_scanningCounterReg = s_scanningCounterNext;",
              "    s_tickReg            = s_tickNext;",
              "end");
    }
    return contents.getWithIndent();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        (new LineBuffer())
            .addPair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
            .addPair("outs", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .addPair("activeLow", activeLowString)
            .addPair("nrOfLeds", nrOfLedsString)
            .addPair("nrOfColumns", nrOfColumnsString)
            .add(getRowCounterCode());

    if (HDL.isVHDL()) {
      contents.add(
          "makeVirtualInputs : PROCESS ( internalLeds ) IS",
          "BEGIN",
          "   s_maxLedInputs <= (OTHERS => '0');",
          "   IF ({{activeLow}} = 1) THEN",
          "      s_maxLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= NOT {{ins}};",
          "   ELSE",
          "      s_maxLedInputs({{nrOfLeds}}-1 DOWNTO 0) <= {{ins}};",
          "   END IF;",
          "END PROCESS makeVirtualInputs;",
          "",
          "GenOutputs : FOR n IN {{nrOfColumns}}-1 DOWNTO 0 GENERATE",
          "   {{outs}}(n) <= s_maxLedInputs({{nrOfColumns}} * to_integer(unsigned(s_rowCounterReg)) + n);",
          "END GENERATE GenOutputs;");
    } else {
      contents.add(
          "",
          "genvar i;",
          "generate",
          "   for (i = 0; i < {{nrOfColumns}}; i = i + 1) begin",
          "      assign {{outs}}[i] = (activeLow == 1)",
          "         ? ~{{ins}}[{{nrOfColumns}} * s_rowCounterReg + i]",
          "         :  {{ins}}[{{nrOfColumns}} * s_rowCounterReg + i];",
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
