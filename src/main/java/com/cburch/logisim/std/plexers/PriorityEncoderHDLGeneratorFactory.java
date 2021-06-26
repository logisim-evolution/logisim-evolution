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

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityEncoderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfSelectBitsStr = "NrOfSelectBits";
  private static final int NrOfSelectBitsId = -1;
  private static final String NrOfInputBitsStr = "NrOfInputBits";
  private static final int NrOfInputBitsId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "PRIENC";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    SortedMap<String, Integer> inputs = new TreeMap<>();
    inputs.put("enable", 1);
    inputs.put("input_vector", NrOfInputBitsId);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add("   -- Output Signals");
      contents.add("   GroupSelect <= NOT(s_in_is_zero) AND enable;");
      contents.add("   EnableOut   <= s_in_is_zero AND enable;");
      contents.add("   Address     <= (OTHERS => '0') WHEN enable = '0' ELSE");
      contents.add("                  s_address(" + NrOfSelectBitsStr + "-1 DOWNTO 0);");
      contents.add("");
      contents.add("   -- Control Signals ");
      contents.add(
          "   s_in_is_zero  <= '1' WHEN input_vector = std_logic_vector(to_unsigned(0,"
              + NrOfInputBitsStr
              + ")) ELSE '0';");
      contents.add("");
      contents.add("   -- Processes");
      contents.add(
          "   make_addr : PROCESS( input_vector , v_select_1_vector , v_select_2_vector , v_select_3_vector , v_select_4_vector )");
      contents.add("   BEGIN");
      contents.add(
          "      v_select_1_vector(32 DOWNTO " + NrOfInputBitsStr + ")  <= (OTHERS => '0');");
      contents.add("      v_select_1_vector(" + NrOfInputBitsStr + "-1 DOWNTO 0) <= input_vector;");
      contents.add(
          "      IF (v_select_1_vector(31 DOWNTO 16) = X\"0000\") THEN s_address(4)      <= '0';");
      contents.add(
          "                                                          v_select_2_vector <= v_select_1_vector(15 DOWNTO 0);");
      contents.add(
          "                                                     ELSE s_address(4)      <= '1';");
      contents.add(
          "                                                          v_select_2_vector <= v_select_1_vector(31 DOWNTO 16);");
      contents.add("      END IF;");
      contents.add(
          "      IF (v_select_2_vector(15 DOWNTO 8) = X\"00\") THEN s_address(3)      <= '0';");
      contents.add(
          "                                                       v_select_3_vector <= v_select_2_vector(7 DOWNTO 0);");
      contents.add(
          "                                                  ELSE s_address(3)      <= '1';");
      contents.add(
          "                                                       v_select_3_vector <= v_select_2_vector(15 DOWNTO 8);");
      contents.add("      END IF;");
      contents.add(
          "      IF (v_select_3_vector(7 DOWNTO 4) = X\"0\") THEN s_address(2)      <= '0';");
      contents.add(
          "                                                     v_select_4_vector <= v_select_3_vector(3 DOWNTO 0);");
      contents.add(
          "                                                ELSE s_address(2)      <= '1';");
      contents.add(
          "                                                     v_select_4_vector <= v_select_3_vector(7 DOWNTO 4);");
      contents.add("      END IF;");
      contents.add("      IF (v_select_4_vector(3 DOWNTO 2) = \"00\") THEN s_address(1) <= '0';");
      contents.add(
          "                                                     s_address(0) <= v_select_4_vector(1);");
      contents.add("                                                ELSE s_address(1) <= '1';");
      contents.add(
          "                                                     s_address(0) <= v_select_4_vector(3);");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_addr;");
    } else {
      contents.add("   assign GroupSelect = ~s_in_is_zero&enable;");
      contents.add("   assign EnableOut = s_in_is_zero&enable;");
      contents.add("   assign Address = (~enable) ? 0 : s_address[" + NrOfSelectBitsStr + "-1:0];");
      contents.add("   assign s_in_is_zero = (input_vector == 0) ? 1'b1 : 1'b0;");
      contents.add("");
      contents.add("   assign v_select_1_vector[32:" + NrOfInputBitsStr + "] = 0;");
      contents.add("   assign v_select_1_vector[" + NrOfInputBitsStr + "-1:0] = input_vector;");
      contents.add("   assign s_address[4] = (v_select_1_vector[31:16] == 0) ? 1'b0 : 1'b1;");
      contents.add(
          "   assign v_select_2_vector = (v_select_1_vector[31:16] == 0) ? v_select_1_vector[15:0] : v_select_1_vector[31:16];");
      contents.add("   assign s_address[3] = (v_select_2_vector[15:8] == 0) ? 1'b0 : 1'b1;");
      contents.add(
          "   assign v_select_3_vector = (v_select_2_vector[15:8] == 0) ? v_select_2_vector[7:0] : v_select_2_vector[15:8];");
      contents.add("   assign s_address[2] = (v_select_3_vector[7:4] == 0) ? 1'b0 : 1'b1;");
      contents.add(
          "   assign v_select_4_vector = (v_select_3_vector[7:4] == 0) ? v_select_3_vector[3:0] : v_select_2_vector[7:4];");
      contents.add("   assign s_address[1] = (v_select_4_vector[3:2] == 0) ? 1'b0 : 1'b1;");
      contents.add(
          "   assign s_address[0] = (v_select_4_vector[3:2] == 0) ? v_select_4_vector[1] : v_select_4_vector[3];");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<>();
    outputs.put("GroupSelect", 1);
    outputs.put("EnableOut", 1);
    outputs.put("Address", NrOfSelectBitsId);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> params = new TreeMap<>();
    params.put(NrOfSelectBitsId, NrOfSelectBitsStr);
    params.put(NrOfInputBitsId, NrOfInputBitsStr);
    return params;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    final var nrOfBits = componentInfo.NrOfEnds() - 4;
    final var nrOfSelectBits = componentInfo.GetComponent().getEnd(nrOfBits + PriorityEncoder.OUT).getWidth().getWidth();
    ParameterMap.put(NrOfSelectBitsStr, nrOfSelectBits);
    ParameterMap.put(NrOfInputBitsStr, 1 << nrOfSelectBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(mapInfo instanceof NetlistComponent)) return PortMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    final var nrOfBits = componentInfo.NrOfEnds() - 4;
    PortMap.putAll(
        GetNetMap(
            "enable",
            false,
            componentInfo,
            nrOfBits + PriorityEncoder.EN_IN,
            nets));
    final var vectorList = new StringBuilder();
    for (var i = nrOfBits - 1; i >= 0; i--) {
      if (HDL.isVHDL())
        PortMap.putAll(GetNetMap("input_vector(" + i + ")", true, componentInfo, i, nets));
      else {
        if (vectorList.length() > 0) vectorList.append(",");
        vectorList.append(GetNetName(componentInfo, i, true, nets));
      }
    }
    if (HDL.isVerilog()) PortMap.put("input_vector", vectorList.toString());
    PortMap.putAll(GetNetMap("GroupSelect", true, componentInfo, nrOfBits + PriorityEncoder.GS, nets));
    PortMap.putAll(GetNetMap("EnableOut", true, componentInfo, nrOfBits + PriorityEncoder.EN_OUT, nets));
    PortMap.putAll(GetNetMap("Address", true, componentInfo, nrOfBits + PriorityEncoder.OUT, nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("s_in_is_zero", 1);
    wires.put("s_address", 5);
    wires.put("v_select_1_vector", 33);
    wires.put("v_select_2_vector", 16);
    wires.put("v_select_3_vector", 8);
    wires.put("v_select_4_vector", 4);
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
