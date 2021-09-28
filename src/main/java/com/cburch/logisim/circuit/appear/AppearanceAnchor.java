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

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearanceAnchor extends AppearanceElement {
  public static final Attribute<Direction> FACING = Attributes.forDirection("facing", S.getter("appearanceFacingAttr"));
  static final List<Attribute<?>> ATTRIBUTES = UnmodifiableList.create(new Attribute<?>[] {FACING});

  private static final int RADIUS = 3;
  private static final int INDICATOR_LENGTH = 8;
  private static final Color SYMBOL_COLOR = new Color(0, 128, 0);

  private Direction factingDirection;

  public AppearanceAnchor(Location location) {
    super(location);
    factingDirection = Direction.EAST;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    if (super.isInCircle(loc, RADIUS)) {
      return true;
    }

    final var center = getLocation();
    final var end = center.translate(factingDirection, RADIUS + INDICATOR_LENGTH);
    return (factingDirection == Direction.EAST || factingDirection == Direction.WEST)
        ? Math.abs(loc.getY() - center.getY()) < 2
            && (loc.getX() < center.getX()) != (loc.getX() < end.getX())
        : Math.abs(loc.getX() - center.getX()) < 2
            && (loc.getY() < center.getY()) != (loc.getY() < end.getY());
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @Override
  public Bounds getBounds() {
    final var bds = super.getBounds(RADIUS);
    final var center = getLocation();
    final var end = center.translate(factingDirection, RADIUS + INDICATOR_LENGTH);
    return bds.add(end);
  }

  @Override
  public String getDisplayName() {
    return S.get("circuitAnchor");
  }

  public Direction getFacingDirection() {
    return factingDirection;
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    final var c = getLocation();
    final var end = c.translate(factingDirection, RADIUS + INDICATOR_LENGTH);
    return UnmodifiableList.create(new Handle[] {new Handle(this, c), new Handle(this, end)});
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    return (attr == FACING) ? (V) factingDirection : super.getValue(attr);
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof AppearanceAnchor that) {
      return super.matches(that) && this.factingDirection.equals(that.factingDirection);
    }
    return false;
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode() * 31 + factingDirection.hashCode();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    final var location = getLocation();
    final var x = location.getX();
    final var y = location.getY();
    g.setColor(SYMBOL_COLOR);
    g.drawOval(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
    final var e0 = location.translate(factingDirection, RADIUS);
    final var e1 = location.translate(factingDirection, RADIUS + INDICATOR_LENGTH);
    g.drawLine(e0.getX(), e0.getY(), e1.getX(), e1.getY());
  }

  @Override
  public Element toSvgElement(Document doc) {
    final var loc = getLocation();
    final var ret = doc.createElement("circ-anchor");
    ret.setAttribute("x", "" + (loc.getX() - RADIUS));
    ret.setAttribute("y", "" + (loc.getY() - RADIUS));
    ret.setAttribute("width", "" + 2 * RADIUS);
    ret.setAttribute("height", "" + 2 * RADIUS);
    ret.setAttribute("facing", factingDirection.toString());
    return ret;
  }

  @Override
  protected void updateValue(Attribute<?> attr, Object value) {
    if (attr == FACING) {
      factingDirection = (Direction) value;
    } else {
      super.updateValue(attr, value);
    }
  }
}
