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

package com.cburch.logisim.std.arith;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;

public class Negator extends InstanceFactory {
	private static final int IN = 0;
	private static final int OUT = 1;

	public Negator() {
		super("Negator", Strings.getter("negatorComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH },
				new Object[] { BitWidth.create(8) });
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setOffsetBounds(Bounds.create(-40, -20, 40, 40));
		setIconName("negator.gif");

		Port[] ps = new Port[2];
		ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.WIDTH);
		ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
		ps[IN].setToolTip(Strings.getter("negatorInputTip"));
		ps[OUT].setToolTip(Strings.getter("negatorOutputTip"));
		setPorts(ps);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		if (attrs.getValue(StdAttr.WIDTH).getWidth() == 1)
			CompleteName.append("BitNegator");
		else
			CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
		return CompleteName.toString();
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new NegatorHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		painter.drawBounds();
		painter.drawPort(IN);
		painter.drawPort(OUT, "-x", Direction.WEST);
	}

	@Override
	public void propagate(InstanceState state) {
		// get attributes
		BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

		// compute outputs
		Value in = state.getPortValue(IN);
		Value out;
		if (in.isFullyDefined()) {
			out = Value.createKnown(in.getBitWidth(), -in.toIntValue());
		} else {
			Value[] bits = in.getAll();
			Value fill = Value.FALSE;
			int pos = 0;
			while (pos < bits.length) {
				if (bits[pos] == Value.FALSE) {
					bits[pos] = fill;
				} else if (bits[pos] == Value.TRUE) {
					if (fill != Value.FALSE)
						bits[pos] = fill;
					pos++;
					break;
				} else if (bits[pos] == Value.ERROR) {
					fill = Value.ERROR;
				} else {
					if (fill == Value.FALSE)
						fill = bits[pos];
					else
						bits[pos] = fill;
				}
				pos++;
			}
			while (pos < bits.length) {
				if (bits[pos] == Value.TRUE) {
					bits[pos] = Value.FALSE;
				} else if (bits[pos] == Value.FALSE) {
					bits[pos] = Value.TRUE;
				}
				pos++;
			}
			out = Value.create(bits);
		}

		// propagate them
		int delay = (dataWidth.getWidth() + 2) * Adder.PER_DELAY;
		state.setPort(OUT, out, delay);
	}
}
