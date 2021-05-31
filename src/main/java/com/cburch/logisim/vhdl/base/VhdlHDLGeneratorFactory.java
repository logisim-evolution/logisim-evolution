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
import java.util.SortedMap;
import java.util.TreeMap;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName) {
    ArrayList<String> contents = new ArrayList<>();
    contents.addAll(FileWriter.getGenerateRemark(ComponentName, TheNetlist.projName()));

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    for (Attribute<Integer> a : content.getGenericAttributes()) {
      VhdlEntityAttributes.VhdlGenericAttribute va = (VhdlEntityAttributes.VhdlGenericAttribute) a;
      VhdlContent.Generic g = va.getGeneric();
      Integer v = attrs.getValue(a);
      if (v != null) {
        ParameterMap.put(g.getName(), v);
      } else {
        ParameterMap.put(g.getName(), g.getDefaultValue());
      }
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

    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
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
