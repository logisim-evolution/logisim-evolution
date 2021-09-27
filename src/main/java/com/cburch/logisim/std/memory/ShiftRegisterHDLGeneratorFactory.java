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
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.fpga.hdlgenerator.HDLPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShiftRegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NEGATE_CLOCK_STRING = "negateClock";
  private static final int NEGATE_CLOCK_ID = -1;
  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -2;
  private static final String NR_OF_STAGES_STR = "NrOfStages";
  private static final int NR_OF_STAGES_ID = -3;
  private static final String NR_OF_PAR_BITS_STRING = "NrOfParBits";
  private static final int NR_OF_PAR_BITS_ID = -4;

  public ShiftRegisterHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NEGATE_CLOCK_STRING, NEGATE_CLOCK_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, StdAttr.EDGE_TRIGGER, AbstractFlipFlopHDLGeneratorFactory.TRIGGER_MAP)
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID)
        .add(NR_OF_PAR_BITS_STRING, NR_OF_PAR_BITS_ID, HDLParameters.MAP_MULTIPLY, StdAttr.WIDTH, ShiftRegister.ATTR_LENGTH)
        .add(NR_OF_STAGES_STR, NR_OF_STAGES_ID, HDLParameters.MAP_INT_ATTRIBUTE, ShiftRegister.ATTR_LENGTH);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
    myPorts
        .add(Port.CLOCK, HDLPorts.getClockName(1), 1, ShiftRegister.CK)
        .add(Port.INPUT, "Reset", 1, ShiftRegister.CLR)
        .add(Port.INPUT, "ShiftEnable", 1, ShiftRegister.SH)
        .add(Port.INPUT, "ShiftIn", NR_OF_BITS_ID, ShiftRegister.IN)
        .add(Port.INPUT, "D", NR_OF_PAR_BITS_ID, "DUMMY_MAP")
        .add(Port.OUTPUT, "ShiftOut", NR_OF_BITS_ID, ShiftRegister.OUT)
        .add(Port.OUTPUT, "Q", NR_OF_PAR_BITS_ID, "DUMMY_MAP");
    if (hasParallelLoad) {
      myPorts.add(Port.INPUT, "ParLoad", 1, ShiftRegister.LD);
    } else {
      myPorts.add(Port.INPUT, "ParLoad", 1, HDL.zeroBit());
    }
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    map.putAll(super.getPortMap(nets, mapInfo));
    if (mapInfo instanceof netlistComponent) {
      final var comp = ((netlistComponent) mapInfo);
      final var attrs = comp.getComponent().getAttributeSet();
      final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
      final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
      final var vector = new StringBuilder();
      if (HDL.isVhdl() && nrOfBits == 1) {
        final var shiftMap = map.get("ShiftIn");
        final var outMap = map.get("ShiftOut");
        map.remove("ShiftIn");
        map.remove("ShiftOut");
        map.put("ShiftIn(0)", shiftMap);
        map.put("ShiftOut(0)", outMap);
      }
      map.remove("D");
      map.remove("Q");
      if (hasParallelLoad) {
        if (nrOfBits == 1) {
          if (HDL.isVhdl()) {
            for (var stage = 0; stage < nrOfStages; stage++)
              map.putAll(GetNetMap(String.format("D(%d)", stage), true, comp, 6 + 2 * stage, nets));
            final var nrOfOutStages = attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC
                ? nrOfStages : nrOfStages - 1;
            for (var stage = 0; stage < nrOfOutStages; stage++)
              map.putAll(GetNetMap(String.format("Q(%d)", stage), true, comp, 7 + 2 * stage, nets));
            map.put(String.format("Q(%d)", nrOfStages - 1), "OPEN");
          } else {
            for (var stage = nrOfStages - 1; stage >= 0; stage--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(HDL.getNetName(comp, 6 + 2 * stage, true, nets));
            }
            map.put("D", vector.toString());
            vector.setLength(0);
            vector.append("open");
            for (var stage = nrOfStages - 2; stage >= 0; stage--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(HDL.getNetName(comp, 7 + 2 * stage, true, nets));
            }
            map.put("Q", vector.toString());
          }
        } else {
          if (HDL.isVhdl()) {
            for (var bit = 0; bit < nrOfBits; bit++) {
              for (var stage = 0; stage < nrOfStages; stage++) {
                final var index = bit * nrOfStages + stage;
                final var id = 6 + 2 * stage;
                map.put(String.format("D(%d)", index), HDL.getBusEntryName(comp, id, true, bit, nets));
                if (stage == nrOfStages - 1) continue;
                map.put(String.format("Q(%d)", index), HDL.getBusEntryName(comp, id + 1, true, bit, nets));
              }
              map.put(String.format("Q(%d)", (bit + 1) * nrOfStages - 1), "OPEN");
            }
          } else {
            vector.setLength(0);
            for (var bit = nrOfBits - 1; bit >= 0; bit--) {
              for (var stage = nrOfStages - 1; stage >= 0; stage--) {
                if (vector.length() != 0) vector.append(",");
                vector.append(HDL.getBusEntryName(comp, 6 + 2 * stage, true, bit, nets));
              }
            }
            map.put("D", vector.toString());
            vector.setLength(0);
            for (var bit = nrOfBits - 1; bit >= 0; bit--) {
              if (vector.length() != 0) vector.append(",");
              vector.append("open");
              for (var stage = nrOfStages - 2; stage >= 0; stage--) {
                if (vector.length() != 0) vector.append(",");
                vector.append(HDL.getBusEntryName(comp, 7 + 2 * stage, true, bit, nets));
              }
            }
            map.put("Q", vector.toString());
          }
        }
      } else {
        map.put("D", HDL.getConstantVector(0, nrOfBits * nrOfStages));
        map.put("Q", HDL.unconnected(true));
      }
    }
    return map;
  }

  @Override
  public ArrayList<String> getArchitecture(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer()
            .pair("clock", HDLPorts.getClockName(1))
            .pair("tick", HDLPorts.getTickName(1))
            .pair("nrOfStages", NR_OF_STAGES_STR)
            .pair("invertClock", NEGATE_CLOCK_STRING)
            .add(FileWriter.getGenerateRemark(componentName, nets.projName()));
    if (HDL.isVhdl()) {
      contents
          .add("""
              ARCHITECTURE NoPlatformSpecific OF SingleBitShiftReg IS
              
                 SIGNAL s_state_reg  : std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );
                 SIGNAL s_state_next : std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );
                 SIGNAL s_clock      : std_logic;
              
              BEGIN
                 Q        <= s_state_reg;
                 ShiftOut <= s_state_reg({{nrOfStages}}-1);
                 s_clock  <= {{clock}} WHEN {{invertClock}} = 0 ELSE NOT({{clock}});
              
                 s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg(({{nrOfStages}}-2) DOWNTO 0)&ShiftIn;
              
                 make_state : PROCESS(s_clock, ShiftEnable, {{tick}}, Reset, s_state_next, ParLoad)
                 BEGIN
                    IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');
                    ELSIF (rising_edge(s_clock)) THEN
                       IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND ({{tick}} = '1')) THEN
                          s_state_reg <= s_state_next;
                       END IF;
                    END IF;
                 END PROCESS make_state;
              END NoPlatformSpecific;
              
              """);
    } else {
      contents
          .add("""
              module SingleBitShiftReg ( Reset,
                                         {{tick}},
                                         {{clock}},
                                         ShiftEnable,
                                         ParLoad,
                                         ShiftIn,
                                         D,
                                         ShiftOut,
                                         Q);
              
                 parameter {{nrOfStages}} = 1;
                 parameter {{invertClock}} = 1;
              
                 input Reset;
                 input {{tick}};
                 input {{clock}};
                 input ShiftEnable;
                 input ParLoad;
                 input ShiftIn;
                 input[{{nrOfStages}}:0] D;
                 output ShiftOut;
                 output[{{nrOfStages}}:0] Q;
              
                 wire[{{nrOfStages}}:0] s_state_next;
                 wire s_clock;
                 reg[{{nrOfStages}}:0] s_state_reg;
              
                 assign Q        = s_state_reg;
                 assign ShiftOut = s_state_reg[{{nrOfStages}}-1];
                 assign s_clock  = {{invertClock}} == 0 ? {{clock}} : ~{{clock}};
                 assign s_state_next = (ParLoad) ? D : {s_state_reg[{{nrOfStages}}-2:0],ShiftIn};
              
                 always @(posedge s_clock or posedge Reset)
                 begin
                    if (Reset) s_state_reg <= 0;
                    else if ((ShiftEnable|ParLoad)&{{tick}}) s_state_reg <= s_state_next;
                 end
              
              endmodule
              
              """);
    }
    contents.add(super.getArchitecture(nets, attrs, componentName));
    return contents.get();
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist nets, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .pair("clock", HDLPorts.getClockName(1))
        .pair("tick", HDLPorts.getTickName(1))
        .pair("nrOfStages", NR_OF_STAGES_STR)
        .pair("invertClock", NEGATE_CLOCK_STRING)
        .add("""
            COMPONENT SingleBitShiftReg
               GENERIC ( {{invertClock}} : INTEGER;
                         {{nrOfStages}}  : INTEGER );
               PORT ( Reset       : IN  std_logic;
                      {{tick}}        : IN  std_logic;
                      {{clock}}       : IN  std_logic;
                      ShiftEnable : IN  std_logic;
                      ParLoad     : IN  std_logic;
                      ShiftIn     : IN  std_logic;
                      D           : IN  std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );
                      ShiftOut    : OUT std_logic;
                      Q           : OUT std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 ) );
            END COMPONENT;
            """)
        .getWithIndent();
  }

  @Override
  public ArrayList<String> getEntity(Netlist nets, AttributeSet attrs, String componentName) {

    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HDLPorts.getClockName(1))
        .pair("tick", HDLPorts.getTickName(1))
        .pair("nrOfStages", NR_OF_STAGES_STR)
        .pair("invertClock", NEGATE_CLOCK_STRING);
    if (HDL.isVhdl()) {
      contents
          .add(FileWriter.getGenerateRemark(componentName, nets.projName()))
          .add(FileWriter.getExtendedLibrary())
          .add("""
              ENTITY SingleBitShiftReg IS
                 GENERIC ( {{invertClock}} : INTEGER;
                           {{nrOfStages}}  : INTEGER);
                 PORT ( Reset       : IN  std_logic;
                        {{tick}}        : IN  std_logic;
                        {{clock}}       : IN  std_logic;
                        ShiftEnable : IN  std_logic;
                        ParLoad     : IN  std_logic;
                        ShiftIn     : IN  std_logic;
                        D           : IN  std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );
                        ShiftOut    : OUT std_logic;
                        Q           : OUT std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 ));
              END SingleBitShiftReg;
              
              """);
    }
    contents.add(super.getEntity(nets, attrs, componentName));
    return contents.get();
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HDLPorts.getClockName(1))
        .pair("tick", HDLPorts.getTickName(1))
        .pair("nrOfStages", NR_OF_STAGES_STR)
        .pair("invertClock", NEGATE_CLOCK_STRING)
        .pair("nrOfBits", NR_OF_BITS_STR);
    if (HDL.isVhdl()) {
      contents.add("""
          GenBits : FOR n IN ({{nrOfBits}}-1) DOWNTO 0 GENERATE
             OneBit : SingleBitShiftReg
             GENERIC MAP ( {{invertClock}} => {{invertClock}},
                           {{nrOfStages}} => {{nrOfStages}} )
             PORT MAP ( Reset       => Reset,
                        {{tick}}        => {{tick}},
                        {{clock}}       => {{clock}},
                        ShiftEnable => ShiftEnable,
                        ParLoad     => ParLoad,
                        ShiftIn     => ShiftIn(n),
                        D           => D( ((n+1) * {{nrOfStages}})-1 DOWNTO (n*{{nrOfStages}})),
                        ShiftOut    => ShiftOut(n),
                        Q           => Q( ((n+1) * {{nrOfStages}})-1 DOWNTO (n*{{nrOfStages}})) );
          END GENERATE genbits;
          """);
    } else {
      contents.add("""
          genvar n;
          generate
             for (n = 0 ; n < {{nrOfBits}}; n=n+1)
             begin:Bit
                SingleBitShiftReg #(.{{invertClock}}({{invertClock}}),
                                    .{{nrOfStages}}({{nrOfStages}}))
                   OneBit (.Reset(Reset),
                           .{{tick}}({{tick}}),
                           .{{clock}}({{clock}}),
                           .ShiftEnable(ShiftEnable),
                           .ParLoad(ParLoad),
                           .ShiftIn(ShiftIn[n]),
                           .D(D[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]),
                           .ShiftOut(ShiftOut[n]),
                           .Q(Q[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]) );
             end
          endgenerate
          """);
    }
    return contents.getWithIndent();
  }
}
