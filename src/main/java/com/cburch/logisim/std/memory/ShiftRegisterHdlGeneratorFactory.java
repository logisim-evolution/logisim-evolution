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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShiftRegisterHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NEGATE_CLOCK_STRING = "negateClock";
  private static final int NEGATE_CLOCK_ID = -1;
  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -2;

  public ShiftRegisterHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NEGATE_CLOCK_STRING, NEGATE_CLOCK_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, StdAttr.EDGE_TRIGGER, AbstractFlipFlopHdlGeneratorFactory.TRIGGER_MAP)
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, ShiftRegister.CK)
        .add(Port.INPUT, "reset", 1, ShiftRegister.CLR)
        .add(Port.INPUT, "shiftEnable", 1, ShiftRegister.SH)
        .add(Port.INPUT, "shiftIn", NR_OF_BITS_ID, ShiftRegister.IN);
    if (hasParallelLoad) {
      final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
      final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      myPorts.add(Port.INPUT, "parLoad", 1, ShiftRegister.LD);
      for (var idx = 0; idx < nrOfStages; idx++) {
        myPorts.add(Port.INPUT, String.format("dIn%d", idx), nrOfBits, )
      }
    } else {
      myPorts.add(Port.INPUT, "parLoad", 1, Hdl.zeroBit());
    }
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof final netlistComponent comp) {
      final var attrs = comp.getComponent().getAttributeSet();
      final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
      final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
      final var vector = new StringBuilder();
      if (Hdl.isVhdl() && nrOfBits == 1) {
        final var shiftMap = map.get("shiftIn");
        final var outMap = map.get("shiftOut");
        map.remove("shiftIn");
        map.remove("shiftOut");
        map.put("shiftIn(0)", shiftMap);
        map.put("shiftOut(0)", outMap);
      }
      map.remove("d");
      map.remove("q");
      if (hasParallelLoad) {
        if (nrOfBits == 1) {
          if (Hdl.isVhdl()) {
            for (var stage = 0; stage < nrOfStages; stage++)
              map.putAll(Hdl.getNetMap(String.format("d(%d)", stage), true, comp, 6 + (2 * stage), nets));
            final var nrOfOutStages = attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC
                ? nrOfStages : nrOfStages - 1;
            for (var stage = 0; stage < nrOfOutStages; stage++)
              map.putAll(Hdl.getNetMap(String.format("q(%d)", stage), true, comp, 7 + (2 * stage), nets));
            map.put(String.format("q(%d)", nrOfStages - 1), "OPEN");
          } else {
            for (var stage = nrOfStages - 1; stage >= 0; stage--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(Hdl.getNetName(comp, 6 + (2 * stage), true, nets));
            }
            map.put("d", vector.toString());
            vector.setLength(0);
            vector.append("open");
            for (var stage = nrOfStages - 2; stage >= 0; stage--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(Hdl.getNetName(comp, 7 + (2 * stage), true, nets));
            }
            map.put("q", vector.toString());
          }
        } else {
          if (Hdl.isVhdl()) {
            for (var bit = 0; bit < nrOfBits; bit++) {
              for (var stage = 0; stage < nrOfStages; stage++) {
                final var index = (bit * nrOfStages) + stage;
                final var id = 6 + (2 * stage);
                map.put(String.format("d(%d)", index), Hdl.getBusEntryName(comp, id, true, bit, nets));
                if (stage == nrOfStages - 1) continue;
                map.put(String.format("q(%d)", index), Hdl.getBusEntryName(comp, id + 1, true, bit, nets));
              }
              map.put(String.format("q(%d)", ((bit + 1) * nrOfStages) - 1), "OPEN");
            }
          } else {
            vector.setLength(0);
            for (var bit = nrOfBits - 1; bit >= 0; bit--) {
              for (var stage = nrOfStages - 1; stage >= 0; stage--) {
                if (vector.length() != 0) vector.append(",");
                vector.append(Hdl.getBusEntryName(comp, 6 + (2 * stage), true, bit, nets));
              }
            }
            map.put("d", vector.toString());
            vector.setLength(0);
            for (var bit = nrOfBits - 1; bit >= 0; bit--) {
              if (vector.length() != 0) vector.append(",");
              vector.append("open");
              for (var stage = nrOfStages - 2; stage >= 0; stage--) {
                if (vector.length() != 0) vector.append(",");
                vector.append(Hdl.getBusEntryName(comp, 7 + (2 * stage), true, bit, nets));
              }
            }
            map.put("q", vector.toString());
          }
        }
      } else {
        map.put("d", Hdl.getConstantVector(0, nrOfBits * nrOfStages));
        map.put("q", Hdl.unconnected(true));
      }
    }
    return map;
  }

  @Override
  public List<String> getArchitecture(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer()
            .pair("clock", HdlPorts.getClockName(1))
            .pair("tick", HdlPorts.getTickName(1))
            .pair("invertClock", NEGATE_CLOCK_STRING)
            .add(super.getArchitecture(nets, attrs, componentName))
            .empty(3);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords()
          .add("""
              {{architecture}} noPlatformSpecific {{of}} singleBitShiftReg {{is}}

                 {{signal}} s_stateReg  : std_logic_vector( ({{nrOfStages}}-1) {{downto}} 0 );
                 {{signal}} s_stateNext : std_logic_vector( ({{nrOfStages}}-1) {{downto}} 0 );
                 {{signal}} s_clock     : std_logic;

              {{begin}}
                 q        <= s_stateReg;
                 shiftOut <= s_stateReg({{nrOfStages}}-1);
                 s_clock  <= {{clock}} {{when}} {{invertClock}} = 0 {{else}} {{not}}({{clock}});

                 s_stateNext <= d {{when}} parLoad = '1' {{else}} s_stateReg(({{nrOfStages}}-2) {{downto}} 0)&shiftIn;

                 makeState : {{process}}(s_clock, shiftEnable, {{tick}}, reset, s_stateNext, parLoad) {{is}}
                 {{begin}}
                    {{if}} (reset = '1') {{then}} s_stateReg <= ({{others}} => '0');
                    {{elsif}} (rising_edge(s_clock)) {{then}}
                       {{if}} (((shiftEnable = '1') {{or}} (parLoad = '1')) {{and}} ({{tick}} = '1')) {{then}}
                          s_stateReg <= s_stateNext;
                       {{end}} {{if}};
                    {{end}} {{if}};
                 {{end}} {{process}} makeState;
              {{end}} noPlatformSpecific;

              """);
    } else {
      contents
          .add("""
              module singleBitShiftReg ( reset,
                                         {{tick}},
                                         {{clock}},
                                         shiftEnable,
                                         parLoad,
                                         shiftIn,
                                         d,
                                         shiftOut,
                                         q);

                 parameter {{nrOfStages}} = 1;
                 parameter {{invertClock}} = 1;

                 input reset;
                 input {{tick}};
                 input {{clock}};
                 input shiftEnable;
                 input parLoad;
                 input shiftIn;
                 input[{{nrOfStages}}:0] d;
                 output shiftOut;
                 output[{{nrOfStages}}:0] q;

                 wire[{{nrOfStages}}:0] s_stateNext;
                 wire s_clock;
                 reg[{{nrOfStages}}:0] s_stateReg;

                 assign q        = s_stateReg;
                 assign shiftOut = s_stateReg[{{nrOfStages}}-1];
                 assign s_clock  = {{invertClock}} == 0 ? {{clock}} : ~{{clock}};
                 assign s_stateNext = (parLoad) ? d : {s_stateReg[{{nrOfStages}}-2:0],shiftIn};

                 always @(posedge s_clock or posedge reset)
                 begin
                    if (reset) s_stateReg <= 0;
                    else if ((shiftEnable|parLoad)&{{tick}}) s_stateReg <= s_stateNext;
                 end

              endmodule
              """);
    }
    contents.empty();
    return contents.get();
  }

  @Override
  public List<String> getEntity(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents
          .add(super.getEntity(nets, attrs, componentName))
          .empty();
    }
    return contents.get();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1))
        .pair("invertClock", NEGATE_CLOCK_STRING)
        .pair("nrOfBits", NR_OF_BITS_STRING);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          genBits : {{for}} n {{in}} ({{nrOfBits}}-1) {{downto}} 0 {{generate}}
             OneBit : singleBitShiftReg
             {{generic}} {{map}} ( {{invertClock}} => {{invertClock}},
                           {{nrOfStages}} => {{nrOfStages}} )
             {{port}} {{map}} ( reset       => reset,
                        {{tick}}        => {{tick}},
                        {{clock}}       => {{clock}},
                        shiftEnable => shiftEnable,
                        parLoad     => parLoad,
                        shiftIn     => shiftIn(n),
                        d           => d( ((n+1) * {{nrOfStages}})-1 {{downto}} (n*{{nrOfStages}})),
                        shiftOut    => shiftOut(n),
                        q           => q( ((n+1) * {{nrOfStages}})-1 {{downto}} (n*{{nrOfStages}})) );
          {{end}} {{generate}} genBits;
          """);
    } else {
      contents.add("""
          genvar n;
          generate
             for (n = 0 ; n < {{nrOfBits}}; n=n+1)
             begin:Bit
                singleBitShiftReg #(.{{invertClock}}({{invertClock}}),
                                    .{{nrOfStages}}({{nrOfStages}}))
                   OneBit (.reset(reset),
                           .{{tick}}({{tick}}),
                           .{{clock}}({{clock}}),
                           .shiftEnable(shiftEnable),
                           .parLoad(parLoad),
                           .shiftIn(shiftIn[n]),
                           .d(d[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]),
                           .shiftOut(shiftOut[n]),
                           .q(q[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]) );
             end
          endgenerate
          """);
    }
    return contents.empty();
  }
}
