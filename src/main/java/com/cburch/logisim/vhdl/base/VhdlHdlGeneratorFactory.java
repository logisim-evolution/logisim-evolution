/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import java.util.ArrayList;

public class VhdlHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final String HDL_DIRECTORY = "circuit";

  public VhdlHdlGeneratorFactory() {
    super(HDL_DIRECTORY);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    var i = 0;
    for (final var port : content.getPorts()) {
      myPorts.add(port.getType(), port.getName(), port.getWidth().getWidth(), i++);
    }
  }

  @Override
  public ArrayList<String> getArchitecture(
      Netlist theNetlist, AttributeSet attrs, String componentName) {
    ArrayList<String> contents =
        new ArrayList<>(FileWriter.getGenerateRemark(componentName, theNetlist.projName()));

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  /* FIXME: implement the generics in the VHDL class (keeping this code for reference)
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
  */

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }
}
