/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class TickComponentHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private final long fpgaClockFrequency;
  private final double tickFrequency;
  private static final String RELOAD_VALUE_STRING = "ReloadValue";
  private static final Integer RELOAD_VALUE_ID = -1;
  private static final String NR_OF_COUNTER_BITS_STRING = "NrOfBits";
  private static final Integer NR_OF_COUNTER_BITS_ID = -2;

  public static final String FPGA_CLOCK = "FPGA_GlobalClock";
  public static final String FPGA_TICK = "s_FPGA_Tick";
  public static final String HDL_IDENTIFIER = "LogisimTickGenerator";
  public static final String HDL_DIRECTORY = "base";

  public TickComponentHDLGeneratorFactory(long fpga_clock_frequency, double tick_frequency) {
    super(HDL_DIRECTORY);
    fpgaClockFrequency = fpga_clock_frequency;
    tickFrequency = tick_frequency;
    myParametersList
        .add(RELOAD_VALUE_STRING, RELOAD_VALUE_ID)
        .add(NR_OF_COUNTER_BITS_STRING, NR_OF_COUNTER_BITS_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("FPGAClock", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents =
        LineBuffer.getHdlBuffer()
            .pair("nrOfCounterBits", NR_OF_COUNTER_BITS_STRING)
            .add("")
            .addRemarkBlock("Here the Output is defined")
            .add(
                TheNetlist.requiresGlobalClockConnection()
                    ? "   {{assign}} FPGATick {{=}} '1';"
                    : "   {{assign}} FPGATick {{=}} s_tick_reg;")
            .add("")
            .addRemarkBlock("Here the update logic is defined");

    if (HDL.isVHDL()) {
      Contents.add("""
          s_tick_next   <= '1' WHEN s_count_reg = std_logic_vector(to_unsigned(0, {{nrOfCounterBits}})) ELSE '0';
          s_count_next  <= (OTHERS => '0') WHEN s_tick_reg /= '0' AND s_tick_reg /= '1' ELSE -- For simulation only!
                           std_logic_vector(to_unsigned((ReloadValue-1), {{nrOfCounterBits}})) WHEN s_tick_next = '1' ELSE
                           std_logic_vector(unsigned(s_count_reg)-1);
          
          """);
    } else {
      Contents.add("""
              assign s_tick_next  = (s_count_reg == 0) ? 1'b1 : 1'b0;
              assign s_count_next = (s_count_reg == 0) ? ReloadValue-1 : s_count_reg-1;
              
              """)
          .addRemarkBlock("Here the simulation only initial is defined")
          .add("""
              initial
              begin
                 s_count_reg = 0;
                 s_tick_reg  = 1'b0;
              end

              """);
    }
    Contents.addRemarkBlock("Here the flipflops are defined");
    if (HDL.isVHDL()) {
      Contents.add("""
          make_tick : PROCESS( FPGAClock , s_tick_next )
          BEGIN
             IF (FPGAClock'event AND (FPGAClock = '1')) THEN
                s_tick_reg <= s_tick_next;
             END IF;
          END PROCESS make_tick;
          
          make_counter : PROCESS( FPGAClock , s_count_next )
          BEGIN
             IF (FPGAClock'event AND (FPGAClock = '1')) THEN
                s_count_reg <= s_count_next;
             END IF;
          END PROCESS make_counter;
          """);
    } else {
      Contents.add("""
          always @(posedge FPGAClock)
          begin
              s_count_reg <= s_count_next;
              s_tick_reg  <= s_tick_next;
          end
          """);
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("FPGATick", 1);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    double ReloadValueAcc = ((double) fpgaClockFrequency) / tickFrequency;
    long ReloadValue = (long) ReloadValueAcc;
    int nr_of_bits = 0;
    if ((ReloadValue > (long) 0x7FFFFFFF) | (ReloadValue < 0)) ReloadValue = 0x7FFFFFFF;
    ParameterMap.put(RELOAD_VALUE_STRING, (int) ReloadValue);
    while (ReloadValue != 0) {
      nr_of_bits++;
      ReloadValue /= 2;
    }
    ParameterMap.put(NR_OF_COUNTER_BITS_STRING, nr_of_bits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    PortMap.put("FPGAClock", FPGA_CLOCK);
    PortMap.put("FPGATick", FPGA_TICK);
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    Regs.put("s_tick_reg", 1);
    Regs.put("s_count_reg", NR_OF_COUNTER_BITS_ID);
    return Regs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_tick_next", 1);
    Wires.put("s_count_next", NR_OF_COUNTER_BITS_ID);
    return Wires;
  }
}
