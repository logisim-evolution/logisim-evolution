package com.ita.logisim.ttl;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;

public class AbstractGateHDLGenerator extends AbstractHDLGeneratorFactory {
	
	public boolean IsInverter() {
		return false;
	}
	
	@Override
	public String getComponentStringIdentifier() {
		return "TTL";
	}
	
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		int NrOfGates = (IsInverter()) ? 6 : 4;
		for (int i = 0 ; i < NrOfGates ; i++) {
			MyInputs.put("gate_"+Integer.toString(i)+"_A", 1);
			if (!IsInverter())
				MyInputs.put("gate_"+Integer.toString(i)+"_B", 1);
		}
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		int NrOfGates = (IsInverter()) ? 6 : 4;
		for (int i = 0 ; i < NrOfGates ; i++) {
			MyOutputs.put("gate_"+Integer.toString(i)+"_O", 1);
		}
		return MyOutputs;
	}

	public ArrayList<String> GetLogicFunction(int index, String HDLType) {
		return new ArrayList<String>();
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		int NrOfGates = (IsInverter()) ? 6 : 4;
		for (int i = 0 ; i < NrOfGates ; i++) {
			Contents.addAll(MakeRemarkBlock("Here gate "+Integer.toString(i)+" is described",
					3, HDLType));
			Contents.addAll(GetLogicFunction(i,HDLType));
		}
		return Contents;
	}
	
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		int NrOfGates = (IsInverter()) ? 6 : 4;
		for (int i = 0 ; i < NrOfGates ; i++) {
			if (IsInverter()) {
				int inindex = (i < 3) ? i*2 : i*2+1;
				int outindex = (i<3) ? i*2+1 : i*2;
				PortMap.putAll(GetNetMap("gate_"+Integer.toString(i)+"_A",true,ComponentInfo,inindex, Reporter,
						HDLType, Nets));
				PortMap.putAll(GetNetMap("gate_"+Integer.toString(i)+"_O",true,ComponentInfo,outindex, Reporter,
						HDLType, Nets));
			} else {
				int inindex1 = (i<2) ? i*3 :i*3+1;
				int inindex2 = inindex1+1;
				int outindex = (i<2)? i*3+2 : i*3;
				PortMap.putAll(GetNetMap("gate_"+Integer.toString(i)+"_A",true,ComponentInfo,inindex1, Reporter,
						HDLType, Nets));
				PortMap.putAll(GetNetMap("gate_"+Integer.toString(i)+"_B",true,ComponentInfo,inindex2, Reporter,
						HDLType, Nets));
				PortMap.putAll(GetNetMap("gate_"+Integer.toString(i)+"_O",true,ComponentInfo,outindex, Reporter,
						HDLType, Nets));
			}
		}
		return PortMap;
	}
	
	@Override
	public String GetSubDir() {
		/*
		 * this method returns the module directory where the HDL code needs to
		 * be placed
		 */
		return "ttl";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		/* TODO: Add support for the ones with VCC and Ground Pin */
		if (attrs==null)
			return false;
		return !attrs.getValue(TTL.VCC_GND);
	}

	
}
