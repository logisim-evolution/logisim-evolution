/**
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
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class VhdlHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> contents = new ArrayList<String>();
    contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType, TheNetlist.projName()));

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    ;
    contents.add(content.getLibraries());
    contents.add(content.getArchitecture());

    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
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
    ;
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
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
    SortedMap<String, Integer> inputs = new TreeMap<String, Integer>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType() == Port.INPUT) inputs.put(p.getName(), p.getWidth().getWidth());
    }

    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<String, Integer>();

    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();
    for (VhdlParser.PortDescription p : content.getPorts()) {
      if (p.getType() == Port.OUTPUT) outputs.put(p.getName(), p.getWidth().getWidth());
    }

    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;

    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    VhdlContent content = ((VhdlEntityAttributes) attrs).getContent();

    int i = 0;
    for (VhdlParser.PortDescription p : content.getPorts()) {
      PortMap.putAll(GetNetMap(p.getName(), true, ComponentInfo, i++, Reporter, HDLType, Nets));
    }
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "circuit";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return HDLType.equals(HDLGeneratorFactory.VHDL);
  }
}
