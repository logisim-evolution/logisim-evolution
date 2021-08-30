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
            .pair("nrOfLeds", nrOfLedsString)
            .pair("nrOfLedsVal", nrOfRows * nrOfColumns)
            .pair("nrOfRows", nrOfRowsString)
            .pair("nrOfRowsVal", nrOfRows)
            .pair("nrOfColumns", nrOfColumnsString)
            .pair("nrOfColumnsVal", nrOfColumns)
            .pair("nrOfRowAddressBits", nrOfRowAddressBitsString)
            .pair("nrOfRowAddressBitsVal", nrRowAddrBits)
            .pair("scanningCounterBits", scanningCounterBitsString)
            .pair("scanningCounterBitsVal", nrOfScanningBits)
            .pair("scanningCounterValue", scanningCounterValueString)
            .pair("scanningCounterValueVal", scanningReload - 1)
            .pair("maxNrLeds", maxNrLedsString)
            .pair("maxNrLedsVal", maxNrLeds)
            .pair("activeLow", activeLowString)
            .pair("activeLowVal", activeLow ? "1" : "0");

    if (HDL.isVHDL()) {
      contents.addLines(
          "GENERIC MAP ( {{nrOfLeds}} => {{nrOfLedsVal}},",
          "              {{nrOfRows}} => {{nrOfRowsVal}},",
          "              {{nrOfColumns}} => {{nrOfColumnsVal}},",
          "              {{nrOfRowAddressBits}} => {{nrOfRowAddressBitsVal}},",
          "              {{scanningCounterBits}} => {{scanningCounterBitsVal}},",
          "              {{scanningCounterValue}} => {{scanningCounterValueVal}},",
          "              {{maxNrLeds}} => {{maxNrLedsVal}},",
          "              {{activeLow}} => {{activeLowVal}} )");
    } else {
      contents.addLines(
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
            .pair("rowAddr", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("colOuts", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK)
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
                .pair("id", id);
    if (HDL.isVHDL()) {
      map.addLines(
          "PORT MAP ( {{rowAddr}} => {{rowAddr}}{{id}},",
          "           {{outs}} => {{outs}}{{id}},",
          "           {{clock}} => {{clock}},",
          "           {{ins}} => => s_{{ins}}{{id}} );");
    } else {
      map.addLines(
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
    inputs.put(TickComponentHDLGeneratorFactory.FPGA_CLOCK, 1);
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
            .pair("rowAddress", LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress)
            .pair("bits", scanningCounterBitsString)
            .pair("value", scanningCounterValueString)
            .pair("clock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);
    if (HDL.isVHDL()) {
      contents.addLines(
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
          .addLines(
              "",
              "assign rowAddress = s_rowCounterReg;",
              "",
              "assign s_tickNext = (s_scanningCounterReg == 0) ? 1'b1 : 1'b0;",
              "assign s_scanningCounterNext = (s_scanningCounterReg == 0) ? {{value}} : s_scanningCounterReg - 1;",
              "assign s_rowCounterNext = (s_tickReg == 1'b0) ? s_rowCounterReg : ",
              "                          (s_rowCounterReg == 0) ? nrOfRows-1 : s_rowCounterReg-1;",
              "")
          .addRemarkBlock("Here the simulation only initial is defined")
          .addLines(
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
            .pair("ins", LedArrayGenericHDLGeneratorFactory.LedArrayInputs)
            .pair("outs", LedArrayGenericHDLGeneratorFactory.LedArrayColumnOutputs)
            .pair("activeLow", activeLowString)
            .pair("nrOfLeds", nrOfLedsString)
            .pair("nrOfColumns", nrOfColumnsString)
            .add(getRowCounterCode());

    if (HDL.isVHDL()) {
      contents.addLines(
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
      contents.addLines(
          "",
          "genvar i;",
          "generate",
          "   for (i = 0; i < {{nrOfColumns}}; i = i + 1)",
          "   begin:outputs",
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
