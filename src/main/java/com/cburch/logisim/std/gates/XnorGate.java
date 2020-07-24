/**
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

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.WireRepairData;

import java.awt.Graphics2D;
import java.util.ArrayList;

class XnorGate extends AbstractGate {
  private class XNorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
    @Override
    public ArrayList<String> GetLogicFunction(
        int nr_of_inputs, int bitwidth, boolean is_one_hot, String HDLType) {
      ArrayList<String> Contents = new ArrayList<String>();
      if (is_one_hot) {
        Contents.addAll(GetOneHot(true, nr_of_inputs, bitwidth > 1, HDLType));
      } else {
        Contents.addAll(GetParity(true, nr_of_inputs, bitwidth > 1, HDLType));
      }
      Contents.add("");
      return Contents;
    }
  }

  public static XnorGate FACTORY = new XnorGate();

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
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new XNorGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintXnor(painter, width, height, false);
  }

  @Override
  public void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    XorGate.paintIconANSI(g, iconSize, borderSize, negateSize,true);
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
