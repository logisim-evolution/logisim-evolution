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

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class PriorityEncoderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_SELECT_BITS_STR = "NrOfSelectBits";
  private static final int NrOfSelectBitsId = -1;
  private static final String NR_OF_INPUT_BITS_STR = "NrOfInputBits";
  private static final int NrOfInputBitsId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "PRIENC";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("enable", 1);
    map.put("input_vector", NrOfInputBitsId);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = (new LineBuffer())
            .pair("selBits", NR_OF_SELECT_BITS_STR)
            .pair("inBits", NR_OF_INPUT_BITS_STR);
    if (HDL.isVHDL()) {
      contents.addLines(
          "   -- Output Signals",
          "   GroupSelect <= NOT(s_in_is_zero) AND enable;",
          "   EnableOut   <= s_in_is_zero AND enable;",
          "   Address     <= (OTHERS => '0') WHEN enable = '0' ELSE",
          "                  s_address({{selBits}}-1 DOWNTO 0);",
          "",
          "   -- Control Signals ",
          "   s_in_is_zero  <= '1' WHEN input_vector = std_logic_vector(to_unsigned(0,{{inBits}})) ELSE '0';",
          "",
          "   -- Processes",
          "   make_addr : PROCESS( input_vector , v_select_1_vector , v_select_2_vector , v_select_3_vector , v_select_4_vector )",
          "   BEGIN",
          "      v_select_1_vector(32 DOWNTO {{inBits}})  <= (OTHERS => '0');",
          "      v_select_1_vector({{inBits}}-1 DOWNTO 0) <= input_vector;",
          "      IF (v_select_1_vector(31 DOWNTO 16) = X\"0000\") THEN s_address(4)      <= '0';",
          "                                                          v_select_2_vector <= v_select_1_vector(15 DOWNTO 0);",
          "                                                     ELSE s_address(4)      <= '1';",
          "                                                          v_select_2_vector <= v_select_1_vector(31 DOWNTO 16);",
          "      END IF;",
          "      IF (v_select_2_vector(15 DOWNTO 8) = X\"00\") THEN s_address(3)      <= '0';",
          "                                                       v_select_3_vector <= v_select_2_vector(7 DOWNTO 0);",
          "                                                  ELSE s_address(3)      <= '1';",
          "                                                       v_select_3_vector <= v_select_2_vector(15 DOWNTO 8);",
          "      END IF;",
          "      IF (v_select_3_vector(7 DOWNTO 4) = X\"0\") THEN s_address(2)      <= '0';",
          "                                                     v_select_4_vector <= v_select_3_vector(3 DOWNTO 0);",
          "                                                ELSE s_address(2)      <= '1';",
          "                                                     v_select_4_vector <= v_select_3_vector(7 DOWNTO 4);",
          "      END IF;",
          "      IF (v_select_4_vector(3 DOWNTO 2) = \"00\") THEN s_address(1) <= '0';",
          "                                                     s_address(0) <= v_select_4_vector(1);",
          "                                                ELSE s_address(1) <= '1';",
          "                                                     s_address(0) <= v_select_4_vector(3);",
          "      END IF;",
          "   END PROCESS make_addr;");
    } else {
      contents.addLines(
          "assign GroupSelect = ~s_in_is_zero&enable;",
          "assign EnableOut = s_in_is_zero&enable;",
          "assign Address = (~enable) ? 0 : s_address[{{selBits}}-1:0];",
          "assign s_in_is_zero = (input_vector == 0) ? 1'b1 : 1'b0;",
          "",
          "assign v_select_1_vector[32:{{selBits}}] = 0;",
          "assign v_select_1_vector[{{selBits}}-1:0] = input_vector;",
          "assign s_address[4] = (v_select_1_vector[31:16] == 0) ? 1'b0 : 1'b1;",
          "assign v_select_2_vector = (v_select_1_vector[31:16] == 0) ? v_select_1_vector[15:0] : v_select_1_vector[31:16];",
          "assign s_address[3] = (v_select_2_vector[15:8] == 0) ? 1'b0 : 1'b1;",
          "assign v_select_3_vector = (v_select_2_vector[15:8] == 0) ? v_select_2_vector[7:0] : v_select_2_vector[15:8];",
          "assign s_address[2] = (v_select_3_vector[7:4] == 0) ? 1'b0 : 1'b1;",
          "assign v_select_4_vector = (v_select_3_vector[7:4] == 0) ? v_select_3_vector[3:0] : v_select_2_vector[7:4];",
          "assign s_address[1] = (v_select_4_vector[3:2] == 0) ? 1'b0 : 1'b1;",
          "assign s_address[0] = (v_select_4_vector[3:2] == 0) ? v_select_4_vector[1] : v_select_4_vector[3];");
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("GroupSelect", 1);
    map.put("EnableOut", 1);
    map.put("Address", NrOfSelectBitsId);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(NrOfSelectBitsId, NR_OF_SELECT_BITS_STR);
    map.put(NrOfInputBitsId, NR_OF_INPUT_BITS_STR);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits = componentInfo.nrOfEnds() - 4;
    final var nrOfSelectBits = componentInfo.getComponent().getEnd(nrOfBits + PriorityEncoder.OUT).getWidth().getWidth();
    map.put(NR_OF_SELECT_BITS_STR, nrOfSelectBits);
    map.put(NR_OF_INPUT_BITS_STR, 1 << nrOfSelectBits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfBits = comp.nrOfEnds() - 4;
    map.putAll(GetNetMap("enable", false, comp, nrOfBits + PriorityEncoder.EN_IN, nets));
    final var vectorList = new StringBuilder();
    for (var i = nrOfBits - 1; i >= 0; i--) {
      if (HDL.isVHDL())
        map.putAll(GetNetMap("input_vector(" + i + ")", true, comp, i, nets));
      else {
        if (vectorList.length() > 0) vectorList.append(",");
        vectorList.append(GetNetName(comp, i, true, nets));
      }
    }
    if (HDL.isVerilog()) map.put("input_vector", vectorList.toString());
    map.putAll(GetNetMap("GroupSelect", true, comp, nrOfBits + PriorityEncoder.GS, nets));
    map.putAll(GetNetMap("EnableOut", true, comp, nrOfBits + PriorityEncoder.EN_OUT, nets));
    map.putAll(GetNetMap("Address", true, comp, nrOfBits + PriorityEncoder.OUT, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_in_is_zero", 1);
    map.put("s_address", 5);
    map.put("v_select_1_vector", 33);
    map.put("v_select_2_vector", 16);
    map.put("v_select_3_vector", 8);
    map.put("v_select_4_vector", 4);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
