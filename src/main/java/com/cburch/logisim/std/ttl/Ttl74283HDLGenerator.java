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

public class Ttl74283HDLGenerator extends AbstractHdlGeneratorFactory {

  public Ttl74283HDLGenerator() {
    super();
    myWires
        .addWire("oppA", 5)
        .addWire("oppB", 5)
        .addWire("oppC", 5)
        .addWire("Result", 5);
    myPorts
        .add(Port.INPUT, "A1", 1, 4)
        .add(Port.INPUT, "A2", 1, 2)
        .add(Port.INPUT, "A3", 1, 12)
        .add(Port.INPUT, "A4", 1, 10)
        .add(Port.INPUT, "B1", 1, 5)
        .add(Port.INPUT, "B2", 1, 1)
        .add(Port.INPUT, "B3", 1, 13)
        .add(Port.INPUT, "B4", 1, 9)
        .add(Port.INPUT, "Cin", 1, 6)
        .add(Port.OUTPUT, "S1", 1, 3)
        .add(Port.OUTPUT, "S2", 1, 0)
        .add(Port.OUTPUT, "S3", 1, 11)
        .add(Port.OUTPUT, "S4", 1, 8)
        .add(Port.OUTPUT, "Cout", 1, 7);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return LineBuffer.getBuffer()
        .add("""
            oppA   <= "0"&A4&A3&A2&A1;
            oppB   <= "0"&B4&B3&B2&B1;
            oppC   <= "0000"&Cin;
            Result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));
            S1     <= Result(0);
            S2     <= Result(1);
            S3     <= Result(2);
            S4     <= Result(3);
            Cout   <= Result(4);
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
