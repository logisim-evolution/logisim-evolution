/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShiftRegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ACTIVE_LEVEL_STR = "ActiveLevel";
  private static final int ActiveLevelId = -1;
  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NrOfBitsId = -2;
  private static final String NR_OF_STAGES_STR = "NrOfStages";
  private static final int NrOfStagesId = -3;
  private static final String NrOfParBitsStr = "NrOfParBits";
  private static final int NrOfParBitsId = -4;

  private LineBuffer.Pairs sharedPairs =
      new LineBuffer.Pairs() {
        {
          add("nrOfStages", NR_OF_STAGES_STR);
          add("activeLevel", ACTIVE_LEVEL_STR);
        }
      };

  @Override
  public ArrayList<String> GetArchitecture(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents =
        (new LineBuffer(sharedPairs))
            .add(FileWriter.getGenerateRemark(componentName, nets.projName()));
    if (HDL.isVHDL()) {
      contents
          .addLines(
              "ARCHITECTURE NoPlatformSpecific OF SingleBitShiftReg IS",
              "",
              "   SIGNAL s_state_reg  : std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );",
              "   SIGNAL s_state_next : std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );",
              "",
              "BEGIN",
              "   Q        <= s_state_reg;",
              "   ShiftOut <= s_state_reg({{nrOfStages}}-1);",
              "",
              "   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg(({{nrOfStages}}-2) DOWNTO 0)&ShiftIn;",
              "",
              "   make_state : PROCESS(Clock, ShiftEnable, Tick, Reset, s_state_next, ParLoad)",
              "      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );",
              "   BEGIN",
              "      temp := std_logic_vector(to_unsigned({{activeLevel}}, 1));",
              "      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');",
              "      ELSIF (Clock'event AND (Clock = temp(0) )) THEN",
              "         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (Tick = '1')) THEN",
              "            s_state_reg <= s_state_next;",
              "         END IF;",
              "      END IF;",
              "   END PROCESS make_state;",
              "END NoPlatformSpecific;")
          .empty(3);
    } else {
      contents
          .addLines(
              "module SingleBitShiftReg ( Reset,",
              "                           Tick,",
              "                           Clock,",
              "                           ShiftEnable,",
              "                           ParLoad,",
              "                           ShiftIn,",
              "                           D,",
              "                           ShiftOut,",
              "                           Q);",
              "",
              "   parameter {{nrOfStages}} = 1;",
              "   parameter {{activeLevel}} = 1;",
              "",
              "   input Reset;",
              "   input Tick;",
              "   input Clock;",
              "   input ShiftEnable;",
              "   input ParLoad;",
              "   input ShiftIn;",
              "   input[{{nrOfStages}}:0] D;",
              "   output ShiftOut;",
              "   output[{{nrOfStages}}:0] Q;",
              "",
              "   wire[{{nrOfStages}}:0] s_state_next;",
              "   reg[{{nrOfStages}}:0] s_state_reg;",
              "   reg[{{nrOfStages}}:0] s_state_reg_neg_edge;",
              "",
              "   assign Q        = ({{activeLevel}}) ? s_state_reg : s_state_reg_neg_edge;",
              "   assign ShiftOut = ({{activeLevel}}) ? s_state_reg[{{activeLevel}}-1] : s_state_reg_neg_edge[{{activeLevel}}-1];",
              "   assign s_state_next = (ParLoad) ? D :",
              "                         ({{activeLevel}}) ? {s_state_reg[{{activeLevel}}-2:0],ShiftIn}",
              "                                           : {s_state_reg_neg_edge[{{nrOfStages}}-2:0],ShiftIn};",
              "",
              "   always @(posedge Clock or posedge Reset)",
              "   begin",
              "      if (Reset) s_state_reg <= 0;",
              "      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg <= s_state_next;",
              "   end",
              "",
              "   always @(negedge Clock or posedge Reset)",
              "   begin",
              "      if (Reset) s_state_reg_neg_edge <= 0;",
              "      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg_neg_edge <= s_state_next;",
              "   end",
              "",
              "endmodule")
          .empty(3);
    }
    contents.add(super.GetArchitecture(nets, attrs, componentName));
    return contents.get();
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist nets, AttributeSet attrs) {
    return (new LineBuffer(sharedPairs))
        .addLines(
            "COMPONENT SingleBitShiftReg",
            "   GENERIC ( {{activeLevel}} : INTEGER;",
            "             {{nrOfStages}}  : INTEGER );",
            "   PORT ( Reset       : IN  std_logic;",
            "          Tick        : IN  std_logic;",
            "          Clock       : IN  std_logic;",
            "          ShiftEnable : IN  std_logic;",
            "          ParLoad     : IN  std_logic;",
            "          ShiftIn     : IN  std_logic;",
            "          D           : IN  std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );",
            "          ShiftOut    : OUT std_logic;",
            "          Q           : OUT std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 ));",
            "END COMPONENT;")
        .getWithIndent();
  }

  @Override
  public String getComponentStringIdentifier() {
    return "SHIFTER";
  }

  @Override
  public ArrayList<String> GetEntity(Netlist nets, AttributeSet attrs, String componentName) {

    final var contents = new LineBuffer(sharedPairs);
    if (HDL.isVHDL()) {
      contents
          .add(FileWriter.getGenerateRemark(componentName, nets.projName()))
          .add(FileWriter.getExtendedLibrary())
          .addLines(
              "ENTITY SingleBitShiftReg IS",
              "   GENERIC ( {{activeLevel}} : INTEGER;",
              "             {{nrOfStages}}  : INTEGER);",
              "   PORT ( Reset       : IN  std_logic;",
              "          Tick        : IN  std_logic;",
              "          Clock       : IN  std_logic;",
              "          ShiftEnable : IN  std_logic;",
              "          ParLoad     : IN  std_logic;",
              "          ShiftIn     : IN  std_logic;",
              "          D           : IN  std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 );",
              "          ShiftOut    : OUT std_logic;",
              "          Q           : OUT std_logic_vector( ({{nrOfStages}}-1) DOWNTO 0 ));",
              "END SingleBitShiftReg;")
          .empty(3);
    }
    contents.add(super.GetEntity(nets, attrs, componentName));
    return contents.get();
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Reset", 1);
    map.put("Tick", 1);
    map.put("Clock", 1);
    map.put("ShiftEnable", 1);
    map.put("ParLoad", 1);
    map.put("ShiftIn", NrOfBitsId);
    map.put("D", NrOfParBitsId);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new LineBuffer(sharedPairs);
    if (HDL.isVHDL()) {
      contents.addLines(
          "GenBits : FOR n IN ({{nrOfBits}}-1) DOWNTO 0 GENERATE",
          "   OneBit : SingleBitShiftReg",
          "   GENERIC MAP ( {{activeLevel}} => {{activeLevel}},",
          "                 {{nrOfStages}} => {{nrOfStages}} )",
          "   PORT MAP ( Reset       => Reset,",
          "              Tick        => Tick,",
          "              Clock       => Clock,",
          "              ShiftEnable => ShiftEnable,",
          "              ParLoad     => ParLoad,",
          "              ShiftIn     => ShiftIn(n),",
          "              D           => D( ((n+1) * {{nrOfStages}})-1 DOWNTO (n*{{nrOfStages}})),",
          "              ShiftOut    => ShiftOut(n),",
          "              Q           => Q( ((n+1) * {{nrOfStages}})-1 DOWNTO (n*{{nrOfStages}})));",
          "END GENERATE genbits;");
    } else {
      contents.addLines(
          "genvar n;",
          "generate",
          "   for (n = 0 ; n < {{nrOfBits}}; n=n+1)",
          "   begin:Bit",
          "      SingleBitShiftReg #(.{{activeLevel}}({{activeLevel}}),",
          "                          .{{nrOfStages}}({{nrOfStages}}))",
          "         OneBit (.Reset(Reset),",
          "                 .Tick(Tick),",
          "                 .Clock(Clock),",
          "                 .ShiftEnable(ShiftEnable),",
          "                 .ParLoad(ParLoad),",
          "                 .ShiftIn(ShiftIn[n]),",
          "                 .D(D[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]),",
          "                 .ShiftOut(ShiftOut[n]),",
          "                 .Q(Q[((n+1)*{{nrOfStages}})-1:(n*{{nrOfStages}})]));",
          "   end",
          "endgenerate");
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("ShiftOut", NrOfBitsId);
    map.put("Q", NrOfParBitsId);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(ActiveLevelId, ACTIVE_LEVEL_STR);
    map.put(NrOfBitsId, NR_OF_BITS_STR);
    map.put(NrOfStagesId, NR_OF_STAGES_STR);
    map.put(NrOfParBitsId, NrOfParBitsStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var attrs = componentInfo.getComponent().getAttributeSet();
    var activeLevel = 1;
    var gatedClock = false;
    var activeLow = false;
    final var clockNetName = GetClockNetName(componentInfo, ShiftRegister.CK, nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    activeLow = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    if (gatedClock && activeLow) {
      activeLevel = 0;
    }
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    map.put(ACTIVE_LEVEL_STR, activeLevel);
    map.put(NR_OF_BITS_STR, nrOfBits);
    map.put(NR_OF_STAGES_STR, nrOfStages);
    map.put(NrOfParBitsStr, nrOfBits * nrOfStages);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    var activeLow = false;
    final var attrs = comp.getComponent().getAttributeSet();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    if (!comp.isEndConnected(ShiftRegister.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Shift Register\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(comp, ShiftRegister.CK, nets);
    gatedClock = clockNetName.isEmpty();
    activeLow = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
    map.putAll(GetNetMap("Reset", true, comp, ShiftRegister.CLR, nets));
    if (hasClock && !gatedClock) {
      if (nets.requiresGlobalClockConnection()) {
        map.put(
            "Tick",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
                + HDL.BracketClose());
      } else {
        if (activeLow)
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NEGATIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
        else
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
      }
      map.put(
          "Clock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
              + HDL.BracketClose());
    } else if (!hasClock) {
      map.put("Tick", HDL.zeroBit());
      map.put("Clock", HDL.zeroBit());
    } else {
      map.put("Tick", HDL.oneBit());
      if (!gatedClock) {
        if (activeLow)
          map.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX
                  + HDL.BracketClose());
        else
          map.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX
                  + HDL.BracketClose());
      } else {
        map.put("Clock", GetNetName(comp, ShiftRegister.CK, true, nets));
      }
    }
    map.putAll(GetNetMap("ShiftEnable", false, comp, ShiftRegister.SH, nets));
    if (hasParallelLoad) {
      map.putAll(GetNetMap("ParLoad", true, comp, ShiftRegister.LD, nets));
    } else {
      map.put("ParLoad", HDL.zeroBit());
    }
    var shiftName = "ShiftIn";
    if (HDL.isVHDL() & (nrOfBits == 1)) shiftName += "(0)";
    map.putAll(GetNetMap(shiftName, true, comp, ShiftRegister.IN, nets));
    if (hasParallelLoad) {
      final var vector = new StringBuilder();
      if (nrOfBits == 1) {
        if (HDL.isVHDL()) {
          for (var i = 0; i < nrOfStages; i++) {
            map.putAll(
                GetNetMap(
                    "D" + HDL.BracketOpen() + i + HDL.BracketClose(),
                    true,
                    comp,
                    6 + 2 * i,
                    nets));
          }
          int nrOfOutStages = nrOfStages - 1;
          if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC)
            nrOfOutStages = nrOfStages;
          for (var i = 0; i < nrOfOutStages; i++) {
            map.putAll(
                GetNetMap(
                    "Q" + HDL.BracketOpen() + i + HDL.BracketClose(),
                    true,
                    comp,
                    7 + 2 * i,
                    nets));
            map.put("Q" + HDL.BracketOpen() + (nrOfStages - 1) + HDL.BracketClose(), "OPEN");
          }
        } else {
          for (var i = nrOfStages - 1; i >= 0; i--) {
            if (vector.length() != 0) vector.append(",");
            vector.append(GetNetName(comp, 6 + 2 * i, true, nets));
          }
          map.put("D", vector.toString());
          vector.setLength(0);
          vector.append("open");
          for (var i = nrOfStages - 2; i >= 0; i--) {
            if (vector.length() != 0) vector.append(",");
            vector.append(GetNetName(comp, 7 + 2 * i, true, nets));
          }
          map.put("Q", vector.toString());
        }
      } else {
        if (HDL.isVHDL()) {
          for (var bit = 0; bit < nrOfBits; bit++) {
            for (var i = 0; i < nrOfStages; i++) {
              map.put(
                  "D" + HDL.BracketOpen() + (bit * nrOfStages + i) + HDL.BracketClose(),
                  GetBusEntryName(comp, 6 + 2 * i, true, bit, nets));
            }
          }
          for (var bit = 0; bit < nrOfBits; bit++) {
            for (var i = 0; i < nrOfStages - 1; i++) {
              map.put(
                  "Q" + HDL.BracketOpen() + (bit * nrOfStages + i) + HDL.BracketClose(),
                  GetBusEntryName(comp, 7 + 2 * i, true, bit, nets));
            }
            map.put(
                "Q" + HDL.BracketOpen() + ((bit + 1) * nrOfStages - 1) + HDL.BracketClose(),
                "OPEN");
          }
        } else {
          vector.setLength(0);
          for (var bit = nrOfBits - 1; bit >= 0; bit--) {
            for (var i = nrOfStages - 1; i >= 0; i--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(GetBusEntryName(comp, 6 + 2 * i, true, bit, nets));
            }
          }
          map.put("D", vector.toString());
          vector.setLength(0);
          for (var bit = nrOfBits - 1; bit >= 0; bit--) {
            if (vector.length() != 0) vector.append(",");
            vector.append("open");
            for (var i = nrOfStages - 2; i >= 0; i--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(GetBusEntryName(comp, 7 + 2 * i, true, bit, nets));
            }
          }
          map.put("Q", vector.toString());
        }
      }
    } else {
      map.put("Q", HDL.unconnected(true));
      final var temp = new StringBuilder();
      if (HDL.isVerilog()) {
        temp.append("0");
      } else {
        temp.append("\"");
        temp.append("0".repeat(nrOfBits * nrOfStages));
        temp.append("\"");
      }
      map.put("D", temp.toString());
    }
    var shiftOut = "ShiftOut";
    if (HDL.isVHDL() & (nrOfBits == 1)) shiftOut += "(0)";
    map.putAll(GetNetMap(shiftOut, true, comp, ShiftRegister.OUT, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module sub-directory where the HDL code is
     * placed
     */
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
