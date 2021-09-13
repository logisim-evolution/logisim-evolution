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
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl74175HDLGenerator extends AbstractHDLGeneratorFactory {

  public Ttl74175HDLGenerator() {
    super("TTL74175");
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("nCLR", 1);
    map.put("CLK", 1);
    map.put("Tick", 1);
    map.put("D1", 1);
    map.put("D2", 1);
    map.put("D3", 1);
    map.put("D4", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("nQ1", 1);
    map.put("Q1", 1);
    map.put("nQ2", 1);
    map.put("Q2", 1);
    map.put("nQ3", 1);
    map.put("Q3", 1);
    map.put("nQ4", 1);
    map.put("Q4", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("CurState", 4);
    map.put("NextState", 4);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (new LineBuffer())
        .add("""
            NextState <= CurState WHEN tick = '0' ELSE
                         D4&D3&D2&D1;
            
            dffs : PROCESS( CLK , nCLR ) IS
               BEGIN
                  IF (nCLR = '0') THEN CurState <= "0000";
                  ELSIF (rising_edge(CLK)) THEN
                     CurState <= NextState;
                  END IF;
               END PROCESS dffs;
            
            nQ1 <= NOT(CurState(0));
            Q1  <= CurState(0);
            nQ2 <= NOT(CurState(1));
            Q2  <= CurState(1);
            nQ3 <= NOT(CurState(2));
            Q3  <= CurState(2);
            nQ4 <= NOT(CurState(3));
            Q4  <= CurState(3);")
            """)
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    int clockPinIndex = comp.getComponent().getFactory().ClockPinIndex(null)[0];
    if (!comp.isEndConnected(clockPinIndex)) {
      Reporter.Report.AddSevereWarning("Component \"TTL74165\" in circuit \"" + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = HDL.getClockNetName(comp, clockPinIndex, nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      map.put("CLK", "'0'");
      map.put("Tick", "'0'");
    } else if (gatedClock) {
      map.put("Tick", "'1'");
      map.put("CLK", HDL.getNetName(comp, clockPinIndex, true, nets));
    } else {
      if (nets.requiresGlobalClockConnection()) {
        map.put("Tick", "'1'");
      } else {
        map.put(
            "Tick",
            clockNetName
                + "("
                + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                + ")");
      }
      map.put(
          "CLK",
          clockNetName + "(" + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX + ")");
    }
    map.putAll(GetNetMap("nCLR", true, comp, 0, nets));
    map.putAll(GetNetMap("Q1", true, comp, 1, nets));
    map.putAll(GetNetMap("nQ1", true, comp, 2, nets));
    map.putAll(GetNetMap("D1", true, comp, 3, nets));
    map.putAll(GetNetMap("D2", true, comp, 4, nets));
    map.putAll(GetNetMap("nQ2", true, comp, 5, nets));
    map.putAll(GetNetMap("Q2", true, comp, 6, nets));
    map.putAll(GetNetMap("Q3", true, comp, 8, nets));
    map.putAll(GetNetMap("nQ3", true, comp, 9, nets));
    map.putAll(GetNetMap("D3", true, comp, 10, nets));
    map.putAll(GetNetMap("D4", true, comp, 11, nets));
    map.putAll(GetNetMap("nQ4", true, comp, 12, nets));
    map.putAll(GetNetMap("Q4", true, comp, 13, nets));
    return map;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
