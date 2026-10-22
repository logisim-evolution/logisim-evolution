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

public class FpSubtractorHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataA", width, FpSubtractor.IN0)
        .add(Port.INPUT, "dataB", width, FpSubtractor.IN1)
        .add(Port.OUTPUT, "result", width, FpSubtractor.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpSubtractor.ERR);
    myWires.addWire("s_result", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addAdd();
    return FpHdl.wrap(resultStatements("fp_add(dataA, dataB, '1')", "fp_add(dataA, dataB, 1'b1)"), hdl);
  }
}
