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

/**
 * Note that the remainder output is the IEEE remainder, which needs a divider as wide as the
 * exponent range of the format (about 2100 bits for 64 bit floating point numbers).
 */
public class FpDividerHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataA", width, FpDivider.IN0)
        .add(Port.INPUT, "dataB", width, FpDivider.IN1)
        .add(Port.OUTPUT, "quotient", width, FpDivider.OUT1)
        .add(Port.OUTPUT, "remainder", width, FpDivider.OUT2)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpDivider.ERR);
    myWires.addWire("s_quotient", width).addWire("s_remainder", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addDiv().addRemainder();
    return FpHdl.wrap(
        select(
            """
            s_quotient  <= fp_div(dataA, dataB);
            s_remainder <= fp_rem(dataA, dataB);
            quotient    <= s_quotient;
            remainder   <= s_remainder;
            fpError     <= '1' when fp_isnan(s_quotient) or fp_isnan(s_remainder) else '0';
            """,
            """
            wire s_nanQuotient  = fp_isnan(s_quotient);
            wire s_nanRemainder = fp_isnan(s_remainder);
            assign s_quotient  = fp_div(dataA, dataB);
            assign s_remainder = fp_rem(dataA, dataB);
            assign quotient    = s_quotient;
            assign remainder   = s_remainder;
            assign fpError     = s_nanQuotient | s_nanRemainder;
            """),
        hdl);
  }
}
