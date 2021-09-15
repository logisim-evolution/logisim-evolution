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

public class Ttl7410HDLGenerator extends AbstractHDLGeneratorFactory {

  private boolean Inverted = true;
  private boolean andgate = true;

  public Ttl7410HDLGenerator() {
    super();
  }

  public Ttl7410HDLGenerator(boolean invert, boolean IsAnd) {
    super();
    Inverted = invert;
    andgate = IsAnd;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A0", 1);
    map.put("B0", 1);
    map.put("C0", 1);
    map.put("A1", 1);
    map.put("B1", 1);
    map.put("C1", 1);
    map.put("A2", 1);
    map.put("B2", 1);
    map.put("C2", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y0", 1);
    map.put("Y1", 1);
    map.put("Y2", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var Inv = Inverted ? HDL.notOperator() : "";
    final var Func = andgate ? HDL.andOperator() : HDL.orOperator();
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + Inv + " (A0 " + Func + " B0 " + Func + " C0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + Inv + " (A1 " + Func + " B1 " + Func + " C1);");
    contents.add("   " + HDL.assignPreamble() + "Y2" + HDL.assignOperator() + Inv + " (A2 " + Func + " B2 " + Func + " C2);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var componentInfo = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A0", true, componentInfo, 0, nets));
    map.putAll(GetNetMap("B0", true, componentInfo, 1, nets));
    map.putAll(GetNetMap("C0", true, componentInfo, 11, nets));
    map.putAll(GetNetMap("Y0", true, componentInfo, 10, nets));
    map.putAll(GetNetMap("A1", true, componentInfo, 2, nets));
    map.putAll(GetNetMap("B1", true, componentInfo, 3, nets));
    map.putAll(GetNetMap("C1", true, componentInfo, 4, nets));
    map.putAll(GetNetMap("Y1", true, componentInfo, 5, nets));
    map.putAll(GetNetMap("A2", true, componentInfo, 9, nets));
    map.putAll(GetNetMap("B2", true, componentInfo, 8, nets));
    map.putAll(GetNetMap("C2", true, componentInfo, 7, nets));
    map.putAll(GetNetMap("Y2", true, componentInfo, 6, nets));
    return map;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
