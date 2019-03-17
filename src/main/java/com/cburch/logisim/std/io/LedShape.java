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

import static com.cburch.logisim.std.Strings.S;

import java.awt.Graphics;
import java.awt.Color;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.instance.InstanceDataSingleton;

public class LedShape extends DynamicElement {
	static final int DEFAULT_STROKE_WIDTH = 1;
	static final int DEFAULT_RADIUS = 5;


	public LedShape(int x, int y, DynamicElement.Path p) {
		super(p, Bounds.create(x, y, 2*DEFAULT_RADIUS, 2*DEFAULT_RADIUS));
		strokeWidth = DEFAULT_STROKE_WIDTH;
	}

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		int x = bounds.getX();
		int y = bounds.getY();
		int w = bounds.getWidth();
		int h = bounds.getHeight();
		int qx = loc.getX();
		int qy = loc.getY();
		double dx = qx - (x + 0.5 * w);
		double dy = qy - (y + 0.5 * h);
		double sum = (dx * dx) / (w * w) + (dy * dy) / (h * h);
		return sum <= 0.25;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return UnmodifiableList.create(new Attribute<?>[] { DrawAttr.STROKE_WIDTH});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == DrawAttr.STROKE_WIDTH) {
			return (V) Integer.valueOf(strokeWidth);
		} else {
			return null;
		}
	}

	@Override
	public void updateValue(Attribute<?> attr, Object value) {
		if (attr == DrawAttr.STROKE_WIDTH) {
			strokeWidth = ((Integer) value).intValue();
		}
	}

	@Override
	public void paintDynamic(Graphics g, CircuitState state) {
		Color offColor = path.leaf().getAttributeSet().getValue(Io.ATTR_OFF_COLOR);
		Color onColor = path.leaf().getAttributeSet().getValue(Io.ATTR_ON_COLOR);
		int x = bounds.getX()+1;
		int y = bounds.getY()+1;
		int w = bounds.getWidth()-2;
		int h = bounds.getHeight()-2;
		GraphicsUtil.switchToWidth(g, strokeWidth);
		if (state == null) {
			g.setColor(offColor);
			g.fillOval(x, y, w, h);
			g.setColor(DynamicElement.COLOR);
			g.drawOval(x, y, w, h);
		} else {
			Boolean activ = path.leaf().getAttributeSet().getValue(Io.ATTR_ACTIVE);
			Object desired = activ.booleanValue() ? Value.TRUE : Value.FALSE;
			InstanceDataSingleton data = (InstanceDataSingleton)getData(state);
			Value val = data == null ? Value.FALSE : (Value) data.getValue();
			g.setColor(val == desired ? onColor : offColor);
			g.fillOval(x, y, w, h);
			g.setColor(Color.darkGray);
			g.drawOval(x, y, w, h);
		}
	}

	@Override
	public Element toSvgElement(Document doc) {
		return toSvgElement(doc.createElement("visible-led"));
	}

	public Element toSvgElement(Element ret) {
		ret.setAttribute("x", "" + bounds.getX());
		ret.setAttribute("y", "" + bounds.getY());
		ret.setAttribute("width", "" + bounds.getWidth());
		ret.setAttribute("height", "" + bounds.getHeight());
		if (strokeWidth != DEFAULT_STROKE_WIDTH)
			ret.setAttribute("stroke-width", "" + strokeWidth);
		ret.setAttribute("path", path.toSvgString());
		return ret;
	}

	@Override
	public void parseSvgElement(Element elt) {
		if (elt.hasAttribute("stroke-width"))
			strokeWidth = Integer.parseInt(elt.getAttribute("stroke-width").trim());
	}
	
	@Override
	public String getDisplayName() {
		return S.get("ledComponent");
	}

	@Override
	public String toString() {
		return "Led:" + getBounds();
	}
}