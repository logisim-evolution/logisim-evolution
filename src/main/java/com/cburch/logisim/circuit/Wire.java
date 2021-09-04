/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.tools.CustomHandles;
import com.cburch.logisim.util.Cache;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public final class Wire implements Component, AttributeSet, CustomHandles, Iterable<Location> {
  private class EndList extends AbstractList<EndData> {
    @Override
    public EndData get(int i) {
      return getEnd(i);
    }

    @Override
    public int size() {
      return 2;
    }
  }

  public static Wire create(Location e0, Location e1) {
    return (Wire) cache.get(new Wire(e0, e1));
  }

  /** Stroke width when drawing wires. */
  public static final int WIDTH = 3;

  public static final int WIDTH_BUS = 4;
  public static final int HIGHLIGHTED_WIDTH = 4;
  public static final int HIGHLIGHTED_WIDTH_BUS = 5;
  public static final Stroke HIGHLIGHTED_STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {7}, 0);
  public static final double DOT_MULTIPLY_FACTOR = 1.35; // multiply factor for the intersection points
  public static final AttributeOption VALUE_HORZ = new AttributeOption("horz", S.getter("wireDirectionHorzOption"));
  public static final AttributeOption VALUE_VERT = new AttributeOption("vert", S.getter("wireDirectionVertOption"));

  public static final Attribute<AttributeOption> DIR_ATTR = Attributes.forOption("direction", S.getter("wireDirectionAttr"), new AttributeOption[] {VALUE_HORZ, VALUE_VERT});
  public static final Attribute<Integer> LEN_ATTR = Attributes.forInteger("length", S.getter("wireLengthAttr"));

  @Getter private final List<Attribute<?>> attributes = Arrays.asList(DIR_ATTR, LEN_ATTR);

  private static final Cache cache = new Cache();

  @Getter private final Location end0;
  @Getter private final Location end1;
  final boolean xIsEqual;
  @Setter @Getter private boolean DRCHighlighted = false;
  @Getter @Setter private Color DRCHighlightColor = Netlist.DRC_WIRE_MARK_COLOR;

  private Wire(Location end0, Location e1) {
    this.xIsEqual = end0.getX() == e1.getX();
    if (xIsEqual) {
      if (end0.getY() > e1.getY()) {
        this.end0 = e1;
        this.end1 = end0;
      } else {
        this.end0 = end0;
        this.end1 = e1;
      }
    } else {
      if (end0.getX() > e1.getX()) {
        this.end0 = e1;
        this.end1 = end0;
      } else {
        this.end0 = end0;
        this.end1 = e1;
      }
    }
  }

  // Component methods
  //
  // (Wire never issues ComponentEvents, so we don't need ComponentListener to track listeners,
  // hence no addComponentListener() implementation.

  //
  // AttributeSet methods
  //
  // It makes some sense for a wire to be its own attribute, since
  // after all it is immutable.
  //
  @Override
  public Object clone() {
    return this;
  }

  @Override
  public boolean contains(Location q) {
    int qx = q.getX();
    int qy = q.getY();
    if (xIsEqual) {
      int wx = end0.getX();
      return qx >= wx - 2 && qx <= wx + 2 && end0.getY() <= qy && qy <= end1.getY();
    } else {
      int wy = end0.getY();
      return qy >= wy - 2 && qy <= wy + 2 && end0.getX() <= qx && qx <= end1.getX();
    }
  }

  @Override
  public boolean contains(Location pt, Graphics g) {
    return contains(pt);
  }

  @Override
  public boolean containsAttribute(Attribute<?> attr) {
    return attributes.contains(attr);
  }

  @Override
  public void draw(ComponentDrawContext context) {
    final var state = context.getCircuitState();
    final var g = context.getGraphics();
    GraphicsUtil.switchToWidth(g, WIDTH);
    g.setColor(state.getValue(end0).getColor());
    g.drawLine(end0.getX(), end0.getY(), end1.getX(), end1.getY());
  }

  @Override
  public void drawHandles(ComponentDrawContext context) {
    context.drawHandle(end0);
    context.drawHandle(end1);
  }

  @Override
  public boolean endsAt(Location pt) {
    return end0.equals(pt) || end1.equals(pt);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Wire)) return false;
    final var w = (Wire) other;
    return w.end0.equals(this.end0) && w.end1.equals(this.end1);
  }

  //
  // user interface methods
  //
  @Override
  public void expose(ComponentDrawContext context) {
    java.awt.Component dest = context.getDestination();
    final var x0 = end0.getX();
    final var y0 = end0.getY();
    dest.repaint(x0 - 5, y0 - 5, end1.getX() - x0 + 10, end1.getY() - y0 + 10);
  }

  @Override
  public Attribute<?> getAttribute(String name) {
    for (Attribute<?> attr : attributes) {
      if (name.equals(attr.getName())) return attr;
    }
    return null;
  }

  @Override
  public AttributeSet getAttributeSet() {
    return this;
  }

  @Override
  public Bounds getBounds() {
    final var x0 = end0.getX();
    final var y0 = end0.getY();
    return Bounds.create(x0 - 2, y0 - 2, end1.getX() - x0 + 5, end1.getY() - y0 + 5);
  }

  @Override
  public Bounds getBounds(Graphics g) {
    return getBounds();
  }

  @Override
  public EndData getEnd(int index) {
    return new EndData(getEndLocation(index), BitWidth.UNKNOWN, EndData.INPUT_OUTPUT);
  }

  public Location getEndLocation(int index) {
    return index == 0 ? end0 : end1;
  }

  //
  // propagation methods
  //
  @Override
  public List<EndData> getEnds() {
    return new EndList();
  }

  @Override
  public ComponentFactory getFactory() {
    return WireFactory.instance;
  }

  @Override
  public Object getFeature(Object key) {
    return (key == CustomHandles.class) ? this : null;
  }

  public int getLength() {
    return (end1.getY() - end0.getY()) + (end1.getX() - end0.getX());
  }

  // location/extent methods
  @Override
  public Location getLocation() {
    return end0;
  }

  public Location getOtherEnd(Location loc) {
    return loc.equals(end0) ? end1 : end0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == DIR_ATTR) {
      return (V) (xIsEqual ? VALUE_VERT : VALUE_HORZ);
    } else if (attr == LEN_ATTR) {
      return (V) Integer.valueOf(getLength());
    } else {
      return null;
    }
  }

  @Override
  public int hashCode() {
    return end0.hashCode() * 31 + end1.hashCode();
  }

  public boolean isParallel(Wire other) {
    return this.xIsEqual == other.xIsEqual;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return true;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return false;
  }

  //
  // other methods
  //
  public boolean isVertical() {
    return xIsEqual;
  }

  @Override
  public Iterator<Location> iterator() {
    return new WireIterator(end0, end1);
  }

  private boolean overlaps(Location q0, Location q1, boolean includeEnds) {
    if (xIsEqual) {
      int x0 = q0.getX();
      if (x0 != q1.getX() || x0 != end0.getX()) return false;
      return includeEnds
          ? end1.getY() >= q0.getY() && end0.getY() <= q1.getY()
          : end1.getY() > q0.getY() && end0.getY() < q1.getY();
    } else {
      int y0 = q0.getY();
      if (y0 != q1.getY() || y0 != end0.getY()) return false;
      return includeEnds
          ? end1.getX() >= q0.getX() && end0.getX() <= q1.getX()
          : end1.getX() > q0.getX() && end0.getX() < q1.getX();
    }
  }

  public boolean overlaps(Wire other, boolean includeEnds) {
    return overlaps(other.end0, other.end1, includeEnds);
  }

  @Override
  public void propagate(CircuitState state) {
    // Normally this is handled by CircuitWires, and so it won't get
    // called. The exception is when a wire is added or removed
    state.markPointAsDirty(end0);
    state.markPointAsDirty(end1);
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    throw new IllegalArgumentException("read only attribute");
  }

  public boolean sharesEnd(Wire other) {
    return this.end0.equals(other.end0)
        || this.end1.equals(other.end0)
        || this.end0.equals(other.end1)
        || this.end1.equals(other.end1);
  }

  @Override
  public String toString() {
    return "Wire[" + end0 + "-" + end1 + "]";
  }
}
