/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;
import java.util.Map;

public class AbstractFlipFlopHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String INVERT_CLOCK_STRING = "invertClockEnable";
  private static final int INVERT_CLOCK_ID = -1;
  private final int nrOfInputs;

  public static final Map<AttributeOption, Integer> TRIGGER_MAP = new HashMap<>() {{
        put(StdAttr.TRIG_HIGH, 0);
        put(StdAttr.TRIG_LOW, 1);
        put(StdAttr.TRIG_FALLING, 1);
        put(StdAttr.TRIG_RISING, 0);
      }};

  public AbstractFlipFlopHdlGeneratorFactory(int numInputs, Attribute<AttributeOption> triggerAttr) {
    super();
    nrOfInputs = numInputs;
    myParametersList
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, triggerAttr, TRIGGER_MAP);
    myWires
        .addWire("s_clock", 1)
        .addWire("s_nextState", 1)
        .addRegister("s_currentState", 1);
    myPorts
        .add(Port.INPUT, "reset", 1, nrOfInputs + 3)
        .add(Port.INPUT, "preset", 1, nrOfInputs + 4)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, nrOfInputs)
        .add(Port.OUTPUT, "q", 1, nrOfInputs + 1)
        .add(Port.OUTPUT, "qBar", 1, nrOfInputs + 2);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    contents
        .pair("invertClock", INVERT_CLOCK_STRING)
        .pair("Clock", HdlPorts.CLOCK)
        .pair("Tick", HdlPorts.TICK)
        .empty()
        .addRemarkBlock("Here the output signals are defined")
        .add("""
             {{assign}}q       {{=}}s_currentState;
             {{assign}}qBar    {{=}}{{not}}(s_currentState);
             """);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords()
          .add("s_clock {{=}}{{Clock}} {{when}} {{invertClock}} = 0 {{else}} {{not}}({{Clock}});")
          .empty();
    } else {
      contents
          .add("assign s_clock {{=}}({{invertClock}} == 0) ? {{Clock}} : ~{{Clock}};")
          .empty()
          .addRemarkBlock("Here the initial register value is defined; for simulation only")
          .add("""
               initial
               begin
                  s_currentState = 0;
               end
               """)
          .empty();
    }
    contents
        .addRemarkBlock("Here the update logic is defined")
        .add(getUpdateLogic())
        .empty()
        .addRemarkBlock("Here the actual state register is defined");
    if (Hdl.isVhdl()) {
      contents.add("""
          makeMemory : {{process}}( s_clock , reset , preset , {{Tick}} , s_nextState ) {{is}}
          {{begin}}
             {{if}} (reset = '1') {{then}} s_currentState <= '0';
             {{elsif}} (preset = '1') {{then}} s_currentState <= '1';
          """);
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("   {{elsif}} (rising_edge(s_clock)) {{then}}");
      } else {
        contents.add("   {{elsif}} (s_clock = '1') {{then}}");
      }
      contents.add("""
                {{if}} ({{Tick}} = '1') {{then}}
                   s_currentState <= s_nextState;
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeMemory;
          """);
    } else {
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("""
            always @(posedge reset or posedge preset or posedge s_clock)
            begin
               if (reset) s_currentState <= 1'b0;
               else if (preset) s_currentState <= 1'b1;
               else if ({{Tick}}) s_currentState <= s_nextState;
            end
            """);
      } else {
        contents
            .add("""
                always @(*)
                begin
                   if (reset) s_currentState <= 1'b0;
                   else if (preset) s_currentState <= 1'b1;
                   else if ({{Tick}} & (s_clock == 1'b1)) s_currentState <= s_nextState;
                end
                """);
      }
    }
    return contents.empty();
  }

  public LineBuffer getUpdateLogic() {
    return LineBuffer.getHdlBuffer();
  }
}
