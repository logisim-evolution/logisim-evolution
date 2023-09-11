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

public class InlinedHdlGeneratorFactory implements HdlGeneratorFactory {

  @Override
  public boolean generateAllHDLDescriptions(
      Set<String> handledComponents, String workingDirectory, List<String> hierarchy) {
    throw new IllegalAccessError("BUG: generateAllHDLDescriptions not supported");
  }

  @Override
  public List<String> getArchitecture(
      Netlist theNetlist, AttributeSet attrs, String componentName) {
    throw new IllegalAccessError("BUG: getArchitecture not supported");
  }

  @Override
  public LineBuffer getComponentInstantiation(
      Netlist theNetlist, AttributeSet attrs, String componentName) {
    throw new IllegalAccessError("BUG: getComponentInstantiation not supported");
  }

  @Override
  public LineBuffer getComponentMap(
      Netlist nets, Long componentId, Object componentInfo, String name) {
    throw new IllegalAccessError("BUG: getComponentMap not supported");
  }

  @Override
  public List<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName) {
    throw new IllegalAccessError("BUG: getEntity not supported");
  }

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    return LineBuffer.getHdlBuffer();
  }

  @Override
  public String getRelativeDirectory() {
    throw new IllegalAccessError("BUG: getRelativeDirectory not supported");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean isOnlyInlined() {
    return true;
  }
}
