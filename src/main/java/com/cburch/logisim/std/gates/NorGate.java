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
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.WireRepairData;
import java.awt.Graphics2D;
import java.util.ArrayList;

class NorGate extends AbstractGate {
  private static class NorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
    @Override
    public ArrayList<String> GetLogicFunction(int nrOfInputs, int bitwidth, boolean isOneHot) {
      final var contents = new ArrayList<String>();
      final var oneLine = new StringBuilder();
      oneLine
          .append("   ")
          .append(HDL.assignPreamble())
          .append("Result")
          .append(HDL.assignOperator())
          .append(HDL.notOperator())
          .append("(");
      final var tabWidth = oneLine.length();
      var first = true;
      for (var i = 0; i < nrOfInputs; i++) {
        if (!first) {
          oneLine.append(HDL.orOperator());
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          while (oneLine.length() < tabWidth) {
            oneLine.append(" ");
          }
        } else {
          first = false;
        }
        oneLine.append("s_real_input_").append(i + 1);
      }
      oneLine.append(");");
      contents.add(oneLine.toString());
      contents.add("");
      return contents;
    }
  }

  public static final NorGate FACTORY = new NorGate();

  private NorGate() {
    super("NOR Gate", S.getter("norGateComponent"));
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
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new NorGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
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
