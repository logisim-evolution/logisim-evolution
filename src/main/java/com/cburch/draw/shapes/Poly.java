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
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

public class Poly extends FillableCanvasObject {
	private boolean closed;
	// "handles" should be immutable - create a new array and change using
	// setHandles rather than changing contents
	private Handle[] handles;
	private GeneralPath path;
	private double[] lens;
	private Bounds bounds;

	public Poly(boolean closed, List<Location> locations) {
		Handle[] hs = new Handle[locations.size()];
		int i = -1;
		for (Location loc : locations) {
			i++;
			hs[i] = new Handle(this, loc.getX(), loc.getY());
		}

		this.closed = closed;
		handles = hs;
		recomputeBounds();
	}

	@Override
	public Handle canDeleteHandle(Location loc) {
		int minHandles = closed ? 3 : 2;
		Handle[] hs = handles;
		if (hs.length <= minHandles) {
			return null;
		} else {
			int qx = loc.getX();
			int qy = loc.getY();
			int w = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
			for (Handle h : hs) {
				int hx = h.getX();
				int hy = h.getY();
				if (LineUtil.distance(qx, qy, hx, hy) < w * w) {
					return h;
				}
			}
			return null;
		}
	}

	@Override
	public Handle canInsertHandle(Location loc) {
		PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(loc, closed,
				handles);
		int thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
		if (result.getDistanceSq() < thresh * thresh) {
			Location resLoc = result.getLocation();
			if (result.getPreviousHandle().isAt(resLoc)
					|| result.getNextHandle().isAt(resLoc)) {
				return null;
			} else {
				return new Handle(this, result.getLocation());
			}
		} else {
			return null;
		}
	}

	@Override
	public boolean canMoveHandle(Handle handle) {
		return true;
	}

	/**
	 * Clone function taken from Cornell's version of Logisim:
	 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
	 */
	public Poly clone() {
		Poly ret = (Poly) super.clone();
		Handle[] hs = this.handles.clone();

		for (int i = 0, n = hs.length; i < n; ++i) {
			Handle oldHandle = hs[i];
			hs[i] = new Handle(ret, oldHandle.getX(), oldHandle.getY());
		}
		ret.handles = hs;

		return (ret);
	}

	@Override
	public final boolean contains(Location loc, boolean assumeFilled) {
		Object type = getPaintType();
		if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
			type = DrawAttr.PAINT_STROKE_FILL;
		}
		if (type == DrawAttr.PAINT_STROKE) {
			int thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
			PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(loc,
					closed, handles);
			return result.getDistanceSq() < thresh * thresh;
		} else if (type == DrawAttr.PAINT_FILL) {
			GeneralPath path = getPath();
			return path.contains(loc.getX(), loc.getY());
		} else { // fill and stroke
			GeneralPath path = getPath();
			if (path.contains(loc.getX(), loc.getY()))
				return true;
			int width = getStrokeWidth();
			PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(loc,
					closed, handles);
			return result.getDistanceSq() < (width * width) / 4;
		}
	}

	@Override
	public Handle deleteHandle(Handle handle) {
		Handle[] hs = handles;
		int n = hs.length;
		Handle[] is = new Handle[n - 1];
		Handle previous = null;
		boolean deleted = false;
		for (int i = 0; i < n; i++) {
			if (deleted) {
				is[i - 1] = hs[i];
			} else if (hs[i].equals(handle)) {
				if (previous == null) {
					previous = hs[n - 1];
				}
				deleted = true;
			} else {
				previous = hs[i];
				is[i] = hs[i];
			}
		}
		setHandles(is);
		return previous;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.getFillAttributes(getPaintType());
	}

	@Override
	public Bounds getBounds() {
		return bounds;
	}

	@Override
	public String getDisplayName() {
		if (closed) {
			return Strings.get("shapePolygon");
		} else {
			return Strings.get("shapePolyline");
		}
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		Handle[] hs = handles;
		if (gesture == null) {
			return UnmodifiableList.create(hs);
		} else {
			Handle g = gesture.getHandle();
			Handle[] ret = new Handle[hs.length];
			for (int i = 0, n = hs.length; i < n; i++) {
				Handle h = hs[i];
				if (h.equals(g)) {
					int x = h.getX() + gesture.getDeltaX();
					int y = h.getY() + gesture.getDeltaY();
					Location r;
					if (gesture.isShiftDown()) {
						Location prev = hs[(i + n - 1) % n].getLocation();
						Location next = hs[(i + 1) % n].getLocation();
						if (!closed) {
							if (i == 0)
								prev = null;
							if (i == n - 1)
								next = null;
						}
						if (prev == null) {
							r = LineUtil.snapTo8Cardinals(next, x, y);
						} else if (next == null) {
							r = LineUtil.snapTo8Cardinals(prev, x, y);
						} else {
							Location to = Location.create(x, y);
							Location a = LineUtil.snapTo8Cardinals(prev, x, y);
							Location b = LineUtil.snapTo8Cardinals(next, x, y);
							int ad = a.manhattanDistanceTo(to);
							int bd = b.manhattanDistanceTo(to);
							r = ad < bd ? a : b;
						}
					} else {
						r = Location.create(x, y);
					}
					ret[i] = new Handle(this, r);
				} else {
					ret[i] = h;
				}
			}
			return UnmodifiableList.create(ret);
		}
	}

	private GeneralPath getPath() {
		GeneralPath p = path;
		if (p == null) {
			p = new GeneralPath();
			Handle[] hs = handles;
			if (hs.length > 0) {
				boolean first = true;
				for (Handle h : hs) {
					if (first) {
						p.moveTo(h.getX(), h.getY());
						first = false;
					} else {
						p.lineTo(h.getX(), h.getY());
					}
				}
			}
			path = p;
		}
		return p;
	}

	private Location getRandomBoundaryPoint(Bounds bds, Random rand) {
		Handle[] hs = handles;
		double[] ls = lens;
		if (ls == null) {
			ls = new double[hs.length + (closed ? 1 : 0)];
			double total = 0.0;
			for (int i = 0; i < ls.length; i++) {
				int j = (i + 1) % hs.length;
				total += LineUtil.distance(hs[i].getX(), hs[i].getY(),
						hs[j].getX(), hs[j].getY());
				ls[i] = total;
			}
			lens = ls;
		}
		double pos = ls[ls.length - 1] * rand.nextDouble();
		for (int i = 0; true; i++) {
			if (pos < ls[i]) {
				Handle p = hs[i];
				Handle q = hs[(i + 1) % hs.length];
				double u = Math.random();
				int x = (int) Math.round(p.getX() + u * (q.getX() - p.getX()));
				int y = (int) Math.round(p.getY() + u * (q.getY() - p.getY()));
				return Location.create(x, y);
			}
		}
	}

	@Override
	public final Location getRandomPoint(Bounds bds, Random rand) {
		if (getPaintType() == DrawAttr.PAINT_STROKE) {
			Location ret = getRandomBoundaryPoint(bds, rand);
			int w = getStrokeWidth();
			if (w > 1) {
				int dx = rand.nextInt(w) - w / 2;
				int dy = rand.nextInt(w) - w / 2;
				ret = ret.translate(dx, dy);
			}
			return ret;
		} else {
			return super.getRandomPoint(bds, rand);
		}
	}

	@Override
	public void insertHandle(Handle desired, Handle previous) {
		Location loc = desired.getLocation();
		Handle[] hs = handles;
		Handle prev;
		if (previous == null) {
			PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(loc,
					closed, hs);
			prev = result.getPreviousHandle();
		} else {
			prev = previous;
		}
		Handle[] is = new Handle[hs.length + 1];
		boolean inserted = false;
		for (int i = 0; i < hs.length; i++) {
			if (inserted) {
				is[i + 1] = hs[i];
			} else if (hs[i].equals(prev)) {
				inserted = true;
				is[i] = hs[i];
				is[i + 1] = desired;
			} else {
				is[i] = hs[i];
			}
		}
		if (!inserted) {
			throw new IllegalArgumentException("no such handle");
		}
		setHandles(is);
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof Poly) {
			Poly that = (Poly) other;
			Handle[] a = this.handles;
			Handle[] b = that.handles;
			if (this.closed != that.closed || a.length != b.length) {
				return false;
			} else {
				for (int i = 0, n = a.length; i < n; i++) {
					if (!a[i].equals(b[i]))
						return false;
				}
				return super.matches(that);
			}
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		int ret = super.matchesHashCode();
		ret = ret * 3 + (closed ? 1 : 0);
		Handle[] hs = handles;
		for (int i = 0, n = hs.length; i < n; i++) {
			ret = ret * 31 + hs[i].hashCode();
		}
		return ret;
	}

	@Override
	public Handle moveHandle(HandleGesture gesture) {
		List<Handle> hs = getHandles(gesture);
		Handle[] is = new Handle[hs.size()];
		Handle ret = null;
		int i = -1;
		for (Handle h : hs) {
			i++;
			is[i] = h;
		}
		setHandles(is);
		return ret;
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		List<Handle> hs = getHandles(gesture);
		int[] xs = new int[hs.size()];
		int[] ys = new int[hs.size()];
		int i = -1;
		for (Handle h : hs) {
			i++;
			xs[i] = h.getX();
			ys[i] = h.getY();
		}

		if (setForFill(g)) {
			g.fillPolygon(xs, ys, xs.length);
		}
		if (setForStroke(g)) {
			if (closed)
				g.drawPolygon(xs, ys, xs.length);
			else
				g.drawPolyline(xs, ys, xs.length);
		}
	}

	private void recomputeBounds() {
		Handle[] hs = handles;
		int x0 = hs[0].getX();
		int y0 = hs[0].getY();
		int x1 = x0;
		int y1 = y0;
		for (int i = 1; i < hs.length; i++) {
			int x = hs[i].getX();
			int y = hs[i].getY();
			if (x < x0)
				x0 = x;
			if (x > x1)
				x1 = x;
			if (y < y0)
				y0 = y;
			if (y > y1)
				y1 = y;
		}
		Bounds bds = Bounds.create(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
		int stroke = getStrokeWidth();
		bounds = stroke < 2 ? bds : bds.expand(stroke / 2);
	}

	private void setHandles(Handle[] hs) {
		handles = hs;
		lens = null;
		path = null;
		recomputeBounds();
	}

	@Override
	public Element toSvgElement(Document doc) {
		return SvgCreator.createPoly(doc, this);
	}

	@Override
	public void translate(int dx, int dy) {
		Handle[] hs = handles;
		Handle[] is = new Handle[hs.length];
		for (int i = 0; i < hs.length; i++) {
			is[i] = new Handle(this, hs[i].getX() + dx, hs[i].getY() + dy);
		}
		setHandles(is);
	}
}
