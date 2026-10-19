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

public class FpAbsoluteHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataIn", width, FpAbsolute.IN0)
        .add(Port.OUTPUT, "result", width, FpAbsolute.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpAbsolute.ERR);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addBase();
    return FpHdl.wrap(
        hdl.code(
            select(
                """
                result  <= '0' & dataIn(@WB@-2 downto 0);
                fpError <= '1' when fp_isnan(dataIn) else '0';
                """,
                """
                assign result  = {1'b0, dataIn[@WB@-2:0]};
                assign fpError = fp_isnan(dataIn);
                """)),
        hdl);
  }
}
