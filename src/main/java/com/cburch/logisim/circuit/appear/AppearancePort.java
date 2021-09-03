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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearancePort extends AppearanceElement {
  private static final int INPUT_RADIUS = 4;
  private static final int OUTPUT_RADIUS = 5;
  private static final int MINOR_RADIUS = 2;
  public static final Color COLOR = Color.BLUE;

  private Instance pin;

  public AppearancePort(Location location, Instance pin) {
    super(location);
    this.pin = pin;
  }
  
  public static boolean isInputAppearance(int radius) {
    return radius == INPUT_RADIUS;
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    if (isInput()) {
      return getBounds().contains(loc);
    } else {
      return super.isInCircle(loc, OUTPUT_RADIUS);
    }
  }

  @Override
  public Bounds getBounds() {
    int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
    return super.getBounds(r);
  }

  @Override
  public String getDisplayName() {
    return S.get("circuitPort");
  }

  @Override
  public String getDisplayNameAndLabel() {
    String label = pin.getAttributeValue(StdAttr.LABEL);
    if (label != null && label.length() > 0) return getDisplayName() + " \"" + label + "\"";
    else return getDisplayName();
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    Location loc = getLocation();

    int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
    return UnmodifiableList.create(
        new Handle[] {
          new Handle(this, loc.translate(-r, -r)),
          new Handle(this, loc.translate(r, -r)),
          new Handle(this, loc.translate(r, r)),
          new Handle(this, loc.translate(-r, r))
        });
  }

  public Instance getPin() {
    return pin;
  }

  private boolean isInput() {
    Instance p = pin;
    return p == null || Pin.FACTORY.isInputPin(p);
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof AppearancePort) {
      AppearancePort that = (AppearancePort) other;
      return this.matches(that) && this.pin == that.pin;
    } else {
      return false;
    }
  }

  @Override
  public int matchesHashCode() {
    return super.matchesHashCode() + pin.hashCode();
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    Location location = getLocation();
    int x = location.getX();
    int y = location.getY();
    g.setColor(COLOR);
    if (isInput()) {
      int r = INPUT_RADIUS;
      g.drawRect(x - r, y - r, 2 * r, 2 * r);
    } else {
      int r = OUTPUT_RADIUS;
      g.drawOval(x - r, y - r, 2 * r, 2 * r);
    }
    g.fillOval(x - MINOR_RADIUS, y - MINOR_RADIUS, 2 * MINOR_RADIUS, 2 * MINOR_RADIUS);
  }

  void setPin(Instance value) {
    pin = value;
  }

  @Override
  public Element toSvgElement(Document doc) {
    Location loc = getLocation();
    Location pinLoc = pin.getLocation();
    Element ret = doc.createElement("circ-port");
    int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
    ret.setAttribute("x", "" + (loc.getX() - r));
    ret.setAttribute("y", "" + (loc.getY() - r));
    ret.setAttribute("width", "" + 2 * r);
    ret.setAttribute("height", "" + 2 * r);
    ret.setAttribute("pin", "" + pinLoc.getX() + "," + pinLoc.getY());
    return ret;
  }
}
