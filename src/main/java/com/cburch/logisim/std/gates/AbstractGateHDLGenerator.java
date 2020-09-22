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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractGateHDLGenerator extends AbstractHDLGeneratorFactory {

  private static final int BitWidthGeneric = -1;
  private static final String BitWidthString = "NrOfBits";
  private static final int BubblesGeneric = -2;
  private static final String BubblesString = "BubblesMask";

  @Override
  public String getComponentStringIdentifier() {
    return "GATE";
  }

  public boolean GetFloatingValue(boolean is_inverted) {
    return !is_inverted;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
    int Bitwidth = (is_bus(attrs)) ? BitWidthGeneric : 1;
    int NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    for (int i = 0; i < NrOfInputs; i++) {
      MyInputs.put("Input_" + Integer.toString(i + 1), Bitwidth);
    }
    return MyInputs;
  }

  public ArrayList<String> GetLogicFunction(
      int nr_of_inputs, int bitwidth, boolean is_one_hot, String HDLType) {
    return new ArrayList<String>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int Bitwidth = attrs.getValue(StdAttr.WIDTH).getWidth();
    int NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;

    if (NrOfInputs > 1) {
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the bubbles are processed", 3, HDLType));
      if (HDLType.equals(VHDL)) {
        String AllignmentSpaces;
        if (NrOfInputs < 10) AllignmentSpaces = " ";
        else if (NrOfInputs < 100) AllignmentSpaces = "  ";
        else AllignmentSpaces = "   ";
        Contents.add(
            "   s_signal_invert_mask <= std_logic_vector(to_unsigned("
                + BubblesString
                + ","
                + Integer.toString(NrOfInputs)
                + "));");
        String WhenLineBegin = "";
        for (int i = 0; i < 21 + AllignmentSpaces.length(); i++) WhenLineBegin += " ";
        for (int i = 0; i < NrOfInputs; i++) {
          String LocalSpaces;
          if (i < 10) LocalSpaces = AllignmentSpaces;
          else if (i < 100)
            LocalSpaces = AllignmentSpaces.substring(0, AllignmentSpaces.length() - 1);
          else if (i < 1000)
            LocalSpaces = AllignmentSpaces.substring(0, AllignmentSpaces.length() - 2);
          else LocalSpaces = " ";
          Contents.add(
              "   s_real_input_"
                  + Integer.toString(i + 1)
                  + LocalSpaces
                  + " <= NOT( Input_"
                  + Integer.toString(i + 1)
                  + " )");
          Contents.add(
              WhenLineBegin
                  + "   WHEN s_signal_invert_mask("
                  + Integer.toString(i)
                  + ") = '1' ELSE");
          Contents.add(WhenLineBegin + "Input_" + Integer.toString(i + 1) + ";");
        }
      } else {
        Contents.add("   assign s_signal_invert_mask = " + BubblesString + ";");
        for (int i = 0; i < NrOfInputs; i++) {
          Contents.add(
              "   assign s_real_input_"
                  + Integer.toString(i + 1)
                  + " = (s_signal_invert_mask["
                  + Integer.toString(i)
                  + "]) ? ~Input_"
                  + Integer.toString(i + 1)
                  + ":"
                  + " Input_"
                  + Integer.toString(i + 1)
                  + ";");
        }
      }
    }
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the functionality is defined", 3, HDLType));
    boolean onehot = false;
    if (attrs.containsAttribute(GateAttributes.ATTR_XOR)) {
      onehot = attrs.getValue(GateAttributes.ATTR_XOR) == GateAttributes.XOR_ONE;
    }
    Contents.addAll(GetLogicFunction(NrOfInputs, Bitwidth, onehot, HDLType));
    return Contents;
  }

  public ArrayList<String> GetOneHot(
      boolean inverted, int nr_of_inputs, boolean is_bus, String HDLType) {
    ArrayList<String> Lines = new ArrayList<String>();
    String Spaces = "   ";
    String IndexString = "";
    String NotOperation = (HDLType.equals(VHDL)) ? "NOT" : "~";
    String AndOperation = (HDLType.equals(VHDL)) ? "AND" : "&";
    String OrOperation = (HDLType.equals(VHDL)) ? "OR" : "|";
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperation = (HDLType.equals(VHDL)) ? " <= " : " = ";
    if (is_bus) {
      if (HDLType.equals(VHDL)) {
        Lines.add(Spaces + "GenBits : FOR n IN (" + BitWidthString + "-1) DOWNTO 0 GENERATE");
        Spaces += "   ";
        IndexString = "(n)";
      } else {
        Lines.add("   genvar n;");
        Lines.add("   generate");
        Lines.add("      for (n = 0 ; n < " + BitWidthString + " ; n = n + 1)");
        Lines.add("         begin: bit");
        Spaces += "         ";
        IndexString = "[n]";
      }
    }
    StringBuffer OneLine = new StringBuffer();
    OneLine.append(Spaces + Preamble + "Result" + IndexString + AssignOperation);
    if (inverted) OneLine.append(NotOperation + "(");
    int spaces = OneLine.length();
    for (int termloop = 0; termloop < nr_of_inputs; termloop++) {
      while (OneLine.length() < spaces) {
        OneLine.append(" ");
      }
      OneLine.append("(");
      for (int i = 0; i < nr_of_inputs; i++) {
        if (i == termloop) OneLine.append("s_real_input_" + Integer.toString(i + 1) + IndexString);
        else
          OneLine.append(
              NotOperation + "(s_real_input_" + Integer.toString(i + 1) + IndexString + ")");
        if (i < (nr_of_inputs - 1)) {
          OneLine.append(" " + AndOperation + " ");
        }
      }
      OneLine.append(")");
      if (termloop < (nr_of_inputs - 1)) {
        OneLine.append(" " + OrOperation + " ");
      } else {
        if (inverted) OneLine.append(")");
        OneLine.append(";");
      }
      Lines.add(OneLine.toString());
      OneLine.setLength(0);
    }
    if (is_bus) {
      if (HDLType.equals(VHDL)) {
        Lines.add("   END GENERATE GenBits;");
      } else {
        Lines.add("         end");
        Lines.add("   endgenerate");
      }
    }
    return Lines;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
    int Bitwidth = (is_bus(attrs)) ? BitWidthGeneric : 1;
    MyOutputs.put("Result", Bitwidth);
    return MyOutputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> MyParameters = new TreeMap<Integer, String>();
    int NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    if (is_bus(attrs)) {
      MyParameters.put(BitWidthGeneric, BitWidthString);
    }
    if (NrOfInputs > 1) {
      MyParameters.put(BubblesGeneric, BubblesString);
    }
    return MyParameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    boolean is_bus = is_bus(ComponentInfo.GetComponent().getAttributeSet());
    AttributeSet Myattrs = ComponentInfo.GetComponent().getAttributeSet();
    int NrOfInputs =
        Myattrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? Myattrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    int bubleMask, mask;
    if (is_bus) {
      ParameterMap.put(BitWidthString, Myattrs.getValue(StdAttr.WIDTH).getWidth());
    }
    if (NrOfInputs > 1) {
      bubleMask = 0;
      mask = 1;
      for (int i = 0; i < NrOfInputs; i++) {
        boolean input_is_inverted =
            ComponentInfo.GetComponent().getAttributeSet().getValue(new NegateAttribute(i, null));
        if (input_is_inverted) bubleMask |= mask;
        mask <<= 1;
      }
      ParameterMap.put(BubblesString, bubleMask);
    }

    return ParameterMap;
  }

  public ArrayList<String> GetParity(
      boolean inverted, int nr_of_inputs, boolean is_bus, String HDLType) {
    ArrayList<String> Lines = new ArrayList<String>();
    String Spaces = "   ";
    String IndexString = "";
    String XorOperation = (HDLType.equals(VHDL)) ? " XOR" : "^";
    String NotOperation = (HDLType.equals(VHDL)) ? "NOT" : "~";
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperation = (HDLType.equals(VHDL)) ? " <= " : " = ";
    if (is_bus) {
      if (HDLType.equals(VHDL)) {
        Lines.add(Spaces + "GenBits : FOR n IN (" + BitWidthString + "-1) DOWNTO 0 GENERATE");
        Spaces += "   ";
        IndexString = "(n)";
      } else {
        Lines.add("   genvar n;");
        Lines.add("   generate");
        Lines.add("      for (n = 0 ; n < " + BitWidthString + " ; n = n + 1)");
        Lines.add("         begin: bits");
        Spaces += "         ";
        IndexString = "[n]";
      }
    }
    StringBuffer OneLine = new StringBuffer();
    OneLine.append(Spaces + Preamble + "Result" + IndexString + AssignOperation);
    if (inverted) OneLine.append(NotOperation + "(");
    int spaces = OneLine.length();
    for (int i = 0; i < nr_of_inputs; i++) {
      while (OneLine.length() < spaces) {
        OneLine.append(" ");
      }
      OneLine.append("s_real_input_" + Integer.toString(i + 1) + IndexString);
      if (i < (nr_of_inputs - 1)) {
        OneLine.append(XorOperation);
      } else {
        if (inverted) OneLine.append(")");
        OneLine.append(";");
      }
      Lines.add(OneLine.toString());
      OneLine.setLength(0);
    }
    if (is_bus) {
      if (HDLType.equals(VHDL)) {
        Lines.add("   END GENERATE GenBits;");
      } else {
        Lines.add("         end");
        Lines.add("   endgenerate");
      }
    }
    return Lines;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    int NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    boolean[] InputFloatingValues = new boolean[NrOfInputs];
    if (NrOfInputs == 1) {
      InputFloatingValues[0] = true;
    } else {
      for (int i = 1; i <= NrOfInputs; i++) {
        boolean input_is_inverted = attrs.getValue(new NegateAttribute(i - 1, null));
        InputFloatingValues[i - 1] = GetFloatingValue(input_is_inverted);
      }
    }
    for (int i = 1; i <= NrOfInputs; i++) {
      PortMap.putAll(
          GetNetMap(
              "Input_" + Integer.toString(i),
              InputFloatingValues[i - 1],
              ComponentInfo,
              i,
              Reporter,
              HDLType,
              Nets));
    }
    PortMap.putAll(GetNetMap("Result", true, ComponentInfo, 0, Reporter, HDLType, Nets));

    return PortMap;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "gates";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    int Bitwidth = attrs.getValue(StdAttr.WIDTH).getWidth();
    int NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    if (NrOfInputs > 1) {
      for (int i = 0; i < NrOfInputs; i++) {
        if (Bitwidth > 1) Wires.put("s_real_input_" + Integer.toString(i + 1), BitWidthGeneric);
        else Wires.put("s_real_input_" + Integer.toString(i + 1), 1);
      }
      Wires.put("s_signal_invert_mask", NrOfInputs);
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }

  private boolean is_bus(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth() != 1;
  }
}
