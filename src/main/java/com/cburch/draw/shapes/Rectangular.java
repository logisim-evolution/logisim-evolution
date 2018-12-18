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

package com.cburch.draw.shapes;

import java.awt.Graphics;
import java.util.List;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

abstract class Rectangular extends FillableCanvasObject {
	private Bounds bounds; // excluding the stroke's width

	public Rectangular(int x, int y, int w, int h) {
		bounds = Bounds.create(x, y, w, h);
	}

	@Override
	public boolean canMoveHandle(Handle handle) {
		return true;
	}

	protected abstract boolean contains(int x, int y, int w, int h, Location q);

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		Object type = getPaintType();
		if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
			type = DrawAttr.PAINT_STROKE_FILL;
		}
		Bounds b = bounds;
		int x = b.getX();
		int y = b.getY();
		int w = b.getWidth();
		int h = b.getHeight();
		int qx = loc.getX();
		int qy = loc.getY();
		if (type == DrawAttr.PAINT_FILL) {
			return isInRect(qx, qy, x, y, w, h) && contains(x, y, w, h, loc);
		} else if (type == DrawAttr.PAINT_STROKE) {
			int stroke = getStrokeWidth();
			int tol2 = Math.max(2 * Line.ON_LINE_THRESH, stroke);
			int tol = tol2 / 2;
			return isInRect(qx, qy, x - tol, y - tol, w + tol2, h + tol2)
					&& contains(x - tol, y - tol, w + tol2, h + tol2, loc)
					&& !contains(x + tol, y + tol, w - tol2, h - tol2, loc);
		} else if (type == DrawAttr.PAINT_STROKE_FILL) {
			int stroke = getStrokeWidth();
			int tol2 = stroke;
			int tol = tol2 / 2;
			return isInRect(qx, qy, x - tol, y - tol, w + tol2, h + tol2)
					&& contains(x - tol, y - tol, w + tol2, h + tol2, loc);
		} else {
			return false;
		}
	}

	protected abstract void draw(Graphics g, int x, int y, int w, int h);

	@Override
	public Bounds getBounds() {
		int wid = getStrokeWidth();
		Object type = getPaintType();
		if (wid < 2 || type == DrawAttr.PAINT_FILL) {
			return bounds;
		} else {
			return bounds.expand(wid / 2);
		}
	}

	private Handle[] getHandleArray(HandleGesture gesture) {
		Bounds bds = bounds;
		int x0 = bds.getX();
		int y0 = bds.getY();
		int x1 = x0 + bds.getWidth();
		int y1 = y0 + bds.getHeight();
		if (gesture == null) {
			return new Handle[] { new Handle(this, x0, y0),
					new Handle(this, x1, y0), new Handle(this, x1, y1),
					new Handle(this, x0, y1) };
		} else {
			int hx = gesture.getHandle().getX();
			int hy = gesture.getHandle().getY();
			int dx = gesture.getDeltaX();
			int dy = gesture.getDeltaY();
			int newX0 = x0 == hx ? x0 + dx : x0;
			int newY0 = y0 == hy ? y0 + dy : y0;
			int newX1 = x1 == hx ? x1 + dx : x1;
			int newY1 = y1 == hy ? y1 + dy : y1;
			if (gesture.isShiftDown()) {
				if (gesture.isAltDown()) {
					if (x0 == hx)
						newX1 -= dx;
					if (x1 == hx)
						newX0 -= dx;
					if (y0 == hy)
						newY1 -= dy;
					if (y1 == hy)
						newY0 -= dy;

					int w = Math.abs(newX1 - newX0);
					int h = Math.abs(newY1 - newY0);
					if (w > h) { // reduce width to h
						int dw = (w - h) / 2;
						newX0 -= (newX0 > newX1 ? 1 : -1) * dw;
						newX1 -= (newX1 > newX0 ? 1 : -1) * dw;
					} else {
						int dh = (h - w) / 2;
						newY0 -= (newY0 > newY1 ? 1 : -1) * dh;
						newY1 -= (newY1 > newY0 ? 1 : -1) * dh;
					}
				} else {
					int w = Math.abs(newX1 - newX0);
					int h = Math.abs(newY1 - newY0);
					if (w > h) { // reduce width to h
						if (x0 == hx) {
							newX0 = newX1 + (newX0 > newX1 ? 1 : -1) * h;
						}
						if (x1 == hx) {
							newX1 = newX0 + (newX1 > newX0 ? 1 : -1) * h;
						}
					} else { // reduce height to w
						if (y0 == hy) {
							newY0 = newY1 + (newY0 > newY1 ? 1 : -1) * w;
						}
						if (y1 == hy) {
							newY1 = newY0 + (newY1 > newY0 ? 1 : -1) * w;
						}
					}
				}
			} else {
				if (gesture.isAltDown()) {
					if (x0 == hx)
						newX1 -= dx;
					if (x1 == hx)
						newX0 -= dx;
					if (y0 == hy)
						newY1 -= dy;
					if (y1 == hy)
						newY0 -= dy;
				} else {
					; // already handled
				}
			}
			return new Handle[] { new Handle(this, newX0, newY0),
					new Handle(this, newX1, newY0),
					new Handle(this, newX1, newY1),
					new Handle(this, newX0, newY1) };
		}
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		return UnmodifiableList.create(getHandleArray(gesture));
	}

	public int getHeight() {
		return bounds.getHeight();
	}

	public int getWidth() {
		return bounds.getWidth();
	}

	public int getX() {
		return bounds.getX();
	}

	public int getY() {
		return bounds.getY();
	}

	boolean isInRect(int qx, int qy, int x0, int y0, int w, int h) {
		return qx >= x0 && qx < x0 + w && qy >= y0 && qy < y0 + h;
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof Rectangular) {
			Rectangular that = (Rectangular) other;
			return this.bounds.equals(that.bounds) && super.matches(that);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return bounds.hashCode() * 31 + super.matchesHashCode();
	}

	@Override
	public Handle moveHandle(HandleGesture gesture) {
		Handle[] oldHandles = getHandleArray(null);
		Handle[] newHandles = getHandleArray(gesture);
		Handle moved = gesture == null ? null : gesture.getHandle();
		Handle result = null;
		int x0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		int y0 = Integer.MAX_VALUE;
		int y1 = Integer.MIN_VALUE;
		int i = -1;
		for (Handle h : newHandles) {
			i++;
			if (oldHandles[i].equals(moved)) {
				result = h;
			}
			int hx = h.getX();
			int hy = h.getY();
			if (hx < x0)
				x0 = hx;
			if (hx > x1)
				x1 = hx;
			if (hy < y0)
				y0 = hy;
			if (hy > y1)
				y1 = hy;
		}
		bounds = Bounds.create(x0, y0, x1 - x0, y1 - y0);
		return result;
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		if (gesture == null) {
			Bounds bds = bounds;
			draw(g, bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
		} else {
			Handle[] handles = getHandleArray(gesture);
			Handle p0 = handles[0];
			Handle p1 = handles[2];
			int x0 = p0.getX();
			int y0 = p0.getY();
			int x1 = p1.getX();
			int y1 = p1.getY();
			if (x1 < x0) {
				int t = x0;
				x0 = x1;
				x1 = t;
			}
			if (y1 < y0) {
				int t = y0;
				y0 = y1;
				y1 = t;
			}

			draw(g, x0, y0, x1 - x0, y1 - y0);
		}
	}

	@Override
	public void translate(int dx, int dy) {
		bounds = bounds.translate(dx, dy);
	}
}
