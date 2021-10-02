/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class bcd2sevensegHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  public bcd2sevensegHDLGeneratorFactory() {
    myWires
        .addWire("s_output_value", 7);
    myPorts
        .add(Port.INPUT, "BCDin", 4, bcd2sevenseg.BCD_IN)
        .add(Port.OUTPUT, "Segment_a", 1, bcd2sevenseg.SEGMENT_A)
        .add(Port.OUTPUT, "Segment_b", 1, bcd2sevenseg.SEGMENT_B)
        .add(Port.OUTPUT, "Segment_c", 1, bcd2sevenseg.SEGMENT_C)
        .add(Port.OUTPUT, "Segment_d", 1, bcd2sevenseg.SEGMENT_D)
        .add(Port.OUTPUT, "Segment_e", 1, bcd2sevenseg.SEGMENT_E)
        .add(Port.OUTPUT, "Segment_f", 1, bcd2sevenseg.SEGMENT_F)
        .add(Port.OUTPUT, "Segment_g", 1, bcd2sevenseg.SEGMENT_G);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return LineBuffer.getBuffer()
        .add("""
            Segment_a <= s_output_value(0);
            Segment_b <= s_output_value(1);
            Segment_c <= s_output_value(2);
            Segment_d <= s_output_value(3);
            Segment_e <= s_output_value(4);
            Segment_f <= s_output_value(5);
            Segment_g <= s_output_value(6);

            MakeSegs : PROCESS( BCDin )
            BEGIN
               CASE (BCDin) IS
                  WHEN "0000" => s_output_value <= "0111111";
                  WHEN "0001" => s_output_value <= "0000110";
                  WHEN "0010" => s_output_value <= "1011011";
                  WHEN "0011" => s_output_value <= "1001111";
                  WHEN "0100" => s_output_value <= "1100110";
                  WHEN "0101" => s_output_value <= "1101101";
                  WHEN "0110" => s_output_value <= "1111101";
                  WHEN "0111" => s_output_value <= "0000111";
                  WHEN "1000" => s_output_value <= "1111111";
                  WHEN "1001" => s_output_value <= "1101111";
                  WHEN OTHERS => s_output_value <= "-------";
               END CASE;
            END PROCESS MakeSegs;
            """);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }
}
