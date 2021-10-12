/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.soc.gui.SocCpuShape;
import com.cburch.logisim.soc.vga.SocVgaShape;
import com.cburch.logisim.std.io.HexDigitShape;
import com.cburch.logisim.std.io.LedShape;
import com.cburch.logisim.std.io.RgbLedShape;
import com.cburch.logisim.std.io.SevenSegmentShape;
import com.cburch.logisim.std.io.TtyShape;
import com.cburch.logisim.std.memory.CounterShape;
import com.cburch.logisim.std.memory.RegisterShape;
import com.cburch.logisim.std.wiring.Pin;
import java.util.List;
import org.w3c.dom.Element;

public class AppearanceSvgReader {
  public static class PinInfo {
    private final Location myLocation;
    private final Instance myInstance;
    private Boolean pinIsUsed;

    public PinInfo(Location loc, Instance inst) {
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

  public static PinInfo getPinInfo(Location loc, Instance inst) {
    return new PinInfo(loc, inst);
  }

  public static AbstractCanvasObject createShape(Element elt, List<PinInfo> pins, Circuit circuit) {
    final var name = elt.getTagName();
    if (name.equals("circ-anchor") || name.equals("circ-origin")) {
      final var loc = getLocation(elt);
      final var ret = new AppearanceAnchor(loc);
      if (elt.hasAttribute("facing")) {
        final var facing = Direction.parse(elt.getAttribute("facing"));
        ret.setValue(AppearanceAnchor.FACING, facing);
      }
      return ret;
    }

    if (name.equals("circ-port")) {
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
    }

    if (name.startsWith("visible-")) {
      final var pathStr = elt.getAttribute("path");
      if (pathStr == null || pathStr.length() == 0) return null;
      try {
        final var path = DynamicElement.Path.fromSvgString(pathStr, circuit);
        final var x = (int) Double.parseDouble(elt.getAttribute("x").trim());
        final var y = (int) Double.parseDouble(elt.getAttribute("y").trim());
        final var shape = getDynamicElement(name, path, x, y);
        if (shape == null) return null;
        try {
          shape.parseSvgElement(elt);
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
        return shape;
      } catch (IllegalArgumentException e) {
        System.out.println(e.getMessage());
        return null;
      }
    }

    return SvgReader.createShape(elt);
  }

  private static DynamicElement getDynamicElement(String name, DynamicElement.Path path, int x, int y) {
    return switch (name) {
      case "visible-led" -> new LedShape(x, y, path);
      case "visible-rgbled" -> new RgbLedShape(x, y, path);
      case "visible-hexdigit" -> new HexDigitShape(x, y, path);
      case "visible-sevensegment" -> new SevenSegmentShape(x, y, path);
      case "visible-register" -> new RegisterShape(x, y, path);
      case "visible-counter" -> new CounterShape(x, y, path);
      case "visible-vga" -> new SocVgaShape(x, y, path);
      case "visible-soc-cpu" -> new SocCpuShape(x, y, path);
      case "visible-tty" -> new TtyShape(x, y, path);
      default -> null;
    };
  }

  private static Boolean isInputPinReference(Element elt) {
    final var width = Double.parseDouble(elt.getAttribute("width"));
    final var radius = (int) Math.round(width / 2.0);
    return AppearancePort.isInputAppearance(radius);
  }

  private static Location getLocation(Element elt) {
    final var x = Double.parseDouble(elt.getAttribute("x"));
    final var y = Double.parseDouble(elt.getAttribute("y"));
    final var w = Double.parseDouble(elt.getAttribute("width"));
    final var h = Double.parseDouble(elt.getAttribute("height"));
    final var px = (int) Math.round(x + w / 2);
    final var py = (int) Math.round(y + h / 2);
    return Location.create(px, py);
  }
}
