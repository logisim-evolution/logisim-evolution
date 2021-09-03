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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class DecoderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "BINDECODER";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist theNetList, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Enable", 1);
    map.put("Sel", attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth());
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = new LineBuffer();
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var numOutputs = (1 << nrOfSelectBits);
    var space = " ";
    for (var i = 0; i < numOutputs; i++) {
      if (i == 7) space = "";
      contents.pair("bin", IntToBin(i, nrOfSelectBits))
              .pair("space", space)
              .pair("i", i);
      if (HDL.isVHDL()) {
        contents.addLines(
            "DecoderOut_{{i}}{{space}}<= '1' WHEN sel = {{bin}} AND",
            "DecoderOut_{{i}}{{space}}<= '1' WHEN sel = {{bin}} AND",
            "{{space}}                             Enable = '1' ELSE '0';");
      } else {
        contents.add("assign DecoderOut_{{i}}{{space}} = (Enable&(sel == {{bin}})) ? 1'b1 : 1'b0;");
      }
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetList, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    for (var i = 0; i < (1 << attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth()); i++) {
      map.put("DecoderOut_" + i, 1);
    }
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfSelectBits =
        comp.getComponent().getAttributeSet().getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var selectInputIndex = (1 << nrOfSelectBits);
    // first outputs
    for (var i = 0; i < selectInputIndex; i++)
      map.putAll(GetNetMap("DecoderOut_" + i, true, comp, i, nets));
    // select..
    map.putAll(GetNetMap("Sel", true, comp, selectInputIndex, nets));

    // now connect enable input...
    if (comp.getComponent().getAttributeSet().getValue(PlexersLibrary.ATTR_ENABLE).booleanValue()) {
      map.putAll(GetNetMap("Enable", false, comp, selectInputIndex + 1, nets));
    } else {
      map.put("Enable", HDL.oneBit());
    }
    return map;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
