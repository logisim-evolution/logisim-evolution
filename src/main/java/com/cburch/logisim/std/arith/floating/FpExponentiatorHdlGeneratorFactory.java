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

/** CORDIC based, see {@link FpCordic} for the accuracy. */
public class FpExponentiatorHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static String mode(AttributeSet attrs) {
    final var mode = attrs.getValue(FpExponentiator.EXP_MODE);
    if (mode == FpExponentiator.ARB) return "ARB";
    return mode == FpExponentiator.EXP ? "EXP" : "EXPM1";
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts.add(Port.INPUT, "dataExp", width, FpExponentiator.EXPO);
    if (mode(attrs).equals("ARB")) myPorts.add(Port.INPUT, "dataBase", width, FpExponentiator.BASE);
    myPorts
        .add(Port.OUTPUT, "result", width, FpExponentiator.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpExponentiator.ERR);
    myWires.addWire("s_result", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs));
    final var cordic = new FpCordic(hdl);
    return switch (mode(attrs)) {
      case "ARB" -> {
        cordic.addPow();
        yield FpHdl.wrap(resultStatements("fm_pow(dataBase, dataExp)", "fm_pow(dataBase, dataExp)"), hdl);
      }
      case "EXP" -> {
        cordic.addExp();
        yield FpHdl.wrap(resultStatements("fm_exp(dataExp)", "fm_exp(dataExp)"), hdl);
      }
      default -> {
        cordic.addExpm1();
        yield FpHdl.wrap(resultStatements("fm_expm1(dataExp)", "fm_expm1(dataExp)"), hdl);
      }
    };
  }
}
