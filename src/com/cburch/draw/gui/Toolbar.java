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

package com.cburch.draw.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

class Toolbar extends JComponent {
	private class Listener implements MouseListener, MouseMotionListener {
		private AbstractTool toolPressed;
		private boolean inTool;
		private int toolX;
		private int toolY;

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			int mx = e.getX();
			int my = e.getY();
			int x0 = toolX;
			int y0 = toolY;

			boolean was = inTool;
			boolean now = toolPressed != null && mx >= x0
					&& mx < x0 + ICON_WIDTH && my >= y0
					&& my < y0 + ICON_HEIGHT;
			if (was != now) {
				inTool = now;
				repaint();
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			int mx = e.getX();
			int my = e.getY();
			int col = (e.getX() - ICON_SEP) / (ICON_WIDTH + ICON_SEP);
			int row = (e.getY() - ICON_SEP) / (ICON_HEIGHT + ICON_SEP);
			int x0 = ICON_SEP + col * (ICON_SEP + ICON_WIDTH);
			int y0 = ICON_SEP + row * (ICON_SEP + ICON_HEIGHT);

			if (mx >= x0 && mx < x0 + ICON_WIDTH && my >= y0
					&& my < y0 + ICON_HEIGHT && col >= 0 && col < tools.length
					&& row >= 0 && row < tools[col].length) {
				toolPressed = tools[col][row];
				inTool = true;
				toolX = x0;
				toolY = y0;
				repaint();
			} else {
				toolPressed = null;
				inTool = false;
			}
		}

		public void mouseReleased(MouseEvent e) {
			mouseDragged(e);
			if (inTool) {
				canvas.setTool(toolPressed);
				repaint();
			}
			toolPressed = null;
			inTool = false;
		}

	}

	private static final long serialVersionUID = 1L;
	private static int ICON_WIDTH = 16;
	private static int ICON_HEIGHT = 16;

	private static int ICON_SEP = 4;

	private Canvas canvas;
	private AbstractTool[][] tools;
	private Listener listener;

	public Toolbar(Canvas canvas, DrawingAttributeSet attrs) {
		this.canvas = canvas;
		this.tools = new AbstractTool[][] { AbstractTool.getTools(attrs) };
		this.listener = new Listener();

		AbstractTool[] toolBase = AbstractTool.getTools(attrs);
		this.tools = new AbstractTool[2][];
		this.tools[0] = new AbstractTool[(toolBase.length + 1) / 2];
		this.tools[1] = new AbstractTool[toolBase.length / 2];
		for (int i = 0; i < toolBase.length; i++) {
			this.tools[i % 2][i / 2] = toolBase[i];
		}

		setPreferredSize(new Dimension(3 * ICON_SEP + 2 * ICON_WIDTH, ICON_SEP
				+ tools[0].length * (ICON_HEIGHT + ICON_SEP)));
		addMouseListener(listener);
		addMouseMotionListener(listener);
	}

	public AbstractTool getDefaultTool() {
		return tools[0][0];
	}

	@Override
	public void paintComponent(Graphics g) {
		if (AppPreferences.AntiAliassing.getBoolean()) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.clearRect(0, 0, getWidth(), getHeight());
		CanvasTool current = canvas.getTool();
		for (int i = 0; i < tools.length; i++) {
			AbstractTool[] column = tools[i];
			int x = ICON_SEP + i * (ICON_SEP + ICON_WIDTH);
			int y = ICON_SEP;
			for (int j = 0; j < column.length; j++) {
				AbstractTool tool = column[j];
				if (tool == listener.toolPressed && listener.inTool) {
					g.setColor(Color.darkGray);
					g.fillRect(x, y, ICON_WIDTH, ICON_HEIGHT);
				}
				Icon icon = tool.getIcon();
				if (icon != null)
					icon.paintIcon(this, g, x, y);
				if (tool.equals(current)) {
					GraphicsUtil.switchToWidth(g, 2);
					g.setColor(Color.black);
					g.drawRect(x - 1, y - 1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
				}
				y += ICON_HEIGHT + ICON_SEP;
			}
		}
		g.setColor(Color.black);
		GraphicsUtil.switchToWidth(g, 1);
	}
}
