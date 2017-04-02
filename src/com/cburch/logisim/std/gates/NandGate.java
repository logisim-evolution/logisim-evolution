/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.gates;

import java.awt.Graphics;
import java.util.ArrayList;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;

class NandGate extends AbstractGate {

	private class NandGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
		@Override
		public boolean GetFloatingValue(boolean is_inverted) {
			return is_inverted;
		}

		@Override
		public ArrayList<String> GetLogicFunction(int nr_of_inputs,
				int bitwidth, boolean is_one_hot, String HDLType) {
			ArrayList<String> Contents = new ArrayList<String>();
			String Preamble = (HDLType.equals(VHDL) ? "" : "assign ");
			String AndOperation = (HDLType.equals(VHDL) ? " AND"
					: " &");
			String AssignOperation = (HDLType.equals(VHDL) ? " <= "
					: " = ");
			String NotOperation = (HDLType.equals(VHDL) ? "NOT" : "~");
			StringBuffer OneLine = new StringBuffer();
			OneLine.append("   " + Preamble + "Result" + AssignOperation
					+ NotOperation + "(");
			int TabWidth = OneLine.length();
			boolean first = true;
			for (int i = 0; i < nr_of_inputs; i++) {
				if (!first) {
					OneLine.append(AndOperation);
					Contents.add(OneLine.toString());
					OneLine.setLength(0);
					while (OneLine.length() < TabWidth) {
						OneLine.append(" ");
					}
				} else {
					first = false;
				}
				OneLine.append("s_real_input_" + Integer.toString(i + 1));
			}
			OneLine.append(");");
			Contents.add(OneLine.toString());
			Contents.add("");
			return Contents;
		}

	}

	public static NandGate FACTORY = new NandGate();

	private NandGate() {
		super("NAND Gate", Strings.getter("nandGateComponent"));
		setNegateOutput(true);
		setRectangularLabel(AndGate.FACTORY.getRectangularLabel(null));
		setIconNames("nandGate.gif", "nandGateRect.gif", "dinNandGate.gif");
	}

	@Override
	protected Expression computeExpression(Expression[] inputs, int numInputs) {
		Expression ret = inputs[0];
		for (int i = 1; i < numInputs; i++) {
			ret = Expressions.and(ret, inputs[i]);
		}
		return Expressions.not(ret);
	}

	@Override
	protected Value computeOutput(Value[] inputs, int numInputs,
			InstanceState state) {
		return GateFunctions.computeAnd(inputs, numInputs).not();
	}

	@Override
	protected Value getIdentity() {
		return Value.TRUE;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new NandGateHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void paintDinShape(InstancePainter painter, int width,
			int height, int inputs) {
		PainterDin.paintAnd(painter, width, height, true);
	}

	@Override
	public void paintIconShaped(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		int[] xp = new int[] { 8, 0, 0, 8 };
		int[] yp = new int[] { 2, 2, 18, 18 };
		g.drawPolyline(xp, yp, 4);
		GraphicsUtil.drawCenteredArc(g, 8, 10, 8, -90, 180);
		g.drawOval(16, 8, 4, 4);
	}

	@Override
	protected void paintShape(InstancePainter painter, int width, int height) {
		PainterShaped.paintAnd(painter, width, height);
	}

}
