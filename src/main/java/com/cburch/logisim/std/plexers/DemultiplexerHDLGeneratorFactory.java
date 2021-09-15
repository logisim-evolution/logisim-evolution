/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class DemultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public DemultiplexerHDLGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    int nr_of_select_bits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    map.put("DemuxIn", NrOfBits);
    map.put("Enable", 1);
    map.put("Sel", nr_of_select_bits);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = new LineBuffer();
    var space = "  ";
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    var numOutputs = (1 << nrOfSelectBits);
    for (var i = 0; i < numOutputs; i++) {
      if (i == 10) space = " ";
      final var binValue = HDL.getConstantVector(i, nrOfSelectBits);
      if (HDL.isVHDL()) {
        contents.add("DemuxOut_{{1}}{{2}}<= DemuxIn WHEN sel = {{3}} AND", i, space, binValue);
        if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) {
          contents.add("                            Enable = '1' ELSE (OTHERS => '0');");
        } else {
          contents.add("                            Enable = '1' ELSE '0';");
        }
      } else {
        contents.add("assign DemuxOut_{{1}}{{2}} = (Enable&(sel == {{3}} )) ? DemuxIn : 0;", i, space, binValue);
      }
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetList, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    for (var i = 0; i < (1 << nrOfSelectBits); i++) {
      map.put("DemuxOut_" + i, nrOfBits);
    }
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits =
        componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrOfBits > 1) map.put(NR_OF_BITS_STRING, nrOfBits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfSelectBits =
        comp.getComponent().getAttributeSet().getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    var selectInputIndex = (1 << nrOfSelectBits);
    // begin with connecting all outputs of demultiplexer
    for (var i = 0; i < selectInputIndex; i++)
      map.putAll(GetNetMap("DemuxOut_" + i, true, comp, i, nets));
    // now select..
    map.putAll(GetNetMap("Sel", true, comp, selectInputIndex, nets));
    // now connect enable input...
    if (comp.getComponent().getAttributeSet().getValue(PlexersLibrary.ATTR_ENABLE)) {
      map.putAll(GetNetMap("Enable", false, comp, selectInputIndex + 1, nets));
    } else {
      map.put("Enable", HDL.oneBit());
      selectInputIndex--; // decrement pin index because enable doesn't exist...
    }
    // finally input
    map.putAll(GetNetMap("DemuxIn", true, comp, selectInputIndex + 2, nets));
    return map;
  }
}
