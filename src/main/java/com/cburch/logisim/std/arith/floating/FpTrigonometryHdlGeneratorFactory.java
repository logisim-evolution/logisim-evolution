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
public class FpTrigonometryHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static String mode(AttributeSet attrs) {
    final var mode = attrs.getValue(FpTrigonometry.TRIG_MODE);
    if (mode == FpTrigonometry.TRIG) return "TRIG";
    return mode == FpTrigonometry.ARC ? "ARC" : "HYP";
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataIn", width, FpTrigonometry.IN)
        .add(Port.OUTPUT, "sine", width, FpTrigonometry.SIN)
        .add(Port.OUTPUT, "tangent", width, FpTrigonometry.TAN)
        .add(Port.OUTPUT, "cosine", width, FpTrigonometry.COS)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpTrigonometry.ERR);
    myWires
        .addWire("s_sin", width)
        .addWire("s_tan", width)
        .addWire("s_cos", width)
        .addWire("s_nanIn", 1)
        .addWire("s_nanSin", 1)
        .addWire("s_nanTan", 1)
        .addWire("s_nanCos", 1);
    if (!mode(attrs).equals("ARC")) myWires.addWire("s_sc", 2 * width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs));
    final var cordic = new FpCordic(hdl);
    final var mode = mode(attrs);
    final var functions =
        switch (mode) {
          case "TRIG" -> {
            cordic.addSinCos();
            hdl.addDiv();
            yield select(
                """
                s_sc  <= fm_sincos(dataIn);
                s_sin <= s_sc(@WB@-1 downto 0);
                s_cos <= s_sc(2*@WB@-1 downto @WB@);
                s_tan <= fp_div(s_sin, s_cos);
                """,
                """
                assign s_sc  = fm_sincos(dataIn);
                assign s_sin = s_sc[@WB@-1:0];
                assign s_cos = s_sc[2*@WB@-1:@WB@];
                assign s_tan = fp_div(s_sin, s_cos);
                """);
          }
          case "ARC" -> {
            cordic.addArc();
            yield select(
                """
                s_sin <= fm_asin(dataIn);
                s_tan <= fm_atan(dataIn);
                s_cos <= fm_acos(dataIn);
                """,
                """
                assign s_sin = fm_asin(dataIn);
                assign s_tan = fm_atan(dataIn);
                assign s_cos = fm_acos(dataIn);
                """);
          }
          default -> {
            cordic.addTanh();
            yield select(
                """
                s_sc  <= fm_sinhcosh(dataIn);
                s_sin <= s_sc(@WB@-1 downto 0);
                s_cos <= s_sc(2*@WB@-1 downto @WB@);
                s_tan <= fm_tanh(dataIn);
                """,
                """
                assign s_sc  = fm_sinhcosh(dataIn);
                assign s_sin = s_sc[@WB@-1:0];
                assign s_cos = s_sc[2*@WB@-1:@WB@];
                assign s_tan = fm_tanh(dataIn);
                """);
          }
        };
    final var common =
        select(
            """
            sine      <= s_sin;
            tangent   <= s_tan;
            cosine    <= s_cos;
            s_nanIn   <= '1' when fp_isnan(dataIn) else '0';
            s_nanSin  <= '1' when fp_isnan(s_sin) else '0';
            s_nanTan  <= '1' when fp_isnan(s_tan) else '0';
            s_nanCos  <= '1' when fp_isnan(s_cos) else '0';
            fpError   <= s_nanIn or s_nanSin or s_nanTan or s_nanCos;
            """,
            """
            assign sine      = s_sin;
            assign tangent   = s_tan;
            assign cosine    = s_cos;
            assign s_nanIn   = fp_isnan(dataIn);
            assign s_nanSin  = fp_isnan(s_sin);
            assign s_nanTan  = fp_isnan(s_tan);
            assign s_nanCos  = fp_isnan(s_cos);
            assign fpError   = s_nanIn | s_nanSin | s_nanTan | s_nanCos;
            """);
    return FpHdl.wrap(hdl.code(functions + "\n" + common), hdl);
  }
}
