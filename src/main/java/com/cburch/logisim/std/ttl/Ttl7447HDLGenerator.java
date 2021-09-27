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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class Ttl7447HDLGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7447HDLGenerator() {
    super();
    myWires
        .addWire("segments", 7)
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
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    return contents
        .add("""
            Sega  <= segments(0);
            Segb  <= segments(1);
            Segc  <= segments(2);
            Segd  <= segments(3);
            Sege  <= segments(4);
            Segf  <= segments(5);
            Segg  <= segments(6);
            
            bcd   <= BCD3&BCD2&BCD1&BCD0;
            
            Decode : PROCESS ( bcd , LT , BI , RBI ) IS
               BEGIN
                  CASE bcd IS
                     WHEN "0000" => segments <= "0111111";
                     WHEN "0001" => segments <= "0000110";
                     WHEN "0010" => segments <= "1011011";
                     WHEN "0011" => segments <= "1001111";
                     WHEN "0100" => segments <= "1100110";
                     WHEN "0101" => segments <= "1101101";
                     WHEN "0110" => segments <= "1111101";
                     WHEN "0111" => segments <= "0000111";
                     WHEN "1000" => segments <= "1111111";
                     WHEN "1001" => segments <= "1100111";
                     WHEN "1010" => segments <= "1110111";
                     WHEN "1011" => segments <= "1111100";
                     WHEN "1100" => segments <= "0111001";
                     WHEN "1101" => segments <= "1011110";
                     WHEN "1110" => segments <= "1111001";
                     WHEN OTHERS => segments <= "1110001";
                  END CASE;
                  IF (BI = '0') THEN segments <= "0000000";
                  ELSIF (LT = '0') THEN segments <= "1111111";
                  ELSIF ((RBI='0') AND (bcd="0000")) THEN segments <= "0000000";
                  END IF;
               END PROCESS Decode;
            """)
        .getWithIndent();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && Hdl.isVhdl());
  }
}
