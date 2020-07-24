/**
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
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
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
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType, TheNetlist.projName()));
    if (HDLType.equals(VHDL)) {
      Contents.add("ARCHITECTURE NoPlatformSpecific OF SingleBitShiftReg IS");
      Contents.add("");
      Contents.add(
          "   SIGNAL s_state_reg  : std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      Contents.add(
          "   SIGNAL s_state_next : std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      Contents.add("");
      Contents.add("BEGIN");
      Contents.add("   Q        <= s_state_reg;");
      Contents.add("   ShiftOut <= s_state_reg(" + NrOfStagesStr + "-1);");
      Contents.add("");
      Contents.add(
          "   s_state_next <= D WHEN ParLoad = '1' ELSE s_state_reg(("
              + NrOfStagesStr
              + "-2) DOWNTO 0)&ShiftIn;");
      Contents.add("");
      Contents.add(
          "   make_state : PROCESS(Clock, ShiftEnable, Tick, Reset, s_state_next, ParLoad)");
      Contents.add("      VARIABLE temp : std_logic_vector( 0 DOWNTO 0 );");
      Contents.add("   BEGIN");
      Contents.add("      temp := std_logic_vector(to_unsigned(" + ActiveLevelStr + ",1));");
      Contents.add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      Contents.add("      ELSIF (Clock'event AND (Clock = temp(0) )) THEN");
      Contents.add("         IF (((ShiftEnable = '1') OR (ParLoad = '1')) AND (Tick = '1')) THEN");
      Contents.add("            s_state_reg <= s_state_next;");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_state;");
      Contents.add("END NoPlatformSpecific;");
      Contents.add("");
      Contents.add("");
      Contents.add("");
    } else {
      Contents.add("module SingleBitShiftReg ( Reset,");
      Contents.add("                           Tick,");
      Contents.add("                           Clock,");
      Contents.add("                           ShiftEnable,");
      Contents.add("                           ParLoad,");
      Contents.add("                           ShiftIn,");
      Contents.add("                           D,");
      Contents.add("                           ShiftOut,");
      Contents.add("                           Q);");
      Contents.add("");
      Contents.add("   parameter " + NrOfStagesStr + " = 1;");
      Contents.add("   parameter " + ActiveLevelStr + " = 1;");
      Contents.add("");
      Contents.add("   input Reset;");
      Contents.add("   input Tick;");
      Contents.add("   input Clock;");
      Contents.add("   input ShiftEnable;");
      Contents.add("   input ParLoad;");
      Contents.add("   input ShiftIn;");
      Contents.add("   input[" + NrOfStagesStr + ":0] D;");
      Contents.add("   output ShiftOut;");
      Contents.add("   output[" + NrOfStagesStr + ":0] Q;");
      Contents.add("");
      Contents.add("   wire[" + NrOfStagesStr + ":0] s_state_next;");
      Contents.add("   reg[" + NrOfStagesStr + ":0] s_state_reg;");
      Contents.add("   reg[" + NrOfStagesStr + ":0] s_state_reg_neg_edge;");
      Contents.add("");
      Contents.add(
          "   assign Q        = (" + ActiveLevelStr + ") ? s_state_reg : s_state_reg_neg_edge;");
      Contents.add(
          "   assign ShiftOut = ("
              + ActiveLevelStr
              + ") ? s_state_reg["
              + NrOfStagesStr
              + "-1] : s_state_reg_neg_edge["
              + NrOfStagesStr
              + "-1];");
      Contents.add("   assign s_state_next = (ParLoad) ? D :");
      Contents.add(
          "                         ("
              + ActiveLevelStr
              + ") ? {s_state_reg["
              + NrOfStagesStr
              + "-2:0],ShiftIn} :");
      Contents.add(
          "                                                {s_state_reg_neg_edge["
              + NrOfStagesStr
              + "-2:0],ShiftIn};");
      Contents.add("");
      Contents.add("   always @(posedge Clock or posedge Reset)");
      Contents.add("   begin");
      Contents.add("      if (Reset) s_state_reg <= 0;");
      Contents.add("      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg <= s_state_next;");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(negedge Clock or posedge Reset)");
      Contents.add("   begin");
      Contents.add("      if (Reset) s_state_reg_neg_edge <= 0;");
      Contents.add(
          "      else if ((ShiftEnable|ParLoad)&Tick) s_state_reg_neg_edge <= s_state_next;");
      Contents.add("   end");
      Contents.add("");
      Contents.add("endmodule");
      Contents.add("");
      Contents.add("");
      Contents.add("");
    }
    Contents.addAll(super.GetArchitecture(TheNetlist, attrs, ComponentName, Reporter, HDLType));
    return Contents;
  }

  @Override
  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Components = new ArrayList<String>();
    Components.add("   COMPONENT SingleBitShiftReg");
    Components.add("      GENERIC ( " + ActiveLevelStr + " : INTEGER;");
    Components.add("                " + NrOfStagesStr + " : INTEGER);");
    Components.add("      PORT ( Reset       : IN  std_logic;");
    Components.add("             Tick        : IN  std_logic;");
    Components.add("             Clock       : IN  std_logic;");
    Components.add("             ShiftEnable : IN  std_logic;");
    Components.add("             ParLoad     : IN  std_logic;");
    Components.add("             ShiftIn     : IN  std_logic;");
    Components.add(
        "             D           : IN  std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
    Components.add("             ShiftOut    : OUT std_logic;");
    Components.add(
        "             Q           : OUT std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 ));");
    Components.add("   END COMPONENT;");
    return Components;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "SHIFTER";
  }

  @Override
  public ArrayList<String> GetEntity(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(VHDL)) {
      Contents.addAll(FileWriter.getGenerateRemark(ComponentName, VHDL, TheNetlist.projName()));
      Contents.addAll(FileWriter.getExtendedLibrary());
      Contents.add("ENTITY SingleBitShiftReg IS");
      Contents.add("   GENERIC ( " + ActiveLevelStr + " : INTEGER;");
      Contents.add("             " + NrOfStagesStr + " : INTEGER);");
      Contents.add("   PORT ( Reset       : IN  std_logic;");
      Contents.add("          Tick        : IN  std_logic;");
      Contents.add("          Clock       : IN  std_logic;");
      Contents.add("          ShiftEnable : IN  std_logic;");
      Contents.add("          ParLoad     : IN  std_logic;");
      Contents.add("          ShiftIn     : IN  std_logic;");
      Contents.add(
          "          D           : IN  std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 );");
      Contents.add("          ShiftOut    : OUT std_logic;");
      Contents.add(
          "          Q           : OUT std_logic_vector( (" + NrOfStagesStr + "-1) DOWNTO 0 ));");
      Contents.add("END SingleBitShiftReg;");
      Contents.add("");
      Contents.add("");
      Contents.add("");
    }
    Contents.addAll(super.GetEntity(TheNetlist, attrs, ComponentName, Reporter, HDLType));
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Reset", 1);
    Inputs.put("Tick", 1);
    Inputs.put("Clock", 1);
    Inputs.put("ShiftEnable", 1);
    Inputs.put("ParLoad", 1);
    Inputs.put("ShiftIn", NrOfBitsId);
    Inputs.put("D", NrOfParBitsId);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(VHDL)) {
      Contents.add("   GenBits : FOR n IN (" + NrOfBitsStr + "-1) DOWNTO 0 GENERATE");
      Contents.add("      OneBit : SingleBitShiftReg");
      Contents.add("      GENERIC MAP ( " + ActiveLevelStr + " => " + ActiveLevelStr + ",");
      Contents.add("                    " + NrOfStagesStr + " => " + NrOfStagesStr + " )");
      Contents.add("      PORT MAP ( Reset       => Reset,");
      Contents.add("                 Tick        => Tick,");
      Contents.add("                 Clock       => Clock,");
      Contents.add("                 ShiftEnable => ShiftEnable,");
      Contents.add("                 ParLoad     => ParLoad,");
      Contents.add("                 ShiftIn     => ShiftIn(n),");
      Contents.add(
          "                 D           => D( ((n+1)*"
              + NrOfStagesStr
              + ")-1 DOWNTO (n*"
              + NrOfStagesStr
              + ")),");
      Contents.add("                 ShiftOut    => ShiftOut(n),");
      Contents.add(
          "                 Q           => Q( ((n+1)*"
              + NrOfStagesStr
              + ")-1 DOWNTO (n*"
              + NrOfStagesStr
              + ")));");
      Contents.add("   END GENERATE genbits;");
    } else {
      Contents.add("   genvar n;");
      Contents.add("   generate");
      Contents.add("      for (n = 0 ; n < " + NrOfBitsStr + "; n =n+1)");
      Contents.add("      begin:Bit");
      Contents.add("         SingleBitShiftReg #(." + ActiveLevelStr + "(" + ActiveLevelStr + "),");
      Contents.add("                             ." + NrOfStagesStr + "(" + NrOfStagesStr + "))");
      Contents.add("            OneBit (.Reset(Reset),");
      Contents.add("                    .Tick(Tick),");
      Contents.add("                    .Clock(Clock),");
      Contents.add("                    .ShiftEnable(ShiftEnable),");
      Contents.add("                    .ParLoad(ParLoad),");
      Contents.add("                    .ShiftIn(ShiftIn[n]),");
      Contents.add(
          "                    .D(D[((n+1)*" + NrOfStagesStr + ")-1:(n*" + NrOfStagesStr + ")]),");
      Contents.add("                    .ShiftOut(ShiftOut[n]),");
      Contents.add(
          "                    .Q(Q[((n+1)*" + NrOfStagesStr + ")-1:(n*" + NrOfStagesStr + ")]));");
      Contents.add("      end");
      Contents.add("   endgenerate");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("ShiftOut", NrOfBitsId);
    Outputs.put("Q", NrOfParBitsId);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    Parameters.put(ActiveLevelId, ActiveLevelStr);
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    Parameters.put(NrOfStagesId, NrOfStagesStr);
    Parameters.put(NrOfParBitsId, NrOfParBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    int ActiveLevel = 1;
    Boolean GatedClock = false;
    Boolean ActiveLow = false;
    String ClockNetName = GetClockNetName(ComponentInfo, ShiftRegister.CK, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
    }
    ActiveLow = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    if (GatedClock && ActiveLow) {
      ActiveLevel = 0;
    }
    int NrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int NrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    ParameterMap.put(ActiveLevelStr, ActiveLevel);
    ParameterMap.put(NrOfBitsStr, NrOfBits);
    ParameterMap.put(NrOfStagesStr, NrOfStages);
    ParameterMap.put(NrOfParBitsStr, NrOfBits * NrOfStages);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
        Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    Boolean GatedClock = false;
    Boolean HasClock = true;
    Boolean ActiveLow = false;
    String ZeroBit = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String SetBit = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    int NrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    int NrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    if (!ComponentInfo.EndIsConnected(ShiftRegister.CK)) {
      Reporter.AddSevereWarning(
          "Component \"Shift Register\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      HasClock = false;
    }
    String ClockNetName = GetClockNetName(ComponentInfo, ShiftRegister.CK, Nets);
    GatedClock = ClockNetName.isEmpty();
    ActiveLow = attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING;
    Boolean HasParallelLoad = attrs.getValue(ShiftRegister.ATTR_LOAD).booleanValue();
    PortMap.putAll(
        GetNetMap("Reset", true, ComponentInfo, ShiftRegister.CLR, Reporter, HDLType, Nets));
    if (HasClock && !GatedClock) {
      if (Nets.RequiresGlobalClockConnection()) {
        PortMap.put(
            "Tick",
            ClockNetName
                + BracketOpen
                + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
                + BracketClose);
      } else {
        if (ActiveLow)
          PortMap.put(
              "Tick",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.NegativeEdgeTickIndex)
                  + BracketClose);
        else
          PortMap.put(
              "Tick",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
                  + BracketClose);
      }
      PortMap.put(
          "Clock",
          ClockNetName
              + BracketOpen
              + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
              + BracketClose);
    } else if (!HasClock) {
      PortMap.put("Tick", ZeroBit);
      PortMap.put("Clock", ZeroBit);
    } else {
      PortMap.put("Tick", SetBit);
      if (!GatedClock) {
        if (ActiveLow)
          PortMap.put(
              "Clock",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.InvertedDerivedClockIndex)
                  + BracketClose);
        else
          PortMap.put(
              "Clock",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.DerivedClockIndex)
                  + BracketClose);
      } else {
        PortMap.put("Clock", GetNetName(ComponentInfo, ShiftRegister.CK, true, HDLType, Nets));
      }
    }
    PortMap.putAll(
        GetNetMap("ShiftEnable", false, ComponentInfo, ShiftRegister.SH, Reporter, HDLType, Nets));
    if (HasParallelLoad) {
      PortMap.putAll(
          GetNetMap("ParLoad", true, ComponentInfo, ShiftRegister.LD, Reporter, HDLType, Nets));
    } else {
      PortMap.put("ParLoad", ZeroBit);
    }
    String ShiftName = "ShiftIn";
    if (HDLType.equals(VHDL) & (NrOfBits == 1)) ShiftName += "(0)";
    PortMap.putAll(
        GetNetMap(ShiftName, true, ComponentInfo, ShiftRegister.IN, Reporter, HDLType, Nets));
    if (HasParallelLoad) {
      StringBuffer Vector = new StringBuffer();
      if (NrOfBits == 1) {
        if (HDLType.equals(VHDL)) {
          for (int i = 0; i < NrOfStages; i++) {
            PortMap.putAll(
                GetNetMap(
                    "D" + BracketOpen + Integer.toString(i) + BracketClose,
                    true,
                    ComponentInfo,
                    6 + 2 * i,
                    Reporter,
                    HDLType,
                    Nets));
          }
          int NrOfOutStages = NrOfStages - 1;
          if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC)
            NrOfOutStages = NrOfStages;
          for (int i = 0; i < NrOfOutStages; i++) {
            PortMap.putAll(
                GetNetMap(
                    "Q" + BracketOpen + Integer.toString(i) + BracketClose,
                    true,
                    ComponentInfo,
                    7 + 2 * i,
                    Reporter,
                    HDLType,
                    Nets));
            PortMap.put(
                "Q" + BracketOpen + Integer.toString(NrOfStages - 1) + BracketClose, "OPEN");
          }
        } else {
          for (int i = NrOfStages-1; i >= 0; i--) {
            if (Vector.length() != 0) Vector.append(",");
            Vector.append(GetNetName(ComponentInfo, 6 + 2 * i, true, HDLType, Nets));
          }
          PortMap.put("D", Vector.toString());
          Vector.setLength(0);
          Vector.append("open");
          for (int i = NrOfStages-2; i >= 0; i--) {
            if (Vector.length() != 0) Vector.append(",");
            Vector.append(GetNetName(ComponentInfo, 7 + 2 * i, true, HDLType, Nets));
          }
          PortMap.put("Q", Vector.toString());
        }
      } else {
        if (HDLType.equals(VHDL)) {
          for (int bit = 0; bit < NrOfBits; bit++) {
            for (int i = 0; i < NrOfStages; i++) {
              PortMap.put(
                  "D" + BracketOpen + Integer.toString(bit * NrOfStages + i) + BracketClose,
                  GetBusEntryName(ComponentInfo, 6 + 2 * i, true, bit, HDLType, Nets));
            }
          }
          for (int bit = 0; bit < NrOfBits; bit++) {
            for (int i = 0; i < NrOfStages - 1; i++) {
              PortMap.put(
                  "Q" + BracketOpen + Integer.toString(bit * NrOfStages + i) + BracketClose,
                  GetBusEntryName(ComponentInfo, 7 + 2 * i, true, bit, HDLType, Nets));
            }
            PortMap.put(
                "Q" + BracketOpen + Integer.toString((bit + 1) * NrOfStages - 1) + BracketClose,
                "OPEN");
          }
        } else {
          Vector.setLength(0);
          for (int bit = NrOfBits-1 ; bit >= 0; bit--) {
            for (int i = NrOfStages-1 ; i >= 0 ; i--) {
              if (Vector.length() != 0) Vector.append(",");
              Vector.append(GetBusEntryName(ComponentInfo, 6 + 2 * i, true, bit, HDLType, Nets));
            }
          }
          PortMap.put("D", Vector.toString());
          Vector.setLength(0);
          for (int bit = NrOfBits-1; bit >= 0 ; bit--) {
            if (Vector.length() != 0) Vector.append(",");
            Vector.append("open");
            for (int i = NrOfStages-2; i >= 0; i--) {
              if (Vector.length() != 0) Vector.append(",");
              Vector.append(GetBusEntryName(ComponentInfo, 7 + 2 * i, true, bit, HDLType, Nets));
            }
          }
          PortMap.put("Q", Vector.toString());
        }
      }
    } else {
      PortMap.put("Q", (HDLType.equals(VHDL)) ? "OPEN" : "");
      StringBuffer Temp = new StringBuffer();
      if (HDLType.equals(VERILOG)) {
        Temp.append("0");
      } else {
        Temp.append("\"");
        for (int i = 0; i < NrOfBits * NrOfStages; i++) Temp.append("0");
        Temp.append("\"");
      }
      PortMap.put("D", Temp.toString());
    }
    String ShiftOut = "ShiftOut";
    if (HDLType.equals(VHDL) & (NrOfBits == 1)) ShiftOut += "(0)";
    PortMap.putAll(
        GetNetMap(ShiftOut, true, ComponentInfo, ShiftRegister.OUT, Reporter, HDLType, Nets));
    return PortMap;
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
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
