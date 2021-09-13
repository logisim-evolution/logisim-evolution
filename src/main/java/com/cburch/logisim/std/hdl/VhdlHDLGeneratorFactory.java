/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static final String HDL_DIRECTORY = "circuit";

  public VhdlHDLGeneratorFactory() {
    super(HDL_DIRECTORY);
  }
  
  @Override
  public ArrayList<String> getArchitecture(
      Netlist theNetlist,
      AttributeSet attrs,
      String componentName) {
    ArrayList<String> contents = new ArrayList<>();
    contents.addAll(FileWriter.getGenerateRemark(componentName, theNetlist.projName()));

    VhdlContentComponent content =
        attrs.getValue(VhdlEntityComponent.CONTENT_ATTR);
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();

    final var rawInputs = attrs.getValue(VhdlEntityComponent.CONTENT_ATTR).getInputs();
    for (final var rawInput : rawInputs)
      inputs.put(rawInput.getToolTip(), rawInput.getFixedBitWidth().getWidth());

    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();

    final var rawOutputs = attrs.getValue(VhdlEntityComponent.CONTENT_ATTR).getOutputs();
    for (final var rawOutput : rawOutputs)
      outputs.put(rawOutput.getToolTip(), rawOutput.getFixedBitWidth().getWidth());

    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) MapInfo;

    final var attrs = componentInfo.getComponent().getAttributeSet();
    final var content = attrs.getValue(VhdlEntityComponent.CONTENT_ATTR);

    final var inputs = content.getInputs();
    final var outputs = content.getOutputs();

    for (var i = 0; i < inputs.length; i++)
      portMap.putAll(GetNetMap(inputs[i].getToolTip(), true, componentInfo, i, Nets));
    for (var i = 0; i < outputs.length; i++)
      portMap.putAll(
          GetNetMap(
              outputs[i].getToolTip(),
              true,
              componentInfo,
              i + inputs.length,
              Nets));

    return portMap;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
