/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.hdl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import java.util.ArrayList;
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

    VhdlContentComponent content =
        attrs.getValue(VhdlEntityComponent.CONTENT_ATTR);
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "VHDL";
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
  public String GetSubDir() {
    return "circuit";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
