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
import java.awt.Graphics2D;

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
import com.cburch.logisim.prefs.AppPreferences;
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

	public static void configureLabel(Instance instance, Direction labelLoc,
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
	public static Bounds getOffsetBounds(Direction dir, BitWidth width,
			RadixOption radix, boolean NewLayout, boolean IsPin) {
		int len = radix == null || radix == RadixOption.RADIX_2 ? width
				.getWidth() : radix.getMaxLength(width);
		int bwidth,bheight,x,y;
		if (radix == RadixOption.RADIX_2) {
			bwidth = (len < 2) ? 20 : (len >= 8) ? 80 : len*10;
			bheight = (len > 24) ? 80 : (len > 16) ? 60 : (len > 8) ? 40 : 20;
		} else {
			bwidth = (len<2) ? 20 : len*10;
			bheight = 20;
		}
		if (NewLayout) bwidth+=20;
		if (dir == Direction.EAST) {
			x = -bwidth;
			y = -(bheight/2);
		} else if (dir == Direction.WEST) {
			x = 0;
			y = -(bheight/2);
		} else if (dir == Direction.SOUTH) {
			if (NewLayout&IsPin) bheight += 10;
			if ((len==1)&NewLayout&IsPin) {
				bwidth = 20;
				bheight += 10;
			}
			x = -(bwidth/2);
			y = -bheight;
		} else {
			if (NewLayout&IsPin) bheight += 10;
			if ((len==1)&NewLayout&IsPin) {
				bwidth = 20;
				bheight += 10;
			}
			x = -(bwidth/2);
			y = 0;
		}
		return Bounds.create(x, y, bwidth, bheight);
	}

	private static Value getValue(InstanceState state) {
		StateData data = (StateData) state.getData();
		return data == null ? Value.NIL : data.curValue;
	}
	static void paintValue(InstancePainter painter, Value value) {
		if (AppPreferences.NEW_INPUT_OUTPUT_SHAPES.getBoolean())
			paintValue(painter,value,false,false);
		else
			paintOldStyleValue(painter,value);
	}
	
	static void paintOldStyleValue(InstancePainter painter, Value value) {
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
	
	static int BinairyXoffset(Direction dir, boolean IsPin, boolean IsOutput) {
		boolean East  = dir == Direction.EAST;
		boolean West  = dir == Direction.WEST;
		if (IsPin&East&!IsOutput)
			return 25;
		if (IsPin&West&!IsOutput)
			return 15;
		return 20;
	}
	
	static void paintValue(InstancePainter painter, Value value, boolean colored, boolean extend) {
		if (!AppPreferences.NEW_INPUT_OUTPUT_SHAPES.getBoolean()) {
			paintOldStyleValue(painter,value);
			return;
		}
		Graphics g = painter.getGraphics();
		Graphics2D g2 = (Graphics2D)g;
		Bounds bds = painter.getBounds(); // intentionally with no graphics
											// object - we don't want label
											// included

		RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
		Direction dir = painter.getAttributeValue(StdAttr.FACING);
		boolean IsOutput = (painter.getAttributeSet().containsAttribute(Pin.ATTR_TYPE)) ? painter.getAttributeValue(Pin.ATTR_TYPE):false;
		boolean IsPin = (painter.getAttributeSet().containsAttribute(Pin.ATTR_TYPE));
		boolean North = dir == Direction.NORTH;
		boolean South = dir == Direction.SOUTH;
		boolean East  = dir == Direction.EAST;
		boolean West  = dir == Direction.WEST;
		int LabelYOffset = extend&(South&!IsOutput) ? 10 : extend&IsOutput&(South|North) ? 7 : 0;
		int LabelValueXOffset = IsPin&(North|South)&(bds.getWidth()==20) ? 7 : 
			                    IsPin&!IsOutput&East ? 20 :
			                    IsPin&!IsOutput&West ? 7 : 15;
		g.setColor(Color.BLUE);
		g2.scale(0.7, 0.7);
		g2.drawString(radix.GetIndexChar(), (int)((bds.getX()+bds.getWidth()-LabelValueXOffset)/0.7), 
				     (int)((bds.getY()+bds.getHeight()-(2+LabelYOffset))/0.7));
		g2.scale(1.0/0.7, 1.0/0.7);
		g.setColor(Color.BLACK);
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
			int yoffset = extend&(South&!IsOutput) ? 22 : extend&IsOutput&(South|North) ? 17 : 12;
			int x0 = bds.getX() + bds.getWidth() - BinairyXoffset(dir,IsPin,IsOutput);
			int cx = x0;
			int cy = bds.getY() + bds.getHeight() - yoffset;
			int cur = 0;
			for (int k = 0; k < wid; k++) {
				if (colored) {
					g.setColor(value.get(k).getColor());
					g.fillOval(cx-4, cy-6, 9, 16);
					g.setColor(Color.WHITE);
				}
				GraphicsUtil.drawCenteredText(g,
						value.get(k).toDisplayString(), cx, cy);
				if (colored)
					g.setColor(Color.BLACK);
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
			int off1 = IsOutput ? 5 : 10;
			int off2 = IsOutput ? 5 : 0;
			int ypos = (North&extend) ? bds.getY()+off1+(bds.getHeight()-15)/2 :
				       extend&South ? bds.getY()+off2+(bds.getHeight()-15)/2 :
				    	   bds.getY() + bds.getHeight() / 2;
			GraphicsUtil.drawText(g, text, bds.getX() + bds.getWidth()-LabelValueXOffset,ypos , 
					GraphicsUtil.H_RIGHT, GraphicsUtil.H_CENTER);
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
		return getOffsetBounds(attrs.facing, attrs.width, attrs.radix,AppPreferences.NEW_INPUT_OUTPUT_SHAPES.getBoolean(),false);
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
		/* dirty hack to make the pin change shape correctly when in the preferences the new -> old shapes are changed */
        painter.getInstance().recomputeBounds();
        /* end dirty hack */
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