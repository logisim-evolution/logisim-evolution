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
import com.cburch.logisim.instance.StdAttr;

public class NegatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	final private static String NrOfBitsStr = "NrOfBits";
	final private static int NrOfBitsId = -1;

	@Override
	public String getComponentStringIdentifier() {
		return "NEGATOR2C";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1
				: NrOfBitsId;
		Inputs.put("DataX", inputbits);
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
			if (nrOfBits == 1)
				Contents.add("   MinDataX <= DataX;");
			else
				Contents.add("   MinDataX <= std_logic_vector(unsigned(NOT(DataX)) + 1);");
		} else {
			Contents.add("   assign   MinDataX = -DataX;");
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		int outputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1
				: NrOfBitsId;
		Outputs.put("MinDataX", outputbits);
		return Outputs;
	}

	@Override
	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
		int outputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
		if (outputbits > 1)
			Parameters.put(NrOfBitsId, NrOfBitsStr);
		return Parameters;
	}

	@Override
	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
		int nrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth()
				.getWidth();
		if (nrOfBits > 1)
			ParameterMap.put(NrOfBitsStr, nrOfBits);
		return ParameterMap;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("DataX", true, ComponentInfo, 0, Reporter,
				HDLType, Nets));
		PortMap.putAll(GetNetMap("MinDataX", true, ComponentInfo, 1, Reporter,
				HDLType, Nets));
		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "arithmetic";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return true;
	}

}
