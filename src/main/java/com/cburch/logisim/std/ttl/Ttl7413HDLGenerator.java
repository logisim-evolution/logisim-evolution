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

public class Ttl7413HDLGenerator extends AbstractHDLGeneratorFactory {

  private boolean Inverted = true;

  public Ttl7413HDLGenerator() {
    super();
  }

  public Ttl7413HDLGenerator(boolean inv) {
    super();
    Inverted = inv;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A0", 1);
    map.put("B0", 1);
    map.put("C0", 1);
    map.put("D0", 1);
    map.put("A1", 1);
    map.put("B1", 1);
    map.put("C1", 1);
    map.put("D1", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y0", 1);
    map.put("Y1", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var Inv = Inverted ? HDL.notOperator() : "";
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + Inv
            + " (A0" + HDL.andOperator() + "B0" + HDL.andOperator() + "C0" + HDL.andOperator() + "D0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + Inv
            + " (A1" + HDL.andOperator() + "B1" + HDL.andOperator() + "C1" + HDL.andOperator() + "D1);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A0", true, comp, 0, nets));
    map.putAll(GetNetMap("B0", true, comp, 1, nets));
    map.putAll(GetNetMap("C0", true, comp, 2, nets));
    map.putAll(GetNetMap("D0", true, comp, 3, nets));
    map.putAll(GetNetMap("Y0", true, comp, 4, nets));
    map.putAll(GetNetMap("A1", true, comp, 9, nets));
    map.putAll(GetNetMap("B1", true, comp, 8, nets));
    map.putAll(GetNetMap("C1", true, comp, 7, nets));
    map.putAll(GetNetMap("D1", true, comp, 6, nets));
    map.putAll(GetNetMap("Y1", true, comp, 5, nets));
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
