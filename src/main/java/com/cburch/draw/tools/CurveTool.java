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
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.Icon;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.shapes.Curve;
import com.cburch.draw.shapes.CurveUtil;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;

public class CurveTool extends AbstractTool {
	private static final int BEFORE_CREATION = 0;
	private static final int ENDPOINT_DRAG = 1;
	private static final int CONTROL_DRAG = 2;

	private DrawingAttributeSet attrs;
	private int state;
	private Location end0;
	private Location end1;
	private Curve curCurve;
	private boolean mouseDown;
	private int lastMouseX;
	private int lastMouseY;

	public CurveTool(DrawingAttributeSet attrs) {
		this.attrs = attrs;
		state = BEFORE_CREATION;
		mouseDown = false;
	}

	@Override
	public void draw(Canvas canvas, Graphics g) {
		g.setColor(Color.GRAY);
		switch (state) {
		case ENDPOINT_DRAG:
			g.drawLine(end0.getX(), end0.getY(), end1.getX(), end1.getY());
			break;
		case CONTROL_DRAG:
			((Graphics2D) g).draw(curCurve.getCurve2D());
			break;
		default:
			break;
		}
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.getFillAttributes(attrs.getValue(DrawAttr.PAINT_TYPE));
	}

	@Override
	public Cursor getCursor(Canvas canvas) {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public Icon getIcon() {
		return Icons.getIcon("drawcurv.gif");
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		int code = e.getKeyCode();
		if (mouseDown
				&& (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT)) {
			updateMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
			repaintArea(canvas);
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		keyPressed(canvas, e);
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
		char ch = e.getKeyChar();
		if (ch == '\u001b') { // escape key
			state = BEFORE_CREATION;
			repaintArea(canvas);
			canvas.toolGestureComplete(this, null);
		}
	}

	@Override
	public void mouseDragged(Canvas canvas, MouseEvent e) {
		updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
		repaintArea(canvas);
	}

	@Override
	public void mousePressed(Canvas canvas, MouseEvent e) {
		int mx = e.getX();
		int my = e.getY();
		lastMouseX = mx;
		lastMouseY = my;
		mouseDown = true;
		int mods = e.getModifiersEx();
		if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
			mx = canvas.snapX(mx);
			my = canvas.snapY(my);
		}

		switch (state) {
		case BEFORE_CREATION:
		case CONTROL_DRAG:
			end0 = Location.create(mx, my);
			end1 = end0;
			state = ENDPOINT_DRAG;
			break;
		case ENDPOINT_DRAG:
			curCurve = new Curve(end0, end1, Location.create(mx, my));
			state = CONTROL_DRAG;
			break;
		default:
			break;
		}
		repaintArea(canvas);
	}

	@Override
	public void mouseReleased(Canvas canvas, MouseEvent e) {
		Curve c = updateMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
		mouseDown = false;
		if (state == CONTROL_DRAG) {
			if (c != null) {
				attrs.applyTo(c);
				CanvasModel model = canvas.getModel();
				canvas.doAction(new ModelAddAction(model, c));
				canvas.toolGestureComplete(this, c);
			}
			state = BEFORE_CREATION;
		}
		repaintArea(canvas);
	}

	private void repaintArea(Canvas canvas) {
		canvas.repaint();
	}

	@Override
	public void toolDeselected(Canvas canvas) {
		state = BEFORE_CREATION;
		repaintArea(canvas);
	}

	private Curve updateMouse(Canvas canvas, int mx, int my, int mods) {
		lastMouseX = mx;
		lastMouseY = my;

		boolean shiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
		boolean ctrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;
		boolean altDown = (mods & MouseEvent.ALT_DOWN_MASK) != 0;
		Curve ret = null;
		switch (state) {
		case ENDPOINT_DRAG:
			if (mouseDown) {
				if (shiftDown) {
					Location p = LineUtil.snapTo8Cardinals(end0, mx, my);
					mx = p.getX();
					my = p.getY();
				}
				if (ctrlDown) {
					mx = canvas.snapX(mx);
					my = canvas.snapY(my);
				}
				end1 = Location.create(mx, my);
			}
			break;
		case CONTROL_DRAG:
			if (mouseDown) {
				int cx = mx;
				int cy = my;
				if (ctrlDown) {
					cx = canvas.snapX(cx);
					cy = canvas.snapY(cy);
				}
				if (shiftDown) {
					double x0 = end0.getX();
					double y0 = end0.getY();
					double x1 = end1.getX();
					double y1 = end1.getY();
					double midx = (x0 + x1) / 2;
					double midy = (y0 + y1) / 2;
					double dx = x1 - x0;
					double dy = y1 - y0;
					double[] p = LineUtil.nearestPointInfinite(cx, cy, midx,
							midy, midx - dy, midy + dx);
					cx = (int) Math.round(p[0]);
					cy = (int) Math.round(p[1]);
				}
				if (altDown) {
					double[] e0 = { end0.getX(), end0.getY() };
					double[] e1 = { end1.getX(), end1.getY() };
					double[] mid = { cx, cy };
					double[] ct = CurveUtil.interpolate(e0, e1, mid);
					cx = (int) Math.round(ct[0]);
					cy = (int) Math.round(ct[1]);
				}
				ret = new Curve(end0, end1, Location.create(cx, cy));
				curCurve = ret;
			}
			break;
		default:
			break;
		}
		return ret;
	}
}
