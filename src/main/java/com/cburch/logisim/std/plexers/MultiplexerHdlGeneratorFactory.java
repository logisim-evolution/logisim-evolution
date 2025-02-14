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

public class MultiplexerHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public MultiplexerHdlGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var selectInputIndex = (1 << nrOfSelectBits);
    final var hasenable = attrs.getValue(PlexersLibrary.ATTR_ENABLE);
    for (var inp = 0; inp < selectInputIndex; inp++)
      myPorts.add(Port.INPUT, String.format("muxIn_%d", inp), NR_OF_BITS_ID, inp, StdAttr.WIDTH);
    myPorts
        .add(Port.INPUT, "sel", nrOfSelectBits, selectInputIndex)
        .add(Port.OUTPUT, "muxOut", NR_OF_BITS_ID, hasenable ? selectInputIndex + 2 : selectInputIndex + 1, StdAttr.WIDTH);
    if (hasenable)
      myPorts.add(Port.INPUT, "enable", 1, selectInputIndex + 1);
    else
      myPorts.add(Port.INPUT, "enable", 1, Hdl.oneBit());
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("makeMux : {{process}}(enable,");
      for (var i = 0; i < (1 << nrOfSelectBits); i++)
        contents.add("                  muxIn_{{1}},", i);
      contents.add("""
                            sel) {{is}}
          {{begin}}
             {{if}} (enable = '0') {{then}}
          """)
          .add(attrs.getValue(StdAttr.WIDTH).getWidth() > 1
              ? "{{2u}}muxOut <= ({{others}} => '0');"
              : "{{2u}}muxOut <= '0';")
          .add("""
                                     {{else}}
                      {{case}} (sel) IS
                """);
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++)
        contents.add("         {{when}} {{1}} => muxOut <= muxIn_{{2}};", Hdl.getConstantVector(i, nrOfSelectBits), i);
      contents.add("         {{when}} {{others}}  => muxOut <= muxIn_{{1}};", (1 << nrOfSelectBits) - 1)
              .add("""
                         {{end}} {{case}};
                      {{end}} {{if}};
                   {{end}} {{process}} makeMux;
                   """);
    } else {
      if (nrOfBits == 1)
        contents.add("reg s_selected_vector;");
      else
        contents.add("reg [{{1}}:0] s_selected_vector;", NR_OF_BITS_STRING);
      contents.add("""
          assign muxOut = s_selected_vector;

          always @(*)
          begin
             if (~enable) s_selected_vector <= 0;
             else case (sel)
          """);
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++) {
        contents
            .add("      {{1}}:", Hdl.getConstantVector(i, nrOfSelectBits))
            .add("         s_selected_vector <= muxIn_{{1}};", i);
      }
      contents
          .add("     default:")
          .add("        s_selected_vector <= muxIn_{{1}};", (1 << nrOfSelectBits) - 1)
          .add("   endcase")
          .add("end");
    }
    return contents.empty();
  }
}
