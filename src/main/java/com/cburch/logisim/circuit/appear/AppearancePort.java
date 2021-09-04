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
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import lombok.Getter;
import lombok.val;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearancePort extends AppearanceElement {
  private static final int INPUT_RADIUS = 4;
  private static final int OUTPUT_RADIUS = 5;
  private static final int MINOR_RADIUS = 2;
  public static final Color COLOR = Color.BLUE;

  @Getter private Instance pin;

  public AppearancePort(Location location, Instance pin) {
    super(location);
    this.pin = pin;
  }

  public static boolean isInputAppearance(int radius) {
    return radius == INPUT_RADIUS;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    return (isInput()) ? getBounds().contains(loc) : super.isInCircle(loc, OUTPUT_RADIUS);
  }

  @Override
  public Bounds getBounds() {
    return super.getBounds(getRadius());
  }

  @Override
  public String getDisplayName() {
    return S.get("circuitPort");
  }

  @Override
  public String getDisplayNameAndLabel() {
    val result = new StringBuffer(getDisplayName());
    val label = pin.getAttributeValue(StdAttr.LABEL);
    if (label != null && label.length() > 0) {
         result.append(" \"" + label + "\"");
    }
    return result.toString();
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    val loc = getLocation();
    val radius = getRadius();
    return UnmodifiableList.create(
        new Handle[] {
          new Handle(this, loc.translate(-radius, -radius)),
          new Handle(this, loc.translate(radius, -radius)),
          new Handle(this, loc.translate(radius, radius)),
          new Handle(this, loc.translate(-radius, radius))
        });
  }

  private boolean isInput() {
    val p = pin;
    return p == null || Pin.FACTORY.isInputPin(p);
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof AppearancePort) {
      val that = (AppearancePort) other;
      return this.matches(that) && this.pin == that.pin;
    }
    return false;
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode() + pin.hashCode();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    val location = getLocation();
    val x = location.getX();
    val y = location.getY();
    g.setColor(COLOR);
    if (isInput()) {
      val radius = INPUT_RADIUS;
      g.drawRect(x - radius, y - radius, 2 * radius, 2 * radius);
    } else {
      val radius = OUTPUT_RADIUS;
      g.drawOval(x - radius, y - radius, 2 * radius, 2 * radius);
    }
    g.fillOval(x - MINOR_RADIUS, y - MINOR_RADIUS, 2 * MINOR_RADIUS, 2 * MINOR_RADIUS);
  }

  void setPin(Instance value) {
    pin = value;
  }

  @Override
  public Element toSvgElement(Document doc) {
    val loc = getLocation();
    val pinLoc = pin.getLocation();
    val ret = doc.createElement("circ-port");
    val radius = getRadius();
    ret.setAttribute("x", "" + (loc.getX() - radius));
    ret.setAttribute("y", "" + (loc.getY() - radius));
    ret.setAttribute("width", "" + 2 * radius);
    ret.setAttribute("height", "" + 2 * radius);
    ret.setAttribute("pin", "" + pinLoc.getX() + "," + pinLoc.getY());
    return ret;
  }

  private int getRadius() {
    return isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
  }
}
