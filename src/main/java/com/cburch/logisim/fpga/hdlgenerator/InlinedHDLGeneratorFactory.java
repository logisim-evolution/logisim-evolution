/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.Set;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;

public class InlinedHDLGeneratorFactory implements HDLGeneratorFactory {

  @Override
  public boolean generateAllHDLDescriptions(Set<String> handledComponents, String workingDirectory,
      ArrayList<String> hierarchy) {
    return false;
  }

  @Override
  public ArrayList<String> getArchitecture(Netlist theNetlist, AttributeSet attrs, String componentName) {
    return null;
  }

  @Override
  public ArrayList<String> getComponentInstantiation(Netlist theNetlist, AttributeSet attrs, String componentName) {
    return null;
  }

  @Override
  public ArrayList<String> getComponentMap(Netlist nets, Long componentId, NetlistComponent componentInfo,
      MappableResourcesContainer mapInfo, String name) {
    return null;
  }

  @Override
  public String getComponentStringIdentifier() {
    return null;
  }

  @Override
  public ArrayList<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName) {
    return null;
  }

  @Override
  public ArrayList<String> getInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo,
      String circuitName) {
    return new ArrayList<String>();
  }

  @Override
  public String getRelativeDirectory() {
    return null;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean isOnlyInlined() {
    return true;
  }

}
