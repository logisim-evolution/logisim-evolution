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
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

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

  @Override
  protected Map<String, String> getParameterMap(AttributeSet attrs) {
    final var parameterMap = new TreeMap<String, String>();
    if (!(attrs instanceof VhdlEntityAttributes vhdlAttrs)) return parameterMap;

    final var content = vhdlAttrs.getContent();
    for (final Attribute<Integer> attribute : content.getGenericAttributes()) {
      final var genericAttribute = (VhdlEntityAttributes.VhdlGenericAttribute) attribute;
      final var generic = genericAttribute.getGeneric();
      final var configuredValue = attrs.getValue(attribute);
      final var value = configuredValue == null ? generic.getDefaultValue() : configuredValue;
      parameterMap.put(
          generic.getName(), generic.getType().equals("time") ? value + " fs" : Integer.toString(value));
    }
    return parameterMap;
  }

  @Override
  protected Map<String, String> getVhdlParameterDeclarations(AttributeSet attrs) {
    final var declarations = new TreeMap<String, String>();
    if (!(attrs instanceof VhdlEntityAttributes vhdlAttrs)) return declarations;

    for (final var generic : vhdlAttrs.getContent().getGenerics()) {
      declarations.put(generic.getName(), generic.getType());
    }
    return declarations;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }
}
