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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractGateHDLGenerator extends AbstractHDLGeneratorFactory {

  public boolean IsInverter() {
    return false;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      map.put("gate_" + i + "_A", 1);
      if (!IsInverter()) map.put("gate_" + i + "_B", 1);
    }
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      map.put("gate_" + i + "_O", 1);
    }
    return map;
  }

  public ArrayList<String> GetLogicFunction(int index) {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    final var nrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < nrOfGates; i++) {
      contents.addRemarkBlock("Here gate %d is described", i).add(GetLogicFunction(i));
    }
    return contents.get();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < nrOfGates; i++) {
      if (IsInverter()) {
        final var inindex = (i < 3) ? i * 2 : i * 2 + 1;
        final var outindex = (i < 3) ? i * 2 + 1 : i * 2;
        map.putAll(GetNetMap("gate_" + i + "_A", true, comp, inindex, nets));
        map.putAll(GetNetMap("gate_" + i + "_O", true, comp, outindex, nets));
      } else {
        final var inindex1 = (i < 2) ? i * 3 : i * 3 + 1;
        final var inindex2 = inindex1 + 1;
        final var outindex = (i < 2) ? i * 3 + 2 : i * 3;
        map.putAll(GetNetMap("gate_" + i + "_A", true, comp, inindex1, nets));
        map.putAll(GetNetMap("gate_" + i + "_B", true, comp, inindex2, nets));
        map.putAll(GetNetMap("gate_" + i + "_O", true, comp, outindex, nets));
      }
    }
    return map;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
