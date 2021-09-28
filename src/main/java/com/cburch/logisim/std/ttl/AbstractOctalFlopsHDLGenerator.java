/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.util.ArrayList;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class AbstractOctalFlopsHDLGenerator extends AbstractHdlGeneratorFactory {

  public AbstractOctalFlopsHDLGenerator(boolean hasClockEnable) {
    super();
    myWires
        .addWire("state", 8)
        .addWire("enable", 1)
        .addWire("nexts", 8);
    if (hasClockEnable)
      myPorts
          .add(Port.INPUT, "nCLR", 1, HdlPorts.PULL_UP)
          .add(Port.INPUT, "nCLKen", 1, 0);
    else
      myPorts
        .add(Port.INPUT, "nCLR", 1, 0)
        .add(Port.INPUT, "nCLKen", 1, HdlPorts.PULL_DOWN);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, 9)
        .add(Port.INPUT, "D0", 1, 2)
        .add(Port.INPUT, "D1", 1, 3)
        .add(Port.INPUT, "D2", 1, 6)
        .add(Port.INPUT, "D3", 1, 7)
        .add(Port.INPUT, "D4", 1, 11)
        .add(Port.INPUT, "D5", 1, 12)
        .add(Port.INPUT, "D6", 1, 15)
        .add(Port.INPUT, "D7", 1, 16)
        .add(Port.OUTPUT, "Q0", 1, 1)
        .add(Port.OUTPUT, "Q1", 1, 4)
        .add(Port.OUTPUT, "Q2", 1, 5)
        .add(Port.OUTPUT, "Q3", 1, 8)
        .add(Port.OUTPUT, "Q4", 1, 10)
        .add(Port.OUTPUT, "Q5", 1, 13)
        .add(Port.OUTPUT, "Q6", 1, 14)
        .add(Port.OUTPUT, "Q7", 1, 17);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getBuffer()
        .pair("CLK", HdlPorts.CLOCK)
        .pair("tick", HdlPorts.TICK)
        .add("""
            enable <= {{tick}} and NOT(nCLKen);
            nexts  <= D7&D6&D5&D4&D3&D2&D1&D0 WHEN enable = '1' ELSE state;
            Q0     <= state(0);
            Q1     <= state(1);
            Q2     <= state(2);
            Q3     <= state(3);
            Q4     <= state(4);
            Q5     <= state(5);
            Q6     <= state(6);
            Q7     <= state(7);
            
            dffs : PROCESS( {{CLK}} , nCLR ) IS
               BEGIN
                  IF (nCLR = '1') THEN state <= (OTHERS => '0');
                  ELSIF (rising_edge({{CLK}})) THEN state <= nexts;
                  END IF;
               END PROCESS dffs; 
            """)
        .getWithIndent();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && (Hdl.isVhdl()));
  }
}
