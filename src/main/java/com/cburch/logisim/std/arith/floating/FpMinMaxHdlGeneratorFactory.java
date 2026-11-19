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

public class FpMinMaxHdlGeneratorFactory extends FpHdlGeneratorFactory {

  @Override
  protected void definePortsAndWires(AttributeSet attrs) {
    final var width = fpWidth(attrs);
    myPorts
        .add(Port.INPUT, "dataA", width, FpMinMax.IN0)
        .add(Port.INPUT, "dataB", width, FpMinMax.IN1)
        .add(Port.OUTPUT, "minimum", width, FpMinMax.MIN)
        .add(Port.OUTPUT, "maximum", width, FpMinMax.MAX)
        .add(Port.OUTPUT, ERROR_PORT, 1, FpMinMax.ERR);
    myWires
        .addWire("s_nanA", 1)
        .addWire("s_nanB", 1)
        .addWire("s_keyA", width)
        .addWire("s_keyB", width);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var hdl = new FpHdl(fpWidth(attrs)).addOrderKey();
    return FpHdl.wrap(
        hdl.code(
            select(
                """
                s_nanA  <= '1' when fp_isnan(dataA) else '0';
                s_nanB  <= '1' when fp_isnan(dataB) else '0';
                s_keyA  <= std_logic_vector(fp_key(dataA));
                s_keyB  <= std_logic_vector(fp_key(dataB));
                minimum <= fp_nan when s_nanA = '1' or s_nanB = '1' else
                           dataA  when unsigned(s_keyA) <= unsigned(s_keyB) else
                           dataB;
                maximum <= fp_nan when s_nanA = '1' or s_nanB = '1' else
                           dataA  when unsigned(s_keyA) >= unsigned(s_keyB) else
                           dataB;
                fpError <= s_nanA or s_nanB;
                """,
                """
                assign s_nanA  = fp_isnan(dataA);
                assign s_nanB  = fp_isnan(dataB);
                assign s_keyA  = fp_key(dataA);
                assign s_keyB  = fp_key(dataB);
                assign minimum = (s_nanA | s_nanB) ? NAN_V : ((s_keyA <= s_keyB) ? dataA : dataB);
                assign maximum = (s_nanA | s_nanB) ? NAN_V : ((s_keyA >= s_keyB) ? dataA : dataB);
                assign fpError = s_nanA | s_nanB;
                """)),
        hdl);
  }
}
