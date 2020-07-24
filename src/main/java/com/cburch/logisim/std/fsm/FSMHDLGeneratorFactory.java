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

package com.cburch.logisim.std.fsm;


import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.statemachine.codegen.FSMVHDLCodeGen;



public class FSMHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	@Override
	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, FPGAReport Reporter,
			String HDLType) {
		ArrayList<String> arrayList = new ArrayList<String>();
		ArrayList<String> contents = arrayList;
		contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType,
				TheNetlist.projName()));

		FSMContent content = (FSMContent) attrs.getValue(FSMEntity.CONTENT_ATTR);
		FSMVHDLCodeGen codegen = new FSMVHDLCodeGen();
		arrayList.add(codegen.generate(content.getFsm()).toString());
		
		return arrayList;
	}

	@Override
	public String getComponentStringIdentifier() {
		return "VHDL";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> inputs = new TreeMap<String, Integer>();

		FSMContent value = attrs.getValue(FSMEntity.CONTENT_ATTR);
		Port[] rawInputs = value.getInputs();
		for (int i = 0; i < rawInputs.length; i++)
			inputs.put(rawInputs[i].getToolTip(), rawInputs[i]
					.getFixedBitWidth().getWidth());

		inputs.put("Clk", 1);
		inputs.put("RST", 1);  
		inputs.put("EN", 1);
		
	
	return inputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> outputs = new TreeMap<String, Integer>();

		Port[] rawOutputs = attrs.getValue(FSMEntity.CONTENT_ATTR)
				.getOutputs();
		for (int i = 0; i < rawOutputs.length; i++)
			outputs.put(rawOutputs[i].getToolTip(), rawOutputs[i]
					.getFixedBitWidth().getWidth());

		return outputs;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();

		AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
		FSMContent content = attrs.getValue(FSMEntity.CONTENT_ATTR);

		Port[] inputs = content.getInputs();
		Port[] outputs = content.getOutputs();

		PortMap.putAll(GetNetMap("Clk", true,ComponentInfo,  0, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Rst", true,ComponentInfo,  1, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("EN", true,ComponentInfo,   2, Reporter, HDLType, Nets));

		for (int i = 0; i < inputs.length; i++)
			PortMap.putAll(GetNetMap(inputs[i].getToolTip(), true,
					ComponentInfo, i+3, Reporter, HDLType, Nets));
		
		for (int i = 0; i < outputs.length; i++)
			PortMap.putAll(GetNetMap(outputs[i].getToolTip(), true,
					ComponentInfo, i + inputs.length+3, Reporter, HDLType, Nets));

		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "circuit";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return HDLType.equals(HDLGeneratorFactory.VHDL);
	}

	
	
}
