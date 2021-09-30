/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CircuitAppearance extends Drawing {
  public static final int PIN_LENGTH = 10;

  private class MyListener implements CanvasModelListener {
    @Override
    public void modelChanged(CanvasModelEvent event) {
      if (!suppressRecompute) {
        setDefaultAppearance(false);
        fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
      }
    }
  }

  private final Circuit circuit;
  private final EventSourceWeakSupport<CircuitAppearanceListener> listeners;
  private final PortManager portManager;
  private final CircuitPins circuitPins;
  private final MyListener myListener;
  private boolean isDefault;
  private boolean suppressRecompute;

  public CircuitAppearance(Circuit circuit) {
    this.circuit = circuit;
    listeners = new EventSourceWeakSupport<>();
    portManager = new PortManager(this);
    circuitPins = new CircuitPins(portManager);
    myListener = new MyListener();
    suppressRecompute = false;
    addCanvasModelListener(myListener);
    setDefaultAppearance(true);
  }

  public String getName() {
    return (circuit == null || circuit.getStaticAttributes() == null)
        ? null
        : circuit.getStaticAttributes().getValue(CircuitAttributes.NAME_ATTR);
  }

  public CircuitPins getCircuitPin() {
    return circuitPins;
  }

  public void addCircuitAppearanceListener(CircuitAppearanceListener l) {
    listeners.add(l);
  }

  public void sortPinsList(List<Instance> pins, Direction facing) {
    DefaultAppearance.sortPinList(pins, facing);
  }

  @Override
  public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
    super.addObjects(index, shapes);
    checkToFirePortsChanged(shapes);
  }

  @Override
  public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
    super.addObjects(shapes);
    checkToFirePortsChanged(shapes.keySet());
  }

  private boolean affectsPorts(Collection<? extends CanvasObject> shapes) {
    for (CanvasObject obj : shapes) {
      if (obj instanceof AppearanceElement) return true;
    }
    return false;
  }

  private void checkToFirePortsChanged(Collection<? extends CanvasObject> shapes) {
    if (affectsPorts(shapes)) recomputePorts();
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public boolean contains(Location loc) {
    Location query;
    final var anchor = findAnchor();

    if (anchor == null) {
      query = loc;
    } else {
      final var anchorLoc = anchor.getLocation();
      query = loc.translate(anchorLoc.getX(), anchorLoc.getY());
    }

    for (final var obj : getObjectsFromBottom()) {
      if (!(obj instanceof AppearanceElement) && obj.contains(query, true)) {
        return true;
      }
    }

    return false;
  }

  private AppearanceAnchor findAnchor() {
    for (CanvasObject shape : getObjectsFromBottom()) {
      if (shape instanceof AppearanceAnchor appAnchor) return appAnchor;
    }
    return null;
  }

  private Location findAnchorLocation() {
    final var anchor = findAnchor();
    return (anchor == null) ? Location.create(100, 100) : anchor.getLocation();
  }

  void fireCircuitAppearanceChanged(int affected) {
    final var event = new CircuitAppearanceEvent(circuit, affected);
    for (final var listener : listeners) {
      listener.circuitAppearanceChanged(event);
    }
  }

  public Bounds getAbsoluteBounds() {
    return getBounds(false);
  }

  private Bounds getBounds(boolean relativeToAnchor) {
    Bounds ret = null;
    Location offset = null;
    for (final var obj : getObjectsFromBottom()) {
      if (obj instanceof AppearanceElement appEl) {
        final var loc = appEl.getLocation();
        if (obj instanceof AppearanceAnchor) offset = loc;
        ret = (ret == null) ? Bounds.create(loc) : ret.add(loc);
      } else {
        ret = (ret == null) ? obj.getBounds() : ret.add(obj.getBounds());
      }
    }
    if (ret == null) {
      return Bounds.EMPTY_BOUNDS;
    } else if (relativeToAnchor && offset != null) {
      return ret.translate(-offset.getX(), -offset.getY());
    } else {
      return ret;
    }
  }

  public CircuitPins getCircuitPins() {
    return circuitPins;
  }

  public AttributeOption getCircuitAppearance() {
    return (circuit == null || circuit.getStaticAttributes() == null)
        ? null
        : circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR);
  }

  public Direction getFacing() {
    final var anchor = findAnchor();
    return (anchor == null) ? Direction.EAST : anchor.getFacingDirection();
  }

  public Bounds getOffsetBounds() {
    return getBounds(true);
  }

  public SortedMap<Location, Instance> getPortOffsets(Direction facing) {
    Location anchor = null;
    var defaultFacing = Direction.EAST;
    final var ports = new ArrayList<AppearancePort>();
    for (final var shape : getObjectsFromBottom()) {
      if (shape instanceof AppearancePort appPort) {
        ports.add(appPort);
      } else if (shape instanceof AppearanceAnchor appAnchor) {
        anchor = appAnchor.getLocation();
        defaultFacing = appAnchor.getFacingDirection();
      }
    }

    final var ret = new TreeMap<Location, Instance>();
    for (final var port : ports) {
      var loc = port.getLocation();
      if (anchor != null) {
        loc = loc.translate(-anchor.getX(), -anchor.getY());
      }
      if (facing != defaultFacing) {
        loc = loc.rotate(defaultFacing, facing, 0, 0);
      }
      ret.put(loc, port.getPin());
    }
    return ret;
  }

  public boolean isDefaultAppearance() {
    return isDefault;
  }

  public void paintSubcircuit(InstancePainter painter, Graphics g, Direction facing) {
    final var defaultFacing = getFacing();
    var rotate = 0.0D;
    if (facing != defaultFacing && g instanceof Graphics2D g2d) {
      rotate = defaultFacing.toRadians() - facing.toRadians();
      g2d.rotate(rotate);
    }
    final var offset = findAnchorLocation();
    g.translate(-offset.getX(), -offset.getY());
    CircuitState state = null;
    if (painter.getShowState()) {
      try {
        state = (CircuitState) painter.getData();
      } catch (UnsupportedOperationException ignored) {
        // Do nothing.
      }
    }
    for (final var shape : getObjectsFromBottom()) {
      if (!(shape instanceof AppearanceElement)) {
        final var dup = g.create();
        if (shape instanceof DynamicElement dynEl) {
          dynEl.paintDynamic(dup, state);
          if (shape instanceof DynamicElementWithPoker dynElWithPoker)
            dynElWithPoker.setAnchor(offset);
        } else shape.paint(dup, null);
        dup.dispose();
      }
    }
    g.translate(offset.getX(), offset.getY());
    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
  }

  public boolean isNamedBoxShapedFixedSize() {
    if (circuit == null || circuit.getStaticAttributes() == null) return true;
    if (circuit
        .getStaticAttributes()
        .containsAttribute(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE))
      return circuit.getStaticAttributes().getValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE);
    return true;
  }

  public void recomputeDefaultAppearance() {
    if (isDefault) {
      final var shapes =
          DefaultAppearance.build(
              circuitPins.getPins(),
              getCircuitAppearance(),
              isNamedBoxShapedFixedSize(),
              getName());
      setObjectsForce(shapes);
    }
  }

  void recomputePorts() {
    if (isDefault) {
      recomputeDefaultAppearance();
    } else {
      fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
    }
  }

  public void removeCircuitAppearanceListener(CircuitAppearanceListener l) {
    listeners.remove(l);
  }

  @Override
  public void removeObjects(Collection<? extends CanvasObject> shapes) {
    super.removeObjects(shapes);
    checkToFirePortsChanged(shapes);
  }

  public void removeDynamicElement(InstanceComponent c) {
    final var toRemove = new ArrayList<CanvasObject>();
    for (final var obj : getObjectsFromBottom()) {
      if (obj instanceof DynamicElement el && el.getPath().contains(c)) {
        toRemove.add(obj);
      }
    }
    if (toRemove.isEmpty()) return;
    var oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      removeObjects(toRemove);
      recomputeDefaultAppearance();
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  void replaceAutomatically(List<AppearancePort> removes, List<AppearancePort> adds) {
    // this should be called only when substituting ports via PortManager
    var oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      removeObjects(removes);
      addObjects(getObjectsFromBottom().size() - 1, adds);
      recomputeDefaultAppearance();
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  public void setDefaultAppearance(boolean value) {
    if (isDefault != value) {
      isDefault = value;
      if (value) {
        recomputeDefaultAppearance();
      } else {
        circuit
            .getStaticAttributes()
            .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CUSTOM);
      }
    }
  }

  public void setObjectsForce(List<? extends CanvasObject> shapesBase) {
    // This shouldn't ever be an issue, but just to make doubly sure, we'll
    // check that the anchor and all ports are in their proper places.
    final var shapes = new ArrayList<CanvasObject>(shapesBase);
    final var n = shapes.size();
    var ports = 0;
    for (var i = n - 1; i >= 0; i--) { // count ports, move anchor to end
      final var obj = shapes.get(i);
      if (obj instanceof AppearanceAnchor) {
        if (i != n - 1) {
          shapes.remove(i);
          shapes.add(obj);
        }
      } else if (obj instanceof AppearancePort) {
        ports++;
      }
    }
    for (var i = (n - ports - 1) - 1; i >= 0; i--) { // move ports to top
      final var obj = shapes.get(i);
      if (obj instanceof AppearancePort) {
        shapes.remove(i);
        shapes.add(n - ports - 1, obj);
        i--;
      }
    }

    try {
      suppressRecompute = true;
      super.removeObjects(new ArrayList<>(getObjectsFromBottom()));
      super.addObjects(0, shapes);
    } finally {
      suppressRecompute = false;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  @Override
  public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    super.translateObjects(shapes, dx, dy);
    checkToFirePortsChanged(shapes);
  }
}
