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
package com.cburch.logisim.std.io;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class RGBLed extends InstanceFactory {

	public static class Logger extends InstanceLogger {
		@Override
		public String getLogName(InstanceState state, Object option) {
			return state.getAttributeValue(StdAttr.LABEL);
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			InstanceDataSingleton data = (InstanceDataSingleton) state
					.getData();
			if (data == null)
				return Value.FALSE;
			return data.getValue() == Value.TRUE ? Value.TRUE : Value.FALSE;
		}
	}

	public static final ArrayList<String> GetLabels() {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < 3; i++)
			LabelNames.add("");
		LabelNames.set(RED, "RED");
		LabelNames.set(GREEN, "GREEN");
		LabelNames.set(BLUE, "BLUE");
		return LabelNames;
	}

	public static final int RED = 0;

	public static final int GREEN = 1;

	public static final int BLUE = 2;

	public RGBLed() {
		super("RGBLED", Strings.getter("RGBledComponent"));
		setAttributes(new Attribute[] { Io.ATTR_ACTIVE, StdAttr.LABEL,
				Io.ATTR_LABEL_LOC, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR, StdAttr.LABEL_VISIBILITY },
				new Object[] { Boolean.TRUE, "", Direction.EAST,
						StdAttr.DEFAULT_LABEL_FONT, StdAttr.DEFAULT_LABEL_COLOR, true });
		setFacingAttribute(StdAttr.FACING);
		setIconName("rgbled.gif");
		Port[] ps = new Port[3];
		ps[RED] = new Port(0, 0, Port.INPUT, 1);
		ps[GREEN] = new Port(10, -10, Port.INPUT, 1);
		ps[BLUE] = new Port(10, 10, Port.INPUT, 1);
		ps[RED].setToolTip(Strings.getter("RED"));
		ps[GREEN].setToolTip(Strings.getter("GREEN"));
		ps[BLUE].setToolTip(Strings.getter("BLUE"));
		setPorts(ps);
		setInstanceLogger(Logger.class);
		MyIOInformation = new IOComponentInformationContainer(0, 3, 0, null,
				GetLabels(), null,
				FPGAIOInformationContainer.IOComponentTypes.RGBLED);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.LED);
	}

	@Override
	public boolean ActiveOnHigh(AttributeSet attrs) {
		return attrs.getValue(Io.ATTR_ACTIVE);
	}

	private void computeTextField(Instance instance) {
		Direction facing = Direction.WEST;
		Object labelLoc = instance.getAttributeValue(Io.ATTR_LABEL_LOC);

		Bounds bds = instance.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int halign = GraphicsUtil.H_CENTER;
		int valign = GraphicsUtil.V_CENTER;
		if (labelLoc == Direction.NORTH) {
			y = bds.getY() - 2;
			valign = GraphicsUtil.V_BOTTOM;
		} else if (labelLoc == Direction.SOUTH) {
			y = bds.getY() + bds.getHeight() + 2;
			valign = GraphicsUtil.V_TOP;
		} else if (labelLoc == Direction.EAST) {
			x = bds.getX() + bds.getWidth() + 2;
			halign = GraphicsUtil.H_LEFT;
		} else if (labelLoc == Direction.WEST) {
			x = bds.getX() - 2;
			halign = GraphicsUtil.H_RIGHT;
		}
		if (labelLoc == facing) {
			if (labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
				x += 2;
				halign = GraphicsUtil.H_LEFT;
			} else {
				y -= 2;
				valign = GraphicsUtil.V_BOTTOM;
			}
		}

		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign,
				valign);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		computeTextField(instance);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST,
				Direction.WEST, 0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new AbstractLedHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		}
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 2,
				bds.getHeight() - 2);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
		int summ = (data == null ? 0 : ((Integer) data.getValue()).intValue());
		Bounds bds = painter.getBounds().expand(-1);

		Graphics g = painter.getGraphics();
		if (painter.getShowState()) {
			Boolean activ = painter.getAttributeValue(Io.ATTR_ACTIVE);
			int mask = activ.booleanValue() ? 0 : 7;
			summ ^= mask;
			int red = ((summ >> RED) & 1) * 0xFF;
			int green = ((summ >> GREEN) & 1) * 0xFF;
			int blue = ((summ >> BLUE) & 1) * 0xFF;
			Color LedColor = new Color(red, green, blue);
			g.setColor(LedColor);
			g.fillOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		}
		g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		GraphicsUtil.switchToWidth(g, 1);
		painter.drawLabel();
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		int summary = 0;
		for (int i = 0; i < 3; i++) {
			Value val = state.getPortValue(i);
			if (val == Value.TRUE)
				summary |= 1 << i;
		}
		Object value = Integer.valueOf(summary);
		InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
		if (data == null) {
			state.setData(new InstanceDataSingleton(value));
		} else {
			data.setValue(value);
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
}
