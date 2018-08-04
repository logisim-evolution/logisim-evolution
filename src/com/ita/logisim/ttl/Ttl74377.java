package com.ita.logisim.ttl;

import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.cburch.logisim.data.AttributeSet;

public class Ttl74377 extends AbstractOctalFlops {

	public class Ttl74377HDLGenerator extends AbstractOctalFlopsHDLGenerator {
		
		@Override
		public String getComponentStringIdentifier() {
			return "TTL74377";
		}
		
		@Override
		public SortedMap<String, String> GetPortMap(Netlist Nets,
				NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
			SortedMap<String, String> PortMap = new TreeMap<String, String>();
			PortMap.putAll(super.GetPortMap(Nets, ComponentInfo, Reporter, HDLType));
			PortMap.put("nCLR", "'1'");
			PortMap.putAll(GetNetMap("nCLKEN",false,ComponentInfo,0,Reporter,HDLType,Nets));
			return PortMap;
		}
	}

	public Ttl74377() {
		super("74377", (byte) 20, new byte[] { 2,5,6,9,12,15,16,19 },
				new String[] { "nCLKen", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4",
						       "Q4", "CLK", "Q5", "D5", "D6" , "Q6", "Q7" , "D7", "D8" , "Q8"});
		super.SetWe(true);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new Ttl74377HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
