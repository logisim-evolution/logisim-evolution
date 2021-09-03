/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String componentName) {
    ArrayList<String> contents = new ArrayList<>();
    contents.addAll(FileWriter.getGenerateRemark(componentName, TheNetlist.projName()));

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    AttributeSet attrs = ComponentInfo.getComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    for (Attribute<Integer> a : content.getGenericAttributes()) {
      VhdlEntityAttributes.VhdlGenericAttribute va = (VhdlEntityAttributes.VhdlGenericAttribute) a;
      VhdlContent.Generic g = va.getGeneric();
      Integer v = attrs.getValue(a);
      ParameterMap.put(g.getName(), Objects.requireNonNullElseGet(v, g::getDefaultValue));
    }
    return ParameterMap;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    int i = -1;
    for (VhdlContent.Generic g : content.getGenerics()) {
      Parameters.put(i--, g.getName());
    }
    return Parameters;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "VHDL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> inputs = new TreeMap<>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType().equals(Port.INPUT)) inputs.put(p.getName(), p.getWidth().getWidth());
    }

    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType().equals(Port.OUTPUT)) outputs.put(p.getName(), p.getWidth().getWidth());
    }

    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;

    AttributeSet attrs = ComponentInfo.getComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();

    int i = 0;
    for (VhdlParser.PortDescription p : content.getPorts()) {
      PortMap.putAll(GetNetMap(p.getName(), true, ComponentInfo, i++, Nets));
    }
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "circuit";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
