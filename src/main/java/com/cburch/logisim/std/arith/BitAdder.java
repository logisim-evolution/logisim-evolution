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

import java.awt.Graphics;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class BitAdder extends InstanceFactory {
	static final Attribute<Integer> NUM_INPUTS = Attributes.forIntegerRange(
			"inputs", Strings.getter("gateInputsAttr"), 1, 32);

	public BitAdder() {
		super("BitAdder", Strings.getter("bitAdderComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH, NUM_INPUTS },
				new Object[] { BitWidth.create(8), Integer.valueOf(1) });
		setKeyConfigurator(JoinedConfigurator.create(new IntegerConfigurator(
				NUM_INPUTS, 1, 32, 0), new BitWidthConfigurator(StdAttr.WIDTH)));
		setIconName("bitadder.gif");
	}

	private int computeOutputBits(int width, int inputs) {
		int maxBits = width * inputs;
		int outWidth = 1;
		while ((1 << outWidth) <= maxBits)
			outWidth++;
		return outWidth;
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
		instance.addAttributeListener();
	}

	private void configurePorts(Instance instance) {
		BitWidth inWidth = instance.getAttributeValue(StdAttr.WIDTH);
		int inputs = instance.getAttributeValue(NUM_INPUTS).intValue();
		int outWidth = computeOutputBits(inWidth.getWidth(), inputs);

		int y;
		int dy = 10;
		switch (inputs) {
		case 1:
			y = 0;
			break;
		case 2:
			y = -10;
			dy = 20;
			break;
		case 3:
			y = -10;
			break;
		default:
			y = ((inputs - 1) / 2) * -10;
		}

		Port[] ps = new Port[inputs + 1];
		ps[0] = new Port(0, 0, Port.OUTPUT, BitWidth.create(outWidth));
		ps[0].setToolTip(Strings.getter("bitAdderOutputManyTip"));
		for (int i = 0; i < inputs; i++) {
			ps[i + 1] = new Port(-40, y + i * dy, Port.INPUT, inWidth);
			ps[i + 1].setToolTip(Strings.getter("bitAdderInputTip"));
		}
		instance.setPorts(ps);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		int inputs = attrs.getValue(NUM_INPUTS).intValue();
		int h = Math.max(40, 10 * inputs);
		int y = inputs < 4 ? 20 : (((inputs - 1) / 2) * 10 + 5);
		return Bounds.create(-40, -y, 40, h);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.WIDTH) {
			configurePorts(instance);
		} else if (attr == NUM_INPUTS) {
			configurePorts(instance);
			instance.recomputeBounds();
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawBounds();
		painter.drawPorts();

		GraphicsUtil.switchToWidth(g, 2);
		Location loc = painter.getLocation();
		int x = loc.getX() - 10;
		int y = loc.getY();
		g.drawLine(x - 2, y - 5, x - 2, y + 5);
		g.drawLine(x + 2, y - 5, x + 2, y + 5);
		g.drawLine(x - 5, y - 2, x + 5, y - 2);
		g.drawLine(x - 5, y + 2, x + 5, y + 2);
	}

	@Override
	public void propagate(InstanceState state) {
		int width = state.getAttributeValue(StdAttr.WIDTH).getWidth();
		int inputs = state.getAttributeValue(NUM_INPUTS).intValue();

		// compute the number of 1 bits
		int minCount = 0; // number that are definitely 1
		int maxCount = 0; // number that are definitely not 0 (incl X/Z)
		for (int i = 1; i <= inputs; i++) {
			Value v = state.getPortValue(i);
			Value[] bits = v.getAll();
			for (int j = 0; j < bits.length; j++) {
				Value b = bits[j];
				if (b == Value.TRUE)
					minCount++;
				if (b != Value.FALSE)
					maxCount++;
			}
		}

		// compute which output bits should be error bits
		int unknownMask = 0;
		for (int i = minCount + 1; i <= maxCount; i++) {
			unknownMask |= (minCount ^ i);
		}

		Value[] out = new Value[computeOutputBits(width, inputs)];
		for (int i = 0; i < out.length; i++) {
			if (((unknownMask >> i) & 1) != 0) {
				out[i] = Value.ERROR;
			} else if (((minCount >> i) & 1) != 0) {
				out[i] = Value.TRUE;
			} else {
				out[i] = Value.FALSE;
			}
		}

		int delay = out.length * Adder.PER_DELAY;
		state.setPort(0, Value.create(out), delay);
	}
}
