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

package com.cburch.logisim.std.hdl;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.Port;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	@Override
	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, FPGAReport Reporter,
			String HDLType) {
		ArrayList<String> contents = new ArrayList<String>();
		contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType,
				TheNetlist.projName()));

		VhdlContent content = (VhdlContent) attrs
				.getValue(VhdlEntity.CONTENT_ATTR);
		contents.add(content.getLibraries());
		contents.add(content.getArchitecture());

		return contents;
	}

	@Override
	public String getComponentStringIdentifier() {
		return "VHDL";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> inputs = new TreeMap<String, Integer>();

		Port[] rawInputs = attrs.getValue(VhdlEntity.CONTENT_ATTR).getInputs();
		for (int i = 0; i < rawInputs.length; i++)
			inputs.put(rawInputs[i].getToolTip(), rawInputs[i]
					.getFixedBitWidth().getWidth());

		return inputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> outputs = new TreeMap<String, Integer>();

		Port[] rawOutputs = attrs.getValue(VhdlEntity.CONTENT_ATTR)
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
		VhdlContent content = attrs.getValue(VhdlEntity.CONTENT_ATTR);

		Port[] inputs = content.getInputs();
		Port[] outputs = content.getOutputs();

		for (int i = 0; i < inputs.length; i++)
			PortMap.putAll(GetNetMap(inputs[i].getToolTip(), true,
					ComponentInfo, i, Reporter, HDLType, Nets));
		for (int i = 0; i < outputs.length; i++)
			PortMap.putAll(GetNetMap(outputs[i].getToolTip(), true,
					ComponentInfo, i + inputs.length, Reporter, HDLType, Nets));

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
