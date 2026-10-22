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

public class FpClassificatorHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    myPorts
        .add(Port.INPUT, "dataIn", fpWidth(attrs), FpClassificator.IN)
        .add(Port.OUTPUT, "isNegative", 1, FpClassificator.NEGATIVE)
        .add(Port.OUTPUT, "isZero", 1, FpClassificator.ZERO)
        .add(Port.OUTPUT, "isSubnormal", 1, FpClassificator.SUBNORMAL)
        .add(Port.OUTPUT, "isNormal", 1, FpClassificator.NORMAL)
        .add(Port.OUTPUT, "isInfinite", 1, FpClassificator.INFINITE)
        .add(Port.OUTPUT, "isNaN", 1, FpClassificator.QUIET_NAN);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs));
    return FpHdl.wrap(
        hdl.code(
            select(
                """
                isNegative  <= dataIn(@WB@-1);
                isZero      <= '1' when unsigned(dataIn(@WB@-2 downto 0)) = 0 else '0';
                isSubnormal <= '1' when unsigned(dataIn(@WB@-2 downto @MB@)) = 0
                                        and unsigned(dataIn(@MB@-1 downto 0)) /= 0 else '0';
                isNormal    <= '1' when unsigned(dataIn(@WB@-2 downto @MB@)) /= 0
                                        and unsigned(dataIn(@WB@-2 downto @MB@)) /= @EMAX@ else '0';
                isInfinite  <= '1' when unsigned(dataIn(@WB@-2 downto @MB@)) = @EMAX@
                                        and unsigned(dataIn(@MB@-1 downto 0)) = 0 else '0';
                isNaN       <= '1' when unsigned(dataIn(@WB@-2 downto @MB@)) = @EMAX@
                                        and unsigned(dataIn(@MB@-1 downto 0)) /= 0 else '0';
                """,
                """
                assign isNegative  = dataIn[@WB@-1];
                assign isZero      = (dataIn[@WB@-2:0] == 0);
                assign isSubnormal = (dataIn[@WB@-2:@MB@] == 0) && (dataIn[@MB@-1:0] != 0);
                assign isNormal    = (dataIn[@WB@-2:@MB@] != 0) && (dataIn[@WB@-2:@MB@] != @EMAX@);
                assign isInfinite  = (dataIn[@WB@-2:@MB@] == @EMAX@) && (dataIn[@MB@-1:0] == 0);
                assign isNaN       = (dataIn[@WB@-2:@MB@] == @EMAX@) && (dataIn[@MB@-1:0] != 0);
                """)),
        hdl);
  }
}
