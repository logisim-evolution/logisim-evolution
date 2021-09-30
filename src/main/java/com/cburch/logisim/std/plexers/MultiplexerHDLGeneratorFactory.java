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

public class MultiplexerHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public MultiplexerHDLGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var selectInputIndex = (1 << nrOfSelectBits);
    final var hasEnable = attrs.getValue(PlexersLibrary.ATTR_ENABLE);
    for (var inp = 0; inp < selectInputIndex; inp++)
      myPorts.add(Port.INPUT, String.format("MuxIn_%d", inp), NR_OF_BITS_ID, inp, StdAttr.WIDTH);
    myPorts
        .add(Port.INPUT, "Sel", nrOfSelectBits, selectInputIndex)
        .add(Port.OUTPUT, "MuxOut", NR_OF_BITS_ID, hasEnable ? selectInputIndex + 2 : selectInputIndex + 1, StdAttr.WIDTH);
    if (hasEnable)
      myPorts.add(Port.INPUT, "Enable", 1, selectInputIndex + 1);
    else
      myPorts.add(Port.INPUT, "Enable", 1, Hdl.oneBit());
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    int nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    if (Hdl.isVhdl()) {
      contents.add("make_mux : PROCESS( Enable,");
      for (var i = 0; i < (1 << nrOfSelectBits); i++)
        contents.add("                    MuxIn_{{1}},", i);
      contents.add("""
                              Sel )
          BEGIN
             IF (Enable = '0') THEN
          """)
          .add(attrs.getValue(StdAttr.WIDTH).getWidth() > 1
              ? "{{2u}}MuxOut <= (OTHERS => '0');"
              : "{{2u}}MuxOut <= '0';")
          .add("""
                                     ELSE
                      CASE (Sel) IS
                """);
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++)
        contents.add("         WHEN {{1}} => MuxOut <= MuxIn_{{2}};", Hdl.getConstantVector(i, nrOfSelectBits), i);
      contents.add("         WHEN OTHERS  => MuxOut <= MuxIn_{{1}};", (1 << nrOfSelectBits) - 1)
              .add("""
                         END CASE; 
                      END IF;
                   END PROCESS make_mux;
                   """);
    } else {
      contents.add("""
          reg [{{1}}:0] s_selected_vector;
          assign MuxOut = s_selected_vector;

          always @(*)
          begin
             if (~Enable) s_selected_vector <= 0;
             else case (Sel)
          """, NR_OF_BITS_STRING);
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++) {
        contents
            .add("      {{1}}:", Hdl.getConstantVector(i, nrOfSelectBits))
            .add("         s_selected_vector <= MuxIn_{{1}};", i);
      }
      contents
          .add("     default:")
          .add("        s_selected_vector <= MuxIn_{{1}};", (1 << nrOfSelectBits) - 1)
          .add("   endcase")
          .add("end");
    }
    return contents;
  }
}
