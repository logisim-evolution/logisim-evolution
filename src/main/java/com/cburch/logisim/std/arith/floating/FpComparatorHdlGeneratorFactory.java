/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class FpComparatorHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataA", width, FpComparator.IN0)
        .add(Port.INPUT, "dataB", width, FpComparator.IN1)
        .add(Port.OUTPUT, "aGreaterThanB", 1, FpComparator.GT)
        .add(Port.OUTPUT, "aEqualsB", 1, FpComparator.EQ)
        .add(Port.OUTPUT, "aLessThanB", 1, FpComparator.LT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpComparator.ERR);
    myWires.addWire("s_nanA", 1).addWire("s_nanB", 1);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addLessThan();
    return FpHdl.wrap(
        select(
            """
            s_nanA        <= '1' when fp_isnan(dataA) else '0';
            s_nanB        <= '1' when fp_isnan(dataB) else '0';
            aLessThanB    <= '1' when fp_lt(dataA, dataB) else '0';
            aGreaterThanB <= '1' when fp_lt(dataB, dataA) else '0';
            aEqualsB      <= '1' when fp_eq(dataA, dataB) else '0';
            fpError       <= s_nanA or s_nanB;
            """,
            """
            assign s_nanA        = fp_isnan(dataA);
            assign s_nanB        = fp_isnan(dataB);
            assign aLessThanB    = fp_lt(dataA, dataB);
            assign aGreaterThanB = fp_lt(dataB, dataA);
            assign aEqualsB      = fp_eq(dataA, dataB);
            assign fpError       = s_nanA | s_nanB;
            """),
        hdl);
  }
}
