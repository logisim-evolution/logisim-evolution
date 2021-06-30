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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
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

  public boolean GetFloatingValue(boolean isInverted) {
    return !isInverted;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    final var Bitwidth = (is_bus(attrs)) ? BitWidthGeneric : 1;
    final var NrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    for (var i = 0; i < NrOfInputs; i++) {
      inputs.put("Input_" + (i + 1), Bitwidth);
    }
    return inputs;
  }

  public ArrayList<String> GetLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var bitWidht = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;

    if (nrOfInputs > 1) {
      contents.add("");
      contents.addAll(MakeRemarkBlock("Here the bubbles are processed", 3));
      if (HDL.isVHDL()) {
        String allignmentSpaces;
        if (nrOfInputs < 10) allignmentSpaces = " ";
        else if (nrOfInputs < 100) allignmentSpaces = "  ";
        else allignmentSpaces = "   ";
        contents.add(
            "   s_signal_invert_mask <= std_logic_vector(to_unsigned("
                + BubblesString
                + ","
                + nrOfInputs
                + "));");
        final var whenLineBegin = new StringBuilder();
        whenLineBegin.append(" ".repeat(21 + allignmentSpaces.length()));
        for (var i = 0; i < nrOfInputs; i++) {
          var localSpaces = " ";
          if (i < 10) localSpaces = allignmentSpaces;
          else if (i < 100)
            localSpaces = allignmentSpaces.substring(0, allignmentSpaces.length() - 1);
          else if (i < 1000)
            localSpaces = allignmentSpaces.substring(0, allignmentSpaces.length() - 2);
          contents.add(
              "   s_real_input_"
                  + (i + 1)
                  + localSpaces
                  + " <= NOT( Input_"
                  + (i + 1)
                  + " )");
          contents.add(
              whenLineBegin
                  + "   WHEN s_signal_invert_mask("
                  + i
                  + ") = '1' ELSE");
          contents.add(whenLineBegin + "Input_" + (i + 1) + ";");
        }
      } else {
        contents.add("   assign s_signal_invert_mask = " + BubblesString + ";");
        for (var i = 0; i < nrOfInputs; i++) {
          contents.add(
              "   assign s_real_input_"
                  + (i + 1)
                  + " = (s_signal_invert_mask["
                  + i
                  + "]) ? ~Input_"
                  + (i + 1)
                  + ":"
                  + " Input_"
                  + (i + 1)
                  + ";");
        }
      }
    }
    contents.add("");
    contents.addAll(MakeRemarkBlock("Here the functionality is defined", 3));
    var onehot = false;
    if (attrs.containsAttribute(GateAttributes.ATTR_XOR)) {
      onehot = attrs.getValue(GateAttributes.ATTR_XOR) == GateAttributes.XOR_ONE;
    }
    contents.addAll(GetLogicFunction(nrOfInputs, bitWidht, onehot));
    return contents;
  }

  public ArrayList<String> GetOneHot(boolean inverted, int nrOfInputs, boolean isBus) {
    var lines = new ArrayList<String>();
    var spaces = "   ";
    var indexString = "";
    if (isBus) {
      if (HDL.isVHDL()) {
        lines.add(spaces + "GenBits : FOR n IN (" + BitWidthString + "-1) DOWNTO 0 GENERATE");
        spaces += "   ";
        indexString = "(n)";
      } else {
        lines.add("   genvar n;");
        lines.add("   generate");
        lines.add("      for (n = 0 ; n < " + BitWidthString + " ; n = n + 1)");
        lines.add("         begin: bit");
        spaces += "         ";
        indexString = "[n]";
      }
    }
    var oneLine = new StringBuilder();
    oneLine
        .append(spaces)
        .append(HDL.assignPreamble())
        .append("Result")
        .append(indexString)
        .append(HDL.assignOperator());
    if (inverted) oneLine.append(HDL.notOperator()).append("(");
    final var spacesLen = oneLine.length();
    for (var termloop = 0; termloop < nrOfInputs; termloop++) {
      while (oneLine.length() < spacesLen) {
        oneLine.append(" ");
      }
      oneLine.append("(");
      for (var i = 0; i < nrOfInputs; i++) {
        if (i == termloop) {
          oneLine.append("s_real_input_").append(i + 1).append(indexString);
        } else {
          oneLine.append(HDL.notOperator()).append("(s_real_input_").append(i + 1).append(indexString).append(")");
        }
        if (i < (nrOfInputs - 1)) {
          oneLine.append(" ").append(HDL.andOperator()).append(" ");
        }
      }
      oneLine.append(")");
      if (termloop < (nrOfInputs - 1)) {
        oneLine.append(" ").append(HDL.orOperator()).append(" ");
      } else {
        if (inverted) oneLine.append(")");
        oneLine.append(";");
      }
      lines.add(oneLine.toString());
      oneLine.setLength(0);
    }
    if (isBus) {
      if (HDL.isVHDL()) {
        lines.add("   END GENERATE GenBits;");
      } else {
        lines.add("         end");
        lines.add("   endgenerate");
      }
    }
    return lines;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    final var bitWidth = (is_bus(attrs)) ? BitWidthGeneric : 1;
    outputs.put("Result", bitWidth);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var params = new TreeMap<Integer, String>();
    int nrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    if (is_bus(attrs)) {
      params.put(BitWidthGeneric, BitWidthString);
    }
    if (nrOfInputs > 1) {
      params.put(BubblesGeneric, BubblesString);
    }
    return params;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    final var isBus = is_bus(componentInfo.GetComponent().getAttributeSet());
    final var myAttrs = componentInfo.GetComponent().getAttributeSet();
    int nrOfInputs =
        myAttrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? myAttrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    int bubleMask, mask;
    if (isBus) {
      parameterMap.put(BitWidthString, myAttrs.getValue(StdAttr.WIDTH).getWidth());
    }
    if (nrOfInputs > 1) {
      bubleMask = 0;
      mask = 1;
      for (var i = 0; i < nrOfInputs; i++) {
        final var inputIsInverted =
            componentInfo.GetComponent().getAttributeSet().getValue(new NegateAttribute(i, null));
        if (inputIsInverted) bubleMask |= mask;
        mask <<= 1;
      }
      parameterMap.put(BubblesString, bubleMask);
    }

    return parameterMap;
  }

  public ArrayList<String> GetParity(boolean inverted, int nrOfInputs, boolean isBus) {
    final var lines = new ArrayList<String>();
    var spaces = "   ";
    var indexString = "";
    if (isBus) {
      if (HDL.isVHDL()) {
        lines.add(spaces + "GenBits : FOR n IN (" + BitWidthString + "-1) DOWNTO 0 GENERATE");
        spaces += "   ";
        indexString = "(n)";
      } else {
        lines.add("   genvar n;");
        lines.add("   generate");
        lines.add("      for (n = 0 ; n < " + BitWidthString + " ; n = n + 1)");
        lines.add("         begin: bit");
        spaces += "         ";
        indexString = "[n]";
      }
    }
    final var oneLine = new StringBuilder();
    oneLine.append(spaces).append(HDL.assignPreamble()).append("Result").append(indexString).append(HDL.assignOperator());
    if (inverted) oneLine.append(HDL.notOperator()).append("(");
    final var spacesLen = oneLine.length();
    for (var i = 0; i < nrOfInputs; i++) {
      while (oneLine.length() < spacesLen) {
        oneLine.append(" ");
      }
      oneLine.append("s_real_input_").append(i + 1).append(indexString);
      if (i < (nrOfInputs - 1)) {
        oneLine.append(HDL.xorOperator());
      } else {
        if (inverted) oneLine.append(")");
        oneLine.append(";");
      }
      lines.add(oneLine.toString());
      oneLine.setLength(0);
    }
    if (isBus) {
      if (HDL.isVHDL()) {
        lines.add("   END GENERATE GenBits;");
      } else {
        lines.add("         end");
        lines.add("   endgenerate");
      }
    }
    return lines;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    final var attrs = componentInfo.GetComponent().getAttributeSet();
    final var nrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    final var inputFloatingValues = new boolean[nrOfInputs];
    if (nrOfInputs == 1) {
      inputFloatingValues[0] = true;
    } else {
      for (var i = 1; i <= nrOfInputs; i++) {
        final var inputIsInverted = attrs.getValue(new NegateAttribute(i - 1, null));
        inputFloatingValues[i - 1] = GetFloatingValue(inputIsInverted);
      }
    }
    for (var i = 1; i <= nrOfInputs; i++) {
      portMap.putAll(
          GetNetMap(
              "Input_" + i,
              inputFloatingValues[i - 1],
              componentInfo,
              i,
              nets));
    }
    portMap.putAll(GetNetMap("Result", true, componentInfo, 0, nets));

    return portMap;
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
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist nets) {
    final var wires = new TreeMap<String, Integer>();
    final var bitWidth = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;
    if (nrOfInputs > 1) {
      for (var i = 0; i < nrOfInputs; i++) {
        if (bitWidth > 1) wires.put("s_real_input_" + (i + 1), BitWidthGeneric);
        else wires.put("s_real_input_" + (i + 1), 1);
      }
      wires.put("s_signal_invert_mask", nrOfInputs);
    }
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  private boolean is_bus(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth() != 1;
  }
}
