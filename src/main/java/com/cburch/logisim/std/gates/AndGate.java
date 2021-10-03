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
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LineBuffer;

import java.awt.Graphics2D;

class AndGate extends AbstractGate {
  private static class AndGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public boolean getFloatingValue(boolean isInverted) {
      return isInverted;
    }

    @Override
    public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      final var contents = LineBuffer.getHdlBuffer();
      var oneLine = new StringBuilder();
      oneLine.append(Hdl.assignPreamble())
          .append("result")
          .append(Hdl.assignOperator());
      final var tabWidth = oneLine.length();
      var first = true;
      for (int i = 0; i < nrOfInputs; i++) {
        if (!first) {
          oneLine.append(Hdl.andOperator());
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          oneLine.append(" ".repeat(tabWidth));
        } else {
          first = false;
        }
        oneLine.append(String.format("s_realInput%d", i + 1));
      }
      oneLine.append(";");
      contents.add(oneLine.toString());
      return contents;
    }
  }

  public static final AndGate FACTORY = new AndGate();

  private AndGate() {
    super("AND Gate", S.getter("andGateComponent"), new AndGateHdlGeneratorFactory());
    setRectangularLabel("&");
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    var ret = inputs[0];
    for (var i = 1; i < numInputs; i++) {
      ret = Expressions.and(ret, inputs[i]);
    }
    return ret;
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    return GateFunctions.computeAnd(inputs, numInputs);
  }

  @Override
  protected Value getIdentity() {
    return Value.TRUE;
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintAnd(painter, width, height, false);
  }

  @Override
  protected void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    paintIconANSI(g, iconSize, borderSize, negateSize, false);
  }

  protected static void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize, boolean inverted) {
    final var ystart = negateSize >> 1;
    final var yend = iconSize - ystart;
    final var rad = (yend - ystart) >> 1;
    final var xstart = 0;
    final var xend = iconSize - negateSize - rad;
    final var xp = new int[] {xend, xstart, xstart, xend};
    final var yp = new int[] {ystart, ystart, yend, yend};
    final var af = g.getTransform();
    g.translate(borderSize, borderSize);
    g.drawPolyline(xp, yp, 4);
    GraphicsUtil.drawCenteredArc(g, xend, iconSize >> 1, rad, -90, 180);
    paintIconPins(g, iconSize, borderSize, negateSize, inverted, false);
    g.setTransform(af);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    PainterShaped.paintAnd(painter, width, height);
  }
}
