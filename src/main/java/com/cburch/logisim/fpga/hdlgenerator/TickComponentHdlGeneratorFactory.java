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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class TickComponentHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private final long fpgaClockFrequency;
  private final double tickFrequency;
  private static final String RELOAD_VALUE_STRING = "reloadValue";
  private static final Integer RELOAD_VALUE_ID = -1;
  private static final String NR_OF_COUNTER_BITS_STRING = "nrOfBits";
  private static final Integer NR_OF_COUNTER_BITS_ID = -2;

  public static final String FPGA_CLOCK = "fpgaGlobalClock";
  public static final String FPGA_TICK = "s_fpgaTick";
  public static final String HDL_IDENTIFIER = "logisimTickGenerator";
  public static final String HDL_DIRECTORY = "base";


  public TickComponentHdlGeneratorFactory(long fpga_clock_frequency, double tick_frequency) {
    super(HDL_DIRECTORY);
    fpgaClockFrequency = fpga_clock_frequency;
    tickFrequency = tick_frequency;
    final var reloadValueAcc = ((double) fpgaClockFrequency) / tickFrequency;
    var reloadValue = (long) reloadValueAcc;
    var nrOfBits = 0;
    if ((reloadValue > 0x7FFFFFFFL) | (reloadValue < 0)) reloadValue = 0x7FFFFFFFL;
    var calcValue = reloadValue;
    while (calcValue != 0) {
      nrOfBits++;
      calcValue /= 2;
    }
    myParametersList
        .add(RELOAD_VALUE_STRING, RELOAD_VALUE_ID, HdlParameters.MAP_CONSTANT, (int) reloadValue)
        .add(NR_OF_COUNTER_BITS_STRING, NR_OF_COUNTER_BITS_ID, HdlParameters.MAP_CONSTANT, nrOfBits);
    myWires
        .addWire("s_tickNext", 1)
        .addWire("s_countNext", NR_OF_COUNTER_BITS_ID)
        .addRegister("s_tickReg", 1)
        .addRegister("s_countReg", NR_OF_COUNTER_BITS_ID);
    myPorts
         .add(Port.INPUT, "FPGAClock", 1, FPGA_CLOCK)
         .add(Port.OUTPUT, "FPGATick", 1, FPGA_TICK);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var res = new TreeMap<String, String>();
    for (var port : myPorts.keySet())
      res.put(port, myPorts.getFixedMap(port));
    return res;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("nrOfCounterBits", NR_OF_COUNTER_BITS_STRING)
            .add("")
            .addRemarkBlock("Here the output is defined")
            .add(
                TheNetlist.requiresGlobalClockConnection()
                    ? "{{assign}} FPGATick {{=}} '1';"
                    : "{{assign}} FPGATick {{=}} s_tickReg;")
            .add("")
            .addRemarkBlock("Here the update logic is defined");

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          s_tickNext   <= '1' {{when}} s_countReg = std_logic_vector(to_unsigned(0, {{nrOfCounterBits}})) {{else}} '0';
          s_countNext  <= ({{others}} => '0') {{when}} s_tickReg /= '0' {{and}} s_tickReg /= '1' {{else}} -- For simulation only!
                          std_logic_vector(to_unsigned((reloadValue-1), {{nrOfCounterBits}})) {{when}} s_tickNext = '1' {{else}}
                          std_logic_vector(unsigned(s_countReg)-1);
          """).empty();
    } else {
      contents.add("""
              assign s_tickNext  = (s_countReg == 0) ? 1'b1 : 1'b0;
              assign s_countNext = (s_countReg == 0) ? reloadValue-1 : s_countReg-1;
              """)
          .empty()
          .addRemarkBlock("Here the simulation only initial is defined")
          .add("""
              initial
              begin
                 s_countReg = 0;
                 s_tickReg  = 1'b0;
              end
              """).empty();
    }
    contents.addRemarkBlock("Here the flipflops are defined");
    if (Hdl.isVhdl()) {
      contents.add("""
          makeFlipFlops : {{process}}(FPGAClock) {{is}}
          {{begin}}
             {{if}} (rising_edge(FPGAClock)) {{then}}
                s_tickReg  <= s_tickNext;
                s_countReg <= s_countNext;
             {{end}} {{if}};
          {{end}} {{process}} makeFlipFlops;
          """).empty();
    } else {
      contents.add("""
          always @(posedge FPGAClock)
          begin
              s_countReg <= s_countNext;
              s_tickReg  <= s_tickNext;
          end
          """).empty();
    }
    return contents;
  }
}
