/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class HexDigitHDLGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var startId = componentInfo.getLocalBubbleOutputStartId();
    final var bubbleBusName = HDLGeneratorFactory.LocalOutputBubbleBusname;
    final var contents =
        (new LineBuffer()).withHdlPairs()
            .pair("busName", HDL.getBusName(componentInfo, HexDigit.HEX, nets))
            .pair("bubbleBusName", bubbleBusName)
            .pair("regName", LineBuffer.format("s_{{1}}_reg", componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL)))
            .pair("sigName", HDL.isVHDL() ? 
                LineBuffer.format("{{1}}({{2}} DOWNTO {{3}})", bubbleBusName, (startId + 6), startId) : 
                LineBuffer.format("{{1}}[{{2}}:{{3}}]", bubbleBusName, (startId + 6), startId)) 
            .pair("dpName", HDL.getNetName(componentInfo, HexDigit.DP, true, nets));
    contents.add("");
    if (componentInfo.isEndConnected(HexDigit.HEX)) {
      if (HDL.isVHDL()) {
        contents
            .add("""
                 WITH ({{busName}}) SELECT {{sigName}} <= 
                    "0111111" WHEN "0000",
                    "0000110" WHEN "0001",
                    "1011011" WHEN "0010",
                    "1001111" WHEN "0011",
                    "1100110" WHEN "0100",
                    "1101101" WHEN "0101",
                    "1111101" WHEN "0110",
                    "0000111" WHEN "0111",
                    "1111111" WHEN "1000",
                    "1100111" WHEN "1001",
                    "1110111" WHEN "1010",
                    "1111100" WHEN "1011",
                    "0111001" WHEN "1100",
                    "1011110" WHEN "1101",
                    "1111001" WHEN "1110",
                    "1110001" WHEN OTHERS;
                 """);
      } else {
        contents
        .add("""
            reg[6:0] {{regName}};
            always @(*)
            begin
               case ({{busName}})
                  4'b0000 : {{regName}} = 7'b0111111;
                  4'b0001 : {{regName}} = 7'b0000110;
                  4'b0010 : {{regName}} = 7'b1011011;
                  4'b0011 : {{regName}} = 7'b1001111;
                  4'b0100 : {{regName}} = 7'b1100110;
                  4'b0101 : {{regName}} = 7'b1101101;
                  4'b0110 : {{regName}} = 7'b1111101;
                  4'b0111 : {{regName}} = 7'b0000111;
                  4'b1000 : {{regName}} = 7'b1111111;
                  4'b1001 : {{regName}} = 7'b1100111;
                  4'b1010 : {{regName}} = 7'b1110111;
                  4'b1011 : {{regName}} = 7'b1111100;
                  4'b1100 : {{regName}} = 7'b0111001;
                  4'b1101 : {{regName}} = 7'b1011110;
                  4'b1110 : {{regName}} = 7'b1111001;
                  default : {{regName}} = 7'b1110001;
               endcase
            end
            
            assign {{sigName}} = {{regName}};
            """);
      }
    } else {
      contents.add("{{assign}} {{sigName}} {{=}} {{1}};", HDL.GetZeroVector(7, true));
    }
    if (componentInfo.getComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP))
      contents.add("{{assign}} {{bubbleBusName}}{{<}}{{1}}{{>}} {{=}} {{dpName}};", (startId + 7));
    return contents.getWithIndent();
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
