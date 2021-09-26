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
import com.cburch.logisim.fpga.hdlgenerator.HDLPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class Ttl74165HDLGenerator extends AbstractHDLGeneratorFactory {

  public Ttl74165HDLGenerator() {
    super();
    myWires
        .addWire("CurState", 8)
        .addWire("NextState", 8)
        .addWire("ParData", 8)
        .addWire("Enable", 1);
    myPorts
        .add(Port.CLOCK, HDLPorts.CLOCK, 1, 1)
        .add(Port.INPUT, "SHnLD", 1, 0)
        .add(Port.INPUT, "CKIh", 1, 13)
        .add(Port.INPUT, "SER", 1, 8)
        .add(Port.INPUT, "P0", 1, 9)
        .add(Port.INPUT, "P1", 1, 10)
        .add(Port.INPUT, "P2", 1, 11)
        .add(Port.INPUT, "P3", 1, 12)
        .add(Port.INPUT, "P4", 1, 2)
        .add(Port.INPUT, "P5", 1, 3)
        .add(Port.INPUT, "P6", 1, 4)
        .add(Port.INPUT, "P7", 1, 5)
        .add(Port.OUTPUT, "Q7", 1, 6)
        .add(Port.OUTPUT, "Q7n", 1, 7);
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (new LineBuffer())
        .pair("CK", HDLPorts.CLOCK)
        .pair("Tick", HDLPorts.TICK)
        .add("""
            Q7  <= CurState(0);
            Q7n <= NOT(CurState(0));
            
            Enable  <= NOT(CKIh) AND {{Tick}};
            ParData <= P7&P6&P5&P4&P3&P2&P1&P0;
            
            NextState <= CurState WHEN Enable = '0' ELSE
                         ParData WHEN SHnLD = '0' ELSE
                         SER&CurState(7 DOWNTO 1);
            
            dffs : PROCESS( {{CK}} ) IS
               BEGIN
                  IF (rising_edge({{CK}})) THEN CurState <= NextState;
                  END IF;
               END PROCESS dffs;
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
