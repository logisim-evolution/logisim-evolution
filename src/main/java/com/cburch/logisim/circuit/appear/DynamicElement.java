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
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *******************************************************************************/

package com.cburch.logisim.circuit.appear;

import java.util.List;

import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.UnmodifiableList;

public abstract class DynamicElement extends AbstractCanvasObject {
	
	public static final Color COLOR = new Color(66, 244, 152);

	public static class Path {
		public InstanceComponent[] elt;
		public Path(InstanceComponent[] elt) {
			this.elt = elt;
		}
		public boolean contains(Component c) {
			for (InstanceComponent ic : elt) {
				if (ic == c)
					return true;
			}
			return false;
		}
		public InstanceComponent leaf() {
			return elt[elt.length-1];
		}
		
		public String toString() {
			return toSvgString();
		}

		public String toSvgString() {
			String s = "";
			for (int i = 0; i < elt.length; i++) {
				Location loc = elt[i].getLocation();
				s += "/" + escape(elt[i].getFactory().getName()) + loc;
			}
			return s;
		}

		public static Path fromSvgString(String s, Circuit circuit) throws IllegalArgumentException {
			if (!s.startsWith("/"))
				throw new IllegalArgumentException("Bad path: " + s);
			String parts[] = s.substring(1).split("(?<!\\\\)/");
			InstanceComponent[] elt = new InstanceComponent[parts.length];
			for (int i = 0; i < parts.length; i++) {
				String ss = parts[i];
				int p = ss.lastIndexOf("(");
				int c = ss.lastIndexOf(",");
				int e = ss.lastIndexOf(")");
				if (e != ss.length()-1 || p <= 0 || c <= p)
					throw new IllegalArgumentException("Bad path element: " + ss);
				int x = Integer.parseInt(ss.substring(p+1, c).trim());
				int y = Integer.parseInt(ss.substring(c+1, e).trim());
				Location loc = Location.create(x, y);
				String name = unescape(ss.substring(0, p));
				Circuit circ = circuit;
				if (i > 0)
					circ = ((SubcircuitFactory)elt[i-1].getFactory()).getSubcircuit();
				InstanceComponent ic = find(circ, loc, name);
				if (ic == null)
					throw new IllegalArgumentException("Missing component: " + ss);
				elt[i] = ic;
			}
			return new Path(elt);
		}

		private static InstanceComponent find(Circuit circuit, Location loc, String name) {
			for (Component c : circuit.getNonWires()) {
				if (name.equals(c.getFactory().getName()) && loc.equals(c.getLocation()))
					return (InstanceComponent)c;
			}
			return null;
		}

		private static String escape(String s) {
			// Slash '/', backslash '\\' are both escaped using an extra
			// backslash. All other escaping is handled by the xml writer.
			return s.replace("\\", "\\\\").replace("/", "\\/");
		}

		private static String unescape(String s) {
			return s.replace("\\/", "/").replace("\\\\", "\\");
		}
	}

	protected Path path;
	protected Bounds bounds; // excluding the stroke's width, if any
	protected int strokeWidth;

	public DynamicElement(Path p, Bounds b) {
		path = p;
		bounds = b;
		strokeWidth = 0;
	}

	public Path getPath() {
		return path;
	}
	
	public InstanceComponent getFirstInstance() {
		return path.elt[0];
	}

	@Override
	public Bounds getBounds() {
		if (strokeWidth < 2)
			return bounds;
		else
			return bounds.expand(strokeWidth / 2);
	}
	
	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		return bounds.contains(loc);
	}

	public Location getLocation() {
		return Location.create(bounds.getX(), bounds.getY());
	}

	@Override
	public void translate(int dx, int dy) {
		bounds = bounds.translate(dx, dy);
	}

	@Override
	public int matchesHashCode() {
		return bounds.hashCode();
	}
	
	@Override
	public boolean matches(CanvasObject other) {
		return (other instanceof DynamicElement) &&
			this.bounds.equals(((DynamicElement)other).bounds);
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		int x0 = bounds.getX();
		int y0 = bounds.getY();
		int x1 = x0 + bounds.getWidth();
		int y1 = y0 + bounds.getHeight();
		return UnmodifiableList.create(new Handle[] {
				new Handle(this, x0, y0), new Handle(this, x1, y0),
			    new Handle(this, x1, y1), new Handle(this, x0, y1) });
	}

	protected Object getData(CircuitState state) {
		Object o = state.getData(path.elt[0]);
		for (int i = 1; i < path.elt.length && o != null; i++) {
			if (!(o instanceof CircuitState)) {
				throw new IllegalStateException(
						"Expecting CircuitState for path[" + (i-1) + "] " + path.elt[i-1]
						+ "  but got: " + o);
			}
			state = (CircuitState)o;
			o = state.getData(path.elt[i]);
		}
		return o;
	}
	
	@Override
	public String getDisplayNameAndLabel() {
		String label = path.leaf().getInstance().getAttributeValue(StdAttr.LABEL);
		if (label != null && label.length() > 0)
			return getDisplayName() + " \"" + label + "\"";
		else
			return getDisplayName();
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
			paintDynamic(g, null);
	}

	public abstract void parseSvgElement(Element elt);

	public abstract void paintDynamic(Graphics g, CircuitState state);

}