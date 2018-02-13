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
package com.cburch.logisim.std.io;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;

public class ReptarLocalBusHDLGeneratorFactory extends
		AbstractHDLGeneratorFactory {

	// @Override
	// public ArrayList<String> GetComponentDeclarationSection(Netlist
	// TheNetlist,
	// AttributeSet attrs) {
	// ArrayList<String> Components = new ArrayList<String>();
	// // Components.add("COMPONENT " + getComponentStringIdentifier());
	// //
	// Components.add("   PORT ( Addr_Data_LB_io     : INOUT std_logic_vector(15 downto 0);");
	// // Components.add("          SP6_LB_nCS3_o       : IN std_logic;");
	// // Components.add("          SP6_LB_nADV_ALE_o   : IN std_logic;");
	// // Components.add("          SP6_LB_RE_nOE_o     : IN std_logic;");
	// // Components.add("          SP6_LB_nWE_o        : IN std_logic;");
	// // Components.add("          SP6_LB_WAIT3_i      : OUT std_logic;");
	// //
	// Components.add("          Addr_Data_LB_o      : IN std_logic_vector(15 downto 0);");
	// //
	// Components.add("          Addr_Data_LB_i      : OUT std_logic_vector(15 downto 0);");
	// // Components.add("          Addr_Data_LB_tris_i : OUT std_logic);");
	// // Components.add("END COMPONENT;");
	// return Components;
	// }
	@Override
	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, FPGAReport Reporter,
			String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
					HDLType, TheNetlist.projName()));
			Contents.add("");
			Contents.add("ARCHITECTURE PlatformIndependent OF "
					+ ComponentName.toString() + " IS ");
			Contents.add("");
			// Contents.add("signal Addr_Data_LB_in_s  :  std_logic_vector(15 downto 0);");
			// Contents.add("signal Addr_Data_LB_out_s  :  std_logic_vector(15 downto 0);");
			// Contents.add("signal local_bus_tris_s   :  std_logic;");
			// Contents.add("");
			Contents.add("BEGIN");
			Contents.add("");
			Contents.add("FPGA_out(0) <= NOT SP6_LB_WAIT3_i;");
			Contents.add("FPGA_out(1) <= NOT IRQ_i;");
			Contents.add("SP6_LB_nCS3_o       <= FPGA_in(0);");
			Contents.add("SP6_LB_nADV_ALE_o   <= FPGA_in(1);");
			Contents.add("SP6_LB_RE_nOE_o     <= FPGA_in(2);");
			Contents.add("SP6_LB_nWE_o        <= FPGA_in(3);");
			Contents.add("Addr_LB_o           <= FPGA_in(11 DOWNTO 4);");
			Contents.add("");
			Contents.add("IOBUF_Addresses_Datas : for i in 0 to Addr_Data_LB_io'length-1 generate");
			Contents.add("  IOBUF_Addresse_Data : IOBUF");
			Contents.add("  generic map (");
			Contents.add("    DRIVE => 12,");
			Contents.add(" IOSTANDARD => \"LVCMOS18\",");
			Contents.add("    SLEW => \"FAST\"");
			Contents.add("  )");
			Contents.add("  port map (");
			Contents.add("    O => Addr_Data_LB_o(i), -- Buffer output");
			Contents.add("    IO => Addr_Data_LB_io(i), -- Buffer inout port (connect directly to top-level port)");
			Contents.add("    I => Addr_Data_LB_i(i), -- Buffer input");
			Contents.add("    T => Addr_Data_LB_tris_i -- 3-state enable input, high=input, low=output");
			Contents.add("  );");
			Contents.add("end generate;");
			Contents.add("");
			Contents.add("END PlatformIndependent;");
		}
		// Contents.addAll(super.GetArchitecture(TheNetlist, attrs,
		// ComponentName, Reporter, HDLType));
		return Contents;
	}

	@Override
	public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, String HDLType/*
																	 * , boolean
																	 * hasLB
																	 */) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("   COMPONENT LocalBus");
		Contents.add("      PORT ( SP6_LB_WAIT3_i            : IN  std_logic;");
		Contents.add("             IRQ_i                     : IN  std_logic;");
		Contents.add("             Addr_Data_LB_io           : INOUT  std_logic_vector( 15 DOWNTO 0 );");
		Contents.add("             Addr_LB_o                 : OUT std_logic_vector( 8 DOWNTO 0 );");
		Contents.add("             SP6_LB_RE_nOE_o           : OUT std_logic;");
		Contents.add("             SP6_LB_nADV_ALE_o         : OUT std_logic;");
		Contents.add("             SP6_LB_nCS3_o             : OUT std_logic;");
		Contents.add("             SP6_LB_nWE_o              : OUT std_logic;");
		Contents.add("          FPGA_in             : IN std_logic_vector(12 downto 0);");
		Contents.add("          FPGA_out            : OUT std_logic_vector(1 downto 0);");
		Contents.add("            Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);");
		Contents.add("            Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);");
		Contents.add("            Addr_Data_LB_tris_i : IN std_logic);");
		Contents.add("   END COMPONENT;");
		// if (HDLType.equals(Settings.VHDL)) {
		// Contents.addAll(GetVHDLBlackBox(TheNetlist, attrs, ComponentName,
		// false, hasLB));
		// }
		return Contents;
	}

	@Override
	public String getComponentStringIdentifier() {
		return "ReptarLB";
	}

	@Override
	public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
			String ComponentName, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
				VHDL, TheNetlist.projName()));
		Contents.addAll(FileWriter.getExtendedLibrary());
		Contents.add("Library UNISIM;");
		Contents.add("use UNISIM.vcomponents.all;");
		Contents.add("");
		Contents.add("ENTITY " + ComponentName.toString() + " IS");
		Contents.add("   PORT ( Addr_Data_LB_io     : INOUT std_logic_vector(15 downto 0);");
		Contents.add("          SP6_LB_nCS3_o       : OUT std_logic;");
		Contents.add("          SP6_LB_nADV_ALE_o   : OUT std_logic;");
		Contents.add("          SP6_LB_RE_nOE_o     : OUT std_logic;");
		Contents.add("          SP6_LB_nWE_o        : OUT std_logic;");
		Contents.add("          SP6_LB_WAIT3_i      : IN std_logic;");
		Contents.add("          IRQ_i               : IN std_logic;");
		Contents.add("          FPGA_in             : IN std_logic_vector(12 downto 0);");
		Contents.add("          FPGA_out            : OUT std_logic_vector(1 downto 0);");
		Contents.add("          Addr_LB_o           : OUT std_logic_vector(8 downto 0);");
		Contents.add("          Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);");
		Contents.add("          Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);");
		Contents.add("          Addr_Data_LB_tris_i : IN std_logic);");
		Contents.add("END " + ComponentName.toString() + ";");

		return Contents;
	}

	@Override
	public ArrayList<String> GetInlinedCode(String HDLType,
			ArrayList<String> ComponentIdentifier, FPGAReport Reporter,
			MappableResourcesContainer MapInfo) {
		ArrayList<String> Contents = new ArrayList<String>();
		StringBuffer Temp = new StringBuffer();
		int OutputId = MapInfo.GetFPGAOutputPinId(MapInfo.GetMapNamesList(
				ComponentIdentifier).get(0));
		Component ThisPin = MapInfo.GetComponent(ComponentIdentifier)
				.GetComponent();

		Temp.append("   ");
		Temp.append(HDLGeneratorFactory.FPGAOutputPinName);
		Temp.append("_" + Integer.toString(OutputId));
		Temp.append(" <= ");
		Temp.append(" NOT ");
		Temp.append("s_"
				+ CorrectLabel.getCorrectLabel(ThisPin.getAttributeSet()
						.getValue(StdAttr.LABEL)));
		Temp.append(";");
		Contents.add(Temp.toString());

		// Contents.add("YOUHOU");
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("Addr_Data_LB_io", 16);
		return Outputs;
	}

	// @Override
	// public SortedMap<Integer,String> GetParameterList(AttributeSet attrs) {
	// SortedMap<Integer,String> Parameters = new TreeMap<Integer,String>();
	// int outputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
	// if (outputbits > 1)
	// Parameters.put(NrOfBitsId, NrOfBitsStr);
	// Parameters.put(ExtendedBitsId, ExtendedBitsStr);
	// return Parameters;
	// }
	// @Override
	// public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
	// Netlist Nets) {
	// SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
	// Wires.put("Addr_Data_LB_in_s", 16);
	// // Wires.put("Addr_LB_in_s", 8);
	// Wires.put("Addr_Data_LB_out_s", 16);
	// Wires.put("local_bus_tris_s", 1);
	// return Wires;
	// }

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("SP6_LB_WAIT3_i", 1);
		Inputs.put("IRQ_i", 1);
		// Outputs.put("Addr_Data_LB_i", 16);
		// Outputs.put("Addr_Data_LB_tris_i", 1);
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(VHDL)) {
			Contents.add(" ");
		} else {
			throw new UnsupportedOperationException(
					"Reptar Local Bus doesn't support verilog yet.");
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("SP6_LB_nCS3_o", 1);
		Outputs.put("SP6_LB_nADV_ALE_o", 1);
		Outputs.put("SP6_LB_RE_nOE_o", 1);
		Outputs.put("SP6_LB_nWE_o", 1);
		Outputs.put("Addr_LB_o", 9);
		// Inputs.put("Addr_Data_LB_o", 16);
		return Outputs;
	}

	// @Override
	// public SortedMap<String,Integer> GetParameterMap(Netlist Nets,
	// NetlistComponent ComponentInfo,
	// FPGAReport Reporter) {
	// SortedMap<String,Integer> ParameterMap = new TreeMap<String,Integer>();
	// int nrOfBits =
	// ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
	// ParameterMap.put(ExtendedBitsStr, nrOfBits+1);
	// if (nrOfBits > 1)
	// ParameterMap.put(NrOfBitsStr, nrOfBits);
	// return ParameterMap;
	// }
	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		
		 /* Ticket 1378 Redsmine -> Issue while having both the port IO and 
		  *  Local bus
		  *
		  */ 
		// Tickets 1378 --> Start here
		 PortMap.put("Addr_Data_LB_io", LocalInOutBubbleBusname + "(" +
		 ComponentInfo.GetLocalBubbleInOutEndId() + " DOWNTO " +
		 ComponentInfo.GetLocalBubbleInOutStartId() + ")");
		/* PortMap.put("Addr_Data_LB_io", LocalInOutBubbleBusname
		 				+ "(0 DOWNTO 15)");
		*/
		// Ticket 1378 --> Ends here
		PortMap.put(
				"FPGA_in",
				CorrectLabel.getCorrectLabel(ComponentInfo.GetComponent()
						.getAttributeSet().getValue(StdAttr.LABEL))
						+ "_i (12 downto 0)");
		PortMap.put(
				"FPGA_out",
				CorrectLabel.getCorrectLabel(ComponentInfo.GetComponent()
						.getAttributeSet().getValue(StdAttr.LABEL))
						+ "_o (1 downto 0)");
		PortMap.putAll(GetNetMap("SP6_LB_nCS3_o", true, ComponentInfo,
				ReptarLocalBus.SP6_LB_nCS3_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("SP6_LB_nADV_ALE_o", true, ComponentInfo,
				ReptarLocalBus.SP6_LB_nADV_ALE_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("SP6_LB_RE_nOE_o", true, ComponentInfo,
				ReptarLocalBus.SP6_LB_RE_nOE_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("SP6_LB_nWE_o", true, ComponentInfo,
				ReptarLocalBus.SP6_LB_nWE_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("SP6_LB_WAIT3_i", true, ComponentInfo,
				ReptarLocalBus.SP6_LB_WAIT3_i, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Addr_Data_LB_o", true, ComponentInfo,
				ReptarLocalBus.Addr_Data_LB_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Addr_Data_LB_i", true, ComponentInfo,
				ReptarLocalBus.Addr_Data_LB_i, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Addr_Data_LB_tris_i", true, ComponentInfo,
				ReptarLocalBus.Addr_Data_LB_tris_i, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Addr_LB_o", true, ComponentInfo,
				ReptarLocalBus.Addr_LB_o, Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("IRQ_i", true, ComponentInfo,
				ReptarLocalBus.IRQ_i, Reporter, HDLType, Nets));
		return PortMap;
	}

	@Override
	public String GetSubDir() {
		return "io";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
		return HDLType.equals(VHDL);
	}
}
