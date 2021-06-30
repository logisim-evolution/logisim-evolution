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
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String ActiveLevelStr = "ActiveLevel";
  private static final int ActiveLevelId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "REGISTER_FILE";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Reset", 1);
    map.put("ClockEnable", 1);
    map.put("Tick", 1);
    map.put("Clock", 1);
    map.put("D", NrOfBitsId);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add("   Q <= s_state_reg;");
      contents.add("");
      contents.add("   make_memory : PROCESS( clock , Reset , ClockEnable , Tick , D )");
      contents.add("   BEGIN");
      contents.add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      if (Netlist.IsFlipFlop(attrs)) {
        contents.add("      ELSIF (" + ActiveLevelStr + " = 1) THEN");
        contents.add("         IF (Clock'event AND (Clock = '1')) THEN");
        contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        contents.add("               s_state_reg <= D;");
        contents.add("            END IF;");
        contents.add("         END IF;");
        contents.add("      ELSIF (" + ActiveLevelStr + " = 0) THEN");
        contents.add("         IF (Clock'event AND (Clock = '0')) THEN");
        contents.add("         IF (ClockEnable = '1' AND Tick = '1') THEN");
        contents.add("               s_state_reg <= D;");
        contents.add("            END IF;");
        contents.add("         END IF;");

        /////
        // Contents.add("      ELSIF (Clock'event AND (Clock = std_logic_vector(to_unsigned("
        //           + ActiveLevelStr + ",1)) )) THEN");
      } else {
        contents.add("      ELSIF (" + ActiveLevelStr + " = 1) THEN");
        contents.add("         IF (Clock = '1') THEN");
        contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        contents.add("               s_state_reg <= D;");
        contents.add("            END IF;");
        contents.add("         END IF;");
        contents.add("      ELSIF (" + ActiveLevelStr + " = 0) THEN");
        contents.add("         IF (Clock = '0') THEN");
        contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        contents.add("               s_state_reg <= D;");
        contents.add("            END IF;");
        contents.add("         END IF;");
        // Contents.add("      ELSIF (Clock = std_logic_vector(to_unsigned("
        //            + ActiveLevelStr + ",1)) ) THEN");
      }
      // Contents.add("         IF (ClockEnable = '1' AND Tick = '1') THEN");
      // Contents.add("            s_state_reg <= D;");
      // Contents.add("         END IF;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_memory;");
    } else {
      if (!Netlist.IsFlipFlop(attrs)) {
        contents.add("   assign Q = s_state_reg;");
        contents.add("");
        contents.add("   always @(*)");
        contents.add("   begin");
        contents.add("      if (Reset) s_state_reg <= 0;");
        contents.add(
            "      else if ((Clock==" + ActiveLevelStr + ")&ClockEnable&Tick) s_state_reg <= D;");
        contents.add("   end");
      } else {
        contents.add(
            "   assign Q = (" + ActiveLevelStr + ") ? s_state_reg : s_state_reg_neg_edge;");
        contents.add("");
        contents.add("   always @(posedge Clock or posedge Reset)");
        contents.add("   begin");
        contents.add("      if (Reset) s_state_reg <= 0;");
        contents.add("      else if (ClockEnable&Tick) s_state_reg <= D;");
        contents.add("   end");
        contents.add("");
        contents.add("   always @(negedge Clock or posedge Reset)");
        contents.add("   begin");
        contents.add("      if (Reset) s_state_reg_neg_edge <= 0;");
        contents.add("      else if (ClockEnable&Tick) s_state_reg_neg_edge <= D;");
        contents.add("   end");
      }
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q", NrOfBitsId);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(ActiveLevelId, ActiveLevelStr);
    map.put(NrOfBitsId, NrOfBitsStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    var activeLevel = 1;
    var gatedclock = false;
    var activeLow = false;
    final var attrs = componentInfo.GetComponent().getAttributeSet();
    final var clockNetName = GetClockNetName(componentInfo, Register.CK, nets);
    if (clockNetName.isEmpty()) {
      gatedclock = true;
      if (Netlist.IsFlipFlop(attrs))
        Reporter.Report.AddWarning(
            "Found a gated clock for component \"Register\" in circuit \""
                + nets.getCircuitName()
                + "\"");
    }
    if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) activeLow = true;

    if (gatedclock && activeLow) {
      activeLevel = 0;
    }
    map.put(ActiveLevelStr, activeLevel);
    map.put(
        NrOfBitsStr, componentInfo.GetComponent().getEnd(Register.IN).getWidth().getWidth());
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) MapInfo;
    var gatedClock = false;
    var hasClock = true;
    var activeLow = false;
    final var attrs = comp.GetComponent().getAttributeSet();
    if (!comp.EndIsConnected(Register.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Register\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(comp, Register.CK, Nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) activeLow = true;
    map.putAll(GetNetMap("Reset", true, comp, Register.CLR, Nets));
    map.putAll(
        GetNetMap("ClockEnable", false, comp, Register.EN, Nets));

    if (hasClock && !gatedClock && Netlist.IsFlipFlop(attrs)) {
      if (Nets.RequiresGlobalClockConnection()) {
        map.put("Tick", HDL.oneBit());
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
        map.put("Clock", GetNetName(comp, Register.CK, true, Nets));
      }
    }
    var input = "D";
    var output = "Q";
    if (HDL.isVHDL()
        & (comp.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth()
            == 1)) {
      input += "(0)";
      output += "(0)";
    }
    map.putAll(GetNetMap(input, true, comp, Register.IN, Nets));
    map.putAll(GetNetMap(output, true, comp, Register.OUT, Nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var regs = new TreeMap<String, Integer>();
    regs.put("s_state_reg", NrOfBitsId);
    if (HDL.isVerilog() & Netlist.IsFlipFlop(attrs))
      regs.put("s_state_reg_neg_edge", NrOfBitsId);
    return regs;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
