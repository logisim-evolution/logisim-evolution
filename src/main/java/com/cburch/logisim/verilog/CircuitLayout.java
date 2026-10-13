/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.WordNetlist.attrs;
import static com.cburch.logisim.verilog.YosysJson.checkCancelled;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.tools.AddTool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Bounded orthogonal routing with circuit-local tunnels between blocks. */
final class CircuitLayout {
  private record Pin(Location point, String net, boolean tunnel) {}
  private final LogisimFile file;
  private final Map<String, Circuit> children;

  CircuitLayout(LogisimFile file, Map<String, Circuit> children) {
    this.file = file;
    this.children = children;
  }

  private static Location point(int x, int y) {
    return Location.create(x, y, true);
  }

  private Component component(
      String name, String library, int x, int y, Map<String, String> config) {
    ComponentFactory factory;
    if (library == null) {
      var child = children.get(name);
      if (child == null) {
        throw new ImportException("Missing subcircuit " + name);
      }
      factory = child.getSubcircuitFactory();
    } else {
      var lib = file.getLoader().getBuiltin().getLibrary(library);
      if (lib == null || !(lib.getTool(name) instanceof AddTool tool)) {
        throw new ImportException("This Logisim version has no " + library + "/" + name);
      }
      factory = tool.getFactory();
    }
    var attributes = factory.createAttributeSet();
    config.forEach((key, value) -> setAttribute(attributes, key, value));
    return factory.createComponent(point(x, y), attributes);
  }

  private static <V> void setParsed(AttributeSet set, Attribute<V> attr, String value) {
    set.setValue(attr, attr.parse(value));
  }

  private static void setAttribute(AttributeSet set, String key, String value) {
    var attr = set.getAttribute(key);
    if (attr == null) {
      throw new ImportException("Unsupported component attribute " + key);
    }
    setParsed(set, attr, value);
  }

  Circuit build(WordNetlist netlist, String name) {
    var circuit = new Circuit(name, file, null);
    var mutation = new CircuitMutation(circuit);
    var elems = netlist.elements;
    int blockSize = 32;
    int blockCount = (elems.size() + blockSize - 1) / blockSize;
    var first = new HashMap<String, Integer>();
    var external = new HashSet<String>();
    int right = 0;
    int maximumNets = 0;
    int maximumHeight = 0;
    for (int bi = 0; bi < blockCount; bi++) {
      var nets = new HashSet<String>();
      int height = 0;
      for (var el : elems.subList(bi * blockSize, Math.min(elems.size(), (bi + 1) * blockSize))) {
        height += el.height() + el.top();
        for (var t : el.terminals()) {
          nets.add(t.net());
          right = Math.max(right, Math.max(t.x(), t.escapeX()));
          Integer previous = first.putIfAbsent(t.net(), bi);
          if (previous != null && previous != bi) {
            external.add(t.net());
          }
        }
      }
      maximumHeight = Math.max(maximumHeight, height);
      maximumNets = Math.max(maximumNets, nets.size());
    }
    int width = maximumNets * 10 + 600 + right;
    int height = maximumHeight + 100;
    int columns = Math.max(1, (int) Math.sqrt(blockCount));
    var portComponents = new HashMap<String, Component>();
    for (int bi = 0; bi < blockCount; bi++) {
      checkCancelled();
      int bx = (bi % columns) * width + 100;
      int by = (bi / columns) * height + 100;
      int ox = bx + width - 350 - right;
      int oy = by + 40;
      var branches = new LinkedHashMap<String, List<Location>>();
      var wires = new ArrayList<Wire>();
      var pins = new ArrayList<Pin>();
      for (var el : elems.subList(bi * blockSize, Math.min(elems.size(), (bi + 1) * blockSize))) {
        oy += el.top();
        var comp = component(el.name(), el.library(), ox, oy, el.attributes());
        mutation.add(comp);
        if (el.port() != null) {
          portComponents.put(el.port(), comp);
        }
        for (var t : el.terminals()) {
          var a = point(ox + t.x(), oy + t.y());
          var b = point(ox + t.escapeX(), oy + t.y());
          var c = point(ox + t.escapeX(), oy + t.escapeY());
          // Geometry is checked against the live component, not just our routing model.
          var end = comp.getEnds().stream().filter(e -> e.getLocation().equals(a)).findFirst();
          if (end.isEmpty() || end.get().getWidth().getWidth() != netlist.widths.get(t.net())) {
            throw new ImportException("Pin geometry/width mismatch for " + el.name() + " at " + a);
          }
          wire(wires, a, b);
          wire(wires, b, c);
          pins.add(new Pin(a, t.net(), false));
          branches.computeIfAbsent(t.net(), key -> new ArrayList<>()).add(c);
        }
        oy += el.height();
      }
      int ni = 0;
      for (var entry : branches.entrySet()) {
        int tx = bx + ni++ * 10;
        int lo = entry.getValue().stream().mapToInt(Location::getY).min().orElseThrow();
        int hi = entry.getValue().stream().mapToInt(Location::getY).max().orElseThrow();
        wire(wires, point(tx, lo), point(tx, hi));
        for (var end : entry.getValue()) {
          wire(wires, point(tx, end.getY()), end);
        }
        if (external.contains(entry.getKey())) {
          mutation.add(component("Tunnel", "Wiring", tx, lo,
              attrs("label", entry.getKey(), "facing", "east", "width",
                  netlist.widths.get(entry.getKey()))));
          pins.add(new Pin(point(tx, lo), entry.getKey(), true));
        }
      }
      verify(wires, pins);
      mutation.addAll(wires);
    }
    mutation.execute();
    var conflicts = circuit.getWidthIncompatibilityData();
    if (conflicts != null && !conflicts.isEmpty()) {
      throw new ImportException("Bus width conflict in " + name);
    }
    var appearance = new ArrayList<CanvasObject>();
    int symbolHeight = netlist.ports.stream().mapToInt(WordNetlist.Port::y).max().orElse(20) + 20;
    appearance.add(new Rectangle(0, 0, 200, symbolHeight));
    appearance.add(new Text(10, 15, name));
    for (var port : netlist.ports) {
      appearance.add(new AppearancePort(
          point(port.x(), port.y()), Instance.getInstanceFor(portComponents.get(port.name()))));
      appearance.add(new Text(port.input() ? 5 : 120, port.y() - 5, port.name()));
    }
    appearance.add(new AppearanceAnchor(point(0, 0)));
    circuit.getStaticAttributes().setValue(
        CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CUSTOM);
    circuit.getAppearance().setObjectsForce(appearance);
    return circuit;
  }

  private static void wire(List<Wire> wires, Location a, Location b) {
    if (!a.equals(b)) {
      wires.add(Wire.create(a, b));
    }
  }

  private static final class Connections {
    private final Map<Location, Location> parent = new HashMap<>();

    Location root(Location point) {
      parent.putIfAbsent(point, point);
      var root = point;
      while (!parent.get(root).equals(root)) {
        root = parent.get(root);
      }
      while (!parent.get(point).equals(root)) {
        var next = parent.put(point, root);
        point = next;
      }
      return root;
    }

    void join(Location a, Location b) {
      parent.put(root(a), root(b));
    }
  }

  private static void verify(List<Wire> wires, List<Pin> pins) {
    var connections = new Connections();
    var points = new HashSet<Location>();
    for (var pin : pins) {
      points.add(pin.point());
    }
    for (var wire : wires) {
      points.add(wire.getEnd0());
      points.add(wire.getEnd1());
      connections.join(wire.getEnd0(), wire.getEnd1());
    }
    var rows = new TreeMap<Integer, TreeSet<Integer>>();
    var columns = new TreeMap<Integer, TreeSet<Integer>>();
    for (var p : points) {
      rows.computeIfAbsent(p.getY(), k -> new TreeSet<>()).add(p.getX());
      columns.computeIfAbsent(p.getX(), k -> new TreeSet<>()).add(p.getY());
    }
    for (var wire : wires) {
      var a = wire.getEnd0();
      var b = wire.getEnd1();
      if (a.getY() == b.getY()) {
        for (int x : rows.get(a.getY()).subSet(
                 Math.min(a.getX(), b.getX()), true, Math.max(a.getX(), b.getX()), true)) {
          connections.join(a, point(x, a.getY()));
        }
      } else {
        for (int y : columns.get(a.getX()).subSet(
                 Math.min(a.getY(), b.getY()), true, Math.max(a.getY(), b.getY()), true)) {
          connections.join(a, point(a.getX(), y));
        }
      }
    }
    var tunnels = new HashMap<String, Location>();
    for (var pin : pins) {
      if (pin.tunnel()) {
        var previous = tunnels.putIfAbsent(pin.net(), pin.point());
        if (previous != null) {
          connections.join(previous, pin.point());
        }
      }
    }
    var labels = new HashMap<Location, String>();
    var nets = new HashMap<String, Location>();
    for (var pin : pins) {
      var root = connections.root(pin.point());
      var previousLabel = labels.putIfAbsent(root, pin.net());
      if (previousLabel != null && !previousLabel.equals(pin.net())) {
        throw new ImportException("Routing short between " + previousLabel + " and " + pin.net());
      }
      var previousRoot = nets.putIfAbsent(pin.net(), root);
      if (previousRoot != null && !previousRoot.equals(root)) {
        throw new ImportException("Split net " + pin.net());
      }
    }
  }
}
