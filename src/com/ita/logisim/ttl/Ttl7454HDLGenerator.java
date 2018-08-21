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

public class Ttl7454HDLGenerator extends AbstractHDLGeneratorFactory {

	@Override
	public String getComponentStringIdentifier() {
		return "TTL";
	}
	
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		MyInputs.put("A", 1);
		MyInputs.put("B", 1);
		MyInputs.put("C", 1);
		MyInputs.put("D", 1);
		MyInputs.put("E", 1);
		MyInputs.put("F", 1);
		MyInputs.put("G", 1);
		MyInputs.put("H", 1);
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		MyOutputs.put("Y", 1);
		return MyOutputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   Y <= NOT((A AND B) OR (C AND D) OR (E AND F) OR (G AND H));");
		return Contents;
	}
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("A",true,ComponentInfo,0,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B",true,ComponentInfo,8,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("C",true,ComponentInfo,1,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D",true,ComponentInfo,2,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("E",true,ComponentInfo,3,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("F",true,ComponentInfo,4,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("G",true,ComponentInfo,6,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("H",true,ComponentInfo,7,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Y",true,ComponentInfo,5,Reporter,HDLType,Nets));
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
