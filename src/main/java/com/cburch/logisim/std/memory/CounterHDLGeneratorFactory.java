/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeOption;
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
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "width";
  private static final int NR_OF_BITS_ID = -1;
  private static final String MAX_VALUE_STRING = "max_val";
  private static final int MAX_VALUE_ID = -2;
  private static final String INVERT_CLOCK_STRING = "InvertClock";
  private static final int INVERT_CLOCK_ID = -3;
  private static final String MODE_STRING = "mode";
  private static final int MODE_ID = -4;

  private static final String LOAD_DATA_INPUT = "LoadData";
  private static final String COUNT_DATA_OUTPUT = "CountValue";

  public CounterHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .addVector(MAX_VALUE_STRING, MAX_VALUE_ID, HdlParameters.MAP_INT_ATTRIBUTE, Counter.ATTR_MAX)
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION,
            StdAttr.EDGE_TRIGGER, AbstractFlipFlopHDLGeneratorFactory.TRIGGER_MAP)
        .add(MODE_STRING, MODE_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Counter.ATTR_ON_GOAL,
            new HashMap<AttributeOption, Integer>() {{
              put(Counter.ON_GOAL_WRAP, 0);
              put(Counter.ON_GOAL_STAY, 1);
              put(Counter.ON_GOAL_CONT, 2);
              put(Counter.ON_GOAL_LOAD, 3);
            }}
        );
    myWires
        .addWire("s_clock", 1)
        .addWire("s_real_enable", 1)
        .addRegister("s_next_counter_value", NR_OF_BITS_ID)
        .addRegister("s_carry", 1)
        .addRegister("s_counter_value", NR_OF_BITS_ID);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Counter.CK)
        .add(Port.INPUT, LOAD_DATA_INPUT, NR_OF_BITS_ID, Counter.IN)
        .add(Port.INPUT, "clear", 1, Counter.CLR)
        .add(Port.INPUT, "load", 1, Counter.LD)
        .add(Port.INPUT, "Up_n_Down", 1, Counter.UD)
        .add(Port.INPUT, "Enable", 1, Counter.EN, false)
        .add(Port.OUTPUT, COUNT_DATA_OUTPUT, NR_OF_BITS_ID, Counter.OUT)
        .add(Port.OUTPUT, "CompareOut", 1, Counter.CARRY);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var result = new TreeMap<String, String>();
    result.putAll(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof netlistComponent && Hdl.isVhdl()) {
      final var compInfo = (netlistComponent) mapInfo;
      final var nrOfBits = compInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
      if (nrOfBits == 1) {
        final var mappedInputData = result.get(LOAD_DATA_INPUT);
        final var mappedOutputData = result.get(COUNT_DATA_OUTPUT);
        result.remove(LOAD_DATA_INPUT);
        result.remove(COUNT_DATA_OUTPUT);
        result.put(LineBuffer.formatHdl("{{1}}{{<}}0{{>}}", LOAD_DATA_INPUT), mappedInputData);
        result.put(LineBuffer.formatHdl("{{1}}{{<}}0{{>}}", COUNT_DATA_OUTPUT), mappedOutputData);
      }
    }
    return result;
  }

  @Override
  public List<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("invertClock", INVERT_CLOCK_STRING)
        .pair("clock", HdlPorts.CLOCK)
        .pair("Tick", HdlPorts.TICK);
    contents.addRemarkBlock(
        "Functionality of the counter:\\ __Load_Count_|_mode\\ ____0____0___|_halt\\ "
            + "____0____1___|_count_up_(default)\\ ____1____0___|load\\ ____1____1___|_count_down");
    if (Hdl.isVhdl()) {
      contents.add("""
          CompareOut   <= s_carry;
          CountValue   <= s_counter_value;

          s_clock      <= {{clock}} WHEN {{invertClock}} = 0 ELSE NOT({{clock}});
          make_carry : PROCESS( Up_n_Down,
                                s_counter_value )
          BEGIN
             IF (Up_n_Down = '0') THEN
                IF (s_counter_value = std_logic_vector(to_unsigned(0,width))) THEN
                   s_carry <= '1';
                ELSE
                   s_carry <= '0';
                END IF; -- Down counting
             ELSE
                IF (s_counter_value = max_val) THEN
                   s_carry <= '1';
                ELSE
                   s_carry <= '0';
                END IF; -- Up counting
             END IF;
          END PROCESS make_carry;

          s_real_enable <= '0' WHEN (load = '0' AND enable = '0') -- Counter disabled
                                 OR (mode = 1 AND s_carry = '1' AND load = '0') -- Stay at value situation
                               ELSE {{Tick}};

          make_next_value : PROCESS( load , Up_n_Down , s_counter_value ,
                                     LoadData , s_carry )
             VARIABLE v_downcount : std_logic;
          BEGIN
             v_downcount := NOT(Up_n_Down);
             IF ((load = '1') OR -- load condition
                 (mode = 3 AND s_carry = '1')    -- Wrap load condition
                ) THEN s_next_counter_value <= LoadData;
             ELSE
                CASE (mode) IS
                   WHEN  0    => IF (s_carry = '1') THEN
                                    IF (v_downcount = '1') THEN
                                       s_next_counter_value <= max_val;
                                    ELSE
                                       s_next_counter_value <= (OTHERS => '0');
                                    END IF;
                                 ELSE
                                    IF (v_downcount = '1') THEN
                                       s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);
                                    ELSE
                                       s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);
                                    END IF;
                                 END IF;
                  WHEN OTHERS => IF (v_downcount = '1') THEN
                                     s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);
                                 ELSE
                                     s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);
                                 END IF;
                END CASE;
             END IF;
          END PROCESS make_next_value;

          make_flops : PROCESS( s_clock , s_real_enable , clear , s_next_counter_value )
          BEGIN
             IF (clear = '1') THEN s_counter_value <= (OTHERS => '0');
             ELSIF (rising_edge(s_clock)) THEN
                IF (s_real_enable = '1') THEN s_counter_value <= s_next_counter_value;
                END IF;
             END IF;
          END PROCESS make_flops;
          """);
    } else {
      contents.add("""

          assign CompareOut = s_carry;
          assign CountValue = s_counter_value;
          assign s_clock = ({{invertClock}} == 0) ? {{clock}} : ~{{clock}};

          always@(*)
          begin
          if (Up_n_Down)
             s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;
          else
             s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;
          end

          assign s_real_enable = ((~(load)&~(Enable))|
                                  ((mode==1)&s_carry&~(load))) ? 1'b0 : {{Tick}};

          always @(*)
          begin
             if ((load)|((mode==3)&s_carry))
                s_next_counter_value = LoadData;
             else if ((mode==0)&s_carry&Up_n_Down)
                s_next_counter_value = 0;
             else if ((mode==0)&s_carry)
                s_next_counter_value = max_val;
             else if (Up_n_Down)
                s_next_counter_value = s_counter_value + 1;
             else
                s_next_counter_value = s_counter_value - 1;
          end

          always @(posedge s_clock or posedge clear)
          begin
             if (clear) s_counter_value <= 0;
             else if (s_real_enable) s_counter_value <= s_next_counter_value;
          end

          """);
    }
    return contents.getWithIndent();
  }
}
