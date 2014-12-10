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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Joystick extends InstanceFactory {
	public static class Poker extends InstancePoker {
		@Override
		public void mouseDragged(InstanceState state, MouseEvent e) {
			Location loc = state.getInstance().getLocation();
			int cx = loc.getX() - 15;
			int cy = loc.getY() + 5;
			updateState(state, e.getX() - cx, e.getY() - cy);
		}

		@Override
		public void mousePressed(InstanceState state, MouseEvent e) {
			mouseDragged(state, e);
		}

		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
			updateState(state, 0, 0);
		}

		@Override
		public void paint(InstancePainter painter) {
			State state = (State) painter.getData();
			if (state == null) {
				state = new State(0, 0);
				painter.setData(state);
			}
			Location loc = painter.getLocation();
			int x = loc.getX();
			int y = loc.getY();
			Graphics g = painter.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(x - 20, y, 10, 10);
			GraphicsUtil.switchToWidth(g, 3);
			g.setColor(Color.BLACK);
			int dx = state.xPos;
			int dy = state.yPos;
			int x0 = x - 15 + (dx > 5 ? 1 : dx < -5 ? -1 : 0);
			int y0 = y + 5 + (dy > 5 ? 1 : dy < 0 ? -1 : 0);
			int x1 = x - 15 + dx;
			int y1 = y + 5 + dy;
			g.drawLine(x0, y0, x1, y1);
			Color ballColor = painter.getAttributeValue(Io.ATTR_COLOR);
			Joystick.drawBall(g, x1, y1, ballColor, true);
		}

		private void updateState(InstanceState state, int dx, int dy) {
			State s = (State) state.getData();
			if (dx < -14)
				dx = -14;
			if (dy < -14)
				dy = -14;
			if (dx > 14)
				dx = 14;
			if (dy > 14)
				dy = 14;
			if (s == null) {
				s = new State(dx, dy);
				state.setData(s);
			} else {
				s.xPos = dx;
				s.yPos = dy;
			}
			state.getInstance().fireInvalidated();
		}
	}

	private static class State implements InstanceData, Cloneable {
		private int xPos;
		private int yPos;

		public State(int x, int y) {
			xPos = x;
			yPos = y;
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	private static void drawBall(Graphics g, int x, int y, Color c,
			boolean inColor) {
		if (inColor) {
			g.setColor(c == null ? Color.RED : c);
		} else {
			int hue = c == null ? 128 : (c.getRed() + c.getGreen() + c
					.getBlue()) / 3;
			g.setColor(new Color(hue, hue, hue));
		}
		GraphicsUtil.switchToWidth(g, 1);
		g.fillOval(x - 4, y - 4, 8, 8);
		g.setColor(Color.BLACK);
		g.drawOval(x - 4, y - 4, 8, 8);
	}

	static final Attribute<BitWidth> ATTR_WIDTH = Attributes.forBitWidth(
			"bits", Strings.getter("ioBitWidthAttr"), 2, 5);

	public Joystick() {
		super("Joystick", Strings.getter("joystickComponent"));
		setAttributes(new Attribute[] { ATTR_WIDTH, Io.ATTR_COLOR },
				new Object[] { BitWidth.create(4), Color.RED });
		setKeyConfigurator(new BitWidthConfigurator(ATTR_WIDTH, 2, 5));
		setOffsetBounds(Bounds.create(-30, -10, 30, 30));
		setIconName("joystick.gif");
		setPorts(new Port[] { new Port(0, 0, Port.OUTPUT, ATTR_WIDTH),
				new Port(0, 10, Port.OUTPUT, ATTR_WIDTH), });
		setInstancePoker(Poker.class);
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRoundRect(-30, -10, 30, 30, 8, 8);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Location loc = painter.getLocation();
		int x = loc.getX();
		int y = loc.getY();

		Graphics g = painter.getGraphics();
		g.drawRoundRect(x - 30, y - 10, 30, 30, 8, 8);
		g.drawRoundRect(x - 28, y - 8, 26, 26, 4, 4);
		drawBall(g, x - 15, y + 5, painter.getAttributeValue(Io.ATTR_COLOR),
				painter.shouldDrawColor());
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth bits = state.getAttributeValue(ATTR_WIDTH);
		int dx;
		int dy;
		State s = (State) state.getData();
		if (s == null) {
			dx = 0;
			dy = 0;
		} else {
			dx = s.xPos;
			dy = s.yPos;
		}

		int steps = (1 << bits.getWidth()) - 1;
		dx = (dx + 14) * steps / 29 + 1;
		dy = (dy + 14) * steps / 29 + 1;
		if (bits.getWidth() > 4) {
			if (dx >= steps / 2)
				dx++;
			if (dy >= steps / 2)
				dy++;
		}
		state.setPort(0, Value.createKnown(bits, dx), 1);
		state.setPort(1, Value.createKnown(bits, dy), 1);
	}
}
