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
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class RandomHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String SEED_STR = "Seed";
  private static final int SEED_ID = -2;

  public RandomHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID)
        // The seed parameter has 32 bits fixed
        .addVector(SEED_STR, SEED_ID, HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED, 32);
    myWires
        .addWire("s_InitSeed", 48)
        .addWire("s_reset", 1)
        .addWire("s_reset_next", 3)
        .addWire("s_mult_shift_next", 36)
        .addWire("s_seed_shift_next", 48)
        .addWire("s_mult_busy", 1)
        .addWire("s_start", 1)
        .addWire("s_mac_lo_in_1", 25)
        .addWire("s_mac_lo_in_2", 25)
        .addWire("s_mac_hi_1_next", 24)
        .addWire("s_mac_hi_in_2", 24)
        .addWire("s_busy_pipe_next", 2)
        .addRegister("s_current_seed", 48)
        .addRegister("s_reset_reg", 3)
        .addRegister("s_mult_shift_reg", 36)
        .addRegister("s_seed_shift_reg", 48)
        .addRegister("s_start_reg", 1)
        .addRegister("s_mac_lo_reg", 25)
        .addRegister("s_mac_hi_reg", 24)
        .addRegister("s_mac_hi_1_reg", 24)
        .addRegister("s_busy_pipe_reg", 2)
        .addRegister("s_output_reg", NR_OF_BITS_ID);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Random.CK)
        .add(Port.INPUT, "clear", 1, Random.RST)
        .add(Port.INPUT, "enable", 1, Random.NXT, false)
        .add(Port.OUTPUT, "Q", NR_OF_BITS_ID, Random.OUT);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    map.putAll(super.getPortMap(Nets, MapInfo));
    if (MapInfo instanceof netlistComponent && Hdl.isVhdl()) {
      final var comp = (netlistComponent) MapInfo;
      final var nrOfBits = comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
      if (nrOfBits == 1) {
        final var outMap = map.get("Q");
        map.remove("Q");
        map.put("Q(0)", outMap);
      }
    }
    return map;
  }

  @Override
  public List<String> getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("seed", SEED_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("GlobalClock", HdlPorts.getClockName(1))
            .pair("ClockEnable", HdlPorts.getTickName(1))
            .addRemarkBlock("This is a multicycle implementation of the Random Component")
            .empty();

    if (Hdl.isVhdl()) {
      contents.add("""
          Q            <= s_output_reg;
          s_InitSeed   <= X"0005DEECE66D" WHEN {{seed}} = X"00000000" ELSE
                          X"0000"&seed;
          s_reset      <= '1' WHEN s_reset_reg /= "010" ELSE '0';
          s_reset_next <= "010" WHEN (s_reset_reg = "101" OR
                                      s_reset_reg = "010") AND
                                      clear = '0' ELSE
                          "101" WHEN s_reset_reg = "001" ELSE
                          "001";
          s_start      <= '1' WHEN ({{ClockEnable}} = '1' AND enable = '1') OR
                                   (s_reset_reg = "101" AND clear = '0') ELSE '0';
          s_mult_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE
                               X"5DEECE66D" WHEN s_start_reg = '1' ELSE
                               '0'&s_mult_shift_reg(35 DOWNTO 1);
          s_seed_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE
                               s_current_seed WHEN s_start_reg = '1' ELSE
                               s_seed_shift_reg(46 DOWNTO 0)&'0';
          s_mult_busy       <= '0' WHEN s_mult_shift_reg = X"000000000" ELSE '1';

          s_mac_lo_in_1     <= (OTHERS => '0') WHEN s_start_reg = '1' OR
                                                    s_reset = '1' ELSE
                               '0'&s_mac_lo_reg(23 DOWNTO 0);
          s_mac_lo_in_2     <= '0'&X"00000B"
                                  WHEN s_start_reg = '1' ELSE
                               '0'&s_seed_shift_reg(23 DOWNTO 0)
                                  WHEN s_mult_shift_reg(0) = '1' ELSE
                               (OTHERS => '0');
          s_mac_hi_in_2     <= (OTHERS => '0') WHEN s_start_reg = '1' ELSE
                               s_mac_hi_reg;
          s_mac_hi_1_next   <= s_seed_shift_reg(47 DOWNTO 24)
                                  WHEN s_mult_shift_reg(0) = '1' ELSE
                               (OTHERS => '0');
          s_busy_pipe_next  <= "00" WHEN s_reset = '1' ELSE
                               s_busy_pipe_reg(0)&s_mult_busy;

          make_current_seed : PROCESS( {{GlobalClock}} , s_busy_pipe_reg , s_reset )
          BEGIN
             IF ({{GlobalClock}}'event AND ({{GlobalClock}} = '1')) THEN
                IF (s_reset = '1') THEN s_current_seed <= s_InitSeed;
                ELSIF (s_busy_pipe_reg = "10") THEN
                   s_current_seed <= s_mac_hi_reg&s_mac_lo_reg(23 DOWNTO 0);
                END IF;
             END IF;
          END PROCESS make_current_seed;

          make_shift_regs : PROCESS({{GlobalClock}},s_mult_shift_next,s_seed_shift_next,
                                    s_mac_lo_in_1,s_mac_lo_in_2)
          BEGIN
             IF ({{GlobalClock}}'event AND ({{GlobalClock}} = '1')) THEN
                s_mult_shift_reg <= s_mult_shift_next;
                s_seed_shift_reg <= s_seed_shift_next;
                s_mac_lo_reg     <= std_logic_vector( unsigned(s_mac_lo_in_1) + unsigned(s_mac_lo_in_2) );
                s_mac_hi_1_reg   <= s_mac_hi_1_next;
                s_mac_hi_reg     <= std_logic_vector( unsigned(s_mac_hi_1_reg) + unsigned(s_mac_hi_in_2) +
                                       unsigned(s_mac_lo_reg(24 DOWNTO 24)) );
                s_busy_pipe_reg  <= s_busy_pipe_next;
             END IF;
          END PROCESS make_shift_regs;

          make_start_reg : PROCESS({{GlobalClock}},s_start)
          BEGIN
             IF ({{GlobalClock}}'event AND ({{GlobalClock}} = '1')) THEN
                s_start_reg <= s_start;
             END IF;
          END PROCESS make_start_reg;

          make_reset_reg : PROCESS({{GlobalClock}},s_reset_next)
          BEGIN
             IF ({{GlobalClock}}'event AND ({{GlobalClock}} = '1')) THEN
                s_reset_reg <= s_reset_next;
             END IF;
          END PROCESS make_reset_reg;

          make_output : PROCESS( {{GlobalClock}} , s_reset , s_InitSeed )
          BEGIN
             IF ({{GlobalClock}}'event AND ({{GlobalClock}} = '1')) THEN
                IF (s_reset = '1') THEN s_output_reg <= s_InitSeed( ({{nrOfBits}}-1) DOWNTO 0 );
                ELSIF ({{ClockEnable}} = '1' AND enable = '1') THEN
                   s_output_reg <= s_current_seed(({{nrOfBits}}+11) DOWNTO 12);
                END IF;
             END IF;
          END PROCESS make_output;
          """);
    } else {
      contents.add("""
          assign Q = s_output_reg;
          assign s_InitSeed = ({{seed}} == 0) ? 48'h5DEECE66D : {{seed}};
          assign s_reset = (s_reset_reg==3'b010) ? 1'b1 : 1'b0;
          assign s_reset_next = (( (s_reset_reg == 3'b101) | (s_reset_reg == 3'b010)) & clear)
                                ? 3'b010
                                : (s_reset_reg==3'b001) ? 3'b101 : 3'b001;
          assign s_start = (({{ClockEnable}}&enable)|((s_reset_reg == 3'b101)&clear)) ? 1'b1 : 1'b0;
          assign s_mult_shift_next = (s_reset)
                                     ? 36'd0
                                     : (s_start_reg) ? 36'h5DEECE66D : {1'b0,s_mult_shift_reg[35:1]};
          assign s_seed_shift_next = (s_reset)
                                     ? 48'd0
                                     : (s_start_reg) ? s_current_seed : {s_seed_shift_reg[46:0],1'b0};
          assign s_mult_busy = (s_mult_shift_reg == 0) ? 1'b0 : 1'b1;
          assign s_mac_lo_in_1 = (s_start_reg|s_reset) ? 25'd0 : {1'b0,s_mac_lo_reg[23:0]};
          assign s_mac_lo_in_2 = (s_start_reg) ? 25'hB
                                 : (s_mult_shift_reg[0])
                                 ? {1'b0,s_seed_shift_reg[23:0]} : 25'd0;
          assign s_mac_hi_in_2 = (s_start_reg) ? 0 : s_mac_hi_reg;
          assign s_mac_hi_1_next = (s_mult_shift_reg[0]) ? s_seed_shift_reg[47:24] : 0;
          assign s_busy_pipe_next = (s_reset) ? 2'd0 : {s_busy_pipe_reg[0],s_mult_busy};

          always @(posedge {{GlobalClock}})
          begin
             if (s_reset) s_current_seed <= s_InitSeed;
             else if (s_busy_pipe_reg == 2'b10) s_current_seed <= {s_mac_hi_reg,s_mac_lo_reg[23:0]};
          end

          always @(posedge {{GlobalClock}})
          begin
                s_mult_shift_reg <= s_mult_shift_next;
                s_seed_shift_reg <= s_seed_shift_next;
                s_mac_lo_reg     <= s_mac_lo_in_1+s_mac_lo_in_2;
                s_mac_hi_1_reg   <= s_mac_hi_1_next;
                s_mac_hi_reg     <= s_mac_hi_1_reg+s_mac_hi_in_2+s_mac_lo_reg[24];
                s_busy_pipe_reg  <= s_busy_pipe_next;
                s_start_reg      <= s_start;
                s_reset_reg      <= s_reset_next;
          end

          always @(posedge {{GlobalClock}})
          begin
             if (s_reset) s_output_reg <= s_InitSeed[({{nrOfBits}}-1):0];
             else if ({{ClockEnable}}&enable) s_output_reg <= s_current_seed[({{nrOfBits}}+11):12];
          end
          """);
    }
    return contents.getWithIndent();
  }
}
