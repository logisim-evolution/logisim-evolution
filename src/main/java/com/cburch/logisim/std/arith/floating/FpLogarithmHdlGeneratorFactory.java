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
public class FpLogarithmHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static String mode(AttributeSet attrs) {
    final var mode = attrs.getValue(FpLogarithm.LOG_MODE);
    if (mode == FpLogarithm.LOGXY) return "LOGXY";
    if (mode == FpLogarithm.LOG) return "LOG";
    return mode == FpLogarithm.LOG10 ? "LOG10" : "LOG1P";
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    final var mode = mode(attrs);
    myPorts.add(Port.INPUT, "dataIn", width, FpLogarithm.ALOG);
    if (mode.equals("LOGXY")) myPorts.add(Port.INPUT, "dataBase", width, FpLogarithm.BASE);
    myPorts
        .add(Port.OUTPUT, "result", width, FpLogarithm.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpLogarithm.ERR);
    myWires.addWire("s_result", width);
    if (mode.equals("LOGXY")) myWires.addWire("s_lnX", width).addWire("s_lnBase", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs));
    final var cordic = new FpCordic(hdl);
    return switch (mode(attrs)) {
      case "LOG" -> {
        cordic.addLn();
        yield FpHdl.wrap(resultStatements("fm_ln(dataIn, '0')", "fm_ln(dataIn, 1'b0)"), hdl);
      }
      case "LOG10" -> {
        cordic.addLn();
        yield FpHdl.wrap(resultStatements("fm_ln(dataIn, '1')", "fm_ln(dataIn, 1'b1)"), hdl);
      }
      case "LOG1P" -> {
        cordic.addLog1p();
        yield FpHdl.wrap(resultStatements("fm_log1p(dataIn)", "fm_log1p(dataIn)"), hdl);
      }
      default -> {
        cordic.addLn();
        hdl.addDiv();
        yield FpHdl.wrap(
            select(
                """
                s_lnX    <= fm_ln(dataIn, '0');
                s_lnBase <= fm_ln(dataBase, '0');
                s_result <= fp_div(s_lnX, s_lnBase);
                result   <= s_result;
                fpError  <= '1' when fp_isnan(s_result) else '0';
                """,
                """
                assign s_lnX    = fm_ln(dataIn, 1'b0);
                assign s_lnBase = fm_ln(dataBase, 1'b0);
                assign s_result = fp_div(s_lnX, s_lnBase);
                assign result   = s_result;
                assign fpError  = fp_isnan(s_result);
                """),
            hdl);
      }
    };
  }
}
