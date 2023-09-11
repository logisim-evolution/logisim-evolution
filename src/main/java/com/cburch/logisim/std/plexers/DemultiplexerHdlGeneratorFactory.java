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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class DemultiplexerHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public DemultiplexerHdlGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth() == 1 ? 1 : NR_OF_BITS_ID;
    final var selectInputIndex = (1 << nrOfSelectBits);
    final var hasenable = attrs.getValue(PlexersLibrary.ATTR_ENABLE);
    for (var outp = 0; outp < selectInputIndex; outp++) {
      myPorts.add(Port.OUTPUT, String.format("demuxOut_%d", outp), nrOfBits, outp, StdAttr.WIDTH);
    }
    myPorts
        .add(Port.INPUT, "sel", nrOfSelectBits, selectInputIndex)
        .add(
            Port.INPUT,
            "demuxIn",
            nrOfBits,
            hasenable ? selectInputIndex + 2 : selectInputIndex + 1);
    if (hasenable) myPorts.add(Port.INPUT, "enable", 1, selectInputIndex + 1, false);
    else myPorts.add(Port.INPUT, "enable", 1, Hdl.oneBit());
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    var space = "  ";
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    var numOutputs = (1 << nrOfSelectBits);
    for (var i = 0; i < numOutputs; i++) {
      if (i == 10) space = " ";
      final var binValue = Hdl.getConstantVector(i, nrOfSelectBits);
      if (Hdl.isVhdl()) {
        contents
            .empty()
            .addVhdlKeywords()
            .add("demuxOut_{{1}}{{2}}<= demuxIn {{when}} sel = {{3}} {{and}}", i, space, binValue);
        if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) {
          contents.add("                            enable = '1' {{else}} ({{others}} => '0');");
        } else {
          contents.add("                            enable = '1' {{else}} '0';");
        }
      } else {
        contents.add(
            "assign demuxOut_{{1}}{{2}} = (enable&(sel == {{3}} )) ? demuxIn : 0;",
            i, space, binValue);
      }
    }
    return contents;
  }
}
