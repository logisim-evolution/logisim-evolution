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

public class FpToFpHdlGeneratorFactory extends FpHdlGeneratorFactory {

  static int inputWidth(AttributeSet attrs) {
    return attrs.getValue(FpToFp.FP_WIDTH_IN).getWidth();
  }

  static int outputWidth(AttributeSet attrs) {
    return attrs.getValue(FpToFp.FP_WIDTH_OUT).getWidth();
  }

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    myPorts
        .add(Port.INPUT, "dataIn", inputWidth(attrs), FpToFp.IN)
        .add(Port.OUTPUT, "result", outputWidth(attrs), FpToFp.OUT)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpToFp.ERR);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var source = new FpHdl(inputWidth(attrs), "i_").addBase();
    if (inputWidth(attrs) == outputWidth(attrs)) {
      return FpHdl.wrap(
          select(
              """
              result  <= dataIn;
              fpError <= '1' when i_fp_isnan(dataIn) else '0';
              """,
              """
              assign result  = dataIn;
              assign fpError = i_fp_isnan(dataIn);
              """),
          source);
    }
    final var destination = new FpHdl(outputWidth(attrs), "o_").addConvertFrom(source);
    return FpHdl.wrap(
        select(
            """
            result  <= o_fp_convert(dataIn);
            fpError <= '1' when i_fp_isnan(dataIn) else '0';
            """,
            """
            assign result  = o_fp_convert(dataIn);
            assign fpError = i_fp_isnan(dataIn);
            """),
        source,
        destination);
  }
}
