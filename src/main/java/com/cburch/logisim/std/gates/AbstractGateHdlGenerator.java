/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class AbstractGateHdlGenerator extends AbstractHdlGeneratorFactory {

  private static final int BIT_WIDTH_GENERIC = -1;
  private static final String BIT_WIDTH_STRING = "NrOfBits";
  private static final int BUBBLES_GENERIC = -2;
  private static final String BUBBLES_MASK = "BubblesMask";

  public AbstractGateHdlGenerator() {
    super();
    myParametersList
        .addBusOnly(BIT_WIDTH_STRING, BIT_WIDTH_GENERIC)
        .addVector(BUBBLES_MASK, BUBBLES_GENERIC, HdlParameters.MAP_GATE_INPUT_BUBLE);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    if (!attrs.containsAttribute(GateAttributes.ATTR_INPUTS)) return;
    final var nrOfInputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
    final var bitWidth = attrs.getValue(StdAttr.WIDTH).getWidth();
    for (var input = 1; input <= nrOfInputs; input++) {
      myWires.addWire(String.format("s_realInput%d", input), bitWidth == 1 ? 1 : BIT_WIDTH_GENERIC);
      final var floatingToZero = getFloatingValue(attrs.getValue(new NegateAttribute(input - 1, null)));
      myPorts.add(Port.INPUT, String.format("input%d", input), bitWidth == 1 ? 1 : BIT_WIDTH_GENERIC, input, floatingToZero);
    }
    myPorts.add(Port.OUTPUT, "result", BIT_WIDTH_GENERIC, 0, StdAttr.WIDTH);
  }

  public boolean getFloatingValue(boolean isInverted) {
    return !isInverted;
  }

  public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
    return LineBuffer.getHdlBuffer();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    final var bitWidth = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfInputs =
        attrs.containsAttribute(GateAttributes.ATTR_INPUTS)
            ? attrs.getValue(GateAttributes.ATTR_INPUTS)
            : 1;

    if (nrOfInputs > 1) {
      contents.empty();
      contents.addRemarkBlock("Here the bubbles are processed");
      for (var i = 0; i < nrOfInputs; i++) {
        if (Hdl.isVhdl()) {
          contents.addVhdlKeywords().add("s_realInput{{1}} <= input{{1}} {{when}} {{2}}{{<}}{{3}}{{>}} = '0' {{else}} {{not}}(input{{1}});", (i + 1), BUBBLES_MASK, i);
        } else {
          contents.add("{{assign}} s_realInput{{1}} = ({{2}}{{<}}{{3}}{{>}} == 1'b0) ? input{{1}} : ~input{{1}};", (i + 1), BUBBLES_MASK, i);
        }
      }
    }
    contents.empty().addRemarkBlock("Here the functionality is defined");
    var onehot = false;
    if (attrs.containsAttribute(GateAttributes.ATTR_XOR)) {
      onehot = attrs.getValue(GateAttributes.ATTR_XOR) == GateAttributes.XOR_ONE;
    }
    contents.add(getLogicFunction(nrOfInputs, bitWidth, onehot));
    return contents.empty();
  }

  public LineBuffer getOneHot(boolean inverted, int nrOfInputs, boolean isBus) {
    final var lines = LineBuffer.getHdlBuffer();
    var spaces = "";
    var indexString = "";
    if (isBus) {
      if (Hdl.isVhdl()) {
        lines.addVhdlKeywords().add(spaces + "genBits : {{for}} n {{in}} (" + BIT_WIDTH_STRING + "-1) {{downto}} 0 {{generate}}");
        spaces += "   ";
        indexString = "(n)";
      } else {
        lines.add("genvar n;");
        lines.add("generate");
        lines.add("   for (n = 0 ; n < " + BIT_WIDTH_STRING + " ; n = n + 1)");
        lines.add("      begin: bit");
        spaces += "      ";
        indexString = "[n]";
      }
    }
    var oneLine = new StringBuilder();
    oneLine
        .append(spaces)
        .append(Hdl.assignPreamble())
        .append("result")
        .append(indexString)
        .append(Hdl.assignOperator());
    if (inverted) oneLine.append(Hdl.notOperator()).append("(");
    final var spacesLen = oneLine.length();
    for (var termloop = 0; termloop < nrOfInputs; termloop++) {
      while (oneLine.length() < spacesLen) {
        oneLine.append(" ");
      }
      oneLine.append("(");
      for (var i = 0; i < nrOfInputs; i++) {
        if (i == termloop) {
          oneLine.append("s_realInput").append(i + 1).append(indexString);
        } else {
          oneLine.append(Hdl.notOperator()).append("(s_realInput").append(i + 1).append(indexString).append(")");
        }
        if (i < (nrOfInputs - 1)) {
          oneLine.append(Hdl.andOperator());
        }
      }
      oneLine.append(")");
      if (termloop < (nrOfInputs - 1)) {
        oneLine.append(Hdl.orOperator());
      } else {
        if (inverted) oneLine.append(")");
        oneLine.append(";");
      }
      lines.add(oneLine.toString());
      oneLine.setLength(0);
    }
    if (isBus) {
      if (Hdl.isVhdl()) {
        lines.add("{{end}} {{generate}} GenBits;");
      } else {
        lines.add("      end");
        lines.add("endgenerate");
      }
    }
    return lines.empty();
  }

  public static LineBuffer getParity(boolean inverted, int nrOfInputs, boolean isBus) {
    final var lines = LineBuffer.getHdlBuffer();
    var spaces = "   ";
    var indexString = "";
    if (isBus) {
      if (Hdl.isVhdl()) {
        lines.addVhdlKeywords().add(spaces + "genBits : {{for}} n {{in}} (" + BIT_WIDTH_STRING + "-1) {{downto}} 0 {{generate}}");
        spaces += "   ";
        indexString = "(n)";
      } else {
        lines.add("genvar n;");
        lines.add("generate");
        lines.add("   for (n = 0 ; n < " + BIT_WIDTH_STRING + " ; n = n + 1)");
        lines.add("      begin: bit");
        spaces += "      ";
        indexString = "[n]";
      }
    }
    final var oneLine = new StringBuilder();
    oneLine.append(spaces).append(Hdl.assignPreamble()).append("result").append(indexString).append(Hdl.assignOperator());
    if (inverted) oneLine.append(Hdl.notOperator()).append("(");
    final var spacesLen = oneLine.length();
    for (var i = 0; i < nrOfInputs; i++) {
      while (oneLine.length() < spacesLen) {
        oneLine.append(" ");
      }
      oneLine.append("s_realInput").append(i + 1).append(indexString);
      if (i < (nrOfInputs - 1)) {
        oneLine.append(Hdl.xorOperator());
      } else {
        if (inverted) oneLine.append(")");
        oneLine.append(";");
      }
      lines.add(oneLine.toString());
      oneLine.setLength(0);
    }
    if (isBus) {
      if (Hdl.isVhdl()) {
        lines.add("{{end}} {{generate}} genBits;");
      } else {
        lines.add("      end");
        lines.add("endgenerate");
      }
    }
    return lines.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    var supported = true;
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      supported = attrs.getValue(GateAttributes.ATTR_OUTPUT).equals(GateAttributes.OUTPUT_01);
    return supported;
  }
}
