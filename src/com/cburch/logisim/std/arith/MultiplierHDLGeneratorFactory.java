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
package com.cburch.logisim.std.arith;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;

public class MultiplierHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
	final private static String NrOfBitsStr = "NrOfBits";
	final private static int NrOfBitsId = -1;
	final private static String CalcBitsStr = "NrOfCalcBits";
	final private static int CalcBitsId = -2;

	@Override
	public String getComponentStringIdentifier() {
		return "MULT";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("INP_A", NrOfBitsId);
		Inputs.put("INP_B", NrOfBitsId);
		Inputs.put("Cin", NrOfBitsId);
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			Contents.add("   s_mult_result <= std_logic_vector(unsigned(INP_A)*unsigned(INP_B));");
			Contents.add("   s_extended_Cin(" + CalcBitsStr + "-1 DOWNTO "
					+ NrOfBitsStr + ") <= (OTHERS => '0');");
			Contents.add("   s_extended_Cin(" + NrOfBitsStr
					+ "-1 DOWNTO 0) <= Cin;");
			Contents.add("   s_new_result  <= std_logic_vector(unsigned(s_mult_result) + unsigned(s_extended_Cin));");
			Contents.add("   Mult_hi       <= s_new_result(" + CalcBitsStr
					+ "-1 DOWNTO " + NrOfBitsStr + ");");
			Contents.add("   Mult_lo       <= s_new_result(" + NrOfBitsStr
					+ "-1 DOWNTO 0);");
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("Mult_lo", NrOfBitsId);
		Outputs.put("Mult_hi", NrOfBitsId);
		return Outputs;
	}

	@Override
	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
		Parameters.put(NrOfBitsId, NrOfBitsStr);
		Parameters.put(CalcBitsId, CalcBitsStr);
		return Parameters;
	}

	@Override
	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
		int NrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth()
				.getWidth();
		ParameterMap.put(NrOfBitsStr, NrOfBits);
		ParameterMap.put(CalcBitsStr, 2 * NrOfBits);
		return ParameterMap;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Multiplier.IN0,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Multiplier.IN1,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Cin", true, ComponentInfo, Multiplier.C_IN,
				Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Mult_lo", true, ComponentInfo,
				Multiplier.OUT, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Mult_hi", true, ComponentInfo,
				Multiplier.C_OUT, Reporter, HDLType, Nets));
		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "arithmetic";
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("s_mult_result", CalcBitsId);
		Wires.put("s_extended_Cin", CalcBitsId);
		Wires.put("s_new_result", CalcBitsId);
		return Wires;
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return HDLType.equals(VHDL);
	}

}
