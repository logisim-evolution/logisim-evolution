/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
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
