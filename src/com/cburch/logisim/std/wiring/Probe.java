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

package com.cburch.logisim.std.wiring;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class Probe extends InstanceFactory {
	public static class ProbeLogger extends InstanceLogger {
		public ProbeLogger() {
		}

		@Override
		public String getLogName(InstanceState state, Object option) {
			String ret = state.getAttributeValue(StdAttr.LABEL);
			return ret != null && !ret.equals("") ? ret : null;
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			return getValue(state);
		}
	}

	private static class StateData implements InstanceData, Cloneable {
		Value curValue = Value.NIL;

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	static void configureLabel(Instance instance, Direction labelLoc,
			Direction facing) {
		Bounds bds = instance.getBounds();
		int x;
		int y;
		int halign;
		int valign;
		if (labelLoc == Direction.NORTH) {
			halign = TextField.H_CENTER;
			valign = TextField.V_BOTTOM;
			x = bds.getX() + bds.getWidth() / 2;
			y = bds.getY() - 2;
			if (facing == labelLoc) {
				halign = TextField.H_LEFT;
				x += 2;
			}
		} else if (labelLoc == Direction.SOUTH) {
			halign = TextField.H_CENTER;
			valign = TextField.V_TOP;
			x = bds.getX() + bds.getWidth() / 2;
			y = bds.getY() + bds.getHeight() + 2;
			if (facing == labelLoc) {
				halign = TextField.H_LEFT;
				x += 2;
			}
		} else if (labelLoc == Direction.EAST) {
			halign = TextField.H_LEFT;
			valign = TextField.V_CENTER;
			x = bds.getX() + bds.getWidth() + 2;
			y = bds.getY() + bds.getHeight() / 2;
			if (facing == labelLoc) {
				valign = TextField.V_BOTTOM;
				y -= 2;
			}
		} else { // WEST
			halign = TextField.H_RIGHT;
			valign = TextField.V_CENTER;
			x = bds.getX() - 2;
			y = bds.getY() + bds.getHeight() / 2;
			if (facing == labelLoc) {
				valign = TextField.V_BOTTOM;
				y -= 2;
			}
		}

		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign,
				valign);
	}

	//
	// static methods
	//
	static Bounds getOffsetBounds(Direction dir, BitWidth width,
			RadixOption radix) {
		Bounds ret = null;
		int len = radix == null || radix == RadixOption.RADIX_2 ? width
				.getWidth() : radix.getMaxLength(width);
		if (dir == Direction.EAST) {
			switch (len) {
			case 0:
			case 1:
				ret = Bounds.create(-20, -10, 20, 20);
				break;
			case 2:
				ret = Bounds.create(-20, -10, 20, 20);
				break;
			case 3:
				ret = Bounds.create(-30, -10, 30, 20);
				break;
			case 4:
				ret = Bounds.create(-40, -10, 40, 20);
				break;
			case 5:
				ret = Bounds.create(-50, -10, 50, 20);
				break;
			case 6:
				ret = Bounds.create(-60, -10, 60, 20);
				break;
			case 7:
				ret = Bounds.create(-70, -10, 70, 20);
				break;
			case 8:
				ret = Bounds.create(-80, -10, 80, 20);
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
				ret = Bounds.create(-80, -20, 80, 40);
				break;
			case 17:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
				ret = Bounds.create(-80, -30, 80, 60);
				break;
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
				ret = Bounds.create(-80, -40, 80, 80);
				break;
			}
		} else if (dir == Direction.WEST) {
			switch (len) {
			case 0:
			case 1:
				ret = Bounds.create(0, -10, 20, 20);
				break;
			case 2:
				ret = Bounds.create(0, -10, 20, 20);
				break;
			case 3:
				ret = Bounds.create(0, -10, 30, 20);
				break;
			case 4:
				ret = Bounds.create(0, -10, 40, 20);
				break;
			case 5:
				ret = Bounds.create(0, -10, 50, 20);
				break;
			case 6:
				ret = Bounds.create(0, -10, 60, 20);
				break;
			case 7:
				ret = Bounds.create(0, -10, 70, 20);
				break;
			case 8:
				ret = Bounds.create(0, -10, 80, 20);
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
				ret = Bounds.create(0, -20, 80, 40);
				break;
			case 17:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
				ret = Bounds.create(0, -30, 80, 60);
				break;
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
				ret = Bounds.create(0, -40, 80, 80);
				break;
			}
		} else if (dir == Direction.SOUTH) {
			switch (len) {
			case 0:
			case 1:
				ret = Bounds.create(-10, -20, 20, 20);
				break;
			case 2:
				ret = Bounds.create(-10, -20, 20, 20);
				break;
			case 3:
				ret = Bounds.create(-15, -20, 30, 20);
				break;
			case 4:
				ret = Bounds.create(-20, -20, 40, 20);
				break;
			case 5:
				ret = Bounds.create(-25, -20, 50, 20);
				break;
			case 6:
				ret = Bounds.create(-30, -20, 60, 20);
				break;
			case 7:
				ret = Bounds.create(-35, -20, 70, 20);
				break;
			case 8:
				ret = Bounds.create(-40, -20, 80, 20);
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
				ret = Bounds.create(-40, -40, 80, 40);
				break;
			case 17:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
				ret = Bounds.create(-40, -60, 80, 60);
				break;
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
				ret = Bounds.create(-40, -80, 80, 80);
				break;
			}
		} else if (dir == Direction.NORTH) {
			switch (len) {
			case 0:
			case 1:
				ret = Bounds.create(-10, 0, 20, 20);
				break;
			case 2:
				ret = Bounds.create(-10, 0, 20, 20);
				break;
			case 3:
				ret = Bounds.create(-15, 0, 30, 20);
				break;
			case 4:
				ret = Bounds.create(-20, 0, 40, 20);
				break;
			case 5:
				ret = Bounds.create(-25, 0, 50, 20);
				break;
			case 6:
				ret = Bounds.create(-30, 0, 60, 20);
				break;
			case 7:
				ret = Bounds.create(-35, 0, 70, 20);
				break;
			case 8:
				ret = Bounds.create(-40, 0, 80, 20);
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
				ret = Bounds.create(-40, 0, 80, 40);
				break;
			case 17:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
				ret = Bounds.create(-40, 0, 80, 60);
				break;
			case 25:
			case 26:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
				ret = Bounds.create(-40, 0, 80, 80);
				break;
			}
		}
		if (ret == null) {
			ret = Bounds.create(0, -10, 20, 20); // should never happen
		}
		return ret;
	}

	private static Value getValue(InstanceState state) {
		StateData data = (StateData) state.getData();
		return data == null ? Value.NIL : data.curValue;
	}

	static void paintValue(InstancePainter painter, Value value) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds(); // intentionally with no graphics
											// object - we don't want label
											// included

		RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
		if (radix == null || radix == RadixOption.RADIX_2) {
			int x = bds.getX();
			int y = bds.getY();
			int wid = value.getWidth();
			if (wid == 0) {
				x += bds.getWidth() / 2;
				y += bds.getHeight() / 2;
				GraphicsUtil.switchToWidth(g, 2);
				g.drawLine(x - 4, y, x + 4, y);
				return;
			}
			int x0 = bds.getX() + bds.getWidth() - 5;
			int compWidth = wid * 10;
			if (compWidth < bds.getWidth() - 3) {
				x0 = bds.getX() + (bds.getWidth() + compWidth) / 2 - 5;
			}
			int cx = x0;
			int cy = bds.getY() + bds.getHeight() - 12;
			int cur = 0;
			for (int k = 0; k < wid; k++) {
				GraphicsUtil.drawCenteredText(g,
						value.get(k).toDisplayString(), cx, cy);
				++cur;
				if (cur == 8) {
					cur = 0;
					cx = x0;
					cy -= 20;
				} else {
					cx -= 10;
				}
			}
		} else {
			String text = radix.toString(value);
			GraphicsUtil.drawCenteredText(g, text, bds.getX() + bds.getWidth()
					/ 2, bds.getY() + bds.getHeight() / 2);
		}
	}

	public static final Probe FACTORY = new Probe();

	public Probe() {
		super("Probe", Strings.getter("probeComponent"));
		setIconName("probe.gif");
		setFacingAttribute(StdAttr.FACING);
		setInstanceLogger(ProbeLogger.class);
	}

	void configureLabel(Instance instance) {
		ProbeAttributes attrs = (ProbeAttributes) instance.getAttributeSet();
		Probe.configureLabel(instance, attrs.labelloc, attrs.facing);
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		instance.setPorts(new Port[] { new Port(0, 0, Port.INPUT,
				BitWidth.UNKNOWN) });
		instance.addAttributeListener();
		configureLabel(instance);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new ProbeAttributes();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrsBase) {
		ProbeAttributes attrs = (ProbeAttributes) attrsBase;
		return getOffsetBounds(attrs.facing, attrs.width, attrs.radix);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		return true;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == Pin.ATTR_LABEL_LOC) {
			configureLabel(instance);
		} else if (attr == StdAttr.FACING || attr == RadixOption.ATTRIBUTE) {
			instance.recomputeBounds();
			configureLabel(instance);
		}
	}

	//
	// graphics methods
	//
	@Override
	public void paintGhost(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getOffsetBounds();
		g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 1,
				bds.getHeight() - 1);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Value value = getValue(painter);

		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds(); // intentionally with no graphics
											// object - we don't want label
											// included
		int x = bds.getX();
		int y = bds.getY();
		Color back = new Color(0xff, 0xf0, 0x99);
		if (value.getWidth() <= 1) {
			g.setColor(back);
			g.fillOval(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2);
			g.setColor(Color.lightGray);
			g.drawOval(x + 1, y + 1, bds.getWidth() - 2, bds.getHeight() - 2);
		} else {
			g.setColor(back);
			g.fillRoundRect(x + 1, y + 1, bds.getWidth() - 2,
					bds.getHeight() - 2, 20, 20);
			g.setColor(Color.lightGray);
			g.drawRoundRect(x + 1, y + 1, bds.getWidth() - 2,
					bds.getHeight() - 2, 20, 20);
		}

		g.setColor(Color.GRAY);
		painter.drawLabel();
		g.setColor(Color.DARK_GRAY);

		if (!painter.getShowState()) {
			if (value.getWidth() > 0) {
				GraphicsUtil.drawCenteredText(g, "x" + value.getWidth(),
						bds.getX() + bds.getWidth() / 2,
						bds.getY() + bds.getHeight() / 2);
			}
		} else {
			paintValue(painter, value);
		}

		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		StateData oldData = (StateData) state.getData();
		Value oldValue = oldData == null ? Value.NIL : oldData.curValue;
		Value newValue = state.getPortValue(0);
		boolean same = oldValue == null ? newValue == null : oldValue
				.equals(newValue);
		if (!same) {
			if (oldData == null) {
				oldData = new StateData();
				oldData.curValue = newValue;
				state.setData(oldData);
			} else {
				oldData.curValue = newValue;
			}
			int oldWidth = oldValue == null ? 1 : oldValue.getBitWidth()
					.getWidth();
			int newWidth = newValue.getBitWidth().getWidth();
			if (oldWidth != newWidth) {
				ProbeAttributes attrs = (ProbeAttributes) state
						.getAttributeSet();
				attrs.width = newValue.getBitWidth();
				state.getInstance().recomputeBounds();
				configureLabel(state.getInstance());
			}
		}
	}
}