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
import com.cburch.logisim.util.ContentBuilder;
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

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist nets,
      AttributeSet attrs,
      String componentName) {
    final var contents = new ContentBuilder();
    contents.add(FileWriter.getGenerateRemark(componentName, nets.projName()));
    if (HDL.isVHDL()) {
      contents
          .add("ARCHITECTURE NoPlatformSpecific OF SingleBitShiftReg IS")
          .add("")
          .add("   SIGNAL s_state_reg  : std_logic_vector( (%s-1) DOWNTO 0 );", NR_OF_STAGES_STR)
          .add("   SIGNAL s_state_next : std_logic_vector( (%s-1) DOWNTO 0 );", NR_OF_STAGES_STR)
          .add("")
          .add("BEGIN")
          .add("   Q        <= s_state_reg;")
          .add("   ShiftOut <= s_state_reg(" + NR_OF_STAGES_STR + "-1);")
          .add("")
          .add("   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg((%s-2) DOWNTO 0)&ShiftIn;", NR_OF_STAGES_STR)
          .add("")
          .add("   make_state : PROCESS(Clock, ShiftEnable, Tick, Reset, s_state_next, ParLoad)")
          .add("      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );")
          .add("   BEGIN")
          .add("      temp := std_logic_vector(to_unsigned(%s, 1));", ACTIVE_LEVEL_STR)
          .add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');")
          .add("      ELSIF (Clock'event AND (Clock = temp(0) )) THEN")
          .add("         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (Tick = '1')) THEN")
          .add("            s_state_reg <= s_state_next;")
          .add("         END IF;")
          .add("      END IF;")
          .add("   END PROCESS make_state;")
          .add("END NoPlatformSpecific;")
          .empty(3);
    } else {
      contents
          .add("module SingleBitShiftReg ( Reset,")
          .add("                           Tick,")
          .add("                           Clock,")
          .add("                           ShiftEnable,")
          .add("                           ParLoad,")
          .add("                           ShiftIn,")
          .add("                           D,")
          .add("                           ShiftOut,")
          .add("                           Q);")
          .add("")
          .add("   parameter %s = 1;", NR_OF_STAGES_STR)
          .add("   parameter %s = 1;", ACTIVE_LEVEL_STR)
          .add("")
          .add("   input Reset;")
          .add("   input Tick;")
          .add("   input Clock;")
          .add("   input ShiftEnable;")
          .add("   input ParLoad;")
          .add("   input ShiftIn;")
          .add("   input[%s:0] D;", NR_OF_STAGES_STR)
          .add("   output ShiftOut;")
          .add("   output[%s:0] Q;", NR_OF_STAGES_STR)
          .add("")
          .add("   wire[%s:0] s_state_next;", NR_OF_STAGES_STR)
          .add("   reg[%s:0] s_state_reg;", NR_OF_STAGES_STR)
          .add("   reg[%s:0] s_state_reg_neg_edge;", NR_OF_STAGES_STR)
          .add("")
          .add("   assign Q        = (%s) ? s_state_reg : s_state_reg_neg_edge;", ACTIVE_LEVEL_STR)
          .add("   assign ShiftOut = (%1$s) ? s_state_reg[%1$s-1] : s_state_reg_neg_edge[%1$s-1];",
              ACTIVE_LEVEL_STR)
          .add("   assign s_state_next = (ParLoad) ? D :")
          .add("                         (%1$s) ? {s_state_reg[%1$s-2:0],ShiftIn}", ACTIVE_LEVEL_STR)
          .add("                                : {s_state_reg_neg_edge[%s-2:0],ShiftIn};", NR_OF_STAGES_STR)
          .add("")
          .add("   always @(posedge Clock or posedge Reset)")
          .add("   begin")
          .add("      if (Reset) s_state_reg <= 0;")
          .add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg <= s_state_next;")
          .add("   end")
          .add("")
          .add("   always @(negedge Clock or posedge Reset)")
          .add("   begin")
          .add("      if (Reset) s_state_reg_neg_edge <= 0;")
          .add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg_neg_edge <= s_state_next;")
          .add("   end")
          .add("")
          .add("endmodule")
          .empty(3);
    }
    contents.add(super.GetArchitecture(nets, attrs, componentName));
    return contents.get();
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist nets, AttributeSet attrs) {
    final var components = new ArrayList<String>();
    components.add("   COMPONENT SingleBitShiftReg");
    components.add("      GENERIC ( " + ACTIVE_LEVEL_STR + " : INTEGER;");
    components.add("                " + NR_OF_STAGES_STR + " : INTEGER);");
    components.add("      PORT ( Reset       : IN  std_logic;");
    components.add("             Tick        : IN  std_logic;");
    components.add("             Clock       : IN  std_logic;");
    components.add("             ShiftEnable : IN  std_logic;");
    components.add("             ParLoad     : IN  std_logic;");
    components.add("             ShiftIn     : IN  std_logic;");
    components.add(
        "             D           : IN  std_logic_vector( (" + NR_OF_STAGES_STR + "-1) DOWNTO 0 );");
    components.add("             ShiftOut    : OUT std_logic;");
    components.add(
        "             Q           : OUT std_logic_vector( (" + NR_OF_STAGES_STR + "-1) DOWNTO 0 ));");
    components.add("   END COMPONENT;");
    return components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "SHIFTER";
  }

  @Override
  public ArrayList<String> GetEntity(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = new ContentBuilder();
    if (HDL.isVHDL()) {
      contents
          .add(FileWriter.getGenerateRemark(componentName, nets.projName()))
          .add(FileWriter.getExtendedLibrary())
          .add("ENTITY SingleBitShiftReg IS")
          .add("   GENERIC ( %s : INTEGER;", ACTIVE_LEVEL_STR)
          .add("             %s : INTEGER);", NR_OF_STAGES_STR)
          .add("   PORT ( Reset       : IN  std_logic;")
          .add("          Tick        : IN  std_logic;")
          .add("          Clock       : IN  std_logic;")
          .add("          ShiftEnable : IN  std_logic;")
          .add("          ParLoad     : IN  std_logic;")
          .add("          ShiftIn     : IN  std_logic;")
          .add("          D           : IN  std_logic_vector( (%s-1) DOWNTO 0 );", NR_OF_STAGES_STR)
          .add("          ShiftOut    : OUT std_logic;")
          .add("          Q           : OUT std_logic_vector( (%s-1) DOWNTO 0 ));", NR_OF_STAGES_STR)
          .add("END SingleBitShiftReg;")
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
    final var contents = new ContentBuilder();
    if (HDL.isVHDL()) {
      contents
          .add("   GenBits : FOR n IN (%s-1) DOWNTO 0 GENERATE", NR_OF_BITS_STR)
          .add("      OneBit : SingleBitShiftReg")
          .add("      GENERIC MAP ( " + ACTIVE_LEVEL_STR + " => " + ACTIVE_LEVEL_STR + ",")
          .add("                    " + NR_OF_STAGES_STR + " => " + NR_OF_STAGES_STR + " )")
          .add("      PORT MAP ( Reset       => Reset,")
          .add("                 Tick        => Tick,")
          .add("                 Clock       => Clock,")
          .add("                 ShiftEnable => ShiftEnable,")
          .add("                 ParLoad     => ParLoad,")
          .add("                 ShiftIn     => ShiftIn(n),")
          .add("                 D           => D( ((n+1) * %1$s)-1 DOWNTO (n*%1$s)),", NR_OF_STAGES_STR)
          .add("                 ShiftOut    => ShiftOut(n),")
          .add("                 Q           => Q( ((n+1) * %1$s)-1 DOWNTO (n*%1$s)));", NR_OF_STAGES_STR)
          .add("   END GENERATE genbits;");
    } else {
      contents
          .add("   genvar n;")
          .add("   generate")
          .add("      for (n = 0 ; n < " + NR_OF_BITS_STR + "; n =n+1)")
          .add("      begin:Bit")
          .add("         SingleBitShiftReg #(.%1$s(%1$s),", ACTIVE_LEVEL_STR)
          .add("                             .%1$s(%1$s))", NR_OF_STAGES_STR)
          .add("            OneBit (.Reset(Reset),")
          .add("                    .Tick(Tick),")
          .add("                    .Clock(Clock),")
          .add("                    .ShiftEnable(ShiftEnable),")
          .add("                    .ParLoad(ParLoad),")
          .add("                    .ShiftIn(ShiftIn[n]),")
          .add("                    .D(D[((n+1)*%1$s)-1:(n*%1$s)]),", NR_OF_STAGES_STR)
          .add("                    .ShiftOut(ShiftOut[n]),")
          .add("                    .Q(Q[((n+1)*%1$s)-1:(n*%1$s)]));", NR_OF_STAGES_STR)
          .add("      end")
          .add("   endgenerate");
    }
    return contents.get();
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
    final var attrs = componentInfo.GetComponent().getAttributeSet();
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
    final var attrs = comp.GetComponent().getAttributeSet();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    if (!comp.EndIsConnected(ShiftRegister.CK)) {
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
      if (nets.RequiresGlobalClockConnection()) {
        map.put(
            "Tick",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
      } else {
        if (activeLow)
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NegativeEdgeTickIndex
                  + HDL.BracketClose());
        else
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                  + HDL.BracketClose());
      }
      map.put(
          "Clock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GlobalClockIndex
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
                  + ClockHDLGeneratorFactory.InvertedDerivedClockIndex
                  + HDL.BracketClose());
        else
          map.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DerivedClockIndex
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
