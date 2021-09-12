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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7485HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7485";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A0", 1);
    map.put("A1", 1);
    map.put("A2", 1);
    map.put("A3", 1);
    map.put("B0", 1);
    map.put("B1", 1);
    map.put("B2", 1);
    map.put("B3", 1);
    map.put("AltBin", 1);
    map.put("AeqBin", 1);
    map.put("AgtBin", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("AltBout", 1);
    map.put("AeqBout", 1);
    map.put("AgtBout", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("oppA", 4);
    map.put("oppB", 4);
    map.put("gt", 1);
    map.put("eq", 1);
    map.put("lt", 1);
    map.put("CompIn", 3);
    map.put("CompOut", 3);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    return contents
        .add("""
            oppA   <= A3&A2&A1&A0;
            oppB   <= B3&B2&B1&B0;
            gt     <= '1' WHEN unsigned(oppA) > unsigned(oppB) ELSE '0';
            eq     <= '1' WHEN unsigned(oppA) = unsigned(oppB) ELSE '0';
            lt     <= '1' WHEN unsigned(oppA) < unsigned(oppB) ELSE '0';
            
            CompIn <= AgtBin&AltBin&AeqBin;
            WITH (CompIn) SELECT CompOut <= 
               "100" WHEN "100",
               "010" WHEN "010",
               "000" WHEN "110",
               "110" WHEN "000",
               "001" WHEN OTHERS;
            
            AgtBout <= '1' WHEN gt = '1' ELSE '0' WHEN lt = '1' ELSE CompOut(2);
            AltBout <= '0' WHEN gt = '1' ELSE '1' WHEN lt = '1' ELSE CompOut(1);
            AeqBout <= '0' WHEN (gt = '1') OR (lt = '1') ELSE CompOut(0);
            """)
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    map.putAll(GetNetMap("A0", true, ComponentInfo, 8, Nets));
    map.putAll(GetNetMap("A1", true, ComponentInfo, 10, Nets));
    map.putAll(GetNetMap("A2", true, ComponentInfo, 11, Nets));
    map.putAll(GetNetMap("A3", true, ComponentInfo, 13, Nets));
    map.putAll(GetNetMap("B0", true, ComponentInfo, 7, Nets));
    map.putAll(GetNetMap("B1", true, ComponentInfo, 9, Nets));
    map.putAll(GetNetMap("B2", true, ComponentInfo, 12, Nets));
    map.putAll(GetNetMap("B3", true, ComponentInfo, 0, Nets));
    map.putAll(GetNetMap("AltBin", true, ComponentInfo, 1, Nets));
    map.putAll(GetNetMap("AeqBin", true, ComponentInfo, 2, Nets));
    map.putAll(GetNetMap("AgtBin", true, ComponentInfo, 3, Nets));
    map.putAll(GetNetMap("AltBout", true, ComponentInfo, 6, Nets));
    map.putAll(GetNetMap("AeqBout", true, ComponentInfo, 5, Nets));
    map.putAll(GetNetMap("AgtBout", true, ComponentInfo, 4, Nets));
    return map;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
