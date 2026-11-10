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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class FpToIntHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    myPorts
        .add(Port.INPUT, "dataIn", fpWidth(attrs), FpToInt.IN)
        .add(Port.OUTPUT, "result", attrs.getValue(StdAttr.WIDTH).getWidth(), FpToInt.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpToInt.ERR);
    myWires.addWire("s_wide", 64);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var outBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var hdl = new FpHdl(fpWidth(attrs)).addToInt(FpRoundHdlGeneratorFactory.mode(attrs));
    final var vhdlResult = outBits == 1 ? "s_wide(0)" : "s_wide(" + (outBits - 1) + " downto 0)";
    final var verilogResult = outBits == 1 ? "s_wide[0]" : "s_wide[" + (outBits - 1) + ":0]";
    return FpHdl.wrap(
        select(
            "s_wide  <= fp_toint(dataIn);\n"
                + "result  <= " + vhdlResult + ";\n"
                + "fpError <= '1' when fp_isnan(dataIn) else '0';",
            "assign s_wide  = fp_toint(dataIn);\n"
                + "assign result  = " + verilogResult + ";\n"
                + "assign fpError = fp_isnan(dataIn);"),
        hdl);
  }
}
