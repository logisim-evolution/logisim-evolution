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

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

public class Line extends AbstractCanvasObject {
	static final int ON_LINE_THRESH = 2;

	private int x0;
	private int y0;
	private int x1;
	private int y1;
	private Bounds bounds;
	private int strokeWidth;
	private Color strokeColor;

	public Line(int x0, int y0, int x1, int y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
		strokeWidth = 1;
		strokeColor = Color.BLACK;
	}

	@Override
	public boolean canMoveHandle(Handle handle) {
		return true;
	}

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		int xq = loc.getX();
		int yq = loc.getY();
		double d = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
		int thresh = Math.max(ON_LINE_THRESH, strokeWidth / 2);
		return d < thresh * thresh;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.ATTRS_STROKE;
	}

	@Override
	public Bounds getBounds() {
		return bounds;
	}

	@Override
	public String getDisplayName() {
		return Strings.get("shapeLine");
	}

	public Location getEnd0() {
		return Location.create(x0, y0);
	}

	public Location getEnd1() {
		return Location.create(x1, y1);
	}

	public List<Handle> getHandles() {
		return getHandles(null);
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		if (gesture == null) {
			return UnmodifiableList.create(new Handle[] {
					new Handle(this, x0, y0), new Handle(this, x1, y1) });
		} else {
			Handle h = gesture.getHandle();
			int dx = gesture.getDeltaX();
			int dy = gesture.getDeltaY();
			Handle[] ret = new Handle[2];
			ret[0] = new Handle(this, h.isAt(x0, y0) ? Location.create(x0 + dx,
					y0 + dy) : Location.create(x0, y0));
			ret[1] = new Handle(this, h.isAt(x1, y1) ? Location.create(x1 + dx,
					y1 + dy) : Location.create(x1, y1));
			return UnmodifiableList.create(ret);
		}
	}

	@Override
	public Location getRandomPoint(Bounds bds, Random rand) {
		double u = rand.nextDouble();
		int x = (int) Math.round(x0 + u * (x1 - x0));
		int y = (int) Math.round(y0 + u * (y1 - y0));
		int w = strokeWidth;
		if (w > 1) {
			x += (rand.nextInt(w) - w / 2);
			y += (rand.nextInt(w) - w / 2);
		}
		return Location.create(x, y);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == DrawAttr.STROKE_COLOR) {
			return (V) strokeColor;
		} else if (attr == DrawAttr.STROKE_WIDTH) {
			return (V) Integer.valueOf(strokeWidth);
		} else {
			return null;
		}
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof Line) {
			Line that = (Line) other;
			return this.x0 == that.x0 && this.y0 == that.x1
					&& this.x1 == that.y0 && this.y1 == that.y1
					&& this.strokeWidth == that.strokeWidth
					&& this.strokeColor.equals(that.strokeColor);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		int ret = x0 * 31 + y0;
		ret = ret * 31 * 31 + x1 * 31 + y1;
		ret = ret * 31 + strokeWidth;
		ret = ret * 31 + strokeColor.hashCode();
		return ret;
	}

	@Override
	public Handle moveHandle(HandleGesture gesture) {
		Handle h = gesture.getHandle();
		int dx = gesture.getDeltaX();
		int dy = gesture.getDeltaY();
		Handle ret = null;
		if (h.isAt(x0, y0)) {
			x0 += dx;
			y0 += dy;
			ret = new Handle(this, x0, y0);
		}
		if (h.isAt(x1, y1)) {
			x1 += dx;
			y1 += dy;
			ret = new Handle(this, x1, y1);
		}
		bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
		return ret;
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		if (setForStroke(g)) {
			int x0 = this.x0;
			int y0 = this.y0;
			int x1 = this.x1;
			int y1 = this.y1;
			Handle h = gesture.getHandle();
			if (h.isAt(x0, y0)) {
				x0 += gesture.getDeltaX();
				y0 += gesture.getDeltaY();
			}
			if (h.isAt(x1, y1)) {
				x1 += gesture.getDeltaX();
				y1 += gesture.getDeltaY();
			}
			g.drawLine(x0, y0, x1, y1);
		}
	}

	@Override
	public Element toSvgElement(Document doc) {
		return SvgCreator.createLine(doc, this);
	}

	@Override
	public void translate(int dx, int dy) {
		x0 += dx;
		y0 += dy;
		x1 += dx;
		y1 += dy;
	}

	@Override
	public void updateValue(Attribute<?> attr, Object value) {
		if (attr == DrawAttr.STROKE_COLOR) {
			strokeColor = (Color) value;
		} else if (attr == DrawAttr.STROKE_WIDTH) {
			strokeWidth = ((Integer) value).intValue();
		}
	}

}
