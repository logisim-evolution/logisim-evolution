/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class BcdToSevenSegmentDisplayHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public BcdToSevenSegmentDisplayHdlGeneratorFactory() {
    myWires
        .addWire("s_outputValue", 7);
    myPorts
        .add(Port.INPUT, "bcdIn", 4, BcdToSevenSegmentDisplay.BCD_IN)
        .add(Port.OUTPUT, "segmentA", 1, BcdToSevenSegmentDisplay.SEGMENT_A)
        .add(Port.OUTPUT, "segmentB", 1, BcdToSevenSegmentDisplay.SEGMENT_B)
        .add(Port.OUTPUT, "segmentC", 1, BcdToSevenSegmentDisplay.SEGMENT_C)
        .add(Port.OUTPUT, "segmentD", 1, BcdToSevenSegmentDisplay.SEGMENT_D)
        .add(Port.OUTPUT, "segmentE", 1, BcdToSevenSegmentDisplay.SEGMENT_E)
        .add(Port.OUTPUT, "segmentF", 1, BcdToSevenSegmentDisplay.SEGMENT_F)
        .add(Port.OUTPUT, "segmentG", 1, BcdToSevenSegmentDisplay.SEGMENT_G);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (Hdl.isVhdl()) ? LineBuffer.getBuffer()
        .addVhdlKeywords()
        .empty()
        .add("""
            segmentA <= s_outputValue(0);
            segmentB <= s_outputValue(1);
            segmentC <= s_outputValue(2);
            segmentD <= s_outputValue(3);
            segmentE <= s_outputValue(4);
            segmentF <= s_outputValue(5);
            segmentG <= s_outputValue(6);

            makeSegs : {{process}} ( bcdIn ) {{is}}
            {{begin}}
               {{case}} (bcdIn) {{is}}
                  {{when}} "0000" => s_outputValue <= "0111111";
                  {{when}} "0001" => s_outputValue <= "0000110";
                  {{when}} "0010" => s_outputValue <= "1011011";
                  {{when}} "0011" => s_outputValue <= "1001111";
                  {{when}} "0100" => s_outputValue <= "1100110";
                  {{when}} "0101" => s_outputValue <= "1101101";
                  {{when}} "0110" => s_outputValue <= "1111101";
                  {{when}} "0111" => s_outputValue <= "0000111";
                  {{when}} "1000" => s_outputValue <= "1111111";
                  {{when}} "1001" => s_outputValue <= "1101111";
                  {{when}} {{others}} => s_outputValue <= "-------";
               {{end}} {{case}};
            {{end}} {{process}} makeSegs;
            """)
        .empty() : LineBuffer.getBuffer()
            .empty()
            .add("""
                assign segmentA = s_outputValue[0];
                assign segmentB = s_outputValue[1];
                assign segmentC = s_outputValue[2];
                assign segmentD = s_outputValue[3];
                assign segmentE = s_outputValue[4];
                assign segmentF = s_outputValue[5];
                assign segmentG = s_outputValue[6];
                
                reg [6:0] s_rOutputValue;
                assign s_outputValue = s_rOutputValue;
                
                always @*
                  begin
                    case (bcdIn)
                      4'h0 : s_rOutputValue <= 7'b0111111;
                      4'h1 : s_rOutputValue <= 7'b0000110;
                      4'h2 : s_rOutputValue <= 7'b1011011;
                      4'h3 : s_rOutputValue <= 7'b1001111;
                      4'h4 : s_rOutputValue <= 7'b1100110;
                      4'h5 : s_rOutputValue <= 7'b1101101;
                      4'h6 : s_rOutputValue <= 7'b1111101;
                      4'h7 : s_rOutputValue <= 7'b0000111;
                      4'h8 : s_rOutputValue <= 7'b1111111;
                      4'h9 : s_rOutputValue <= 7'b1101111;
                      default : s_rOutputValue <= 7'b0000000;
                    endcase 
                  end
            """);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return true;
  }
}
