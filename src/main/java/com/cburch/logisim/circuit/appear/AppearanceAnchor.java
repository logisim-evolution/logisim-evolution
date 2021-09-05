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
import lombok.Getter;
import lombok.val;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearanceAnchor extends AppearanceElement {
  public static final Attribute<Direction> FACING =
      Attributes.forDirection("facing", S.getter("appearanceFacingAttr"));
  static final List<Attribute<?>> ATTRIBUTES = UnmodifiableList.create(new Attribute<?>[] {FACING});

  private static final int RADIUS = 3;
  private static final int INDICATOR_LENGTH = 8;
  private static final Color SYMBOL_COLOR = new Color(0, 128, 0);

  @Getter private Direction facing;

  public AppearanceAnchor(Location location) {
    super(location);
    facing = Direction.EAST;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    if (super.isInCircle(loc, RADIUS)) {
      return true;
    }
    val center = getLocation();
    val end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
    return (facing == Direction.EAST || facing == Direction.WEST)
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
    val bds = super.getBounds(RADIUS);
    val center = getLocation();
    val end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
    return bds.add(end);
  }

  @Override
  public String getDisplayName() {
    return S.get("circuitAnchor");
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    val c = getLocation();
    val end = c.translate(facing, RADIUS + INDICATOR_LENGTH);
    return UnmodifiableList.create(new Handle[] {new Handle(this, c), new Handle(this, end)});
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == FACING) {
      return (V) facing;
    } else {
      return super.getValue(attr);
    }
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof AppearanceAnchor) {
      val that = (AppearanceAnchor) other;
      return super.matches(that) && this.facing.equals(that.facing);
    }
    return false;
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode() * 31 + facing.hashCode();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    val location = getLocation();
    val x = location.getX();
    val y = location.getY();
    g.setColor(SYMBOL_COLOR);
    g.drawOval(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
    val e0 = location.translate(facing, RADIUS);
    val e1 = location.translate(facing, RADIUS + INDICATOR_LENGTH);
    g.drawLine(e0.getX(), e0.getY(), e1.getX(), e1.getY());
  }

  @Override
  public Element toSvgElement(Document doc) {
    val loc = getLocation();
    val ret = doc.createElement("circ-anchor");
    ret.setAttribute("x", "" + (loc.getX() - RADIUS));
    ret.setAttribute("y", "" + (loc.getY() - RADIUS));
    ret.setAttribute("width", "" + 2 * RADIUS);
    ret.setAttribute("height", "" + 2 * RADIUS);
    ret.setAttribute("facing", facing.toString());
    return ret;
  }

  @Override
  protected void updateValue(Attribute<?> attr, Object value) {
    if (attr == FACING) {
      facing = (Direction) value;
    } else {
      super.updateValue(attr, value);
    }
  }
}
