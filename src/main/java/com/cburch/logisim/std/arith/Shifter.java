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

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
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

public class Shifter extends InstanceFactory {
	static final AttributeOption SHIFT_LOGICAL_LEFT = new AttributeOption("ll",
			Strings.getter("shiftLogicalLeft"));
	static final AttributeOption SHIFT_LOGICAL_RIGHT = new AttributeOption(
			"lr", Strings.getter("shiftLogicalRight"));
	static final AttributeOption SHIFT_ARITHMETIC_RIGHT = new AttributeOption(
			"ar", Strings.getter("shiftArithmeticRight"));
	static final AttributeOption SHIFT_ROLL_LEFT = new AttributeOption("rl",
			Strings.getter("shiftRollLeft"));
	static final AttributeOption SHIFT_ROLL_RIGHT = new AttributeOption("rr",
			Strings.getter("shiftRollRight"));
	static final Attribute<AttributeOption> ATTR_SHIFT = Attributes
			.forOption("shift", Strings.getter("shifterShiftAttr"),
					new AttributeOption[] { SHIFT_LOGICAL_LEFT,
							SHIFT_LOGICAL_RIGHT, SHIFT_ARITHMETIC_RIGHT,
							SHIFT_ROLL_LEFT, SHIFT_ROLL_RIGHT });

	static final int IN0 = 0;
	static final int IN1 = 1;
	static final int OUT = 2;

	public Shifter() {
		super("Shifter", Strings.getter("shifterComponent"));
		setAttributes(new Attribute[] { StdAttr.WIDTH, ATTR_SHIFT },
				new Object[] { BitWidth.create(8), SHIFT_LOGICAL_LEFT });
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setOffsetBounds(Bounds.create(-40, -20, 40, 40));
		setIconName("shifter.gif");
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		configurePorts(instance);
		instance.addAttributeListener();
	}

	private void configurePorts(Instance instance) {
		BitWidth dataWid = instance.getAttributeValue(StdAttr.WIDTH);
		int data = dataWid == null ? 32 : dataWid.getWidth();
		int shift = 1;
		while ((1 << shift) < data)
			shift++;

		Port[] ps = new Port[3];
		ps[IN0] = new Port(-40, -10, Port.INPUT, data);
		ps[IN1] = new Port(-40, 10, Port.INPUT, shift);
		ps[OUT] = new Port(0, 0, Port.OUTPUT, data);
		ps[IN0].setToolTip(Strings.getter("shifterInputTip"));
		ps[IN1].setToolTip(Strings.getter("shifterDistanceTip"));
		ps[OUT].setToolTip(Strings.getter("shifterOutputTip"));
		instance.setPorts(ps);
	}

	private void drawArrow(Graphics g, int x, int y, int d) {
		int[] px = { x + d, x, x + d };
		int[] py = { y + d, y, y - d };
		g.fillPolygon(px, py, 3);
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		StringBuffer CompleteName = new StringBuffer();
		CompleteName.append("Shifter_"
				+ attrs.getValue(StdAttr.WIDTH).getWidth() + "_bit");
		return CompleteName.toString();
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new ShifterHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.WIDTH) {
			configurePorts(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawBounds();

		painter.drawPorts();

		Location loc = painter.getLocation();
		int x = loc.getX() - 15;
		int y = loc.getY();
		Object shift = painter.getAttributeValue(ATTR_SHIFT);
		g.setColor(Color.BLACK);
		if (shift == SHIFT_LOGICAL_RIGHT) {
			g.fillRect(x, y - 1, 8, 3);
			drawArrow(g, x + 10, y, -4);
		} else if (shift == SHIFT_ARITHMETIC_RIGHT) {
			g.fillRect(x, y - 1, 2, 3);
			g.fillRect(x + 3, y - 1, 5, 3);
			drawArrow(g, x + 10, y, -4);
		} else if (shift == SHIFT_ROLL_RIGHT) {
			g.fillRect(x, y - 1, 5, 3);
			g.fillRect(x + 8, y - 7, 2, 8);
			g.fillRect(x, y - 7, 2, 8);
			g.fillRect(x, y - 7, 10, 2);
			drawArrow(g, x + 8, y, -4);
		} else if (shift == SHIFT_ROLL_LEFT) {
			g.fillRect(x + 6, y - 1, 4, 3);
			g.fillRect(x + 8, y - 7, 2, 8);
			g.fillRect(x, y - 7, 2, 8);
			g.fillRect(x, y - 7, 10, 2);
			drawArrow(g, x + 3, y, 4);
		} else { // SHIFT_LOGICAL_LEFT
			g.fillRect(x + 2, y - 1, 8, 3);
			drawArrow(g, x, y, 4);
		}
	}

	@Override
	public void propagate(InstanceState state) {
		// compute output
		BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
		int bits = dataWidth == null ? 32 : dataWidth.getWidth();
		Value vx = state.getPortValue(IN0);
		Value vd = state.getPortValue(IN1);
		Value vy; // y will by x shifted by d
		if (vd.isFullyDefined() && vx.getWidth() == bits) {
			int d = vd.toIntValue();
			Object shift = state.getAttributeValue(ATTR_SHIFT);
			if (d == 0) {
				vy = vx;
			} else if (vx.isFullyDefined()) {
				int x = vx.toIntValue();
				int y;
				if (shift == SHIFT_LOGICAL_RIGHT) {
					y = x >>> d;
				} else if (shift == SHIFT_ARITHMETIC_RIGHT) {
					if (d >= bits)
						d = bits - 1;
					y = x >> d | ((x << (32 - bits)) >> (32 - bits + d));
				} else if (shift == SHIFT_ROLL_RIGHT) {
					if (d >= bits)
						d -= bits;
					y = (x >>> d) | (x << (bits - d));
				} else if (shift == SHIFT_ROLL_LEFT) {
					if (d >= bits)
						d -= bits;
					y = (x << d) | (x >>> (bits - d));
				} else { // SHIFT_LOGICAL_LEFT
					y = x << d;
				}
				vy = Value.createKnown(dataWidth, y);
			} else {
				Value[] x = vx.getAll();
				Value[] y = new Value[bits];
				if (shift == SHIFT_LOGICAL_RIGHT) {
					if (d >= bits)
						d = bits;
					System.arraycopy(x, d, y, 0, bits - d);
					Arrays.fill(y, bits - d, bits, Value.FALSE);
				} else if (shift == SHIFT_ARITHMETIC_RIGHT) {
					if (d >= bits)
						d = bits;
					System.arraycopy(x, d, y, 0, x.length - d);
					Arrays.fill(y, bits - d, y.length, x[bits - 1]);
				} else if (shift == SHIFT_ROLL_RIGHT) {
					if (d >= bits)
						d -= bits;
					System.arraycopy(x, d, y, 0, bits - d);
					System.arraycopy(x, 0, y, bits - d, d);
				} else if (shift == SHIFT_ROLL_LEFT) {
					if (d >= bits)
						d -= bits;
					System.arraycopy(x, x.length - d, y, 0, d);
					System.arraycopy(x, 0, y, d, bits - d);
				} else { // SHIFT_LOGICAL_LEFT
					if (d >= bits)
						d = bits;
					Arrays.fill(y, 0, d, Value.FALSE);
					System.arraycopy(x, 0, y, d, bits - d);
				}
				vy = Value.create(y);
			}
		} else {
			vy = Value.createError(dataWidth);
		}

		// propagate them
		int delay = dataWidth.getWidth() * (3 * Adder.PER_DELAY);
		state.setPort(OUT, vy, delay);
	}
}
