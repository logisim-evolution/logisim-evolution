/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.LineBuffer;

import java.awt.Graphics2D;

class NandGate extends AbstractGate {

  private static class NandGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public boolean getFloatingValue(boolean isInverted) {
      return isInverted;
    }

    @Override
    public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      final var contents = LineBuffer.getHdlBuffer();
      final var oneLine = new StringBuilder();
      oneLine
          .append(Hdl.assignPreamble())
          .append("result")
          .append(Hdl.assignOperator())
          .append(Hdl.notOperator())
          .append("(");
      final var tabWidth = oneLine.length();
      var first = true;
      for (var i = 0; i < nrOfInputs; i++) {
        if (!first) {
          oneLine.append(Hdl.andOperator());
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          oneLine.append(" ".repeat(tabWidth));
        } else {
          first = false;
        }
        oneLine.append("s_realInput").append(i + 1);
      }
      oneLine.append(");");
      contents.add(oneLine.toString());
      return contents;
    }
  }

  public static final NandGate FACTORY = new NandGate();

  private NandGate() {
    super("NAND Gate", S.getter("nandGateComponent"), new NandGateHdlGeneratorFactory());
    setNegateOutput(true);
    setRectangularLabel(AndGate.FACTORY.getRectangularLabel(null));
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = Expressions.and(ret, inputs[i]);
    }
    return Expressions.not(ret);
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    return GateFunctions.computeAnd(inputs, numInputs).not();
  }

  @Override
  protected Value getIdentity() {
    return Value.TRUE;
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintAnd(painter, width, height, true);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    AndGate.paintIconANSI(g, iconSize, borderSize, negateSize, true);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    PainterShaped.paintAnd(painter, width, height);
  }
}
