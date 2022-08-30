/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    public final InstanceComponent[] elt;

    public Path(InstanceComponent[] elt) {
      this.elt = elt;
    }

    public boolean contains(Component c) {
      for (final var ic : elt) {
        if (ic == c) return true;
      }
      return false;
    }

    public InstanceComponent leaf() {
      return elt[elt.length - 1];
    }

    @Override
    public String toString() {
      return toSvgString();
    }

    public String toSvgString() {
      final var s = new StringBuilder();
      for (final var instanceComponent : elt) {
        final var loc = instanceComponent.getLocation();
        s.append("/").append(escape(instanceComponent.getFactory().getName())).append(loc);
      }
      return s.toString();
    }

    public static Path fromSvgString(String s, Circuit circuit) throws IllegalArgumentException {
      if (!s.startsWith("/")) throw new IllegalArgumentException("Bad path: " + s);
      final var parts = s.substring(1).split("(?<!\\\\)/");
      final var elt = new InstanceComponent[parts.length];
      for (var i = 0; i < parts.length; i++) {
        final var ss = parts[i];
        final var p = ss.lastIndexOf("(");
        final var c = ss.lastIndexOf(",");
        final var e = ss.lastIndexOf(")");
        if (e != ss.length() - 1 || p <= 0 || c <= p)
          throw new IllegalArgumentException("Bad path element: " + ss);
        final var x = Integer.parseInt(ss.substring(p + 1, c).trim());
        final var y = Integer.parseInt(ss.substring(c + 1, e).trim());
        final var loc = Location.create(x, y, false);
        final var name = unescape(ss.substring(0, p));
        var circ = circuit;
        if (i > 0) circ = ((SubcircuitFactory) elt[i - 1].getFactory()).getSubcircuit();
        final var ic = find(circ, loc, name);
        if (ic == null) throw new IllegalArgumentException("Missing component: " + ss);
        elt[i] = ic;
      }
      return new Path(elt);
    }

    private static InstanceComponent find(Circuit circuit, Location loc, String name) {
      for (final var c : circuit.getNonWires()) {
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
  protected final Path path;
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
    return Location.create(bounds.getX(), bounds.getY(), false);
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
    final var x0 = bounds.getX();
    final var y0 = bounds.getY();
    final var x1 = x0 + bounds.getWidth();
    final var y1 = y0 + bounds.getHeight();
    return UnmodifiableList.create(
        new Handle[] {
          new Handle(this, x0, y0),
          new Handle(this, x1, y0),
          new Handle(this, x1, y1),
          new Handle(this, x0, y1)
        });
  }

  protected Object getData(CircuitState state) {
    var obj = state.getData(path.elt[0]);
    for (var i = 1; i < path.elt.length && obj != null; i++) {
      if (!(obj instanceof CircuitState)) {
        throw new IllegalStateException(
            "Expecting CircuitState for path["
                + (i - 1)
                + "] "
                + path.elt[i - 1]
                + "  but got: "
                + obj);
      }
      state = (CircuitState) obj;
      obj = state.getData(path.elt[i]);
    }
    return obj;
  }

  protected InstanceComponent getComponent(CircuitState state) {
    var obj = state.getData(path.elt[0]);
    var comp = path.elt[0];
    for (var i = 1; i < path.elt.length && obj != null; i++) {
      if (!(obj instanceof CircuitState)) {
        throw new IllegalStateException(
            "Expecting CircuitState for path["
                + (i - 1)
                + "] "
                + path.elt[i - 1]
                + "  but got: "
                + obj);
      }
      state = (CircuitState) obj;
      comp = path.elt[i];
      obj = state.getData(path.elt[i]);
    }
    return comp;
  }

  @Override
  public String getDisplayNameAndLabel() {
    final var label = path.leaf().getInstance().getAttributeValue(StdAttr.LABEL);
    return (label != null && label.length() > 0)
        ? getDisplayName() + " \"" + label + "\""
        : getDisplayName();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    paintDynamic(g, null);
  }

  public void parseSvgElement(Element elt) {
    if (elt.hasAttribute("stroke-width"))
      strokeWidth = Integer.parseInt(elt.getAttribute("stroke-width").trim());
    if (elt.hasAttribute("label")) {
      final var loc = elt.getAttribute("label").trim().toLowerCase();
      labelLoc = switch (loc) {
        case "left" -> LABEL_LEFT;
        case "right" -> LABEL_RIGHT;
        case "top" -> LABEL_TOP;
        case "bottom" -> LABEL_BOTTOM;
        case "center" -> LABEL_CENTER;
        case "none" -> LABEL_NONE;
        default -> LABEL_NONE;
      };
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
    final var label = path.leaf().getAttributeSet().getValue(StdAttr.LABEL);
    if (label == null || label.length() == 0) return;
    final var x = bounds.getX();
    final var y = bounds.getY();
    final var w = bounds.getWidth();
    final var h = bounds.getHeight();
    var vAlign = GraphicsUtil.V_CENTER;
    var hAlign = GraphicsUtil.H_CENTER;
    var pX = x + w / 2;
    var pY = y + h / 2;
    if (labelLoc == LABEL_TOP) {
      pY = y - 1;
      vAlign = GraphicsUtil.V_BOTTOM;
    } else if (labelLoc == LABEL_BOTTOM) {
      pY = y + h + 1;
      vAlign = GraphicsUtil.V_TOP;
    } else if (labelLoc == LABEL_RIGHT) {
      pX = x + w + 1;
      hAlign = GraphicsUtil.H_LEFT;
    } else if (labelLoc == LABEL_LEFT) {
      pX = x - 1;
      hAlign = GraphicsUtil.H_RIGHT;
    }
    g.setColor(labelColor);
    GraphicsUtil.drawText(g, labelFont, label, pX, pY, hAlign, vAlign);
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
      strokeWidth = (Integer) value;
    } else if (attr == ATTR_LABEL) {
      labelLoc = (AttributeOption) value;
    } else if (attr == StdAttr.LABEL_FONT) {
      labelFont = (Font) value;
    } else if (attr == StdAttr.LABEL_COLOR) {
      labelColor = (Color) value;
    }
  }
}
