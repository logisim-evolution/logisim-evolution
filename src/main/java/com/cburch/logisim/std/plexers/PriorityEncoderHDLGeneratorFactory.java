/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.plexers;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;

public class PriorityEncoderHDLGeneratorFactory extends
		AbstractHDLGeneratorFactory {

	private static final String NrOfSelectBitsStr = "NrOfSelectBits";
	private static final int NrOfSelectBitsId = -1;
	private static final String NrOfInputBitsStr = "NrOfInputBits";
	private static final int NrOfInputBitsId = -2;

	@Override
	public String getComponentStringIdentifier() {
		return "PRIENC";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("enable", 1);
		Inputs.put("input_vector", NrOfInputBitsId);
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			Contents.add("   -- Output Signals");
			Contents.add("   GroupSelect <= NOT(s_in_is_zero) AND enable;");
			Contents.add("   EnableOut   <= s_in_is_zero AND enable;");
			Contents.add("   Address     <= (OTHERS => '0') WHEN enable = '0' ELSE");
			Contents.add("                  s_address(" + NrOfSelectBitsStr
					+ "-1 DOWNTO 0);");
			Contents.add("");
			Contents.add("   -- Control Signals ");
			Contents.add("   s_in_is_zero  <= '1' WHEN input_vector = std_logic_vector(to_unsigned(0,"
					+ NrOfInputBitsStr + ")) ELSE '0';");
			Contents.add("");
			Contents.add("   -- Processes");
			Contents.add("   make_addr : PROCESS( input_vector , v_select_1_vector , v_select_2_vector , v_select_3_vector , v_select_4_vector )");
			Contents.add("   BEGIN");
			Contents.add("      v_select_1_vector(32 DOWNTO "
					+ NrOfInputBitsStr + ")  <= (OTHERS => '0');");
			Contents.add("      v_select_1_vector(" + NrOfInputBitsStr
					+ "-1 DOWNTO 0) <= input_vector;");
			Contents.add("      IF (v_select_1_vector(31 DOWNTO 16) = X\"0000\") THEN s_address(4)      <= '0';");
			Contents.add("                                                          v_select_2_vector <= v_select_1_vector(15 DOWNTO 0);");
			Contents.add("                                                     ELSE s_address(4)      <= '1';");
			Contents.add("                                                          v_select_2_vector <= v_select_1_vector(31 DOWNTO 16);");
			Contents.add("      END IF;");
			Contents.add("      IF (v_select_2_vector(15 DOWNTO 8) = X\"00\") THEN s_address(3)      <= '0';");
			Contents.add("                                                       v_select_3_vector <= v_select_2_vector(7 DOWNTO 0);");
			Contents.add("                                                  ELSE s_address(3)      <= '1';");
			Contents.add("                                                       v_select_3_vector <= v_select_2_vector(15 DOWNTO 8);");
			Contents.add("      END IF;");
			Contents.add("      IF (v_select_3_vector(7 DOWNTO 4) = X\"0\") THEN s_address(2)      <= '0';");
			Contents.add("                                                     v_select_4_vector <= v_select_3_vector(3 DOWNTO 0);");
			Contents.add("                                                ELSE s_address(2)      <= '1';");
			Contents.add("                                                     v_select_4_vector <= v_select_3_vector(7 DOWNTO 4);");
			Contents.add("      END IF;");
			Contents.add("      IF (v_select_4_vector(3 DOWNTO 2) = \"00\") THEN s_address(1) <= '0';");
			Contents.add("                                                     s_address(0) <= v_select_4_vector(1);");
			Contents.add("                                                ELSE s_address(1) <= '1';");
			Contents.add("                                                     s_address(0) <= v_select_4_vector(3);");
			Contents.add("      END IF;");
			Contents.add("   END PROCESS make_addr;");
		} else {
			Contents.add("   assign GroupSelect = ~s_in_is_zero&enable;");
			Contents.add("   assign EnableOut = s_in_is_zero&enable;");
			Contents.add("   assign Address = (~enable) ? 0 : s_address["
					+ NrOfSelectBitsStr + "-1:0];");
			Contents.add("   assign s_in_is_zero = (input_vector == 0) ? 1'b1 : 1'b0;");
			Contents.add("");
			Contents.add("   assign v_select_1_vector[32:" + NrOfInputBitsStr
					+ "] = 0;");
			Contents.add("   assign v_select_1_vector[" + NrOfInputBitsStr
					+ "-1:0] = input_vector;");
			Contents.add("   assign s_address[4] = (v_select_1_vector[31:16] == 0) ? 1'b0 : 1'b1;");
			Contents.add("   assign v_select_2_vector = (v_select_1_vector[31:16] == 0) ? v_select_1_vector[15:0] : v_select_1_vector[31:16];");
			Contents.add("   assign s_address[3] = (v_select_2_vector[15:8] == 0) ? 1'b0 : 1'b1;");
			Contents.add("   assign v_select_3_vector = (v_select_2_vector[15:8] == 0) ? v_select_2_vector[7:0] : v_select_2_vector[15:8];");
			Contents.add("   assign s_address[2] = (v_select_3_vector[7:4] == 0) ? 1'b0 : 1'b1;");
			Contents.add("   assign v_select_4_vector = (v_select_3_vector[7:4] == 0) ? v_select_3_vector[3:0] : v_select_2_vector[7:4];");
			Contents.add("   assign s_address[1] = (v_select_4_vector[3:2] == 0) ? 1'b0 : 1'b1;");
			Contents.add("   assign s_address[0] = (v_select_4_vector[3:2] == 0) ? v_select_4_vector[1] : v_select_4_vector[3];");
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("GroupSelect", 1);
		Outputs.put("EnableOut", 1);
		Outputs.put("Address", NrOfSelectBitsId);
		return Outputs;
	}

	@Override
	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
		Parameters.put(NrOfSelectBitsId, NrOfSelectBitsStr);
		Parameters.put(NrOfInputBitsId, NrOfInputBitsStr);
		return Parameters;
	}

	@Override
	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
		int nr_of_bits = ComponentInfo.NrOfEnds() - 4;
		int nr_of_select_bits = ComponentInfo.GetComponent()
				.getEnd(nr_of_bits + PriorityEncoder.OUT).getWidth().getWidth();
		ParameterMap.put(NrOfSelectBitsStr, nr_of_select_bits);
		ParameterMap.put(NrOfInputBitsStr, 1 << nr_of_select_bits);
		return ParameterMap;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		int nr_of_bits = ComponentInfo.NrOfEnds() - 4;
		PortMap.putAll(GetNetMap("enable", false, ComponentInfo, nr_of_bits
				+ PriorityEncoder.EN_IN, Reporter, HDLType, Nets));
		StringBuffer VectorList = new StringBuffer();
		for (int i = nr_of_bits - 1; i >= 0; i--) {
			if (HDLType.equals(VHDL))
				PortMap.putAll(GetNetMap("input_vector(" + Integer.toString(i)
						+ ")", true, ComponentInfo, i, Reporter, HDLType, Nets));
			else {
				if (VectorList.length() > 0)
					VectorList.append(",");
				VectorList.append(GetNetName(ComponentInfo, i, true, HDLType,
						Nets));
			}
		}
		if (HDLType.equals(VERILOG))
			PortMap.put("input_vector", VectorList.toString());
		PortMap.putAll(GetNetMap("GroupSelect", true, ComponentInfo, nr_of_bits
				+ PriorityEncoder.GS, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("EnableOut", true, ComponentInfo, nr_of_bits
				+ PriorityEncoder.EN_OUT, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Address", true, ComponentInfo, nr_of_bits
				+ PriorityEncoder.OUT, Reporter, HDLType, Nets));
		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "plexers";
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("s_in_is_zero", 1);
		Wires.put("s_address", 5);
		Wires.put("v_select_1_vector", 33);
		Wires.put("v_select_2_vector", 16);
		Wires.put("v_select_3_vector", 8);
		Wires.put("v_select_4_vector", 4);
		return Wires;
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return true;
	}

}
