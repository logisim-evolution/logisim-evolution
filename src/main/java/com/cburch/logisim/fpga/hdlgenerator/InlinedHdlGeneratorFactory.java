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

public class InlinedHdlGeneratorFactory implements HDLGeneratorFactory {

  @Override
  public boolean GenerateAllHDLDescriptions(Set<String> HandledComponents, String WorkingDir,
      ArrayList<String> Hierarchy) {
    return false;
  }

  @Override
  public ArrayList<String> GetArchitecture(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    return null;
  }

  @Override
  public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    return null;
  }

  @Override
  public ArrayList<String> GetComponentMap(Netlist Nets, Long ComponentId, NetlistComponent ComponentInfo,
      MappableResourcesContainer MapInfo, String Name) {
    return null;
  }

  @Override
  public String getComponentStringIdentifier() {
    return null;
  }

  @Override
  public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    return null;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist Nets, Long ComponentId, NetlistComponent ComponentInfo,
      String CircuitName) {
    return null;
  }

  @Override
  public String GetRelativeDirectory() {
    return null;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }

}
