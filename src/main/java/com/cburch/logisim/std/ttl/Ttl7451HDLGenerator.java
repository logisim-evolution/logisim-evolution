/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7451HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A1", 1);
    map.put("B1", 1);
    map.put("C1", 1);
    map.put("D1", 1);
    map.put("A2", 1);
    map.put("B2", 1);
    map.put("C2", 1);
    map.put("D2", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y1", 1);
    map.put("Y2", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + HDL.notOperator()
            + "((A1" + HDL.andOperator() + "B1)" + HDL.orOperator() + "(C1" + HDL.andOperator() + "D1));");
    contents.add("   " + HDL.assignPreamble() + "Y2" + HDL.assignOperator() + HDL.notOperator()
            + "((A2" + HDL.andOperator() + "B2)" + HDL.orOperator() + "(C2" + HDL.andOperator() + "D2));");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A1", true, ComponentInfo, 0, nets));
    map.putAll(GetNetMap("B1", true, ComponentInfo, 9, nets));
    map.putAll(GetNetMap("C1", true, ComponentInfo, 7, nets));
    map.putAll(GetNetMap("D1", true, ComponentInfo, 8, nets));
    map.putAll(GetNetMap("Y1", true, ComponentInfo, 6, nets));
    map.putAll(GetNetMap("A2", true, ComponentInfo, 1, nets));
    map.putAll(GetNetMap("B2", true, ComponentInfo, 2, nets));
    map.putAll(GetNetMap("C2", true, ComponentInfo, 3, nets));
    map.putAll(GetNetMap("D2", true, ComponentInfo, 4, nets));
    map.putAll(GetNetMap("Y2", true, ComponentInfo, 5, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "ttl";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
