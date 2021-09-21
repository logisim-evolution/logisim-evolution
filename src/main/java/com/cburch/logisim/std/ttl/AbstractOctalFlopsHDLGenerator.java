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
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractOctalFlopsHDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("nCLR", 1);
    map.put("nCLKen", 1);
    map.put("CLK", 1);
    map.put("tick", 1);
    map.put("D0", 1);
    map.put("D1", 1);
    map.put("D2", 1);
    map.put("D3", 1);
    map.put("D4", 1);
    map.put("D5", 1);
    map.put("D6", 1);
    map.put("D7", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q0", 1);
    map.put("Q1", 1);
    map.put("Q2", 1);
    map.put("Q3", 1);
    map.put("Q4", 1);
    map.put("Q5", 1);
    map.put("Q6", 1);
    map.put("Q7", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("state", 8);
    map.put("enable", 1);
    map.put("nexts", 8);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getBuffer()
        .add("""
            enable <= tick and NOT(nCLKen);
            nexts  <= D7&D6&D5&D4&D3&D2&D1&D0 WHEN enable = '1' ELSE state;
            Q0     <= state(0);
            Q1     <= state(1);
            Q2     <= state(2);
            Q3     <= state(3);
            Q4     <= state(4);
            Q5     <= state(5);
            Q6     <= state(6);
            Q7     <= state(7);

            dffs : PROCESS( CLK , nCLR ) IS
               BEGIN
                  IF (nCLR = '1') THEN state <= (OTHERS => '0');
                  ELSIF (rising_edge(CLK)) THEN state <= nexts;
                  END IF;
               END PROCESS dffs;
            """)
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var netlistComp = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    final var comp = (InstanceFactory) netlistComp.getComponent();
    final var clockPinIndex = netlistComp.getComponent().getFactory().ClockPinIndex(null)[0];
    if (!netlistComp.isEndConnected(clockPinIndex)) {
      Reporter.Report.AddSevereWarning(
          "Component \""
              + comp.getDisplayName()
              + "\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    String ClockNetName = HDL.getClockNetName(netlistComp, clockPinIndex, nets);
    if (ClockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      map.put("CLK", "'0'");
      map.put("tick", "'0'");
    } else if (gatedClock) {
      map.put("tick", "'1'");
      map.put("CLK", HDL.getNetName(netlistComp, clockPinIndex, true, nets));
    } else {
      if (nets.requiresGlobalClockConnection()) {
        map.put("tick", "'1'");
      } else {
        map.put(
            "tick",
            ClockNetName
                + "("
                + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                + ")");
      }
      map.put(
          "CLK",
          ClockNetName + "(" + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX + ")");
    }
    map.putAll(GetNetMap("D0", true, netlistComp, 2, nets));
    map.putAll(GetNetMap("D1", true, netlistComp, 3, nets));
    map.putAll(GetNetMap("D2", true, netlistComp, 6, nets));
    map.putAll(GetNetMap("D3", true, netlistComp, 7, nets));
    map.putAll(GetNetMap("D4", true, netlistComp, 11, nets));
    map.putAll(GetNetMap("D5", true, netlistComp, 12, nets));
    map.putAll(GetNetMap("D6", true, netlistComp, 15, nets));
    map.putAll(GetNetMap("D7", true, netlistComp, 16, nets));
    map.putAll(GetNetMap("Q0", true, netlistComp, 1, nets));
    map.putAll(GetNetMap("Q1", true, netlistComp, 4, nets));
    map.putAll(GetNetMap("Q2", true, netlistComp, 5, nets));
    map.putAll(GetNetMap("Q3", true, netlistComp, 8, nets));
    map.putAll(GetNetMap("Q4", true, netlistComp, 10, nets));
    map.putAll(GetNetMap("Q5", true, netlistComp, 13, nets));
    map.putAll(GetNetMap("Q6", true, netlistComp, 14, nets));
    map.putAll(GetNetMap("Q7", true, netlistComp, 17, nets));
    return map;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && (HDL.isVHDL()));
  }
}
