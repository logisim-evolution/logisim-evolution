/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;

import java.util.ArrayList;
import java.util.Set;

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
  public static final String FPGAToplevelName = "LogisimToplevelShell";
  public static final String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
  public static final String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
  public static final String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
  public static final String BusToBitAddendum = "_bit_";
  public static final String ClockTreeName = "LOGISIM_CLOCK_TREE_";
  public static final String FPGAInputPinName = "FPGA_INPUT_PIN";
  public static final String FPGAInOutPinName = "FPGA_INOUT_PIN";
  public static final String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

  public boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents,
      String WorkingDir,
      ArrayList<String> Hierarchy,
      FPGAReport Reporter,
      String HDLType);

  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType);

  public ArrayList<String> GetComponentInstantiation(
      Netlist TheNetlist, 
      AttributeSet attrs, 
      String ComponentName, 
      String HDLType );

  public ArrayList<String> GetComponentMap(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      MappableResourcesContainer MapInfo,
      FPGAReport Reporter,
      String Name,
      String HDLType);

  public String getComponentStringIdentifier();

  public ArrayList<String> GetEntity(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType);

  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      FPGAReport Reporter,
      String CircuitName,
      String HDLType);

  public String GetRelativeDirectory(String HDLType);

  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs);

  public boolean IsOnlyInlined(String HDLType);

  public boolean IsOnlyInlined(String HDLType, IOComponentTypes map);
}
