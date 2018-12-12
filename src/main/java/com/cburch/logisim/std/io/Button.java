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
import java.awt.event.MouseEvent;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class Button extends InstanceFactory {
	public static class Logger extends InstanceLogger {
		@Override
		public String getLogName(InstanceState state, Object option) {
			return state.getAttributeValue(StdAttr.LABEL);
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			InstanceDataSingleton data = (InstanceDataSingleton) state
					.getData();
			return data == null ? Value.FALSE : (Value) data.getValue();
		}
	}

	public static class Poker extends InstancePoker {
		@Override
		public void mousePressed(InstanceState state, MouseEvent e) {
			setValue(state, Value.TRUE);
		}

		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
			setValue(state, Value.FALSE);
		}

		private void setValue(InstanceState state, Value val) {
			InstanceDataSingleton data = (InstanceDataSingleton) state
					.getData();
			if (data == null) {
				state.setData(new InstanceDataSingleton(val));
			} else {
				data.setValue(val);
			}
			state.getInstance().fireInvalidated();
		}
	}

	private static final int DEPTH = 3;

	public Button() {
		super("Button", Strings.getter("buttonComponent"));
		setAttributes(new Attribute[] { StdAttr.FACING, Io.ATTR_COLOR,
				StdAttr.LABEL, Io.ATTR_LABEL_LOC, StdAttr.LABEL_FONT,
				StdAttr.LABEL_COLOR, StdAttr.LABEL_VISIBILITY }, new Object[] { Direction.EAST,
				Color.WHITE, "", Direction.WEST, StdAttr.DEFAULT_LABEL_FONT,
				StdAttr.DEFAULT_LABEL_COLOR,true });
		setFacingAttribute(StdAttr.FACING);
		setIconName("button.gif");
		setPorts(new Port[] { new Port(0, 0, Port.OUTPUT, 1) });
		setInstancePoker(Poker.class);
		setInstanceLogger(Logger.class);
		MyIOInformation = new IOComponentInformationContainer(1, 0, 0,
				FPGAIOInformationContainer.IOComponentTypes.Button);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
	}

	private void computeTextField(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		Object labelLoc = instance.getAttributeValue(Io.ATTR_LABEL_LOC);

		Bounds bds = instance.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int halign = GraphicsUtil.H_CENTER;
		int valign = GraphicsUtil.V_CENTER;
		if (labelLoc == Io.LABEL_CENTER) {
			x = bds.getX() + (bds.getWidth() - DEPTH) / 2;
			y = bds.getY() + (bds.getHeight() - DEPTH) / 2;
		} else if (labelLoc == Direction.NORTH) {
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
		Direction facing = attrs.getValue(StdAttr.FACING);
		return Bounds.create(-20, -10, 20, 20).rotate(Direction.EAST, facing,
				0, 0);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new ButtonHDLGeneratorFactory();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == StdAttr.FACING) {
			instance.recomputeBounds();
			computeTextField(instance);
		} else if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Bounds bds = painter.getBounds();
		int x = bds.getX();
		int y = bds.getY();
		int w = bds.getWidth();
		int h = bds.getHeight();

		Value val;
		if (painter.getShowState()) {
			InstanceDataSingleton data = (InstanceDataSingleton) painter
					.getData();
			val = data == null ? Value.FALSE : (Value) data.getValue();
		} else {
			val = Value.FALSE;
		}

		Color color = painter.getAttributeValue(Io.ATTR_COLOR);
		if (!painter.shouldDrawColor()) {
			int hue = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
			color = new Color(hue, hue, hue);
		}

		Graphics g = painter.getGraphics();
		int depress;
		if (val == Value.TRUE) {
			x += DEPTH;
			y += DEPTH;
			Object labelLoc = painter.getAttributeValue(Io.ATTR_LABEL_LOC);
			if (labelLoc == Io.LABEL_CENTER || labelLoc == Direction.NORTH
					|| labelLoc == Direction.WEST) {
				depress = DEPTH;
			} else {
				depress = 0;
			}

			Object facing = painter.getAttributeValue(StdAttr.FACING);
			if (facing == Direction.NORTH || facing == Direction.WEST) {
				Location p = painter.getLocation();
				int px = p.getX();
				int py = p.getY();
				GraphicsUtil.switchToWidth(g, Wire.WIDTH);
				g.setColor(Value.TRUE_COLOR);
				if (facing == Direction.NORTH)
					g.drawLine(px, py, px, py + 10);
				else
					g.drawLine(px, py, px + 10, py);
				GraphicsUtil.switchToWidth(g, 1);
			}

			g.setColor(color);
			g.fillRect(x, y, w - DEPTH, h - DEPTH);
			g.setColor(Color.BLACK);
			g.drawRect(x, y, w - DEPTH, h - DEPTH);
		} else {
			depress = 0;
			int[] xp = new int[] { x, x + w - DEPTH, x + w, x + w, x + DEPTH, x };
			int[] yp = new int[] { y, y, y + DEPTH, y + h, y + h, y + h - DEPTH };
			g.setColor(color.darker());
			g.fillPolygon(xp, yp, xp.length);
			g.setColor(color);
			g.fillRect(x, y, w - DEPTH, h - DEPTH);
			g.setColor(Color.BLACK);
			g.drawRect(x, y, w - DEPTH, h - DEPTH);
			g.drawLine(x + w - DEPTH, y + h - DEPTH, x + w, y + h);
			g.drawPolygon(xp, yp, xp.length);
		}

		g.translate(depress, depress);
		painter.drawLabel();
		g.translate(-depress, -depress);
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
		Value val = data == null ? Value.FALSE : (Value) data.getValue();
		state.setPort(0, val, 1);
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

}
