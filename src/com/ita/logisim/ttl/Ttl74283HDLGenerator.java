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

public class Ttl74283HDLGenerator extends AbstractHDLGeneratorFactory {

	@Override
	public String getComponentStringIdentifier() {
		return "TTL74283";
	}
	
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		MyInputs.put("A1", 1);
		MyInputs.put("A2", 1);
		MyInputs.put("A3", 1);
		MyInputs.put("A4", 1);
		MyInputs.put("B1", 1);
		MyInputs.put("B2", 1);
		MyInputs.put("B3", 1);
		MyInputs.put("B4", 1);
		MyInputs.put("Cin", 1);
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		MyOutputs.put("S1", 1);
		MyOutputs.put("S2", 1);
		MyOutputs.put("S3", 1);
		MyOutputs.put("S4", 1);
		MyOutputs.put("Cout", 1);
		return MyOutputs;
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("oppA", 5);
		Wires.put("oppB", 5);
		Wires.put("oppC", 5);
		Wires.put("Result", 5);
		return Wires;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   oppA   <= \"0\"&A4&A3&A2&A1;");
		Contents.add("   oppB   <= \"0\"&B4&B3&B2&B1;");
		Contents.add("   oppC   <= \"0000\"&Cin;");
		Contents.add("   Result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));");
		Contents.add("   S1     <= Result(0);");
		Contents.add("   S2     <= Result(1);");
		Contents.add("   S3     <= Result(2);");
		Contents.add("   S4     <= Result(3);");
		Contents.add("   Cout   <= Result(4);");
		return Contents;
	}
	
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.putAll(GetNetMap("A1",true,ComponentInfo,4,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("A2",true,ComponentInfo,2,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("A3",true,ComponentInfo,12,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("A4",true,ComponentInfo,10,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B1",true,ComponentInfo,5,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B2",true,ComponentInfo,1,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B3",true,ComponentInfo,13,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("B4",true,ComponentInfo,9,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Cin",true,ComponentInfo,6,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("S1",true,ComponentInfo,3,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("S2",true,ComponentInfo,0,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("S3",true,ComponentInfo,11,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("S4",true,ComponentInfo,8,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Cout",true,ComponentInfo,7,Reporter,HDLType,Nets));
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
