/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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

public class Ttl7454HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A", 1);
    map.put("B", 1);
    map.put("C", 1);
    map.put("D", 1);
    map.put("E", 1);
    map.put("F", 1);
    map.put("G", 1);
    map.put("H", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y" + HDL.assignOperator() + HDL.notOperator()
            + "((A" + HDL.andOperator() + "B)" + HDL.orOperator() + "(C" + HDL.andOperator() + "D)" + HDL.orOperator()
            + "(E" + HDL.andOperator() + "F)" + HDL.orOperator() + "(G" + HDL.andOperator() + "H));");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A", true, ComponentInfo, 0, nets));
    map.putAll(GetNetMap("B", true, ComponentInfo, 8, nets));
    map.putAll(GetNetMap("C", true, ComponentInfo, 1, nets));
    map.putAll(GetNetMap("D", true, ComponentInfo, 2, nets));
    map.putAll(GetNetMap("E", true, ComponentInfo, 3, nets));
    map.putAll(GetNetMap("F", true, ComponentInfo, 4, nets));
    map.putAll(GetNetMap("G", true, ComponentInfo, 6, nets));
    map.putAll(GetNetMap("H", true, ComponentInfo, 7, nets));
    map.putAll(GetNetMap("Y", true, ComponentInfo, 5, nets));
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
