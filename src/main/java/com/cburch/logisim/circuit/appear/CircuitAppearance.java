/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
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
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CircuitAppearance extends Drawing implements AttributeListener {
  public static final int PIN_LENGTH = 10;
  
  private class MyListener implements CanvasModelListener {
    @Override
    public void modelChanged(CanvasModelEvent event) {
      if (!suppressRecompute) {
        fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
      }
    }
  }

  private final Circuit circuit;
  private final EventSourceWeakSupport<CircuitAppearanceListener> listeners;
  private final PortManager portManager;
  private final CircuitPins circuitPins;
  private final MyListener myListener;
  private final ArrayList<CanvasObject> defaultCanvasObjects;
  private boolean suppressRecompute;
  private List<CanvasObject> defaultCustomAppearance;

  public CircuitAppearance(Circuit circuit) {
    this.circuit = circuit;
    listeners = new EventSourceWeakSupport<>();
    portManager = new PortManager(this);
    circuitPins = new CircuitPins(portManager);
    myListener = new MyListener();
    suppressRecompute = false;
    addCanvasModelListener(myListener);
    if (circuit != null) circuit.getStaticAttributes().addAttributeListener(this);
    defaultCanvasObjects = new ArrayList<CanvasObject>();
    recomputeDefaultAppearance();
    defaultCustomAppearance = DefaultCustomAppearance.build(circuitPins.getPins()); 
    setObjectsForce(defaultCustomAppearance, false);
  }

  public boolean hasCustomAppearance() {
    final var currentCustom = new ArrayList<CanvasObject>(getCustomObjectsFromBottom());
    final var defaultCustom = new ArrayList<CanvasObject>(defaultCustomAppearance);
    var shapeIterator = currentCustom.iterator();
    while (shapeIterator.hasNext()) {
      final var shape = shapeIterator.next(); 
      if (shape instanceof AppearancePort || shape instanceof AppearanceAnchor)
        shapeIterator.remove();
    }
    shapeIterator = defaultCustom.iterator();
    while (shapeIterator.hasNext()) {
      final var shape = shapeIterator.next(); 
      if (shape instanceof AppearancePort || shape instanceof AppearanceAnchor)
        shapeIterator.remove();
    }
    if (currentCustom.size() != defaultCustom.size()) return true;
    shapeIterator = currentCustom.iterator();
    while (shapeIterator.hasNext()) {
      final var currentShape = shapeIterator.next();
      var deleteIt = false;
      final var shapeDefaultIterator = defaultCustom.iterator();
      while (shapeDefaultIterator.hasNext()) {
        final var defaultShape = shapeDefaultIterator.next();
        final var matches = currentShape.matches(defaultShape);
        deleteIt |= matches;
        if (matches) shapeDefaultIterator.remove();
      }
      if (deleteIt) shapeIterator.remove();
    }
    return !currentCustom.isEmpty();
  }

  public void resetDefaultCustomAppearance() {
    super.removeObjects(this.getCustomObjectsFromBottom());
    defaultCustomAppearance = DefaultCustomAppearance.build(circuitPins.getPins()); 
    setObjectsForce(defaultCustomAppearance, false);
  }
  
  public void loadDefaultLogisimAppearance() {
    super.removeObjects(this.getCustomObjectsFromBottom());
    defaultCustomAppearance.clear();
    setObjectsForce(DefaultEvolutionAppearance.build(circuitPins.getPins(), circuit.getName(), true), false);
  }

  public String getName() {
    return (circuit == null || circuit.getStaticAttributes() == null)
        ? null
        : circuit.getStaticAttributes().getValue(CircuitAttributes.NAME_ATTR);
  }

  public void addCircuitAppearanceListener(CircuitAppearanceListener l) {
    listeners.add(l);
  }
  
  public Drawing getCustomAppearanceDrawing() {
    return super.getCopy();
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
    return (circuit == null) || !circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR).equals(CircuitAttributes.APPEAR_CUSTOM);
  }
  
  public List<CanvasObject> getCustomObjectsFromBottom() {
    return super.getObjectsFromBottom();
  }

  @Override
  public List<CanvasObject> getObjectsFromBottom() {
    return isDefaultAppearance() ? Collections.unmodifiableList(defaultCanvasObjects) : super.getObjectsFromBottom();
  }

  @Override
  public List<CanvasObject> getObjectsFromTop() {
    final var ret = new ArrayList<CanvasObject>(getObjectsFromBottom());
    Collections.reverse(ret);
    return ret;
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
    final var staticAttrs = circuit.getStaticAttributes(); 
    return staticAttrs.containsAttribute(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE) 
        ? staticAttrs.getValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE) 
        : true;
  }

  public void recomputeDefaultAppearance() {
    final var shapes = DefaultAppearance.build(circuitPins.getPins(), getCircuitAppearance(), 
        isNamedBoxShapedFixedSize(), getName());
    setObjectsForce(shapes, true);
  }

  void recomputePorts() {
    if (isDefaultAppearance()) {
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
    for (final var obj : super.getObjectsFromBottom()) {
      if (obj instanceof DynamicElement el && el.getPath().contains(c)) {
        toRemove.add(obj);
      }
    }
    if (toRemove.isEmpty()) return;
    var oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      removeObjects(toRemove);
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  void replaceAutomatically(List<AppearancePort> removes, List<AppearancePort> adds) {
    // this should be called only when substituting ports for the custom appearance
    var oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      final var hasCustom = hasCustomAppearance(); 
      if (hasCustom) {
        defaultCustomAppearance = DefaultCustomAppearance.build(circuitPins.getPins()); 
        removeObjects(removes);
        addObjects(getCustomObjectsFromBottom().size() - 1, adds);
      } else {
        super.removeObjects(getCustomObjectsFromBottom());
        defaultCustomAppearance = DefaultCustomAppearance.build(circuitPins.getPins()); 
        setObjectsForce(defaultCustomAppearance, false);
      }
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  public void setObjectsForce(List<? extends CanvasObject> shapesBase) {
    setObjectsForce(shapesBase, false);
  }

  public void setObjectsForce(List<? extends CanvasObject> shapesBase, boolean isDefault) {
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
      if (isDefault) {
        defaultCanvasObjects.clear();
        defaultCanvasObjects.addAll(shapes);
      } else {
        super.removeObjects(new ArrayList<>(getCustomObjectsFromBottom()));
        super.addObjects(0, shapes);
      }
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

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    if (e.getAttribute() == CircuitAttributes.APPEARANCE_ATTR) {
      if (e.getValue() == CircuitAttributes.APPEAR_CLASSIC
          || e.getValue() == CircuitAttributes.APPEAR_FPGA
          || e.getValue() == CircuitAttributes.APPEAR_EVOLUTION) {
        recomputeDefaultAppearance();
      }
    }  
  }
}
