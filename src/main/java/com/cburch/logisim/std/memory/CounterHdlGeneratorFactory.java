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
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class CounterHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "width";
  private static final int NR_OF_BITS_ID = -1;
  private static final String MAX_VALUE_STRING = "maxVal";
  private static final int MAX_VALUE_ID = -2;
  private static final String INVERT_CLOCK_STRING = "invertClock";
  private static final int INVERT_CLOCK_ID = -3;
  private static final String MODE_STRING = "mode";
  private static final int MODE_ID = -4;

  private static final String LOAD_DATA_INPUT = "loadData";
  private static final String COUNT_DATA_OUTPUT = "countValue";

  public CounterHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .addVector(MAX_VALUE_STRING, MAX_VALUE_ID, HdlParameters.MAP_INT_ATTRIBUTE, Counter.ATTR_MAX)
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION,
            StdAttr.EDGE_TRIGGER, AbstractFlipFlopHdlGeneratorFactory.TRIGGER_MAP)
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
        .addWire("s_realEnable", 1)
        .addRegister("s_nextCounterValue", NR_OF_BITS_ID)
        .addRegister("s_carry", 1)
        .addRegister("s_counterValue", NR_OF_BITS_ID);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Counter.CK)
        .add(Port.INPUT, LOAD_DATA_INPUT, NR_OF_BITS_ID, Counter.IN)
        .add(Port.INPUT, "clear", 1, Counter.CLR)
        .add(Port.INPUT, "load", 1, Counter.LD)
        .add(Port.INPUT, "upNotDown", 1, Counter.UD)
        .add(Port.INPUT, "enable", 1, Counter.EN, false)
        .add(Port.OUTPUT, COUNT_DATA_OUTPUT, NR_OF_BITS_ID, Counter.OUT)
        .add(Port.OUTPUT, "compareOut", 1, Counter.CARRY);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var result = new TreeMap<String, String>(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof final netlistComponent compInfo && Hdl.isVhdl()) {
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
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("invertClock", INVERT_CLOCK_STRING)
        .pair("clock", HdlPorts.CLOCK)
        .pair("Tick", HdlPorts.TICK)
        .empty()
        .addRemarkBlock("""
          Functionality of the counter:
            Load Count | mode
            -----------+-------------------
              0    0   | halt
              0    1   | count up (default)
              1    0   | load
              1    1   | count down 
          """)
        .empty();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          compareOut   <= s_carry;
          countValue   <= s_counterValue;

          s_clock      <= {{clock}} {{when}} {{invertClock}} = 0 {{else}} {{not}}({{clock}});
          
          makeCarry : {{process}}(upNotDown, s_counterValue) {{is}}
          {{begin}}
             {{if}} (upNotDown = '0') {{then}}
                {{if}} (s_counterValue = std_logic_vector(to_unsigned(0,width))) {{then}}
                   s_carry <= '1';
                {{else}}
                   s_carry <= '0';
                {{end}} {{if}}; -- Down counting
             {{else}}
                {{if}} (s_counterValue = maxVal) {{then}}
                   s_carry <= '1';
                {{else}}
                   s_carry <= '0';
                {{end}} {{if}}; -- Up counting
             {{end}} {{if}};
          {{end}} {{process}} makeCarry;

          s_realEnable <= '0' {{when}} (load = '0' {{and}} enable = '0') -- Counter disabled
                                 {{or}} (mode = 1 {{and}} s_carry = '1' {{and}} load = '0') -- Stay at value situation
                               {{else}} {{Tick}};

          makeNextValue : {{process}}(load ,upNotDown ,s_counterValue ,loadData , s_carry) {{is}}
             {{variable}} v_downcount : std_logic;
          {{begin}}
             v_downcount := {{not}}(upNotDown);
             {{if}} ((load = '1') {{or}} -- load condition
                 (mode = 3 {{and}} s_carry = '1')    -- Wrap load condition
                ) {{then}} s_nextCounterValue <= loadData;
             {{else}}
                {{case}} (mode) {{is}}
                   {{when}}  0    => {{if}} (s_carry = '1') {{then}}
                                    {{if}} (v_downcount = '1') {{then}}
                                       s_nextCounterValue <= maxVal;
                                    {{else}}
                                       s_nextCounterValue <= ({{others}} => '0');
                                    {{end}} {{if}};
                                 {{else}}
                                    {{if}} (v_downcount = '1') {{then}}
                                       s_nextCounterValue <= std_logic_vector(unsigned(s_counterValue) - 1);
                                    {{else}}
                                       s_nextCounterValue <= std_logic_vector(unsigned(s_counterValue) + 1);
                                    {{end}} {{if}};
                                 {{end}} {{if}};
                  {{when}} {{others}} => {{if}} (v_downcount = '1') {{then}}
                                     s_nextCounterValue <= std_logic_vector(unsigned(s_counterValue) - 1);
                                 {{else}}
                                     s_nextCounterValue <= std_logic_vector(unsigned(s_counterValue) + 1);
                                 {{end}} {{if}};
                {{end}} {{case}};
             {{end}} {{if}};
          {{end}} {{process}} makeNextValue;

          makeFlops : {{process}}(s_clock, s_realEnable, clear, s_nextCounterValue ) {{is}}
          {{begin}}
             {{if}} (clear = '1') {{then}} s_counterValue <= ({{others}} => '0');
             {{elsif}} (rising_edge(s_clock)) {{then}}
                {{if}} (s_realEnable = '1') {{then}} s_counterValue <= s_nextCounterValue;
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeFlops;
          """);
    } else {
      contents.add("""
          assign compareOut = s_carry;
          assign countValue = s_counterValue;
          assign s_clock = ({{invertClock}} == 0) ? {{clock}} : ~{{clock}};

          always@(*)
          begin
          if (upNotDown)
             s_carry = (s_counterValue == maxVal) ? 1'b1 : 1'b0;
          else
             s_carry = (s_counterValue == 0) ? 1'b1 : 1'b0;
          end

          assign s_realEnable = ((~(load)&~(enable))|
                                  ((mode==1)&s_carry&~(load))) ? 1'b0 : {{Tick}};

          always @(*)
          begin
             if ((load)|((mode==3)&s_carry))
                s_nextCounterValue = loadData;
             else if ((mode==0)&s_carry&upNotDown)
                s_nextCounterValue = 0;
             else if ((mode==0)&s_carry)
                s_nextCounterValue = maxVal;
             else if (upNotDown)
                s_nextCounterValue = s_counterValue + 1;
             else
                s_nextCounterValue = s_counterValue - 1;
          end

          always @(posedge s_clock or posedge clear)
          begin
             if (clear) s_counterValue <= 0;
             else if (s_realEnable) s_counterValue <= s_nextCounterValue;
          end
          """);
    }
    return contents.empty();
  }
}
