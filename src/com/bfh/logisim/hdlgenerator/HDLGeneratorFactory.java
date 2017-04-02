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

package com.bfh.logisim.hdlgenerator;

import java.util.ArrayList;
import java.util.Set;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.cburch.logisim.data.AttributeSet;

public interface HDLGeneratorFactory {

	public static String VHDL = "VHDL";
	public static String VERILOG = "Verilog";
	public static final int PallignmentSize = 26;
	public static final int SallignmentSize = 35;
	public static final String NetName = "s_LOGISIM_NET_";
	public static final String BusName = "s_LOGISIM_BUS_";
	public static final String LocalInputBubbleBusname = "LOGISIM_INPUT_BUBBLES";
	public static final String LocalOutputBubbleBusname = "LOGISIM_OUTPUT_BUBBLES";
	public static final String LocalInOutBubbleBusname = "LOGISIM_INOUT_BUBBLES";
	public final static String FPGAToplevelName = "LogisimToplevelShell";
	public final static String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
	public final static String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
	public final static String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
	public final static String BusToBitAddendum = "_bit_";
	public static final String ClockTreeName = "LOGISIM_CLOCK_TREE_";
	public static final String FPGAInputPinName = "FPGA_INPUT_PIN";
	public static final String FPGAInOutPinName = "FPGA_INOUT_PIN";
	public static final String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

	public boolean GenerateAllHDLDescriptions(Set<String> HandledComponents,
			String WorkingDir, ArrayList<String> Hierarchy,
			FPGAReport Reporter, String HDLType);

	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, FPGAReport Reporter,
			String HDLType);

	public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist,
			AttributeSet attrs, String ComponentName, String HDLType/*
																	 * , boolean
																	 * hasLB
																	 */);

	public ArrayList<String> GetComponentMap(Netlist Nets, Long ComponentId,
			NetlistComponent ComponentInfo, FPGAReport Reporter,
			String CircuitName, String HDLType);

	public String getComponentStringIdentifier();

	public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
			String ComponentName, FPGAReport Reporter, String HDLType);

	public ArrayList<String> GetInlinedCode(Netlist Nets, Long ComponentId,
			NetlistComponent ComponentInfo, FPGAReport Reporter,
			String CircuitName, String HDLType);

	public ArrayList<String> GetInlinedCode(String HDLType,
			ArrayList<String> ComponentIdentifier, FPGAReport Reporter,
			MappableResourcesContainer MapInfo);

	public String GetRelativeDirectory(String HDLType);

	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs);

	public boolean IsOnlyInlined(String HDLType);

	public boolean IsOnlyInlined(String HDLType,
			FPGAIOInformationContainer.IOComponentTypes map);
}
