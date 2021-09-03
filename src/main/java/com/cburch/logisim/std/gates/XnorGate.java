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
import java.util.ArrayList;

class XnorGate extends AbstractGate {
  private static class XNorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
    @Override
    public ArrayList<String> GetLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      return (new LineBuffer())
          .add(
              isOneHot
                  ? GetOneHot(true, nrOfInputs, bitwidth > 1)
                  : GetParity(true, nrOfInputs, bitwidth > 1))
          .add("")
          .get();
    }
  }

  public static final XnorGate FACTORY = new XnorGate();

  private XnorGate() {
    super("XNOR Gate", S.getter("xnorGateComponent"), true);
    setNegateOutput(true);
    setAdditionalWidth(10);
    setPaintInputLines(true);
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    return Expressions.not(XorGate.xorExpression(inputs, numInputs));
  }

  @Override
  protected Value computeOutput(Value[] inputs, int numInputs, InstanceState state) {
    Object behavior = state.getAttributeValue(GateAttributes.ATTR_XOR);
    if (behavior == GateAttributes.XOR_ODD) {
      return GateFunctions.computeOddParity(inputs, numInputs).not();
    } else {
      return GateFunctions.computeExactlyOne(inputs, numInputs).not();
    }
  }

  @Override
  protected Value getIdentity() {
    return Value.FALSE;
  }

  @Override
  protected String getRectangularLabel(AttributeSet attrs) {
    return XorGate.FACTORY.getRectangularLabel(attrs);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new XNorGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintXnor(painter, width, height, false);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    XorGate.paintIconANSI(g, iconSize, borderSize, negateSize, true);
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
