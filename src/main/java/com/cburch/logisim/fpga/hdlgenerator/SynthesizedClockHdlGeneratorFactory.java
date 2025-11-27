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

/*
 * Base class for generating a synthesized clock that hooks in to the clock
 * chain right after the hardware clock. This class simply generates a loopback.
 * Override this class to use a clock tile for a given vendor's chips to
 * in order to accelerate the clock for your project.
 */
public class SynthesizedClockHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final String FPGA_CLOCK = "fpgaGlobalClock";
  public static final String SYNTHESIZED_CLOCK = "s_synthesizedClock";
  public static final String HDL_IDENTIFIER = "synthesizedClockGenerator";
  public static final String HDL_DIRECTORY = "base";

  public SynthesizedClockHdlGeneratorFactory() {
    super(HDL_DIRECTORY);
    myPorts
        .add(Port.INPUT, "FPGAClock", 1, FPGA_CLOCK)
        .add(Port.OUTPUT, "SynthesizedClock", 1, SYNTHESIZED_CLOCK);
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
            .add("")
            .addRemarkBlock("Here the update logic is defined. Loop back the global clock.")
            .add("""
              {{assign}} SynthesizedClock {{=}} FPGAClock;
              """);

    return contents;
  }
}
