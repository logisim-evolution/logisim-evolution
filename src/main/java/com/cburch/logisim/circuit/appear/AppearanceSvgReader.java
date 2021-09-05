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
import lombok.Getter;
import lombok.val;
import org.w3c.dom.Element;

public class AppearanceSvgReader {
  public static class pinInfo {
    @Getter private final Location pinLocation; // my location
    @Getter private final Instance pinInstance; // my instance
    private Boolean pinUsed;  // indicates if pin is used

    public pinInfo(Location loc, Instance inst) {
      pinLocation = loc;
      pinInstance = inst;
      pinUsed = false;
    }

    public Boolean pinIsAlreadyUsed() {
      return pinUsed;
    }

    public void setPinUsed() {
      pinUsed = true;
    }
  }

  public static pinInfo getPinInfo(Location loc, Instance inst) {
    return new pinInfo(loc, inst);
  }

  public static AbstractCanvasObject createShape(Element elt, List<pinInfo> pins, Circuit circuit) {
    val name = elt.getTagName();
    if (name.equals("circ-anchor") || name.equals("circ-origin")) {
      val loc = getLocation(elt);
      val ret = new AppearanceAnchor(loc);
      if (elt.hasAttribute("facing")) {
        val facing = Direction.parse(elt.getAttribute("facing"));
        ret.setValue(AppearanceAnchor.FACING, facing);
      }
      return ret;
    } else if (name.equals("circ-port")) {
      val loc = getLocation(elt);
      val pinStr = elt.getAttribute("pin").split(",");
      val pinLoc = Location.create(Integer.parseInt(pinStr[0].trim()), Integer.parseInt(pinStr[1].trim()));
      for (val pin : pins) {
        if (pin.pinIsAlreadyUsed()) continue;
        if (pin.getPinLocation().equals(pinLoc)) {
          val isInputPin = ((Pin) pin.getPinInstance().getFactory()).isInputPin(pin.getPinInstance());
          val isInputRef = isInputPinReference(elt);
          if (isInputPin == isInputRef) {
            pin.setPinUsed();
            return new AppearancePort(loc, pin.getPinInstance());
          }
        }
      }
      return null;
    } else if (name.startsWith("visible-")) {
      val pathstr = elt.getAttribute("path");
      if (pathstr == null || pathstr.length() == 0) return null;
      DynamicElement.Path path;
      try {
        path = DynamicElement.Path.fromSvgString(pathstr, circuit);
      } catch (IllegalArgumentException e) {
        System.out.println(e.getMessage());
        return null;
      }
      val x = (int) Double.parseDouble(elt.getAttribute("x").trim());
      val y = (int) Double.parseDouble(elt.getAttribute("y").trim());
      val shape = getDynamicElement(name, path, x, y);
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

  private static DynamicElement getDynamicElement(String name, DynamicElement.Path path, int x, int y) {
    return switch (name) {
      case "visible-led" -> new LedShape(x, y, path);
      case "visible-rgbled" -> new RGBLedShape(x, y, path);
      case "visible-hexdigit" -> new HexDigitShape(x, y, path);
      case "visible-sevensegment" -> new SevenSegmentShape(x, y, path);
      case "visible-register" -> new RegisterShape(x, y, path);
      case "visible-counter" -> new CounterShape(x, y, path);
      case "visible-vga" -> new SocVgaShape(x, y, path);
      case "visible-soc-cpu" -> new SocCPUShape(x, y, path);
      case "visible-tty" -> new TtyShape(x, y, path);
      default -> null;
    };
  }

  private static Boolean isInputPinReference(Element elt) {
    val width = Double.parseDouble(elt.getAttribute("width"));
    val radius = (int) Math.round(width / 2.0);
    return AppearancePort.isInputAppearance(radius);
  }

  private static Location getLocation(Element elt) {
    val x = Double.parseDouble(elt.getAttribute("x"));
    val y = Double.parseDouble(elt.getAttribute("y"));
    val w = Double.parseDouble(elt.getAttribute("width"));
    val h = Double.parseDouble(elt.getAttribute("height"));
    val px = (int) Math.round(x + w / 2);
    val py = (int) Math.round(y + h / 2);
    return Location.create(px, py);
  }
}
