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

public class MultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public MultiplexerHDLGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist theNetList, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    final var nrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    for (var i = 0; i < (1 << nrOfSelectBits); i++)
      map.put("MuxIn_" + i, nrOfBits);
    map.put("Enable", 1);
    map.put("Sel", nrOfSelectBits);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = new LineBuffer();
    int nrOfSelectBits = attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    if (HDL.isVHDL()) {
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
        contents.add("         WHEN {{1}} => MuxOut <= MuxIn_{{2}};", HDL.getConstantVector(i, nrOfSelectBits), i);
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
            .add("      {{1}}:", HDL.getConstantVector(i, nrOfSelectBits))
            .add("         s_selected_vector <= MuxIn_{{1}};", i);
      }
      contents
          .add("     default:")
          .add("        s_selected_vector <= MuxIn_{{1}};", (1 << nrOfSelectBits) - 1)
          .add("   endcase")
          .add("end");
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    map.put("MuxOut", NrOfBits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfSelectBits = comp.getComponent().getAttributeSet().getValue(PlexersLibrary.ATTR_SELECT).getWidth();
    var selectInputIndex = (1 << nrOfSelectBits);
    // begin with connecting all inputs of multiplexer
    for (var i = 0; i < selectInputIndex; i++)
      map.putAll(GetNetMap("MuxIn_" + i, true, comp, i, nets));
    // now select..
    map.putAll(GetNetMap("Sel", true, comp, selectInputIndex, nets));
    // now connect enable input...
    if (comp.getComponent()
        .getAttributeSet()
        .getValue(PlexersLibrary.ATTR_ENABLE)) {
      map.putAll(
          GetNetMap(
              "Enable", false, comp, selectInputIndex + 1, nets));
    } else {
      map.put("Enable", HDL.oneBit());
      selectInputIndex--; // decrement pin index because enable doesn't exist...
    }
    // finally output
    map.putAll(GetNetMap("MuxOut", true, comp, selectInputIndex + 2, nets));
    return map;
  }
}
