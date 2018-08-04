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
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class AbstractOctalFlopsHDLGenerator  extends AbstractHDLGeneratorFactory {

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		MyInputs.put("nCLR", 1);
		MyInputs.put("nCLKen", 1);
		MyInputs.put("CLK", 1);
		MyInputs.put("tick", 1);
		MyInputs.put("D0", 1);
		MyInputs.put("D1", 1);
		MyInputs.put("D2", 1);
		MyInputs.put("D3", 1);
		MyInputs.put("D4", 1);
		MyInputs.put("D5", 1);
		MyInputs.put("D6", 1);
		MyInputs.put("D7", 1);
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		MyOutputs.put("Q0", 1);
		MyOutputs.put("Q1", 1);
		MyOutputs.put("Q2", 1);
		MyOutputs.put("Q3", 1);
		MyOutputs.put("Q4", 1);
		MyOutputs.put("Q5", 1);
		MyOutputs.put("Q6", 1);
		MyOutputs.put("Q7", 1);
		return MyOutputs;
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("state", 8);
		Wires.put("enable", 1);
		Wires.put("nexts", 8);
		return Wires;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   enable <= tick and NOT(nCLKen);");
		Contents.add("   nexts  <= D7&D6&D5&D4&D3&D2&D1&D0 WHEN enable = '1' ELSE state;");
		Contents.add("   Q0     <= state(0);");
		Contents.add("   Q1     <= state(1);");
		Contents.add("   Q2     <= state(2);");
		Contents.add("   Q3     <= state(3);");
		Contents.add("   Q4     <= state(4);");
		Contents.add("   Q5     <= state(5);");
		Contents.add("   Q6     <= state(6);");
		Contents.add("   Q7     <= state(7);");
		Contents.add(" ");
		Contents.add("   dffs : PROCESS( CLK , nCLR ) IS");
		Contents.add("      BEGIN");
		Contents.add("         IF (nCLR = '1') THEN state <= (OTHERS => '0');");
		Contents.add("         ELSIF (rising_edge(CLK)) THEN state <= nexts;");
		Contents.add("         END IF;");
		Contents.add("      END PROCESS dffs;");
		return Contents;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		Boolean GatedClock = false;
		Boolean HasClock = true;
		int ClockPinIndex = ComponentInfo.GetComponent().getFactory().ClockPinIndex(null)[0];
		if (!ComponentInfo.EndIsConnected(ClockPinIndex)) {
			Reporter.AddSevereWarning("Component \""+getComponentStringIdentifier()+"\" in circuit \""
					+ Nets.getCircuitName() + "\" has no clock connection");
			HasClock = false;
		}
		String ClockNetName = GetClockNetName(ComponentInfo,ClockPinIndex, Nets);
		if (ClockNetName.isEmpty()) {
			GatedClock = true;
		}
		if (!HasClock) {
			PortMap.put("CLK", "'0'");
			PortMap.put("tick", "'0'");
		} else if (GatedClock) {
			PortMap.put("tick", "'1'");
			PortMap.put("CLK", GetNetName(ComponentInfo, ClockPinIndex, true, HDLType,Nets));
		} else {
			if (Nets.RequiresGlobalClockConnection()) {
				PortMap.put("tick", "'1'");
			} else {
				PortMap.put("tick",ClockNetName + "("
						+ Integer .toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
						+ ")");
			}
			PortMap.put( "CLK", ClockNetName + "("
						+ Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
						+ ")");
		}
		PortMap.putAll(GetNetMap("D0",true,ComponentInfo,2,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D1",true,ComponentInfo,3,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D2",true,ComponentInfo,6,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D3",true,ComponentInfo,7,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D4",true,ComponentInfo,11,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D5",true,ComponentInfo,12,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D6",true,ComponentInfo,15,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("D7",true,ComponentInfo,16,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q0",true,ComponentInfo,1,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q1",true,ComponentInfo,4,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q2",true,ComponentInfo,5,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q3",true,ComponentInfo,8,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q4",true,ComponentInfo,10,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q5",true,ComponentInfo,13,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q6",true,ComponentInfo,14,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q7",true,ComponentInfo,17,Reporter,HDLType,Nets));
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
