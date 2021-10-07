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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.LineBuffer;
import java.awt.Graphics2D;

class OddParityGate extends AbstractGate {
  private static class XorGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public LineBuffer getLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      return LineBuffer.getBuffer().add(getParity(false, nrOfInputs, bitwidth > 1));
    }
  }

  public static final OddParityGate FACTORY = new OddParityGate();
  private final String ODD_PARITY_LABEL = "2k+1";

  private OddParityGate() {
    super("Odd Parity", S.getter("oddParityComponent"), new XorGateHdlGeneratorFactory());
    setRectangularLabel(ODD_PARITY_LABEL);
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    var ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
      ret = Expressions.xor(ret, inputs[i]);
    }
    return ret;
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    return GateFunctions.computeOddParity(inputs, numInputs);
  }

  @Override
  protected Value getIdentity() {
    return Value.FALSE;
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    paintRectangular(painter, width, height);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    paintRectangular(painter, width, height);
  }

  @Override
  protected void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    AbstractGate.paintIconIEC(g, ODD_PARITY_LABEL, false, false);
  }
}
