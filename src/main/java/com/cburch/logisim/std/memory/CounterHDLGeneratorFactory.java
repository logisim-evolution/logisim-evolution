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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "width";
  private static final int NR_OF_BITS_ID = -1;
  private static final String MAX_VALUE_STRING = "max_val";
  private static final int MAX_VALUE_ID = -2;
  private static final String INVERT_CLOCK_STRING = "InvertClock";
  private static final int INVERT_CLOCK_ID = -3;
  private static final String MODE_STRING = "mode";
  private static final int MODE_ID = -4;

  public CounterHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .addVector(MAX_VALUE_STRING, MAX_VALUE_ID, HDLParameters.MAP_INT_ATTRIBUTE, Counter.ATTR_MAX)
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, 
            StdAttr.EDGE_TRIGGER, AbstractFlipFlopHDLGeneratorFactory.TRIGGER_MAP)
        .add(MODE_STRING, MODE_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, Counter.ATTR_ON_GOAL,
            new HashMap<AttributeOption, Integer>() {{
              put(Counter.ON_GOAL_WRAP,0);
              put(Counter.ON_GOAL_STAY,1);
              put(Counter.ON_GOAL_CONT,2);
              put(Counter.ON_GOAL_LOAD,3);
            }}
          );
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("GlobalClock", 1);
    map.put("ClockEnable", 1);
    map.put("LoadData", NR_OF_BITS_ID);
    map.put("clear", 1);
    map.put("load", 1);
    map.put("Up_n_Down", 1);
    map.put("Enable", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer().pair("invertClock", INVERT_CLOCK_STRING);
    contents.addRemarkBlock(
        "Functionality of the counter:\\ __Load_Count_|_mode\\ ____0____0___|_halt\\ "
            + "____0____1___|_count_up_(default)\\ ____1____0___|load\\ ____1____1___|_count_down");
    if (HDL.isVHDL()) {
      contents.add("""
          CompareOut   <= s_carry;
          CountValue   <= s_counter_value;
          
          s_clock      <= GlobalClock WHEN {{invertClock}} = 0 ELSE NOT(GlobalClock); 
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
                               ELSE ClockEnable;
          
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
          assign s_clock = ({{invertClock}} == 0) ? GlobalClock : ~GlobalClock;
          
          always@(*)
          begin
          if (Up_n_Down)
             s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;
          else
             s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;
          end
          
          assign s_real_enable = ((~(load)&~(Enable))|
                                  ((mode==1)&s_carry&~(load))) ? 1'b0 : ClockEnable;
          
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

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("CountValue", NR_OF_BITS_ID);
    map.put("CompareOut", 1);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var componentInfo = (NetlistComponent) mapInfo;
    final var attrs = componentInfo.getComponent().getAttributeSet();
    if (!componentInfo.isEndConnected(Counter.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Counter\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      map.put("GlobalClock", HDL.zeroBit());
      map.put("ClockEnable", HDL.zeroBit());
    } else {
      final var clockNetName = HDL.getClockNetName(componentInfo, Counter.CK, nets);
      if (clockNetName.isEmpty()) {
        map.putAll(GetNetMap("GlobalClock", true, componentInfo, Counter.CK, nets));
        map.put("ClockEnable", HDL.oneBit());
      } else {
        var clockBusIndex = ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX;
        if (nets.requiresGlobalClockConnection()) {
          clockBusIndex = ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX;
        } else {
          if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_LOW)
            clockBusIndex = ClockHDLGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
            clockBusIndex = ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
            clockBusIndex = ClockHDLGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX;
        }
        map.put(
            "GlobalClock",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
                + HDL.BracketClose());
        map.put(
            "ClockEnable", clockNetName + HDL.BracketOpen() + clockBusIndex + HDL.BracketClose());
      }
    }
    var input = "LoadData";
    if (HDL.isVHDL()
        & (componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      input += "(0)";
    map.putAll(GetNetMap(input, true, componentInfo, Counter.IN, nets));
    map.putAll(GetNetMap("clear", true, componentInfo, Counter.CLR, nets));
    map.putAll(GetNetMap("load", true, componentInfo, Counter.LD, nets));
    map.putAll(GetNetMap("Enable", false, componentInfo, Counter.EN, nets));
    map.putAll(GetNetMap("Up_n_Down", false, componentInfo, Counter.UD, nets));
    var output = "CountValue";
    if (HDL.isVHDL()
        & (componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      output += "(0)";
    map.putAll(GetNetMap(output, true, componentInfo, Counter.OUT, nets));
    map.putAll(GetNetMap("CompareOut", true, componentInfo, Counter.CARRY, nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_next_counter_value", NR_OF_BITS_ID);
    map.put("s_carry", 1);
    map.put("s_counter_value", NR_OF_BITS_ID);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_clock", 1);
    map.put("s_real_enable", 1);
    return map;
  }
}
