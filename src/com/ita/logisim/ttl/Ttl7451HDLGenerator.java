package com.ita.logisim.ttl;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;

public class Ttl7451HDLGenerator extends AbstractHDLGeneratorFactory {

	@Override
	public String getComponentStringIdentifier() {
		return "TTL";
	}
	
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		MyInputs.put("A1", 1);
		MyInputs.put("B1", 1);
		MyInputs.put("C1", 1);
		MyInputs.put("D1", 1);
		MyInputs.put("A2", 1);
		MyInputs.put("B2", 1);
		MyInputs.put("C2", 1);
		MyInputs.put("D2", 1);
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		MyOutputs.put("Y1", 1);
		MyOutputs.put("Y2", 1);
		return MyOutputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   Y1 <= (A1 AND B1) NOR (C1 AND D1);");
		Contents.add("   Y2 <= (A2 AND B2) NOR (C2 AND D2);");
		return Contents;
	}
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("A1",true,ComponentInfo,0,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B1",true,ComponentInfo,9,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("C1",true,ComponentInfo,7,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D1",true,ComponentInfo,8,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Y1",true,ComponentInfo,6,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("A2",true,ComponentInfo,1,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B2",true,ComponentInfo,2,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("C2",true,ComponentInfo,3,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D2",true,ComponentInfo,4,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Y2",true,ComponentInfo,5,Reporter,HDLType,Nets));
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
		return (!attrs.getValue(TTL.VCC_GND)&&(HDLType.equals(HDLGeneratorFactory.VHDL)));
	}
}
