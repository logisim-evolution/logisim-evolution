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
 * Differs from the simulation only for values that the simulation turns into a (long) first:
 * infinity and NaN stay as they are, and values of 2^63 or more are already integral.
 */
public class FpRoundHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static String mode(AttributeSet attrs) {
    return attrs.getValue(FpToInt.MODE_ATTRIBUTE).getValue().toString();
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataIn", width, FpRound.IN)
        .add(Port.OUTPUT, "result", width, FpRound.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpRound.ERR);
    myWires.addWire("s_result", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addRound(mode(attrs));
    return FpHdl.wrap(
        select(
            """
            s_result <= fp_round(dataIn);
            result   <= s_result;
            fpError  <= '1' when fp_isnan(dataIn) else '0';
            """,
            """
            assign s_result = fp_round(dataIn);
            assign result   = s_result;
            assign fpError  = fp_isnan(dataIn);
            """),
        hdl);
  }
}
