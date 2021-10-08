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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.LineBuffer;

import java.awt.Graphics2D;

class NorGate extends AbstractGate {
  private static class NorGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
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
          oneLine.append(Hdl.orOperator());
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

  public static final NorGate FACTORY = new NorGate();

  private NorGate() {
    super("NOR Gate", S.getter("norGateComponent"), new NorGateHdlGeneratorFactory());
    setNegateOutput(true);
    setRectangularLabel(OrGate.FACTORY.getRectangularLabel(null));
    setPaintInputLines(true);
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = Expressions.or(ret, inputs[i]);
    }
    return Expressions.not(ret);
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    return GateFunctions.computeOr(inputs, numInputs).not();
  }

  @Override
  protected Value getIdentity() {
    return Value.FALSE;
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintOr(painter, width, height, true);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    OrGate.paintIconANSI(g, iconSize, borderSize, negateSize, true);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    PainterShaped.paintOr(painter, width, height);
  }

  @Override
  protected boolean shouldRepairWire(Instance instance, WireRepairData data) {
    return !data.getPoint().equals(instance.getLocation());
  }
}
