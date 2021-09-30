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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.List;

public class DecoderHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  public DecoderHDLGeneratorFactory() {
    super();
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var selectInputIndex = (1 << nrOfSelectBits);
    for (var outp = 0; outp < selectInputIndex; outp++) {
      myPorts.add(Port.OUTPUT, String.format("DecoderOut_%d", outp), 1, outp);
    }
    myPorts.add(Port.INPUT, "Sel", nrOfSelectBits, selectInputIndex);
    if (attrs.getValue(PlexersLibrary.ATTR_ENABLE).booleanValue())
        myPorts.add(Port.INPUT, "Enable", 1, selectInputIndex + 1, false);
  }

  @Override
  public List<String> getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var numOutputs = (1 << nrOfSelectBits);
    var space = " ";
    for (var i = 0; i < numOutputs; i++) {
      if (i == 7) space = "";
      contents.pair("bin", Hdl.getConstantVector(i, nrOfSelectBits))
              .pair("space", space)
              .pair("i", i);
      if (Hdl.isVhdl()) {
        contents.add("""
            DecoderOut_{{i}}{{space}}<= '1' WHEN sel = {{bin}} AND
            {{space}}                             Enable = '1' ELSE '0';
            """);
      } else {
        contents.add("assign DecoderOut_{{i}}{{space}} = (Enable&(sel == {{bin}})) ? 1'b1 : 1'b0;");
      }
    }
    return contents.getWithIndent();
  }
}
