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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.LineBuffer;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

class XorGate extends AbstractGate {
  private static class XorGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      return LineBuffer.getBuffer()
          .add(isOneHot ? getOneHot(false, nrOfInputs, bitwidth > 1) : getParity(false, nrOfInputs, bitwidth > 1));
    }
  }

  protected static Expression xorExpression(Expression[] inputs, int numInputs) {
    if (numInputs > 2) {
      throw new UnsupportedOperationException("XorGate");
    }
    Expression ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = Expressions.xor(ret, inputs[i]);
    }
    return ret;
  }

  public static final XorGate FACTORY = new XorGate();

  private XorGate() {
    super("XOR Gate", S.getter("xorGateComponent"), true, new XorGateHdlGeneratorFactory());
    setAdditionalWidth(10);
    setPaintInputLines(true);
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    return xorExpression(inputs, numInputs);
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    Object behavior = state.getAttributeValue(GateAttributes.ATTR_XOR);
    if (behavior == GateAttributes.XOR_ODD) {
      return GateFunctions.computeOddParity(inputs, numInputs);
    } else {
      return GateFunctions.computeExactlyOne(inputs, numInputs);
    }
  }

  @Override
  protected Value getIdentity() {
    return Value.FALSE;
  }

  @Override
  public String getRectangularLabel(AttributeSet attrs) {
    if (attrs == null) return "";
    boolean isOdd = false;
    Object behavior = attrs.getValue(GateAttributes.ATTR_XOR);
    if (behavior == GateAttributes.XOR_ODD) {
      Object inputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
      if (inputs == null || (Integer) inputs != 2) {
        isOdd = true;
      }
    }
    return isOdd ? "2k+1" : "=1";
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintXor(painter, width, height, false);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    paintIconANSI(g, iconSize, borderSize, negateSize, false);
  }

  protected static void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize, boolean inverted) {
    int xoff = negateSize >> 1;
    int ystart = negateSize >> 1;
    int yend = iconSize - ystart;
    int xstart = 0;
    int xend = iconSize - negateSize;
    GeneralPath shape = new GeneralPath();
    shape.moveTo(xend, iconSize >> 1);
    shape.quadTo((2 * xend) / 3, ystart, xstart + xoff, ystart);
    shape.quadTo(xoff + xend / 3, iconSize >> 1, xstart + xoff, yend);
    shape.quadTo((2 * xend) / 3, yend, xend, iconSize >> 1);
    shape.closePath();
    shape.moveTo(xstart, ystart);
    shape.quadTo(xend / 3, iconSize >> 1, xstart, yend);
    shape.moveTo(xstart, ystart);
    shape.closePath();
    AffineTransform af = g.getTransform();
    g.translate(borderSize, borderSize);
    g.draw(shape);
    paintIconPins(g, iconSize, borderSize, negateSize, inverted, false);
    g.setTransform(af);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    PainterShaped.paintXor(painter, width, height);
  }

  @Override
  protected boolean shouldRepairWire(Instance instance, WireRepairData data) {
    return !data.getPoint().equals(instance.getLocation());
  }
}
