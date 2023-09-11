/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHdlGenerator;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class Ttl7447HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7447HdlGenerator() {
    super();
    myWires
        .addWire("segments", 7)
        .addWire("realSegments", 7)
        .addWire("bcd", 4);
    myPorts
        .add(Port.INPUT, "BCD0", 1, 6)
        .add(Port.INPUT, "BCD1", 1, 0)
        .add(Port.INPUT, "BCD2", 1, 1)
        .add(Port.INPUT, "BCD3", 1, 5)
        .add(Port.INPUT, "LT", 1, 2, false)
        .add(Port.INPUT, "BI", 1, 3, false)
        .add(Port.INPUT, "RBI", 1, 4, false)
        .add(Port.OUTPUT, "Sega", 1, 11)
        .add(Port.OUTPUT, "Segb", 1, 10)
        .add(Port.OUTPUT, "Segc", 1, 9)
        .add(Port.OUTPUT, "Segd", 1, 8)
        .add(Port.OUTPUT, "Sege", 1, 7)
        .add(Port.OUTPUT, "Segf", 1, 13)
        .add(Port.OUTPUT, "Segg", 1, 12);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var decoder = new WithSelectHdlGenerator("decoder1", "bcd", 4, "segments", 7)
        .setDefault("0000000") // selectValue = 15L
        .add(0L, "0111111")
        .add(1L, "0000110")
        .add(2L, "1011011")
        .add(3L, "1001111")
        .add(4L, "1100110")
        .add(5L, "1101101")
        .add(6L, "1111100")
        .add(7L, "0000111")
        .add(8L, "1111111")
        .add(9L, "1100111")
        .add(10L, "1011000")
        .add(11L, "1001100")
        .add(12L, "1100010")
        .add(13L, "1101001")
        .add(14L, "1111000");
    final var contents = LineBuffer.getHdlBuffer();
    contents.add(decoder.getHdlCode()).empty();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
            Sega  <= realSegments(0);
            Segb  <= realSegments(1);
            Segc  <= realSegments(2);
            Segd  <= realSegments(3);
            Sege  <= realSegments(4);
            Segf  <= realSegments(5);
            Segg  <= realSegments(6);

            bcd   <= BCD3&BCD2&BCD1&BCD0;
            
            realSegments <= ({{others}} => '1') {{when}} BI = '0' {{else}}
                            ({{others}} => '0') {{when}} LT = '0' {{else}}
                            ({{others}} => '1') {{when}} (RBI='0') {{and}} (bcd=x"0") {{else}}
                            {{not}}(segments);
          """);
    } else {
      contents.add("""
          assign Sega = realSegments[0];
          assign Segb = realSegments[1];
          assign Segc = realSegments[2];
          assign Segd = realSegments[3];
          assign Sege = realSegments[4];
          assign Segf = realSegments[5];
          assign Segg = realSegments[6];
          assign bcd  = {BCD3, BCD2, BCD1, BCD0};
          
          assign realSegments = BI == 0 ? 7'h7F : LT == 0 ? 0 : RBI == 0 && bcd == 0 ? 7'h7F : !segments;
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
