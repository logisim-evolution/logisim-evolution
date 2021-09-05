/*
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

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.soc.gui.SocCPUShape;
import com.cburch.logisim.soc.vga.SocVgaShape;
import com.cburch.logisim.std.io.HexDigitShape;
import com.cburch.logisim.std.io.LedShape;
import com.cburch.logisim.std.io.RGBLedShape;
import com.cburch.logisim.std.io.SevenSegmentShape;
import com.cburch.logisim.std.io.TtyShape;
import com.cburch.logisim.std.memory.CounterShape;
import com.cburch.logisim.std.memory.RegisterShape;
import com.cburch.logisim.std.wiring.Pin;

import java.util.List;
import org.w3c.dom.Element;

public class AppearanceSvgReader {
  public static class pinInfo {
    private final Location myLocation;
    private final Instance myInstance;
    private Boolean pinIsUsed;
    
    public pinInfo(Location loc, Instance inst) {
      myLocation = loc;
      myInstance = inst;
      pinIsUsed = false;
    }
    
    public Boolean pinIsAlreadyUsed() {
      return pinIsUsed;
    }
    
    public Location getPinLocation() {
      return myLocation;
    }
    
    public Instance getPinInstance() {
      return myInstance;
    }
    
    public void setPinIsUsed() {
      pinIsUsed = true;
    }
  }
  
  public static pinInfo getPinInfo(Location loc, Instance inst) {
    return new pinInfo(loc, inst);
  }
  
  public static AbstractCanvasObject createShape(Element elt, List<pinInfo> pins, Circuit circuit) {
    final var name = elt.getTagName();
    if (name.equals("circ-anchor") || name.equals("circ-origin")) {
      final var loc = getLocation(elt);
      final var ret = new AppearanceAnchor(loc);
      if (elt.hasAttribute("facing")) {
        final var facing = Direction.parse(elt.getAttribute("facing"));
        ret.setValue(AppearanceAnchor.FACING, facing);
      }
      return ret;
    } else if (name.equals("circ-port")) {
      final var loc = getLocation(elt);
      final var pinStr = elt.getAttribute("pin").split(",");
      final var pinLoc = Location.create(Integer.parseInt(pinStr[0].trim()), Integer.parseInt(pinStr[1].trim()));
      for (final var pin : pins) {
        if (pin.pinIsAlreadyUsed()) continue;
        if (pin.getPinLocation().equals(pinLoc)) {
          final var isInputPin = ((Pin) pin.getPinInstance().getFactory()).isInputPin(pin.getPinInstance());
          final var isInputRef = isInputPinReference(elt);
          if (isInputPin == isInputRef) {
            pin.setPinIsUsed();
            return new AppearancePort(loc, pin.getPinInstance()); 
          }
        }
      }
      return null; 
    } else if (name.startsWith("visible-")) {
      final var pathstr = elt.getAttribute("path");
      if (pathstr == null || pathstr.length() == 0) return null;
      DynamicElement.Path path;
      try {
        path = DynamicElement.Path.fromSvgString(pathstr, circuit);
      } catch (IllegalArgumentException e) {
        System.out.println(e.getMessage());
        return null;
      }
      final var x = (int) Double.parseDouble(elt.getAttribute("x").trim());
      final var y = (int) Double.parseDouble(elt.getAttribute("y").trim());
      final var shape = getDynamicElement(name, path, x, y);
      if (shape == null) {
        return null;
      }
      try {
        shape.parseSvgElement(elt);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
      return shape;
    }
    return SvgReader.createShape(elt);
  }

  private static DynamicElement getDynamicElement(String name, DynamicElement.Path path, int x,
      int y) {
    switch (name) {
      case "visible-led":
        return new LedShape(x, y, path);
      case "visible-rgbled":
        return new RGBLedShape(x, y, path);
      case "visible-hexdigit":
        return new HexDigitShape(x, y, path);
      case "visible-sevensegment":
        return new SevenSegmentShape(x, y, path);
      case "visible-register":
        return new RegisterShape(x, y, path);
      case "visible-counter":
        return new CounterShape(x, y, path);
      case "visible-vga":
        return new SocVgaShape(x, y, path);
      case "visible-soc-cpu":
        return new SocCPUShape(x, y, path);
      case "visible-tty":
        return new TtyShape(x, y, path);
      default:
        return null;
    }
  }
  
  private static Boolean isInputPinReference(Element elt) {
    final var width = Double.parseDouble(elt.getAttribute("width"));
    final var radius = (int) Math.round(width / 2.0);
    return AppearancePort.isInputAppearance(radius);
  }

  private static Location getLocation(Element elt) {
    double x = Double.parseDouble(elt.getAttribute("x"));
    double y = Double.parseDouble(elt.getAttribute("y"));
    double w = Double.parseDouble(elt.getAttribute("width"));
    double h = Double.parseDouble(elt.getAttribute("height"));
    int px = (int) Math.round(x + w / 2);
    int py = (int) Math.round(y + h / 2);
    return Location.create(px, py);
  }
}
