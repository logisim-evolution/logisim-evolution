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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class DemultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public DemultiplexerHDLGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsduringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var selectInputIndex = (1 << nrOfSelectBits);
    final var hasEnable = attrs.getValue(PlexersLibrary.ATTR_ENABLE);
    for (var outp = 0; outp < selectInputIndex; outp++) {
      myPorts.add(Port.OUTPUT, String.format("DemuxOut_%d", outp), NR_OF_BITS_ID, outp, StdAttr.WIDTH);
    }
    myPorts
        .add(Port.INPUT, "sel", nrOfSelectBits, selectInputIndex)
        .add(Port.INPUT, "DemuxIn", NR_OF_BITS_ID, hasEnable ? selectInputIndex + 2 : selectInputIndex + 1);
    if (hasEnable)
      myPorts.add(Port.INPUT, "Enable", 1, selectInputIndex + 1, false);
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
}
