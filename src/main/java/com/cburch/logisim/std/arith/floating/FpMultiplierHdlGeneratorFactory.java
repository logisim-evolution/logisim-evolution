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

public class FpMultiplierHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static boolean isFusedMultiplyAdd(AttributeSet attrs) {
    return attrs.getValue(FpMultiplier.MUL_MODE) == FpMultiplier.FMA;
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataA", width, FpMultiplier.IN0)
        .add(Port.INPUT, "dataB", width, FpMultiplier.IN1)
        .add(Port.OUTPUT, "result", width, FpMultiplier.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpMultiplier.ERR);
    if (isFusedMultiplyAdd(attrs)) myPorts.add(Port.INPUT, "dataC", width, FpMultiplier.IN2);
    myWires.addWire("s_result", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var width = fpWidth(attrs);
    if (isFusedMultiplyAdd(attrs)) {
      final var hdl = new FpHdl(width).addFma();
      return FpHdl.wrap(resultStatements("fp_fma(dataA, dataB, dataC)", "fp_fma(dataA, dataB, dataC)"), hdl);
    }
    final var hdl = new FpHdl(width).addMul();
    return FpHdl.wrap(resultStatements("fp_mul(dataA, dataB)", "fp_mul(dataA, dataB)"), hdl);
  }
}
