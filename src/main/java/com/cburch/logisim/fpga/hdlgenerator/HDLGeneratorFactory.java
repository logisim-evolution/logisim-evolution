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
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;

import java.util.ArrayList;
import java.util.Set;

public interface HDLGeneratorFactory {
  
  public static final String NET_NAME = HDL.NET_NAME;
  public static final String BUS_NAME = HDL.BUS_NAME;
  public static final String CLOCK_TREE_NAME = "LOGISIM_CLOCK_TREE_";
  public static final String VHDL = "VHDL";
  public static final String VERILOG = "Verilog";
  public static final String LOCAL_INPUT_BUBBLE_BUS_NAME = "LOGISIM_INPUT_BUBBLES";
  public static final String LOCAL_OUTPUT_BUBBLE_BUS_NAME = "LOGISIM_OUTPUT_BUBBLES";
  public static final String LOCAL_INOUT_BUBBLE_BUS_NAME = "LOGISIM_INOUT_BUBBLES";
  public static final String FPGA_TOP_LEVEL_NAME = "LogisimToplevelShell";
  public static final int PORT_ALLIGNMENT_SIZE = 26;
  public static final int SIGNAL_ALLIGNMENT_SIZE = 35;

  boolean generateAllHDLDescriptions(
      Set<String> handledComponents,
      String workingDirectory,
      ArrayList<String> hierarchy);

  ArrayList<String> getArchitecture(
      Netlist theNetlist,
      AttributeSet attrs,
      String componentName);

  ArrayList<String> getComponentInstantiation(
      Netlist theNetlist,
      AttributeSet attrs,
      String componentName);

  ArrayList<String> getComponentMap(
      Netlist nets,
      Long componentId,
      NetlistComponent componentInfo,
      MappableResourcesContainer mapInfo,
      String name);

  String getComponentIdentifier();

  ArrayList<String> getEntity(
      Netlist theNetlist,
      AttributeSet attrs,
      String componentName);

  ArrayList<String> getInlinedCode(
      Netlist nets,
      Long componentId,
      NetlistComponent componentInfo,
      String circuitName);

  String getRelativeDirectory();

  boolean isHDLSupportedTarget(AttributeSet attrs);

  boolean isOnlyInlined();
}
