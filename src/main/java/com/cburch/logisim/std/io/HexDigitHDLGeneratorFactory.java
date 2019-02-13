package com.cburch.logisim.std.io;


import java.util.ArrayList;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;

public class HexDigitHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
	
	@Override
	public ArrayList<String> GetInlinedCode(Netlist Nets, Long ComponentId,
			NetlistComponent ComponentInfo, FPGAReport Reporter,
			String CircuitName, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		String Label = ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
		String BusName = GetBusName(ComponentInfo, 0, HDLType, Nets);
		Contents.add(" ");
		Contents.add("   "+Label+" : PROCESS ( "+BusName+" ) IS");
		Contents.add("      VARIABLE v_segs : std_logic_vector( 6 DOWNTO 0 );");
        Contents.add("      BEGIN");
        Contents.add("         CASE ( "+BusName+" ) IS");
        Contents.add("            WHEN \"0000\" => v_segs := \"0111111\";");
        Contents.add("            WHEN \"0001\" => v_segs := \"0000110\";");
        Contents.add("            WHEN \"0010\" => v_segs := \"1011011\";");
        Contents.add("            WHEN \"0011\" => v_segs := \"1001111\";");
        Contents.add("            WHEN \"0100\" => v_segs := \"1100110\";");
        Contents.add("            WHEN \"0101\" => v_segs := \"1101101\";");
        Contents.add("            WHEN \"0110\" => v_segs := \"1111101\";");
        Contents.add("            WHEN \"0111\" => v_segs := \"0000111\";");
        Contents.add("            WHEN \"1000\" => v_segs := \"1111111\";");
        Contents.add("            WHEN \"1001\" => v_segs := \"1100111\";");
        Contents.add("            WHEN \"1010\" => v_segs := \"1110111\";");
        Contents.add("            WHEN \"1011\" => v_segs := \"1111100\";");
        Contents.add("            WHEN \"1100\" => v_segs := \"0111001\";");
        Contents.add("            WHEN \"1101\" => v_segs := \"1011110\";");
        Contents.add("            WHEN \"1110\" => v_segs := \"1111001\";");
        Contents.add("            WHEN OTHERS => v_segs := \"1110001\";");
        Contents.add("         END CASE;");
        for (int i = 0 ; i < 7 ; i++)
        	Contents.add("         "+HDLGeneratorFactory.LocalOutputBubbleBusname+"("+ 
        			Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + i) + ") <= v_segs("+i+");");
        Contents.add("         "+HDLGeneratorFactory.LocalOutputBubbleBusname+"("+ 
        			Integer.toString(ComponentInfo.GetLocalBubbleOutputStartId() + 7) + ") <= '0';");
        Contents.add("      END PROCESS "+Label+";");
		return Contents;
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return HDLType.equals(HDLGeneratorFactory.VHDL);
	}

	@Override
	public boolean IsOnlyInlined(String HDLType) {
		return true;
	}
}
