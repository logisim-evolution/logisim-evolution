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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;

class XorGate extends AbstractGate {
	private class XorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {
		@Override
		public ArrayList<String> GetLogicFunction(int nr_of_inputs,
				int bitwidth, boolean is_one_hot, String HDLType) {
			ArrayList<String> Contents = new ArrayList<String>();
			if (is_one_hot) {
				Contents.addAll(GetOneHot(false, nr_of_inputs, bitwidth > 1,
						HDLType));
			} else {
				Contents.addAll(GetParity(false, nr_of_inputs, bitwidth > 1,
						HDLType));
			}
			Contents.add("");
			return Contents;
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

	public static XorGate FACTORY = new XorGate();

	private XorGate() {
		super("XOR Gate", Strings.getter("xorGateComponent"), true);
		setAdditionalWidth(10);
		setIconNames("xorGate.gif", "xorGateRect.gif", "dinXorGate.gif");
		setPaintInputLines(true);
	}

	@Override
	protected Expression computeExpression(Expression[] inputs, int numInputs) {
		return xorExpression(inputs, numInputs);
	}

	@Override
	protected Value computeOutput(Value[] inputs, int numInputs,
			InstanceState state) {
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
		if (attrs == null)
			return "";
		boolean isOdd = false;
		Object behavior = attrs.getValue(GateAttributes.ATTR_XOR);
		if (behavior == GateAttributes.XOR_ODD) {
			Object inputs = attrs.getValue(GateAttributes.ATTR_INPUTS);
			if (inputs == null || ((Integer) inputs).intValue() != 2) {
				isOdd = true;
			}
		}
		return isOdd ? "2k+1" : "=1";
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new XorGateHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void paintDinShape(InstancePainter painter, int width,
			int height, int inputs) {
		PainterDin.paintXor(painter, width, height, false);
	}

	@Override
	public void paintIconShaped(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		GraphicsUtil.drawCenteredArc(g, 2, -5, 22, -90, 53);
		GraphicsUtil.drawCenteredArc(g, 2, 23, 22, 90, -53);
		GraphicsUtil.drawCenteredArc(g, -10, 9, 16, -30, 60);
		GraphicsUtil.drawCenteredArc(g, -12, 9, 16, -30, 60);
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
