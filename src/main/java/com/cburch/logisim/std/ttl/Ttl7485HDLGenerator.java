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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class Ttl7485HDLGenerator extends AbstractHDLGeneratorFactory {

  public Ttl7485HDLGenerator() {
    super();
    myWires
        .addWire("oppA", 4)
        .addWire("oppB", 4)
        .addWire("gt", 1)
        .addWire("eq", 1)
        .addWire("lt", 1)
        .addWire("CompIn", 3)
        .addWire("CompOut", 3);
    myPorts
        .add(Port.INPUT, "A0", 1, 8)
        .add(Port.INPUT, "A1", 1, 10)
        .add(Port.INPUT, "A2", 1, 11)
        .add(Port.INPUT, "A3", 1, 13)
        .add(Port.INPUT, "B0", 1, 7)
        .add(Port.INPUT, "B1", 1, 9)
        .add(Port.INPUT, "B2", 1, 12)
        .add(Port.INPUT, "B3", 1, 0)
        .add(Port.INPUT, "AltBin", 1, 1)
        .add(Port.INPUT, "AeqBin", 1, 2)
        .add(Port.INPUT, "AgtBin", 1, 3)
        .add(Port.OUTPUT, "AltBout", 1, 6)
        .add(Port.OUTPUT, "AeqBout", 1, 5)
        .add(Port.OUTPUT, "AgtBout", 1, 4);
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    return contents
        .add("""
            oppA   <= A3&A2&A1&A0;
            oppB   <= B3&B2&B1&B0;
            gt     <= '1' WHEN unsigned(oppA) > unsigned(oppB) ELSE '0';
            eq     <= '1' WHEN unsigned(oppA) = unsigned(oppB) ELSE '0';
            lt     <= '1' WHEN unsigned(oppA) < unsigned(oppB) ELSE '0';
            
            CompIn <= AgtBin&AltBin&AeqBin;
            WITH (CompIn) SELECT CompOut <= 
               "100" WHEN "100",
               "010" WHEN "010",
               "000" WHEN "110",
               "110" WHEN "000",
               "001" WHEN OTHERS;
            
            AgtBout <= '1' WHEN gt = '1' ELSE '0' WHEN lt = '1' ELSE CompOut(2);
            AltBout <= '0' WHEN gt = '1' ELSE '1' WHEN lt = '1' ELSE CompOut(1);
            AeqBout <= '0' WHEN (gt = '1') OR (lt = '1') ELSE CompOut(0);
            """)
        .getWithIndent();
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
