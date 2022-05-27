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
import java.awt.geom.GeneralPath;

class OrGate extends AbstractGate {
  private static class OrGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      final var contents = LineBuffer.getHdlBuffer();
      final var oneLine = new StringBuilder();
      oneLine.append(Hdl.assignPreamble()).append("result").append(Hdl.assignOperator());
      final var tabWidth = oneLine.length();
      var first = true;
      for (int i = 0; i < nrOfInputs; i++) {
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
      oneLine.append(";");
      contents.add(oneLine.toString());
      return contents;
    }
  }

  public static final OrGate FACTORY = new OrGate();

  private OrGate() {
    super("OR Gate", S.getter("orGateComponent"), new OrGateHdlGeneratorFactory());
    setRectangularLabel("\u2265" + "1");
    setPaintInputLines(true);
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    var ret = inputs[0];
    for (var i = 1; i < numInputs; i++) {
      ret = Expressions.or(ret, inputs[i]);
    }
    return ret;
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    return GateFunctions.computeOr(inputs, numInputs);
  }

  @Override
  protected Value getIdentity() {
    return Value.FALSE;
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintOr(painter, width, height, false);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    paintIconANSI(g, iconSize, borderSize, negateSize, false);
  }

  protected static void paintIconANSI(
      Graphics2D g, int iconSize, int borderSize, int negateSize, boolean inverted) {
    final var ystart = negateSize >> 1;
    final var yend = iconSize - ystart;
    final var xstart = 0;
    final var xend = iconSize - negateSize;
    final var shape = new GeneralPath();
    shape.moveTo(xend, iconSize >> 1);
    shape.quadTo((2 * xend) / 3, ystart, xstart, ystart);
    shape.quadTo(xend / 3, iconSize >> 1, xstart, yend);
    shape.quadTo((2 * xend) / 3, yend, xend, iconSize >> 1);
    shape.closePath();
    final var af = g.getTransform();
    g.translate(borderSize, borderSize);
    g.draw(shape);
    paintIconPins(g, iconSize, borderSize, negateSize, inverted, false);
    g.setTransform(af);
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
