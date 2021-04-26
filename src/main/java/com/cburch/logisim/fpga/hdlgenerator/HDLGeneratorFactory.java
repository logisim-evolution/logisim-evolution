/*
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
import java.util.ArrayList;
import java.util.Set;

public interface HDLGeneratorFactory {

  String VHDL = "VHDL";
  String VERILOG = "Verilog";
  int PallignmentSize = 26;
  int SallignmentSize = 35;
  String NetName = "s_LOGISIM_NET_";
  String BusName = "s_LOGISIM_BUS_";
  String LocalInputBubbleBusname = "LOGISIM_INPUT_BUBBLES";
  String LocalOutputBubbleBusname = "LOGISIM_OUTPUT_BUBBLES";
  String LocalInOutBubbleBusname = "LOGISIM_INOUT_BUBBLES";
  String FPGAToplevelName = "LogisimToplevelShell";
  String InputBubblePortName = "LOGISIM_INPUT_BUBBLE_";
  String OutputBubblePortName = "LOGISIM_OUTPUT_BUBBLE_";
  String InOutBubblePortName = "LOGISIM_INOUTT_BUBBLE_";
  String BusToBitAddendum = "_bit_";
  String ClockTreeName = "LOGISIM_CLOCK_TREE_";
  String FPGAInputPinName = "FPGA_INPUT_PIN";
  String FPGAInOutPinName = "FPGA_INOUT_PIN";
  String FPGAOutputPinName = "FPGA_OUTPUT_PIN";

  boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents,
      String WorkingDir,
      ArrayList<String> Hierarchy);

  ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName);

  ArrayList<String> GetComponentInstantiation(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName);

  ArrayList<String> GetComponentMap(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      MappableResourcesContainer MapInfo,
      String Name);

  String getComponentStringIdentifier();

  ArrayList<String> GetEntity(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName);

  ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName);

  String GetRelativeDirectory();

  boolean HDLTargetSupported(AttributeSet attrs);

  boolean IsOnlyInlined();

  boolean IsOnlyInlined(IOComponentTypes map);
}
