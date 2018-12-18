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
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;

public class Rectangle extends Rectangular {
	public Rectangle(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	@Override
	protected boolean contains(int x, int y, int w, int h, Location q) {
		return isInRect(q.getX(), q.getY(), x, y, w, h);
	}

	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		if (setForFill(g))
			g.fillRect(x, y, w, h);
		if (setForStroke(g))
			g.drawRect(x, y, w, h);
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return DrawAttr.getFillAttributes(getPaintType());
	}

	@Override
	public String getDisplayName() {
		return Strings.get("shapeRect");
	}

	@Override
	protected Location getRandomPoint(Bounds bds, Random rand) {
		if (getPaintType() == DrawAttr.PAINT_STROKE) {
			int w = getWidth();
			int h = getHeight();
			int u = rand.nextInt(2 * w + 2 * h);
			int x = getX();
			int y = getY();
			if (u < w) {
				x += u;
			} else if (u < 2 * w) {
				x += (u - w);
				y += h;
			} else if (u < 2 * w + h) {
				y += (u - 2 * w);
			} else {
				x += w;
				y += (u - 2 * w - h);
			}
			int d = getStrokeWidth();
			if (d > 1) {
				x += rand.nextInt(d) - d / 2;
				y += rand.nextInt(d) - d / 2;
			}
			return Location.create(x, y);
		} else {
			return super.getRandomPoint(bds, rand);
		}
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof Rectangle) {
			return super.matches(other);
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return super.matchesHashCode();
	}

	@Override
	public String toString() {
		return "Rectangle:" + getBounds();
	}

	@Override
	public Element toSvgElement(Document doc) {
		return SvgCreator.createRectangle(doc, this);
	}
}
