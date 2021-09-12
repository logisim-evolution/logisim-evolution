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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class bcd2sevensegHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public bcd2sevensegHDLGeneratorFactory() {
    super("BCD2SEVENSEGMENT");
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("BCDin", 4);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("Segment_a", 1);
    Outputs.put("Segment_b", 1);
    Outputs.put("Segment_c", 1);
    Outputs.put("Segment_d", 1);
    Outputs.put("Segment_e", 1);
    Outputs.put("Segment_f", 1);
    Outputs.put("Segment_g", 1);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_output_value", 7);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (new LineBuffer())
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
            """)
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("BCDin", true, ComponentInfo, bcd2sevenseg.BCDin, Nets));
    PortMap.putAll(GetNetMap("Segment_a", true, ComponentInfo, bcd2sevenseg.Segment_A, Nets));
    PortMap.putAll(GetNetMap("Segment_b", true, ComponentInfo, bcd2sevenseg.Segment_B, Nets));
    PortMap.putAll(GetNetMap("Segment_c", true, ComponentInfo, bcd2sevenseg.Segment_C, Nets));
    PortMap.putAll(GetNetMap("Segment_d", true, ComponentInfo, bcd2sevenseg.Segment_D, Nets));
    PortMap.putAll(GetNetMap("Segment_e", true, ComponentInfo, bcd2sevenseg.Segment_E, Nets));
    PortMap.putAll(GetNetMap("Segment_f", true, ComponentInfo, bcd2sevenseg.Segment_F, Nets));
    PortMap.putAll(GetNetMap("Segment_g", true, ComponentInfo, bcd2sevenseg.Segment_G, Nets));
    return PortMap;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
