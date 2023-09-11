/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.util.LineBuffer;

import java.util.List;
import java.util.Set;

public interface HdlGeneratorFactory {

  String NET_NAME = Hdl.NET_NAME;
  String BUS_NAME = Hdl.BUS_NAME;
  String CLOCK_TREE_NAME = "logisimClockTree";
  String VHDL = "VHDL";
  String VERILOG = "Verilog";
  String LOCAL_INPUT_BUBBLE_BUS_NAME = "logisimInputBubbles";
  String LOCAL_OUTPUT_BUBBLE_BUS_NAME = "logisimOutputBubbles";
  String LOCAL_INOUT_BUBBLE_BUS_NAME = "logisimInOutBubbles";
  String FPGA_TOP_LEVEL_NAME = "logisimTopLevelShell";

  boolean generateAllHDLDescriptions(
      Set<String> handledComponents, String workingDirectory, List<String> hierarchy);

  List<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName);

  List<String> getArchitecture(Netlist theNetlist, AttributeSet attrs, String componentName);

  LineBuffer getComponentInstantiation(
      Netlist theNetlist, AttributeSet attrs, String componentName);

  LineBuffer getComponentMap(Netlist nets, Long componentId, Object componentInfo, String name);

  LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName);

  String getRelativeDirectory();

  boolean isHdlSupportedTarget(AttributeSet attrs);

  boolean isOnlyInlined();
}
