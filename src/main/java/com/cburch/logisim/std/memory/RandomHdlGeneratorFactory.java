/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class RandomHdlGeneratorFactory extends AbstractHdlGeneratorFactory {
  private static final String NR_OF_BITS_STR = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String[] SEEDS_STR = {"seed0", "seed1", "seed2", "seed3"};
  private static final int[] SEEDS_ID = {-2, -3, -4, -5};

  public RandomHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID)
        // The seed parameters have 64 bit fixed
        .addVector(SEEDS_STR[0], SEEDS_ID[0], HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED0, 64)
        .addVector(SEEDS_STR[1], SEEDS_ID[1], HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED1, 64)
        .addVector(SEEDS_STR[2], SEEDS_ID[2], HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED2, 64)
        .addVector(SEEDS_STR[3], SEEDS_ID[3], HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED3, 64);
    myWires
        .addWire("s_s0", 64)
        .addWire("s_s1", 64)
        .addWire("s_s2", 64)
        .addWire("s_s3", 64)
        .addWire("s_t", 64)
        .addWire("s_ts2", 64)
        .addWire("s_ts3", 64);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Random.CK)
        .add(Port.INPUT, "clear", 1, Random.RST)
        .add(Port.INPUT, "enable", 1, Random.NXT, false)
        .add(Port.OUTPUT, "q", NR_OF_BITS_ID, Random.OUT);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>(super.getPortMap(Nets, MapInfo));
    if (MapInfo instanceof final netlistComponent comp && Hdl.isVhdl()) {
      final var nrOfBits = comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
      if (nrOfBits == 1) {
        final var outMap = map.get("q");
        map.remove("q");
        map.put("q(0)", outMap);
      }
    }
    return map;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("seed0", SEEDS_STR[0])
            .pair("seed1", SEEDS_STR[1])
            .pair("seed2", SEEDS_STR[2])
            .pair("seed3", SEEDS_STR[3])
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("GlobalClock", HdlPorts.getClockName(1))
            .pair("ClockEnable", HdlPorts.getTickName(1))
            .addRemarkBlock("This is a HDL implementation of the xoshiro256++ algorithm.\nInformation about it can be found here: https://prng.di.unimi.it")
            .empty();

    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          {{process}}(s_s0, s_s3) {{is}}     -- output network
          {{begin}}
            q <= std_logic_vector(unsigned(rotate_left(s_s0 + s_s3, 23)) + unsigned(s_s0))({{nrOfBits}} {{downto}} 0);
          {{end}} {{process}};

          {{process}}({{GlobalClock}}, enable, clear, s_s0, s_s1, s_s2, s_s3) {{is}}    -- state
          {{begin}}
              t <= shift_left(s_s1, 17);
              s_ts2 <= s_s2 {{xor}} s_s0;
              s_ts3 <= s_s3 {{xor}} s_s1;
              {{if}} (clear = '1') {{then}}
                  s_s0 <= {{seed0}};
                  s_s1 <= {{seed1}};
                  s_s2 <= {{seed2}};
                  s_s3 <= {{seed3}};
              {{elsif}} (rising_edge({{GlobalClock}}) {{and}} enable = '1') {{then}}
                  s_s0  <= s_s0 {{xor}} s_ts3;
                  s_s1  <= s_s1 {{xor}} s_ts2;
                  s_s2 <= s_ts2 {{xor}} s_t;
                  s_s3 <= rotate_left(s_ts3, 45);
              {{end}} {{if}};
          {{end}} {{process}};
          """);
    } else {
      contents.add("""

          """); // TODO
    }
    return contents.empty();
  }
}
