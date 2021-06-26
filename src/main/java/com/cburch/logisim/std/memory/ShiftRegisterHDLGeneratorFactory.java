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
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShiftRegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ActiveLevelStr = "ActiveLevel";
  private static final int ActiveLevelId = -1;
  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -2;
  private static final String NrOfStagesStr = "NrOfStages";
  private static final int NrOfStagesId = -3;
  private static final String NrOfParBitsStr = "NrOfParBits";
  private static final int NrOfParBitsId = -4;

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist nets,
      AttributeSet attrs,
      String componentName) {
    final var contents = new ArrayList<String>();
    contents.addAll(FileWriter.getGenerateRemark(componentName, nets.projName()));
    if (HDL.isVHDL()) {
      contents.add("ARCHITECTURE NoPlatformSpecific OF SingleBitShiftReg IS");
      contents.add("");
      contents.add(
          "   SIGNAL s_state_reg  : std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      contents.add(
          "   SIGNAL s_state_next : std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      contents.add("");
      contents.add("BEGIN");
      contents.add("   Q        <= s_state_reg;");
      contents.add("   ShiftOut <= s_state_reg(" + NrOfStagesStr + "-1);");
      contents.add("");
      contents.add(
          "   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg(("
              + NrOfStagesStr
              + "-2) DOWNTO 0)&ShiftIn;");
      contents.add("");
      contents.add(
          "   make_state : PROCESS(Clock, ShiftEnable, Tick, Reset, s_state_next, ParLoad)");
      contents.add("      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );");
      contents.add("   BEGIN");
      contents.add("      temp := std_logic_vector(to_unsigned(" + ActiveLevelStr + ",1));");
      contents.add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      contents.add("      ELSIF (Clock'event AND (Clock = temp(0) )) THEN");
      contents.add("         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (Tick = '1')) THEN");
      contents.add("            s_state_reg <= s_state_next;");
      contents.add("         END IF;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_state;");
      contents.add("END NoPlatformSpecific;");
      contents.add("");
      contents.add("");
      contents.add("");
    } else {
      contents.add("module SingleBitShiftReg ( Reset,");
      contents.add("                           Tick,");
      contents.add("                           Clock,");
      contents.add("                           ShiftEnable,");
      contents.add("                           ParLoad,");
      contents.add("                           ShiftIn,");
      contents.add("                           D,");
      contents.add("                           ShiftOut,");
      contents.add("                           Q);");
      contents.add("");
      contents.add("   parameter " + NrOfStagesStr + " = 1;");
      contents.add("   parameter " + ActiveLevelStr + " = 1;");
      contents.add("");
      contents.add("   input Reset;");
      contents.add("   input Tick;");
      contents.add("   input Clock;");
      contents.add("   input ShiftEnable;");
      contents.add("   input ParLoad;");
      contents.add("   input ShiftIn;");
      contents.add("   input[" + NrOfStagesStr + ":0] D;");
      contents.add("   output ShiftOut;");
      contents.add("   output[" + NrOfStagesStr + ":0] Q;");
      contents.add("");
      contents.add("   wire[" + NrOfStagesStr + ":0] s_state_next;");
      contents.add("   reg[" + NrOfStagesStr + ":0] s_state_reg;");
      contents.add("   reg[" + NrOfStagesStr + ":0] s_state_reg_neg_edge;");
      contents.add("");
      contents.add(
          "   assign Q        = (" + ActiveLevelStr + ") ? s_state_reg : s_state_reg_neg_edge;");
      contents.add(
          "   assign ShiftOut = ("
              + ActiveLevelStr
              + ") ? s_state_reg["
              + NrOfStagesStr
              + "-1] : s_state_reg_neg_edge["
              + NrOfStagesStr
              + "-1];");
      contents.add("   assign s_state_next = (ParLoad) ? D :");
      contents.add(
          "                         ("
              + ActiveLevelStr
              + ") ? {s_state_reg["
              + NrOfStagesStr
              + "-2:0],ShiftIn} :");
      contents.add(
          "                                                {s_state_reg_neg_edge["
              + NrOfStagesStr
              + "-2:0],ShiftIn};");
      contents.add("");
      contents.add("   always @(posedge Clock or posedge Reset)");
      contents.add("   begin");
      contents.add("      if (Reset) s_state_reg <= 0;");
      contents.add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg <= s_state_next;");
      contents.add("   end");
      contents.add("");
      contents.add("   always @(negedge Clock or posedge Reset)");
      contents.add("   begin");
      contents.add("      if (Reset) s_state_reg_neg_edge <= 0;");
      contents.add(
          "      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg_neg_edge <= s_state_next;");
      contents.add("   end");
      contents.add("");
      contents.add("endmodule");
      contents.add("");
      contents.add("");
      contents.add("");
    }
    contents.addAll(super.GetArchitecture(nets, attrs, componentName));
    return contents;
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist nets, AttributeSet attrs) {
    final var components = new ArrayList<String>();
    components.add("   COMPONENT SingleBitShiftReg");
    components.add("      GENERIC ( " + ActiveLevelStr + " : INTEGER;");
    components.add("                " + NrOfStagesStr + " : INTEGER);");
    components.add("      PORT ( Reset       : IN  std_logic;");
    components.add("             Tick        : IN  std_logic;");
    components.add("             Clock       : IN  std_logic;");
    components.add("             ShiftEnable : IN  std_logic;");
    components.add("             ParLoad     : IN  std_logic;");
    components.add("             ShiftIn     : IN  std_logic;");
    components.add(
        "             D           : IN  std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
    components.add("             ShiftOut    : OUT std_logic;");
    components.add(
        "             Q           : OUT std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 ));");
    components.add("   END COMPONENT;");
    return components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "SHIFTER";
  }

  @Override
  public ArrayList<String> GetEntity(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.addAll(FileWriter.getGenerateRemark(componentName, nets.projName()));
      contents.addAll(FileWriter.getExtendedLibrary());
      contents.add("ENTITY SingleBitShiftReg IS");
      contents.add("   GENERIC ( " + ActiveLevelStr + " : INTEGER;");
      contents.add("             " + NrOfStagesStr + " : INTEGER);");
      contents.add("   PORT ( Reset       : IN  std_logic;");
      contents.add("          Tick        : IN  std_logic;");
      contents.add("          Clock       : IN  std_logic;");
      contents.add("          ShiftEnable : IN  std_logic;");
      contents.add("          ParLoad     : IN  std_logic;");
      contents.add("          ShiftIn     : IN  std_logic;");
      contents.add(
          "          D           : IN  std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      contents.add("          ShiftOut    : OUT std_logic;");
      contents.add(
          "          Q           : OUT std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 ));");
      contents.add("END SingleBitShiftReg;");
      contents.add("");
      contents.add("");
      contents.add("");
    }
    contents.addAll(super.GetEntity(nets, attrs, componentName));
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    SortedMap<String, Integer> inputs = new TreeMap<>();
    inputs.put("Reset", 1);
    inputs.put("Tick", 1);
    inputs.put("Clock", 1);
    inputs.put("ShiftEnable", 1);
    inputs.put("ParLoad", 1);
    inputs.put("ShiftIn", NrOfBitsId);
    inputs.put("D", NrOfParBitsId);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add("   GenBits : FOR n IN (" + NrOfBitsStr + "-1) DOWNTO 0 GENERATE");
      contents.add("      OneBit : SingleBitShiftReg");
      contents.add("      GENERIC MAP ( " + ActiveLevelStr + " => " + ActiveLevelStr + ",");
      contents.add("                    " + NrOfStagesStr + " => " + NrOfStagesStr + " )");
      contents.add("      PORT MAP ( Reset       => Reset,");
      contents.add("                 Tick        => Tick,");
      contents.add("                 Clock       => Clock,");
      contents.add("                 ShiftEnable => ShiftEnable,");
      contents.add("                 ParLoad     => ParLoad,");
      contents.add("                 ShiftIn     => ShiftIn(n),");
      contents.add(
          "                 D           => D( ((n+1)*"
              + NrOfStagesStr
              + ")-1 DOWNTO (n*"
              + NrOfStagesStr
              + ")),");
      contents.add("                 ShiftOut    => ShiftOut(n),");
      contents.add(
          "                 Q           => Q( ((n+1)*"
              + NrOfStagesStr
              + ")-1 DOWNTO (n*"
              + NrOfStagesStr
              + ")));");
      contents.add("   END GENERATE genbits;");
    } else {
      contents.add("   genvar n;");
      contents.add("   generate");
      contents.add("      for (n = 0 ; n < " + NrOfBitsStr + "; n =n+1)");
      contents.add("      begin:Bit");
      contents.add("         SingleBitShiftReg #(." + ActiveLevelStr + "(" + ActiveLevelStr + "),");
      contents.add("                             ." + NrOfStagesStr + "(" + NrOfStagesStr + "))");
      contents.add("            OneBit (.Reset(Reset),");
      contents.add("                    .Tick(Tick),");
      contents.add("                    .Clock(Clock),");
      contents.add("                    .ShiftEnable(ShiftEnable),");
      contents.add("                    .ParLoad(ParLoad),");
      contents.add("                    .ShiftIn(ShiftIn[n]),");
      contents.add(
          "                    .D(D[((n+1)*" + NrOfStagesStr + ")-1:(n*" + NrOfStagesStr + ")]),");
      contents.add("                    .ShiftOut(ShiftOut[n]),");
      contents.add(
          "                    .Q(Q[((n+1)*" + NrOfStagesStr + ")-1:(n*" + NrOfStagesStr + ")]));");
      contents.add("      end");
      contents.add("   endgenerate");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    SortedMap<String, Integer> outs = new TreeMap<>();
    outs.put("ShiftOut", NrOfBitsId);
    outs.put("Q", NrOfParBitsId);
    return outs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> params = new TreeMap<>();
    params.put(ActiveLevelId, ActiveLevelStr);
    params.put(NrOfBitsId, NrOfBitsStr);
    params.put(NrOfStagesId, NrOfStagesStr);
    params.put(NrOfParBitsId, NrOfParBitsStr);
    return params;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    SortedMap<String, Integer> parameterMap = new TreeMap<>();
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
    parameterMap.put(ActiveLevelStr, activeLevel);
    parameterMap.put(NrOfBitsStr, nrOfBits);
    parameterMap.put(NrOfStagesStr, nrOfStages);
    parameterMap.put(NrOfParBitsStr, nrOfBits * nrOfStages);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    var activeLow = false;
    final var attrs = componentInfo.GetComponent().getAttributeSet();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    if (!componentInfo.EndIsConnected(ShiftRegister.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Shift Register\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(componentInfo, ShiftRegister.CK, nets);
    gatedClock = clockNetName.isEmpty();
    activeLow = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    final var hasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD);
    portMap.putAll(GetNetMap("Reset", true, componentInfo, ShiftRegister.CLR, nets));
    if (hasClock && !gatedClock) {
      if (nets.RequiresGlobalClockConnection()) {
        portMap.put(
            "Tick",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
      } else {
        if (activeLow)
          portMap.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NegativeEdgeTickIndex
                  + HDL.BracketClose());
        else
          portMap.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                  + HDL.BracketClose());
      }
      portMap.put(
          "Clock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GlobalClockIndex
              + HDL.BracketClose());
    } else if (!hasClock) {
      portMap.put("Tick", HDL.zeroBit());
      portMap.put("Clock", HDL.zeroBit());
    } else {
      portMap.put("Tick", HDL.oneBit());
      if (!gatedClock) {
        if (activeLow)
          portMap.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.InvertedDerivedClockIndex
                  + HDL.BracketClose());
        else
          portMap.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DerivedClockIndex
                  + HDL.BracketClose());
      } else {
        portMap.put("Clock", GetNetName(componentInfo, ShiftRegister.CK, true, nets));
      }
    }
    portMap.putAll(GetNetMap("ShiftEnable", false, componentInfo, ShiftRegister.SH, nets));
    if (hasParallelLoad) {
      portMap.putAll(GetNetMap("ParLoad", true, componentInfo, ShiftRegister.LD, nets));
    } else {
      portMap.put("ParLoad", HDL.zeroBit());
    }
    var shiftName = "ShiftIn";
    if (HDL.isVHDL() & (nrOfBits == 1)) shiftName += "(0)";
    portMap.putAll(GetNetMap(shiftName, true, componentInfo, ShiftRegister.IN, nets));
    if (hasParallelLoad) {
      final var vector = new StringBuilder();
      if (nrOfBits == 1) {
        if (HDL.isVHDL()) {
          for (var i = 0; i < nrOfStages; i++) {
            portMap.putAll(
                GetNetMap(
                    "D" + HDL.BracketOpen() + i + HDL.BracketClose(),
                    true,
                    componentInfo,
                    6 + 2 * i,
                    nets));
          }
          int nrOfOutStages = nrOfStages - 1;
          if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC)
            nrOfOutStages = nrOfStages;
          for (var i = 0; i < nrOfOutStages; i++) {
            portMap.putAll(
                GetNetMap(
                    "Q" + HDL.BracketOpen() + i + HDL.BracketClose(),
                    true,
                    componentInfo,
                    7 + 2 * i,
                    nets));
            portMap.put("Q" + HDL.BracketOpen() + (nrOfStages - 1) + HDL.BracketClose(), "OPEN");
          }
        } else {
          for (var i = nrOfStages - 1; i >= 0; i--) {
            if (vector.length() != 0) vector.append(",");
            vector.append(GetNetName(componentInfo, 6 + 2 * i, true, nets));
          }
          portMap.put("D", vector.toString());
          vector.setLength(0);
          vector.append("open");
          for (var i = nrOfStages - 2; i >= 0; i--) {
            if (vector.length() != 0) vector.append(",");
            vector.append(GetNetName(componentInfo, 7 + 2 * i, true, nets));
          }
          portMap.put("Q", vector.toString());
        }
      } else {
        if (HDL.isVHDL()) {
          for (var bit = 0; bit < nrOfBits; bit++) {
            for (var i = 0; i < nrOfStages; i++) {
              portMap.put(
                  "D" + HDL.BracketOpen() + (bit * nrOfStages + i) + HDL.BracketClose(),
                  GetBusEntryName(componentInfo, 6 + 2 * i, true, bit, nets));
            }
          }
          for (var bit = 0; bit < nrOfBits; bit++) {
            for (var i = 0; i < nrOfStages - 1; i++) {
              portMap.put(
                  "Q" + HDL.BracketOpen() + (bit * nrOfStages + i) + HDL.BracketClose(),
                  GetBusEntryName(componentInfo, 7 + 2 * i, true, bit, nets));
            }
            portMap.put(
                "Q" + HDL.BracketOpen() + ((bit + 1) * nrOfStages - 1) + HDL.BracketClose(),
                "OPEN");
          }
        } else {
          vector.setLength(0);
          for (var bit = nrOfBits - 1; bit >= 0; bit--) {
            for (var i = nrOfStages - 1; i >= 0; i--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(GetBusEntryName(componentInfo, 6 + 2 * i, true, bit, nets));
            }
          }
          portMap.put("D", vector.toString());
          vector.setLength(0);
          for (var bit = nrOfBits - 1; bit >= 0; bit--) {
            if (vector.length() != 0) vector.append(",");
            vector.append("open");
            for (var i = nrOfStages - 2; i >= 0; i--) {
              if (vector.length() != 0) vector.append(",");
              vector.append(GetBusEntryName(componentInfo, 7 + 2 * i, true, bit, nets));
            }
          }
          portMap.put("Q", vector.toString());
        }
      }
    } else {
      portMap.put("Q", HDL.unconnected(true));
      final var temp = new StringBuilder();
      if (HDL.isVerilog()) {
        temp.append("0");
      } else {
        temp.append("\"");
        temp.append("0".repeat(nrOfBits * nrOfStages));
        temp.append("\"");
      }
      portMap.put("D", temp.toString());
    }
    var shiftOut = "ShiftOut";
    if (HDL.isVHDL() & (nrOfBits == 1)) shiftOut += "(0)";
    portMap.putAll(GetNetMap(shiftOut, true, componentInfo, ShiftRegister.OUT, nets));
    return portMap;
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
