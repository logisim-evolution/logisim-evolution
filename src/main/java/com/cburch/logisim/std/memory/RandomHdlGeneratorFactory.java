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
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class RandomHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  static final long HDL_DEFAULT_SEED = 0x5DEECE66DL;

  private static final String NR_OF_BITS_STR = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String[] INITIAL_STATE_STR = {
    "initialState0", "initialState1", "initialState2", "initialState3"
  };
  private static final int[] INITIAL_STATE_ID = {-2, -3, -4, -5};

  public RandomHdlGeneratorFactory() {
    super();
    myParametersList.add(NR_OF_BITS_STR, NR_OF_BITS_ID);
    for (var i = 0; i < INITIAL_STATE_STR.length; i++) {
      myParametersList.addVectorWithFixedWidth(INITIAL_STATE_STR[i], INITIAL_STATE_ID[i], 64);
    }
    myWires
        .addWire("s_sum", 64)
        .addWire("s_rotatedSum", 64)
        .addWire("s_result", 64)
        .addWire("s_shiftedState1", 64)
        .addWire("s_nextState0", 64)
        .addWire("s_nextState1", 64)
        .addWire("s_nextState2", 64)
        .addWire("s_nextState3", 64)
        .addWire("s_state3XorState1", 64)
        .addRegister("s_state0", 64)
        .addRegister("s_state1", 64)
        .addRegister("s_state2", 64)
        .addRegister("s_state3", 64)
        .addRegister("s_resetReg", 3);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Random.CK)
        .add(Port.INPUT, "clear", 1, Random.RST)
        .add(Port.INPUT, "enable", 1, Random.NXT, false)
        .add(Port.OUTPUT, "q", NR_OF_BITS_ID, Random.OUT);
  }

  @Override
  protected Map<String, String> getParameterMap(AttributeSet attrs) {
    if (attrs == null) return new TreeMap<>();

    final var parameters = new TreeMap<>(super.getParameterMap(attrs));
    final var configuredSeed = attrs.getValue(Random.ATTR_SEED);
    final var seed =
        configuredSeed == 0 ? HDL_DEFAULT_SEED : Integer.toUnsignedLong(configuredSeed);
    final var initialState = Xoshiro256PlusPlus.initialState(seed);
    for (var i = 0; i < initialState.length; i++) {
      parameters.put(INITIAL_STATE_STR[i], Hdl.getConstantVector(initialState[i], 64));
    }
    return parameters;
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof final netlistComponent comp && Hdl.isVhdl()) {
      final var nrOfBits =
          comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
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
            .pair("initialState0", INITIAL_STATE_STR[0])
            .pair("initialState1", INITIAL_STATE_STR[1])
            .pair("initialState2", INITIAL_STATE_STR[2])
            .pair("initialState3", INITIAL_STATE_STR[3])
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("GlobalClock", HdlPorts.getClockName(1))
            .pair("ClockEnable", HdlPorts.getTickName(1))
            .addRemarkBlock("This is an implementation of the xoshiro256++ algorithm")
            .empty();

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          q                  <= s_result(({{nrOfBits}} - 1) {{downto}} 0);
          s_sum              <= std_logic_vector(unsigned(s_state0) + unsigned(s_state3));
          s_rotatedSum       <= s_sum(40 {{downto}} 0)&s_sum(63 {{downto}} 41);
          s_result           <= std_logic_vector(unsigned(s_rotatedSum) + unsigned(s_state0));
          s_shiftedState1    <= s_state1(46 {{downto}} 0)&"00000000000000000";
          s_state3XorState1  <= s_state3 {{xor}} s_state1;
          s_nextState0       <= s_state0 {{xor}} s_state3XorState1;
          s_nextState1       <= s_state1 {{xor}} s_state2 {{xor}} s_state0;
          s_nextState2       <= s_state2 {{xor}} s_state0 {{xor}} s_shiftedState1;
          s_nextState3       <= s_state3XorState1(18 {{downto}} 0)&
                                s_state3XorState1(63 {{downto}} 19);

          updateState : {{process}}({{GlobalClock}}) {{is}}
          {{begin}}
             {{if}} rising_edge({{GlobalClock}}) {{then}}
                {{case}} s_resetReg {{is}}
                   {{when}} "010" =>
                      {{if}} clear = '1' {{then}}
                         s_state0   <= {{initialState0}};
                         s_state1   <= {{initialState1}};
                         s_state2   <= {{initialState2}};
                         s_state3   <= {{initialState3}};
                         s_resetReg <= "001";
                      {{elsif}} {{ClockEnable}} = '1' {{and}} enable = '1' {{then}}
                         s_state0 <= s_nextState0;
                         s_state1 <= s_nextState1;
                         s_state2 <= s_nextState2;
                         s_state3 <= s_nextState3;
                      {{end}} {{if}};
                   {{when}} "001" =>
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= "101";
                   {{when}} "101" =>
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      {{if}} clear = '1' {{then}}
                         s_resetReg <= "001";
                      {{else}}
                         s_resetReg <= "010";
                      {{end}} {{if}};
                   {{when}} {{others}} =>
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= "001";
                {{end}} {{case}};
             {{end}} {{if}};
          {{end}} {{process}} updateState;
          """);
    } else {
      contents.add("""
          assign q                 = s_result;
          assign s_sum             = s_state0 + s_state3;
          assign s_rotatedSum      = {s_sum[40:0], s_sum[63:41]};
          assign s_result          = s_rotatedSum + s_state0;
          assign s_shiftedState1   = {s_state1[46:0], 17'b0};
          assign s_state3XorState1 = s_state3 ^ s_state1;
          assign s_nextState0      = s_state0 ^ s_state3XorState1;
          assign s_nextState1      = s_state1 ^ s_state2 ^ s_state0;
          assign s_nextState2      = s_state2 ^ s_state0 ^ s_shiftedState1;
          assign s_nextState3      = {s_state3XorState1[18:0], s_state3XorState1[63:19]};

          always @(posedge {{GlobalClock}})
          begin
             case (s_resetReg)
                3'b010:
                   if (clear)
                   begin
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= 3'b001;
                   end
                   else if ({{ClockEnable}} && enable)
                   begin
                      s_state0 <= s_nextState0;
                      s_state1 <= s_nextState1;
                      s_state2 <= s_nextState2;
                      s_state3 <= s_nextState3;
                   end
                3'b001:
                   begin
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= 3'b101;
                   end
                3'b101:
                   begin
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= clear ? 3'b001 : 3'b010;
                   end
                default:
                   begin
                      s_state0   <= {{initialState0}};
                      s_state1   <= {{initialState1}};
                      s_state2   <= {{initialState2}};
                      s_state3   <= {{initialState3}};
                      s_resetReg <= 3'b001;
                   end
             endcase
          end
          """);
    }
    return contents.empty();
  }
}
