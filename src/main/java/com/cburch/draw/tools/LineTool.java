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

package com.cburch.draw.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.draw.shapes.Poly;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.UnmodifiableList;

public class LineTool extends AbstractTool {
	static Location snapTo4Cardinals(Location from, int mx, int my) {
		int px = from.getX();
		int py = from.getY();
		if (mx != px && my != py) {
			if (Math.abs(my - py) < Math.abs(mx - px)) {
				return Location.create(mx, py);
			} else {
				return Location.create(px, my);
			}
		}
		return Location.create(mx, my); // should never happen
	}

	private DrawingAttributeSet attrs;
	private boolean active;
	private Location mouseStart;
	private Location mouseEnd;
	private int lastMouseX;

	private int lastMouseY;

	public LineTool(DrawingAttributeSet attrs) {
		this.attrs = attrs;
		active = false;
	}

	@Override
	public void draw(Canvas canvas, Graphics g) {
		if (active) {
			Location start = mouseStart;
			Location end = mouseEnd;
			g.setColor(Color.GRAY);
			g.drawLine(start.getX(), start.getY(), end.getX(), end.getY());
		}
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.ATTRS_STROKE;
	}

	@Override
	public Cursor getCursor(Canvas canvas) {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public Icon getIcon() {
		return Icons.getIcon("drawline.gif");
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		int code = e.getKeyCode();
		if (active
				&& (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL)) {
			updateMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		keyPressed(canvas, e);
	}

	@Override
	public void mouseDragged(Canvas canvas, MouseEvent e) {
		updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
	}

	@Override
	public void mousePressed(Canvas canvas, MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		int mods = e.getModifiersEx();
		if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
			x = canvas.snapX(x);
			y = canvas.snapY(y);
		}
		Location loc = Location.create(x, y);
		mouseStart = loc;
		mouseEnd = loc;
		lastMouseX = loc.getX();
		lastMouseY = loc.getY();
		active = canvas.getModel() != null;
		repaintArea(canvas);
	}

	@Override
	public void mouseReleased(Canvas canvas, MouseEvent e) {
		if (active) {
			updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
			Location start = mouseStart;
			Location end = mouseEnd;
			CanvasObject add = null;
			if (!start.equals(end)) {
				active = false;
				CanvasModel model = canvas.getModel();
				Location[] ends = { start, end };
				List<Location> locs = UnmodifiableList.create(ends);
				add = attrs.applyTo(new Poly(false, locs));
				add.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
				canvas.doAction(new ModelAddAction(model, add));
				repaintArea(canvas);
			}
			canvas.toolGestureComplete(this, add);
		}
	}

	private void repaintArea(Canvas canvas) {
		canvas.repaint();
	}

	@Override
	public void toolDeselected(Canvas canvas) {
		active = false;
		repaintArea(canvas);
	}

	private void updateMouse(Canvas canvas, int mx, int my, int mods) {
		if (active) {
			boolean shift = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
			Location newEnd;
			if (shift) {
				newEnd = LineUtil.snapTo8Cardinals(mouseStart, mx, my);
			} else {
				newEnd = Location.create(mx, my);
			}

			if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
				int x = newEnd.getX();
				int y = newEnd.getY();
				x = canvas.snapX(x);
				y = canvas.snapY(y);
				newEnd = Location.create(x, y);
			}

			if (!newEnd.equals(mouseEnd)) {
				mouseEnd = newEnd;
				repaintArea(canvas);
			}
		}
		lastMouseX = mx;
		lastMouseY = my;
	}
}
