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

/**
 * Based on PUCTools (v0.9 beta) by CRC - PUC - Minas (pucmg.crc at gmail.com)
 */

package com.cburch.logisim.std.wiring;

import java.awt.Graphics2D;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Power extends InstanceFactory {
	private class PowerHDLGeneratorFactory extends
			AbstractConstantHDLGeneratorFactory {
		@Override
		public int GetConstant(AttributeSet attrs) {
			int ConstantValue = 0;
			for (int bit = 0; bit < attrs.getValue(StdAttr.WIDTH).getWidth(); bit++) {
				ConstantValue <<= 1;
				ConstantValue |= 1;
			}
			return ConstantValue;
		}
	}

	public Power() {
		super("Power", Strings.getter("powerComponent"));
		setIconName("power.gif");
		setAttributes(new Attribute[] { StdAttr.FACING, StdAttr.WIDTH },
				new Object[] { Direction.NORTH, BitWidth.ONE });
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setPorts(new Port[] { new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH) });
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
	}

	private void drawInstance(InstancePainter painter, boolean isGhost) {
		Graphics2D g = (Graphics2D) painter.getGraphics().create();
		Location loc = painter.getLocation();
		g.translate(loc.getX(), loc.getY());

		Direction from = painter.getAttributeValue(StdAttr.FACING);
		int degrees = Direction.EAST.toDegrees() - from.toDegrees();
		double radians = Math.toRadians((degrees + 360) % 360);
		g.rotate(radians);

		GraphicsUtil.switchToWidth(g, Wire.WIDTH);
		if (!isGhost && painter.getShowState()) {
			g.setColor(painter.getPortValue(0).getColor());
		}
		g.drawLine(0, 0, 5, 0);

		GraphicsUtil.switchToWidth(g, 1);
		if (!isGhost && painter.shouldDrawColor()) {
			BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
			g.setColor(Value.repeat(Value.TRUE, width.getWidth()).getColor());
		}
		g.drawPolygon(new int[] { 6, 14, 6 }, new int[] { -8, 0, 8 }, 3);

		g.dispose();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		return Bounds.create(0, -8, 15, 16).rotate(Direction.EAST,
				attrs.getValue(StdAttr.FACING), 0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new PowerHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		drawInstance(painter, true);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		drawInstance(painter, false);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
		state.setPort(0, Value.repeat(Value.TRUE, width.getWidth()), 1);
	}

}
