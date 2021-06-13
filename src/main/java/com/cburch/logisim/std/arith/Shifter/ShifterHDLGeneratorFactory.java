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

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShifterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ShiftModeStr = "ShifterMode";
  private static final int ShiftModeId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "Shifter";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("DataA", attrs.getValue(StdAttr.WIDTH).getWidth());
    Inputs.put("ShiftAmount", getNrofShiftBits(attrs));
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      Contents.add(
          "   -----------------------------------------------------------------------------");
      Contents.add(
          "   --- ShifterMode represents when:                                          ---");
      Contents.add(
          "   --- 0 : Logical Shift Left                                                ---");
      Contents.add(
          "   --- 1 : Rotate Left                                                       ---");
      Contents.add(
          "   --- 2 : Logical Shift Right                                               ---");
      Contents.add(
          "   --- 3 : Arithmetic Shift Right                                            ---");
      Contents.add(
          "   --- 4 : Rotate Right                                                      ---");
      Contents.add(
          "   -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("");
      if (nrOfBits == 1) {
        Contents.add("   Result <= DataA WHEN " + ShiftModeStr + " = 1 OR");
        Contents.add("                        " + ShiftModeStr + " = 3 OR");
        Contents.add(
            "                        " + ShiftModeStr + " = 4 ELSE DataA AND NOT(ShiftAmount);");
      } else {
        int stage;
        for (stage = 0; stage < getNrofShiftBits(attrs); stage++) {
          Contents.addAll(GetStageFunctionalityVHDL(stage, nrOfBits));
        }
        Contents.add(
            "   -----------------------------------------------------------------------------");
        Contents.add(
            "   --- Here we assign the result                                             ---");
        Contents.add(
            "   -----------------------------------------------------------------------------");
        Contents.add("");
        Contents.add(
            "   Result <= s_stage_" + (getNrofShiftBits(attrs) - 1) + "_result;");
        Contents.add("");
      }
    } else {
      Contents.add(
          "   /***************************************************************************");
      Contents.add(
          "    ** ShifterMode represents when:                                          **");
      Contents.add(
          "    ** 0 : Logical Shift Left                                                **");
      Contents.add(
          "    ** 1 : Rotate Left                                                       **");
      Contents.add(
          "    ** 2 : Logical Shift Right                                               **");
      Contents.add(
          "    ** 3 : Arithmetic Shift Right                                            **");
      Contents.add(
          "    ** 4 : Rotate Right                                                      **");
      Contents.add(
          "    ***************************************************************************/");
      Contents.add("");
      Contents.add("");
      if (nrOfBits == 1) {
        Contents.add("   assign Result = ((" + ShiftModeStr + " == 1)||");
        Contents.add("                    (" + ShiftModeStr + " == 3)||");
        Contents.add(
            "                    (" + ShiftModeStr + " == 4)) ? DataA : DataA&(~ShiftAmount);");
      } else {
        int stage;
        for (stage = 0; stage < getNrofShiftBits(attrs); stage++) {
          Contents.addAll(GetStageFunctionalityVerilog(stage, nrOfBits));
        }
        Contents.add(
            "   /***************************************************************************");
        Contents.add(
            "    ** Here we assign the result                                             **");
        Contents.add(
            "    ***************************************************************************/");
        Contents.add("");
        Contents.add(
            "   assign Result = s_stage_"
                + (getNrofShiftBits(attrs) - 1)
                + "_result;");
        Contents.add("");
      }
    }
    return Contents;
  }

  private int getNrofShiftBits(AttributeSet attrs) {
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int shift = 1;
    while ((1 << shift) < inputbits) shift++;
    return shift;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    Outputs.put("Result", inputbits);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    Parameters.put(ShiftModeId, ShiftModeStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    Object shift = ComponentInfo.GetComponent().getAttributeSet().getValue(Shifter.ATTR_SHIFT);
    if (shift == Shifter.SHIFT_LOGICAL_LEFT) ParameterMap.put(ShiftModeStr, 0);
    else if (shift == Shifter.SHIFT_ROLL_LEFT) ParameterMap.put(ShiftModeStr, 1);
    else if (shift == Shifter.SHIFT_LOGICAL_RIGHT) ParameterMap.put(ShiftModeStr, 2);
    else if (shift == Shifter.SHIFT_ARITHMETIC_RIGHT) ParameterMap.put(ShiftModeStr, 3);
    else ParameterMap.put(ShiftModeStr, 4);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("DataA", true, ComponentInfo, Shifter.IN0, Nets));
    PortMap.putAll(GetNetMap("ShiftAmount", true, ComponentInfo, Shifter.IN1, Nets));
    PortMap.putAll(GetNetMap("Result", true, ComponentInfo, Shifter.OUT, Nets));
    return PortMap;
  }

  private ArrayList<String> GetStageFunctionalityVerilog(int StageNumber, int NrOfBits) {
    ArrayList<String> Contents = new ArrayList<>();
    int nr_of_bits_to_shift = (1 << StageNumber);
    Contents.add("   /***************************************************************************");
    Contents.add(
        "    ** Here stage "
            + StageNumber
            + " of the binary shift tree is defined                     **");
    Contents.add(
        "    ***************************************************************************/");
    Contents.add("");
    if (StageNumber == 0) {
      Contents.add(
          "   assign s_stage_0_shiftin = (("
              + ShiftModeStr
              + "== 1)||("
              + ShiftModeStr
              + "==3)) ?");
      Contents.add("                              DataA[" + (NrOfBits - 1) + "] :");
      Contents.add("                              (" + ShiftModeStr + "== 4) ? DataA[0] : 0;");
      Contents.add("");
      Contents.add("   assign s_stage_0_result  = (ShiftAmount == 0) ? DataA :");
      Contents.add(
          "                              (("
              + ShiftModeStr
              + "== 0) || ("
              + ShiftModeStr
              + "== 1)) ?");
      Contents.add(
          "                              {DataA["
              + (NrOfBits - 2)
              + ":0],s_stage_0_shiftin} :");
      Contents.add(
          "                              {s_stage_0_shiftin,DataA["
              + (NrOfBits - 1)
              + ":1]};");
      Contents.add("");
    } else {
      Contents.add("   assign s_stage_" + StageNumber + "_shiftin = (" + ShiftModeStr + "== 1) ?");
      Contents.add(
          "                              s_stage_"
              + (StageNumber - 1)
              + "_result["
              + (NrOfBits - 1)
              + ":"
              + (NrOfBits - nr_of_bits_to_shift)
              + "] : ");
      Contents.add("                              (" + ShiftModeStr + "== 3) ?");
      Contents.add(
          "                              {"
              + nr_of_bits_to_shift
              + "{s_stage_"
              + (StageNumber - 1)
              + "_result["
              + (NrOfBits - 1)
              + "]}} :");
      Contents.add("                              (" + ShiftModeStr + "== 4) ?");
      Contents.add(
          "                              s_stage_"
              + (StageNumber - 1)
              + "_result["
              + (nr_of_bits_to_shift - 1)
              + ":0] : 0;");
      Contents.add("");
      Contents.add(
          "   assign s_stage_"
              + StageNumber
              + "_result  = (ShiftAmount["
              + StageNumber
              + "]==0) ?");
      Contents.add(
          "                              s_stage_"
              + (StageNumber - 1)
              + "_result : ");
      Contents.add(
          "                              (("
              + ShiftModeStr
              + "== 0)||("
              + ShiftModeStr
              + "== 1)) ?");
      Contents.add(
          "                              {s_stage_"
              + (StageNumber - 1)
              + "_result["
              + (NrOfBits - nr_of_bits_to_shift - 1)
              + ":0],s_stage_"
              + StageNumber
              + "_shiftin} :");
      Contents.add(
          "                              {s_stage_"
              + StageNumber
              + "_shiftin,s_stage_"
              + (StageNumber - 1)
              + "_result["
              + (NrOfBits - 1)
              + ":"
              + nr_of_bits_to_shift
              + "]};");
      Contents.add("");
    }
    return Contents;
  }

  private ArrayList<String> GetStageFunctionalityVHDL(int StageNumber, int NrOfBits) {
    ArrayList<String> Contents = new ArrayList<>();
    int nr_of_bits_to_shift = (1 << StageNumber);
    Contents.add(
        "   -----------------------------------------------------------------------------");
    Contents.add(
        "   --- Here stage "
            + StageNumber
            + " of the binary shift tree is defined                     ---");
    Contents.add(
        "   -----------------------------------------------------------------------------");
    Contents.add("");
    if (StageNumber == 0) {
      Contents.add(
          "   s_stage_0_shiftin <= DataA("
              + (NrOfBits - 1)
              + ") WHEN "
              + ShiftModeStr
              + " = 1 OR "
              + ShiftModeStr
              + " = 3 ELSE");
      Contents.add("                        DataA(0) WHEN " + ShiftModeStr + " = 4 ELSE '0';");
      Contents.add("");
      Contents.add("   s_stage_0_result  <= DataA");
      if (NrOfBits == 2) Contents.add("                           WHEN ShiftAmount = '0' ELSE");
      else Contents.add("                           WHEN ShiftAmount(0) = '0' ELSE");
      Contents.add(
          "                        DataA("
              + (NrOfBits - 2)
              + " DOWNTO 0)&s_stage_0_shiftin");
      Contents.add(
          "                           WHEN "
              + ShiftModeStr
              + " = 0 OR "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        s_stage_0_shiftin&DataA("
              + (NrOfBits - 1)
              + " DOWNTO 1);");
    } else {
      Contents.add(
          "   s_stage_"
              + StageNumber
              + "_shiftin <= s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - 1)
              + " DOWNTO "
              + (NrOfBits - nr_of_bits_to_shift)
              + " ) WHEN "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        (OTHERS => s_stage_"
              + (StageNumber - 1)
              + "_result("
              + (NrOfBits - 1)
              + ")) WHEN "
              + ShiftModeStr
              + " = 3 ELSE");
      Contents.add(
          "                        s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (nr_of_bits_to_shift - 1)
              + " DOWNTO 0 ) WHEN "
              + ShiftModeStr
              + " = 4 ELSE");
      Contents.add("                        (OTHERS => '0');");
      Contents.add("");
      Contents.add(
          "   s_stage_"
              + StageNumber
              + "_result  <= s_stage_"
              + (StageNumber - 1)
              + "_result");
      Contents.add("                           WHEN ShiftAmount(" + StageNumber + ") = '0' ELSE");
      Contents.add(
          "                        s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - nr_of_bits_to_shift - 1)
              + " DOWNTO 0 )&s_stage_"
              + StageNumber
              + "_shiftin");
      Contents.add(
          "                           WHEN "
              + ShiftModeStr
              + " = 0 OR "
              + ShiftModeStr
              + " = 1 ELSE");
      Contents.add(
          "                        s_stage_"
              + StageNumber
              + "_shiftin&s_stage_"
              + (StageNumber - 1)
              + "_result( "
              + (NrOfBits - 1)
              + " DOWNTO "
              + nr_of_bits_to_shift
              + " );");
    }
    Contents.add("");
    return Contents;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    int shift = getNrofShiftBits(attrs);
    int loop;
    for (loop = 0; loop < shift; loop++) {
      Wires.put("s_stage_" + loop + "_result", attrs.getValue(StdAttr.WIDTH).getWidth());
      Wires.put("s_stage_" + loop + "_shiftin", 1 << loop);
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
