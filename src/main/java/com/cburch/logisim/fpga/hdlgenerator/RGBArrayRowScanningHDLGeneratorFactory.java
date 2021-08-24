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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RGBArrayRowScanningHDLGeneratorFactory extends LedArrayRowScanningHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayRowScanning";

  public static ArrayList<String> getPortMap(int id) {
    final var clock = TickComponentHDLGeneratorFactory.FPGAClock;
    final var rowAddress = LedArrayGenericHDLGeneratorFactory.LedArrayRowAddress;
    final var redOuts = LedArrayGenericHDLGeneratorFactory.LedArrayColumnRedOutputs;
    final var greenOuts = LedArrayGenericHDLGeneratorFactory.LedArrayColumnGreenOutputs;
    final var blueOuts = LedArrayGenericHDLGeneratorFactory.LedArrayColumnBlueOutputs;

    final var redIns = LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs;
    final var greenIns = LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs;
    final var blueIns = LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs;

    final var content = new ContentBuilder();
    if (HDL.isVHDL()) {
      content
          .add("      PORT MAP ( %1$s => %1$s%2$d,", rowAddress, id)
          .add("                 %1$s => %1$s,", clock)
          .add("                 %1$s => %1$s%2$d,", redOuts, id)
          .add("                 %1$s => %1$s%2$d,", greenOuts, id)
          .add("                 %1$s => %1$s%2$d,", blueOuts, id)
          .add("                 %1$s => s_%1$s%2$d,", redIns, id)
          .add("                 %1$s => s_%1$s%2$d,", greenIns, id)
          .add("                 %1$s => s_%1$s%2$d);", blueIns, id);
    } else {
      content
          .add("      (.%1$s(%1$s%2$d),", rowAddress, id)
          .add("       .%1$s(%1$s),", clock)
          .add("       .%1$s(%1$s%2$d),", redOuts, id)
          .add("       .%1$s(%1$s%2$d),", greenOuts, id)
          .add("       .%1$s(%1$s%2$d),", blueOuts, id)
          .add("       .%1$s(s_%1$s%2$d),", redIns, id)
          .add("       .%1$s(s_%1$s%2$d),", greenIns, id)
          .add("       .%1$s(s_%1$s%2$d)); ", blueIns, id);
    }
    return content.get();
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
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var redIn = LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs;
    final var greenIn = LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs;
    final var blueIn = LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs;

    final var redOut = LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs;
    final var greenOut = LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs;
    final var blueOut = LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs;

    final var contents = new ContentBuilder();
    contents.add(getRowCounterCode());
    if (HDL.isVHDL()) {
      contents
          .add("   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS")
          .add("   BEGIN")
          .add("      s_maxRedLedInputs <= (OTHERS => '0');")
          .add("      s_maxGreenLedInputs <= (OTHERS => '0');")
          .add("      s_maxBlueLedInputs <= (OTHERS => '0');")
          .add("      IF (%s = 1) THEN", activeLowString)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= NOT %s;", nrOfLedsString, redIn)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= NOT %s;", nrOfLedsString, greenIn)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= NOT %s;", nrOfLedsString, blueIn)
          .add("      ELSE")
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= %s;", nrOfLedsString, redIn)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= %s;", nrOfLedsString, greenIn)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0) <= %s;", nrOfLedsString, blueIn)
          .add("      END IF;")
          .add("   END PROCESS makeVirtualInputs;")
          .add()
          .add("   GenOutputs : FOR n IN " + nrOfColumnsString + "-1 DOWNTO 0 GENERATE")
          .add("      %s(n) <= s_maxRedLedInputs(%s * to_integer(unsigned(s_rowCounterReg)) + n);", redOut, nrOfColumnsString)
          .add("      %s(n) <= s_maxRedLedInputs(%s * to_integer(unsigned(s_rowCounterReg)) + n);", greenOut, nrOfColumnsString)
          .add("      %s(n) <= s_maxRedLedInputs(%s * to_integer(unsigned(s_rowCounterReg)) + n);", blueOut, nrOfColumnsString)
          .add("   END GENERATE GenOutputs;");
    } else {
      contents
          .add()
          .add("   genvar i;")
          .add("   generate")
          .add("      for (i = 0; i < %s; i = i + 1) begin", nrOfColumnsString)
          .add("         assign %s[i] = (activeLow == 1)", redOut)
          .add("            ? ~%s[%s * s_rowCounterReg + i]", redIn, nrOfColumnsString)
          .add("            : %s[%s * s_rowCounterReg + i];", redIn, nrOfColumnsString)
          .add("         assign %s[i] = (activeLow == 1)", greenOut)
          .add("            ? ~%s[%s * s_rowCounterReg + i]", greenIn, nrOfColumnsString)
          .add("            : %s[%s * s_rowCounterReg + i];", greenIn, nrOfColumnsString)
          .add("         assign %s[i] = (activeLow == 1)", blueOut)
          .add("            ? ~%s[%s * s_rowCounterReg + i]", blueIn, nrOfColumnsString)
          .add("            : %s[%s * s_rowCounterReg + i];", blueIn, nrOfColumnsString)
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
