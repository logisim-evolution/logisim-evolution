/*
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
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

class AndGate extends AbstractGate {
  private static class AndGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
    @Override
    public boolean GetFloatingValue(boolean is_inverted) {
      return is_inverted;
    }

    @Override
    public ArrayList<String> GetLogicFunction(int nr_of_inputs, int bitwidth, boolean is_one_hot) {
      ArrayList<String> Contents = new ArrayList<>();
      StringBuffer OneLine = new StringBuffer();
      OneLine.append("   " + HDL.assignPreamble() + "Result" + HDL.assignOperator());
      int TabWidth = OneLine.length();
      boolean first = true;
      for (int i = 0; i < nr_of_inputs; i++) {
        if (!first) {
          OneLine.append(HDL.andOperator());
          Contents.add(OneLine.toString());
          OneLine.setLength(0);
          while (OneLine.length() < TabWidth) {
            OneLine.append(" ");
          }
        } else {
          first = false;
        }
        OneLine.append("s_real_input_").append(i + 1);
      }
      OneLine.append(";");
      Contents.add(OneLine.toString());
      Contents.add("");
      return Contents;
    }
  }

  public static final AndGate FACTORY = new AndGate();

  private AndGate() {
    super("AND Gate", S.getter("andGateComponent"));
    setRectangularLabel("&");
  }

  @Override
  protected Expression computeExpression(Expression[] inputs, int numInputs) {
    Expression ret = inputs[0];
    for (int i = 1; i < numInputs; i++) {
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
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new AndGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

  @Override
  protected void paintDinShape(InstancePainter painter, int width, int height, int inputs) {
    PainterDin.paintAnd(painter, width, height, false);
  }

  @Override
  protected void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize) {
    paintIconANSI(g, iconSize, borderSize, negateSize,false);  
  }

  protected static void paintIconANSI(Graphics2D g, int iconSize, int borderSize, int negateSize, boolean inverted) {
    int ystart = negateSize >>1;
    int yend = iconSize-ystart;
    int rad = (yend-ystart)>>1;
    int xstart = 0;
    int xend = iconSize-negateSize-rad;
    int[] xp = new int[] {xend, xstart, xstart, xend};
    int[] yp = new int[] {ystart, ystart, yend, yend};
    AffineTransform af = g.getTransform();
    g.translate(borderSize, borderSize);
    g.drawPolyline(xp, yp, 4);
    GraphicsUtil.drawCenteredArc(g, xend, iconSize>>1 , rad, -90, 180);
    paintIconPins(g,iconSize,borderSize,negateSize,inverted,false);
    g.setTransform(af);
  }

  @Override
  protected void paintShape(InstancePainter painter, int width, int height) {
    PainterShaped.paintAnd(painter, width, height);
  }
}
