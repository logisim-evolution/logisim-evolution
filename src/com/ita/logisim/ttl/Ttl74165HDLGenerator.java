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

public class Ttl74165HDLGenerator extends AbstractHDLGeneratorFactory {

	@Override
	public String getComponentStringIdentifier() {
		return "TTL74165";
	}
	
	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
		MyInputs.put("SHnLD", 1);
		MyInputs.put("CK", 1);
		MyInputs.put("CKIh", 1);
		MyInputs.put("SER", 1);
		MyInputs.put("P0", 1);
		MyInputs.put("P1", 1);
		MyInputs.put("P2", 1);
		MyInputs.put("P3", 1);
		MyInputs.put("P4", 1);
		MyInputs.put("P5", 1);
		MyInputs.put("P6", 1);
		MyInputs.put("P7", 1);
		MyInputs.put("Tick", 1);
		return MyInputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
		MyOutputs.put("Q7", 1);
		MyOutputs.put("Q7n", 1);
		return MyOutputs;
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		Wires.put("CurState", 8);
		Wires.put("NextState", 8);
		Wires.put("ParData", 8);
		Wires.put("Enable", 1);
		return Wires;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   Q7  <= CurState(0);\n");
		Contents.add("   Q7n <= NOT(CurState(0));\n");
		Contents.add("\n");
		Contents.add("   Enable  <= NOT(CKIh) AND Tick;");
		Contents.add("   ParData <= P7&P6&P5&P4&P3&P2&P1&P0;");
		Contents.add("\n");
		Contents.add("   NextState <= CurState WHEN Enable = '0' ELSE");
		Contents.add("                ParData WHEN SHnLD = '0' ELSE");
		Contents.add("                SER&CurState(7 DOWNTO 1);");
		Contents.add("\n");
		Contents.add("   dffs : PROCESS( CK ) IS");
		Contents.add("      BEGIN");
		Contents.add("         IF (rising_edge(CK)) THEN CurState <= NextState;");
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
			Reporter.AddSevereWarning("Component \"TTL74165\" in circuit \""
					+ Nets.getCircuitName() + "\" has no clock connection");
			HasClock = false;
		}
		String ClockNetName = GetClockNetName(ComponentInfo,ClockPinIndex, Nets);
		if (ClockNetName.isEmpty()) {
			GatedClock = true;
		}
		if (!HasClock) {
			PortMap.put("CK", "'0'");
			PortMap.put("Tick", "'0'");
		} else if (GatedClock) {
			PortMap.put("Tick", "'1'");
			PortMap.put("CK", GetNetName(ComponentInfo, ClockPinIndex, true, HDLType,Nets));
		} else {
			if (Nets.RequiresGlobalClockConnection()) {
				PortMap.put("Tick", "'1'");
			} else {
				PortMap.put("Tick",ClockNetName + "("
					+ Integer .toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
					+ ")");
			}
			PortMap.put( "CK", ClockNetName + "("
							+ Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
							+ ")");
		}
		PortMap.putAll(GetNetMap("SHnLD",true,ComponentInfo,0,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("CKIh",true,ComponentInfo,13,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("SER",true,ComponentInfo,8,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P0",true,ComponentInfo,9,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P1",true,ComponentInfo,10,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P2",true,ComponentInfo,11,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P3",true,ComponentInfo,12,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P4",true,ComponentInfo,2,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P5",true,ComponentInfo,3,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P6",true,ComponentInfo,4,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("P7",true,ComponentInfo,5,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q7n",true,ComponentInfo,6,Reporter,HDLType,Nets));
		PortMap.putAll(GetNetMap("Q7",true,ComponentInfo,7,Reporter,HDLType,Nets));
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
