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
import com.cburch.logisim.std.arith.Comparator;
import com.cburch.logisim.util.LineBuffer;

public class IntToFpHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static boolean isSigned(AttributeSet attrs) {
    return !attrs.getValue(Comparator.MODE_ATTR).equals(Comparator.UNSIGNED_OPTION);
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataIn", attrs.getValue(StdAttr.WIDTH).getWidth(), IntToFp.IN)
        .add(Port.OUTPUT, "result", width, IntToFp.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, IntToFp.ERR);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var inBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var hdl = new FpHdl(fpWidth(attrs)).addFromInt(inBits, isSigned(attrs));
    // a one bit port is a std_logic in VHDL, not a vector
    final var vhdlInput = inBits == 1 ? "(0 => dataIn)" : "dataIn";
    return FpHdl.wrap(
        select(
            "result  <= fp_fromint(" + vhdlInput + ");\n"
                + "fpError <= '0';",
            "assign result  = fp_fromint(dataIn);\n"
                + "assign fpError = 1'b0;"),
        hdl);
  }
}
