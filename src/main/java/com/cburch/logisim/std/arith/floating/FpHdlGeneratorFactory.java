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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.StdAttr;

/**
 * Base of all floating point HDL generators. The generated code is specific for the floating point
 * format, therefore ports and wires are defined (with literal widths) when the HDL is generated.
 */
abstract class FpHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  protected static final String ERROR_PORT = "fpError";

  protected FpHdlGeneratorFactory() {
    super();
    getWiresPortsDuringHDLWriting = true;
  }

  protected static int fpWidth(AttributeSet attrs) {
    return attrs.getValue(StdAttr.FP_WIDTH).getWidth();
  }

  protected static String select(String vhdl, String verilog) {
    return Hdl.isVhdl() ? vhdl : verilog;
  }

  /** Drives result and the error flag (result is NaN) from the expression of the result. */
  protected static String resultStatements(String vhdlExpression, String verilogExpression) {
    return select(
        "s_result <= " + vhdlExpression + ";\n"
            + "result   <= s_result;\n"
            + "fpError  <= '1' when fp_isnan(s_result) else '0';",
        "assign s_result = " + verilogExpression + ";\n"
            + "assign result   = s_result;\n"
            + "assign fpError  = fp_isnan(s_result);");
  }

  @Override
  public void getGenerationTimeWiresPorts(
      com.cburch.logisim.fpga.designrulecheck.Netlist theNetlist, AttributeSet attrs) {
    definePortsAndWires(attrs);
  }

  protected abstract void definePortsAndWires(AttributeSet attrs);
}
