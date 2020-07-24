/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
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
 */

package com.cburch.logisim.circuit.appear;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.SvgCreator;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Element;

public abstract class DynamicElement extends AbstractCanvasObject {

  public static final AttributeOption LABEL_NONE =
      new AttributeOption("none", S.getter("circuitLabelNone"));
  public static final AttributeOption LABEL_TOP =
      new AttributeOption("top", S.getter("circuitLabelTop"));
  public static final AttributeOption LABEL_BOTTOM =
      new AttributeOption("bottom", S.getter("circuitLabelBottom"));
  public static final AttributeOption LABEL_LEFT =
      new AttributeOption("left", S.getter("circuitLabelLeft"));
  public static final AttributeOption LABEL_RIGHT =
      new AttributeOption("right", S.getter("circuitLabelRight"));
  public static final AttributeOption LABEL_CENTER =
      new AttributeOption("center", S.getter("circuitLabelCenter"));
  public static final Attribute<AttributeOption> ATTR_LABEL =
      Attributes.forOption(
          "showlabel",
          S.getter("circuitShowLabelAttr"),
          new AttributeOption[] {
            LABEL_NONE, LABEL_TOP, LABEL_BOTTOM, LABEL_LEFT, LABEL_RIGHT, LABEL_CENTER
          });

  public static final Color COLOR = new Color(66, 244, 152);

  public static class Path {
    public InstanceComponent[] elt;

    public Path(InstanceComponent[] elt) {
      this.elt = elt;
    }

    public boolean contains(Component c) {
      for (InstanceComponent ic : elt) {
        if (ic == c) return true;
      }
      return false;
    }

    public InstanceComponent leaf() {
      return elt[elt.length - 1];
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
      if (!s.startsWith("/")) throw new IllegalArgumentException("Bad path: " + s);
      String parts[] = s.substring(1).split("(?<!\\\\)/");
      InstanceComponent[] elt = new InstanceComponent[parts.length];
      for (int i = 0; i < parts.length; i++) {
        String ss = parts[i];
        int p = ss.lastIndexOf("(");
        int c = ss.lastIndexOf(",");
        int e = ss.lastIndexOf(")");
        if (e != ss.length() - 1 || p <= 0 || c <= p)
          throw new IllegalArgumentException("Bad path element: " + ss);
        int x = Integer.parseInt(ss.substring(p + 1, c).trim());
        int y = Integer.parseInt(ss.substring(c + 1, e).trim());
        Location loc = Location.create(x, y);
        String name = unescape(ss.substring(0, p));
        Circuit circ = circuit;
        if (i > 0) circ = ((SubcircuitFactory) elt[i - 1].getFactory()).getSubcircuit();
        InstanceComponent ic = find(circ, loc, name);
        if (ic == null) throw new IllegalArgumentException("Missing component: " + ss);
        elt[i] = ic;
      }
      return new Path(elt);
    }

    private static InstanceComponent find(Circuit circuit, Location loc, String name) {
      for (Component c : circuit.getNonWires()) {
        if (name.equals(c.getFactory().getName()) && loc.equals(c.getLocation()))
          return (InstanceComponent) c;
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

  public static final int DEFAULT_STROKE_WIDTH = 1;
  public static final Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 7);
  protected Path path;
  protected Bounds bounds; // excluding the stroke's width, if any
  protected int strokeWidth;
  protected AttributeOption labelLoc;
  protected Font labelFont;
  protected Color labelColor;

  public DynamicElement(Path p, Bounds b) {
    path = p;
    bounds = b;
    strokeWidth = 0;
    labelLoc = LABEL_NONE;
    labelFont = DEFAULT_LABEL_FONT;
    labelColor = Color.darkGray;
  }

  public Path getPath() {
    return path;
  }

  public InstanceComponent getFirstInstance() {
    return path.elt[0];
  }

  @Override
  public Bounds getBounds() {
    if (strokeWidth < 2) return bounds;
    else return bounds.expand(strokeWidth / 2);
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
    return (other instanceof DynamicElement) && this.bounds.equals(((DynamicElement) other).bounds);
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    int x0 = bounds.getX();
    int y0 = bounds.getY();
    int x1 = x0 + bounds.getWidth();
    int y1 = y0 + bounds.getHeight();
    return UnmodifiableList.create(
        new Handle[] {
          new Handle(this, x0, y0),
          new Handle(this, x1, y0),
          new Handle(this, x1, y1),
          new Handle(this, x0, y1)
        });
  }

  protected Object getData(CircuitState state) {
    Object o = state.getData(path.elt[0]);
    for (int i = 1; i < path.elt.length && o != null; i++) {
      if (!(o instanceof CircuitState)) {
        throw new IllegalStateException(
            "Expecting CircuitState for path["
                + (i - 1)
                + "] "
                + path.elt[i - 1]
                + "  but got: "
                + o);
      }
      state = (CircuitState) o;
      o = state.getData(path.elt[i]);
    }
    return o;
  }
  
  protected InstanceComponent getComponent(CircuitState state) {
    Object o = state.getData(path.elt[0]);
    InstanceComponent comp = path.elt[0];
    for (int i = 1; i < path.elt.length && o != null; i++) {
      if (!(o instanceof CircuitState)) {
        throw new IllegalStateException(
            "Expecting CircuitState for path["
                + (i - 1)
                + "] "
                + path.elt[i - 1]
                + "  but got: "
                + o);
      }
      state = (CircuitState) o;
      comp = path.elt[i];
      o = state.getData(path.elt[i]);
    }
    return comp;
  }

  @Override
  public String getDisplayNameAndLabel() {
    String label = path.leaf().getInstance().getAttributeValue(StdAttr.LABEL);
    if (label != null && label.length() > 0) return getDisplayName() + " \"" + label + "\"";
    else return getDisplayName();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    paintDynamic(g, null);
  }

  public void parseSvgElement(Element elt) {
    if (elt.hasAttribute("stroke-width"))
      strokeWidth = Integer.parseInt(elt.getAttribute("stroke-width").trim());
    if (elt.hasAttribute("label")) {
      String loc = elt.getAttribute("label").trim().toLowerCase();
      if (loc.equals("left")) labelLoc = LABEL_LEFT;
      else if (loc.equals("right")) labelLoc = LABEL_RIGHT;
      else if (loc.equals("top")) labelLoc = LABEL_TOP;
      else if (loc.equals("bottom")) labelLoc = LABEL_BOTTOM;
      else if (loc.equals("center")) labelLoc = LABEL_CENTER;
      else if (loc.equals("none")) labelLoc = LABEL_NONE;
    }
    labelFont = SvgReader.getFontAttribute(elt, "", "SansSerif", 7);
    if (elt.hasAttribute("label-color"))
      labelColor = SvgReader.getColor(elt.getAttribute("label-color"), null);
  }

  protected Element toSvgElement(Element ret) {
    ret.setAttribute("x", "" + bounds.getX());
    ret.setAttribute("y", "" + bounds.getY());
    ret.setAttribute("width", "" + bounds.getWidth());
    ret.setAttribute("height", "" + bounds.getHeight());
    if (labelLoc != LABEL_NONE) {
      if (labelLoc == LABEL_LEFT) ret.setAttribute("label", "left");
      else if (labelLoc == LABEL_RIGHT) ret.setAttribute("label", "right");
      else if (labelLoc == LABEL_TOP) ret.setAttribute("label", "top");
      else if (labelLoc == LABEL_BOTTOM) ret.setAttribute("label", "bottom");
      else if (labelLoc == LABEL_CENTER) ret.setAttribute("label", "center");
    }
    if (!labelFont.equals(DEFAULT_LABEL_FONT)) SvgCreator.setFontAttribute(ret, labelFont, "");
    if (!SvgCreator.colorMatches(labelColor, Color.darkGray))
      ret.setAttribute("label-color", SvgCreator.getColorString(labelColor));
    if (strokeWidth != DEFAULT_STROKE_WIDTH) ret.setAttribute("stroke-width", "" + strokeWidth);
    ret.setAttribute("path", path.toSvgString());
    return ret;
  }

  public abstract void paintDynamic(Graphics g, CircuitState state);

  public void drawLabel(Graphics g) {
    if (labelLoc == LABEL_NONE) return;
    String label = path.leaf().getAttributeSet().getValue(StdAttr.LABEL);
    if (label == null || label.length() == 0) return;
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    int valign = GraphicsUtil.V_CENTER;
    int halign = GraphicsUtil.H_CENTER;
    int px = x + w / 2, py = y + h / 2;
    if (labelLoc == LABEL_TOP) {
      py = y - 1;
      valign = GraphicsUtil.V_BOTTOM;
    } else if (labelLoc == LABEL_BOTTOM) {
      py = y + h + 1;
      valign = GraphicsUtil.V_TOP;
    } else if (labelLoc == LABEL_RIGHT) {
      px = x + w + 1;
      halign = GraphicsUtil.H_LEFT;
    } else if (labelLoc == LABEL_LEFT) {
      px = x - 1;
      halign = GraphicsUtil.H_RIGHT;
    }
    g.setColor(labelColor);
    GraphicsUtil.drawText(g, labelFont, label, px, py, halign, valign);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DrawAttr.STROKE_WIDTH) {
      return (V) Integer.valueOf(strokeWidth);
    } else if (attr == ATTR_LABEL) {
      return (V) labelLoc;
    } else if (attr == StdAttr.LABEL_FONT) {
      return (V) labelFont;
    } else if (attr == StdAttr.LABEL_COLOR) {
      return (V) labelColor;
    }
    return null;
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    if (attr == DrawAttr.STROKE_WIDTH) {
      strokeWidth = ((Integer) value).intValue();
    } else if (attr == ATTR_LABEL) {
      labelLoc = (AttributeOption) value;
    } else if (attr == StdAttr.LABEL_FONT) {
      labelFont = (Font) value;
    } else if (attr == StdAttr.LABEL_COLOR) {
      labelColor = (Color) value;
    }
  }
}
