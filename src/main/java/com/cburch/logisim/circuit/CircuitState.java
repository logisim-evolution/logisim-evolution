/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.TelnetServer;
import com.cburch.logisim.std.io.extra.Buzzer;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamState;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CircuitState holds the simulation state of a Circuit (or Subcircuit), i.e.
 * the values being carried along all wires and buses, along with the
 * InstanceData for all components embedded in the circuit. Most of the
 * dynamically-computed data is actually in CircuitWires. In here there is
 * mostly just a few pointers to other data structures and the dirty lists
 * (lists of locations or components that need to be recomputed).
 *
 * Note: Each CircuitState belongs to (at most) one Propagator. Some of the
 * members in here more properly belong to Propagator (or, vice versa, some of
 * the functionality in Propagator could equally well be in here.
 */
public class CircuitState implements InstanceData {

  private class MyCircuitListener implements CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();

      if (action == CircuitEvent.ACTION_ADD) {
        /* Component was added */
        // Nothing to do: CircuitWires.Connectivity will be voided, causing
        // everything to be marked dirty.
      } else if (action == CircuitEvent.ACTION_REMOVE) {
        /* Component was removed */
        final var comp = (Component) event.getData();
        if (comp == temporaryClock) temporaryClock = null;
        if (comp.getFactory() instanceof Clock) {
          knownClocks = false; // just in case, will be recomputed by simulator
        }
        if (comp.getFactory() instanceof SubcircuitFactory) {
          knownClocks = false; // just in case, will be recomputed by simulator
          // disconnect from tree
          final var substate = (CircuitState) getData(comp);
          if (substate != null && substate.parentComp == comp) {
            synchronized (dirtyLock) {
              substates.remove(substate);
              substatesDirty = true;
            }
            substate.parentState = null;
            substate.parentComp = null;
            substate.reset();
          }
        } else if (getData(comp) instanceof ComponentDataGuiProvider guiProvider) {
          guiProvider.destroy();
        }
        if (comp instanceof Wire w) {
          // Nothing to do: CircuitWires.Connectivity will be voided, causing
          // everything to be marked dirty.
        } else {
          // Nothing else to do: CircuitWires.Connectivity will be voided, causing
          // everything to be marked dirty.
          synchronized (dirtyLock) {
            while (dirtyComponents.remove(comp)) {
            }
          }
        }
      } else if (action == CircuitEvent.ACTION_CLEAR) {
        /* Whole circuit was cleared */
        temporaryClock = null;
        knownClocks = false;
        wireData = null;
        for (final var comp : componentData.keySet()) {
          if (componentData.get(comp) instanceof ComponentDataGuiProvider dataGuiProvider) {
            dataGuiProvider.destroy();
          } else if (componentData.get(comp) instanceof CircuitState circuitState) {
            circuitState.reset();
          }
        }
        componentData.clear();
        synchronized (valuesLock) {
          slowpathValues.clear(); // slow path
          clearFastpathGrid(); // fast path
        }
        synchronized (dirtyLock) {
          dirtyComponents.clear();
          dirtyPoints.clear();
          substates.clear();
          substatesWorking = new CircuitState[0];
          substatesDirty = true;
        }
      } else if (action == CircuitEvent.ACTION_INVALIDATE) {
        /* Component ends changed */
        final var comp = (Component) event.getData();
        markComponentAsDirty(comp);
        // If simulator is in single step mode, we want to hilight the
        // invalidated components (which are likely Pins, Buttons, or other
        // inputs), so pass this component to the simulator for display.
        proj.getSimulator().addPendingInput(CircuitState.this, comp);
      } else if (action == CircuitEvent.TRANSACTION_DONE) {
        final var map = event.getResult().getReplacementMap(circuit);
        if (map == null) return;
        for (final var comp : map.getRemovals()) {
          final var compState = componentData.remove(comp);
          if (compState != null) continue;
          Class<?> compFactory = comp.getFactory().getClass();
          var found = false;
          for (final var repl : map.getReplacementsFor(comp)) {
            if (repl.getFactory().getClass() == compFactory) {
              found = true;
              replaceData(repl, compState);
              break;
            }
          }
          if (!found && compState instanceof RamState state) Ram.closeHexFrame(state);
          if (!found && compState instanceof CircuitState sub) {
            sub.parentState = null;
            synchronized (dirtyLock) {
              substates.remove(sub);
              substatesDirty = true;
            }
          }
        }
      }
    }
  }

  private final MyCircuitListener myCircuitListener = new MyCircuitListener();

  /** Inherited from base of tree of CircuitStates */
  private Propagator base = null;

  /** Project containing this circuit */
  private final Project proj;

  /** Circuit being simulated */
  private final Circuit circuit;

  /** Parent in tree of CircuitStates */
  private CircuitState parentState = null;

  /** subcircuit component containing this */
  private Component parentComp = null;
  // state

  private CircuitWires.State wireData = null;
  private final HashMap<Component, Object> componentData = new HashMap<>();

  private static final int FASTPATH_GRID_WIDTH = 200;
  private static final int FASTPATH_GRID_HEIGHT = 200;

  // slowpathValues and fastpathValues store values resulting from propagation
  // *within* this circuit, i.e. the outputs of componnents in this circuit
  // together with the values carried on wires and buses in this circuit. When
  // components embedded in this circuit are called upon to re-calculate /
  // propagate, the components will call getValue() to pick out values from
  // these data structures. These are the values you would see if you stick a
  // probe at some location on the circuit sheet. They are protected by valuesLock.
  /** values propagated in this circuit. Protected by valuesLock */
  private Map<Location, Value> slowpathValues = new HashMap<>();
  /** values propagated in this circuit. Protected by valuesLock */
  private Value[][] fastpathValues = new Value[FASTPATH_GRID_HEIGHT][FASTPATH_GRID_WIDTH];
  /** Protects slowPathValues and fastPathValues */
  private final Object valuesLock = new Object();

  // dirtyComponents, dirtyPoints, and substates are components being marked as dirty.
  // They will later be shifted to the working sets to be processed.
  // They are protected by dirtyLock
  /** Protected by dirtyLock */
  private ArrayList<Component> dirtyComponents = new ArrayList<>();
  /** Protected by dirtyLock */
  private ArrayList<Propagator.SimulatorEvent> dirtyPoints = new ArrayList<>();
  /** Protected by dirtyLock */
  private HashSet<CircuitState> substates = new HashSet<>();
  /** Protects dirtyComponents, dirtyPoints, and substates */
  private final Object dirtyLock = new Object();

  // dirtyComponentsWorking, dirtyPointsWorking, and substatesWorking are those elements
  // of this circuit that are being processed.
  /** Components being processed. */
  ArrayList<Component> dirtyComponentsWorking = new ArrayList<>();
  /** Points being processed */
  private ArrayList<Propagator.SimulatorEvent> dirtyPointsWorking = new ArrayList<>();
  /** Substates being processed */
  private CircuitState[] substatesWorking = new CircuitState[0];
  private boolean substatesDirty = true;


  private static int lastId = 0;
  private final int id = lastId++;

  public CircuitState(Project proj, Circuit circuit, Propagator prop, Thread thread) {
    this.proj = proj;
    this.circuit = circuit;
    this.base = prop != null ? prop : new Propagator(this, thread != null ? thread : proj.getSimulator().simThread);
    circuit.addCircuitListener(myCircuitListener);
    markAllComponentsDirty();
  }

  public CircuitState(Project proj, Circuit circuit, Propagator prop) {
    this(proj, circuit, prop, null);
  }

  @Override
  public CircuitState clone() {
    return cloneAsNewRootState();
  }

  public static CircuitState createRootState(Project proj, Circuit circuit) {
    return new CircuitState(proj, circuit, null /* make new Propagator */, null);
  }

  public static CircuitState createRootState(Project proj, Circuit circuit, Thread thread) {
    return new CircuitState(proj, circuit, null /* make new Propagator */, thread);
  }

  public CircuitState cloneAsNewRootState(Thread thread) {
    final var ret = new CircuitState(proj, circuit, null, thread);
    ret.copyFrom(this);
    ret.parentComp = null;
    ret.parentState = null;
    return ret;
  }

  public CircuitState cloneAsNewRootState() {
    return cloneAsNewRootState(null);
  }

  private void copyFrom(CircuitState src) {
    this.parentComp = src.parentComp;
    this.parentState = src.parentState;
    final var substateData = new HashMap<CircuitState, CircuitState>();
    this.substates = new HashSet<CircuitState>();
    synchronized (src.dirtyLock) {
      // note: we don't bother with our this.dirtyLock here: it isn't needed
      // (b/c no other threads have a reference to this yet), and to avoid the
      // possibility of deadlock (though that shouldn't happen either since no
      // other threads have references to this yet).
      for (final var oldSub : src.substates) {
        final var newSub = new CircuitState(src.proj, oldSub.circuit, this.base);
        newSub.copyFrom(oldSub);
        newSub.parentState = this;
        this.substates.add(newSub);
        this.substatesDirty = true;
        substateData.put(oldSub, newSub);
      }
    }
    for (final var key : src.componentData.keySet()) {
      final var oldValue = src.componentData.get(key);
      if (oldValue instanceof CircuitState) {
        final var newValue = substateData.get(oldValue);
        if (newValue != null) {
          this.componentData.put(key, newValue);
        } else {
          this.componentData.remove(key);
        }
      } else {
        final var newValue = (oldValue instanceof ComponentState state) ? state.clone() : oldValue;
        this.componentData.put(key, newValue);
      }
    }
    // note: we don't bother with our this.valuesLock here: it isn't needed
    // (b/c no other threads have a reference to this yet), and to avoid the
    // possibility of deadlock (though that shouldn't happen either since no
    // other threads have references to this yet).
    this.slowpathValues.clear(); // slow path
    synchronized (src.valuesLock) {
      this.slowpathValues.putAll(src.slowpathValues); // slow path
      for (var y = 0; y < FASTPATH_GRID_HEIGHT; y++) { // fast path
        System.arraycopy(src.fastpathValues[y], 0, this.fastpathValues[y], 0, FASTPATH_GRID_WIDTH);
      }
    }
    synchronized (src.dirtyLock) {
      // note: we don't bother with our this.dirtyLock here: it isn't needed
      // (b/c no other threads have a reference to this yet), and to avoid the
      // possibility of deadlock (though that shouldn't happen either since no
      // other threads have references to this yet).
      this.dirtyComponents.addAll(src.dirtyComponents);
      this.dirtyPoints.addAll(src.dirtyPoints);
    }
    if (src.wireData != null) {
      this.wireData = circuit.wires.newState(this); // all buses will be marked as dirty
    }
  }

  public void drawOscillatingPoints(ComponentDrawContext context) {
    base.drawOscillatingPoints(context);
  }

  //
  // public methods
  //
  public Circuit getCircuit() {
    return circuit;
  }

  public Object getData(Component comp) {
    return componentData.get(comp);
  }

  private final InstanceStateImpl reusableInstanceState = new InstanceStateImpl(this, null);

  public InstanceState getInstanceState(Component comp) {
    final var factory = comp.getFactory();
    if (factory instanceof InstanceFactory) {
      if (comp != ((InstanceComponent) comp).getInstance().getComponent()) {
        throw new IllegalStateException("instanceComponent.getInstance().getComponent() is wrong");
      }
      return new InstanceStateImpl(this, comp);
    }
    throw new RuntimeException("getInstanceState requires instance component");
  }

  public InstanceState getInstanceState(Instance instance) {
    return new InstanceStateImpl(this, instance.getComponent());
  }

  /** This method returns a reused object. It should only be called using the propagate thread
   *  and with care that there is no conflict with other uses. */
  public InstanceState getReusableInstanceState(Component comp) {
    final var factory = comp.getFactory();
    if (factory instanceof InstanceFactory) {
      if (comp != ((InstanceComponent) comp).getInstance().getComponent()) {
        throw new IllegalStateException("instanceComponent.getInstance().getComponent() is wrong");
      }
      reusableInstanceState.repurpose(this, comp);
      return reusableInstanceState;
    }
    throw new RuntimeException("getInstanceState() requires instance component");
  }

  /** This method returns a reused object. It should only be called using the propagate thread
   *  and with care that there is no conflict with other uses. */
  public InstanceState getReusableInstanceState(Instance instance) {
    reusableInstanceState.repurpose(this, instance.getComponent());
    return reusableInstanceState;
  }

  public CircuitState getParentState() {
    return parentState;
  }

  public Project getProject() {
    return proj;
  }

  public Propagator getPropagator() {
    return base;
  }

  Component getSubcircuit() {
    return parentComp;
  }

  public Set<CircuitState> getSubstates() { // returns Set of CircuitStates
    return substates;
  }

  public Value getValue(Location p) {
    Value value = null;
    if (p.x >= 0 && p.y >= 0
        && p.x % 10 == 0 && p.y % 10 == 0
        && p.x < FASTPATH_GRID_WIDTH * 10
        && p.y < FASTPATH_GRID_HEIGHT * 10) {
      // fast path
      final var x = p.x / 10;
      final var y = p.y / 10;
      synchronized (valuesLock) {
        value = fastpathValues[y][x];
        if (value == null) {
          value = CircuitWires.getBusValue(this, p);
        }
      }
    } else {
      // slow path
      synchronized (valuesLock) {
        value = slowpathValues.get(p);
        if (value == null) {
          value = CircuitWires.getBusValue(this, p);
        }
      }
    }
    return value != null ? value : Value.createUnknown(circuit.getWidth(p));
  }

  CircuitWires.State getWireData() {
    return wireData;
  }

  public boolean isSubstate() {
    return parentState != null;
  }

  private void markAllComponentsDirty() {
    synchronized (dirtyLock) {
      dirtyComponents.addAll(circuit.getNonWires());
    }
  }

  public void markComponentAsDirty(Component comp) {
    synchronized (dirtyLock) {
      dirtyComponents.add(comp);
    }
  }

  public void markComponentsDirty(Collection<Component> comps) {
    synchronized (dirtyLock) {
      dirtyComponents.addAll(comps);
    }
  }

  void markPointAsDirty(Propagator.SimulatorEvent ev) {
    synchronized (dirtyLock) {
      dirtyPoints.add(ev);
    }
  }

  void processDirtyComponents() {
    if (!dirtyComponentsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyComponentsWorking not empty");
    }
    synchronized (dirtyLock) {
      final var other = dirtyComponents;
      dirtyComponents = dirtyComponentsWorking; // dirtyComponents is now empty
      dirtyComponentsWorking = other; // working set is now ready to process
      if (substatesDirty) {
        substatesDirty = false;
        substatesWorking = substates.toArray(substatesWorking);
      }
    }
    try { // comp.propagate() can fail if external (or std) library is buggy
      for (final var comp : dirtyComponentsWorking) {
        comp.propagate(this);
        // pin values also get propagated to parent state
        if (comp.getFactory() instanceof Pin && parentState != null) {
          parentComp.propagate(parentState);
        }
      }
    } finally {
      dirtyComponentsWorking.clear();
    }
    for (final var substate : substatesWorking) {
      if (substate == null) break;
      substate.processDirtyComponents();
    }
  }

  void processDirtyPoints() {
    if (!dirtyPointsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyPointsWorking not empty");
    }
    synchronized (dirtyLock) {
      final var other = dirtyPoints;
      dirtyPoints = dirtyPointsWorking; // dirtyPoints is now empty
      dirtyPointsWorking = other; // working set is now ready to process
      if (substatesDirty) {
        substatesDirty = false;
        substatesWorking = substates.toArray(substatesWorking);
      }
    }
    // Note: When a new wire map is created (because wires or splitters have
    // changed, for example), we need to mark all the splitter locations as
    // dirty. This used to be handled here by detecting when the map was voided,
    // and explicitly marking all the splitter locations as dirty. But We can't
    // reliably touch circuit.wires.points.getAllLocations(), because we are
    // on the simulator thread here, and the UI/AWT thread owns that data
    // structure. So the hack below just tried a few times hoping to not get a
    // run-time exception. Instead, we now put the splitter location list in
    // the wire map itself when it is created (which is done by CircuitWires
    // carefully in a thread-safe way).
    circuit.wires.propagate(this, dirtyPointsWorking);
    dirtyPointsWorking.clear();

    for (final var substate : substatesWorking) {
      if (substate == null) break;
      substate.processDirtyPoints();
    }
  }

  public void reset() {
    temporaryClock = null;
    wireData = null;
    for (final var comp : componentData.keySet()) {
      if (comp.getFactory() instanceof Ram ram) {
        final var remove = ram.reset(this, Instance.getInstanceFor(comp));
        if (remove) componentData.put(comp, null);
      } else if (comp.getFactory() instanceof Buzzer) {
        Buzzer.stopBuzzerSound(comp, this);
      } else if (!(comp.getFactory() instanceof SubcircuitFactory)) {
        if (componentData.get(comp) instanceof ComponentDataGuiProvider guiProvider) guiProvider.destroy();
        if (componentData.get(comp) instanceof TelnetServer telnetServer) telnetServer.deleteAll();
        componentData.put(comp, null);
      }
    }
    synchronized (valuesLock) {
      slowpathValues.clear(); // slow path
      clearFastpathGrid(); // fast path
    }
    synchronized (dirtyLock) {
      dirtyComponents.clear();
      dirtyPoints.clear();
      for (final var sub : substates) {
        sub.reset();
      }
    }
    markAllComponentsDirty();
  }

  public CircuitState createCircuitSubstateFor(Component comp, Circuit circ) {
    final var oldState = (CircuitState) componentData.get(comp);
    if (oldState != null && oldState.parentComp == comp) {
      // fixme: Does this ever happen?
      System.out.println("fixme: removed stale circuitstate... should never happen");
      synchronized (dirtyLock) {
        substates.remove(oldState);
        substatesDirty = true;
      }
      oldState.parentState = null;
      oldState.parentComp = null;
    }
    final var newState = new CircuitState(proj, circ, base);
    synchronized (dirtyLock) {
      substates.add(newState);
      substatesDirty = true;
    }
    newState.parentState = this;
    newState.parentComp = comp;
    componentData.put(comp, newState);
    return newState;
  }

  private void replaceData(Component comp, Object data) {
    // This happens when subcirc is copy/paste/moved, which causes a new
    // component to be created, and we want to transfer the now-defunct
    // component's state over to the newly-created component.
    // If comp is a subcircuit, the data will be a CircuitState.
    // Otherwise data might be a RamState, or some other built-in component state.
    if (data instanceof CircuitState) {
      final var sub = (CircuitState) data;
      // data was already removed from componentData[orig].
      // need to register it now under componentdata[comp], done below.
      // also need to set parentcomp
      // but don't need to add to substates, b/c it should already be there
      sub.parentComp = comp;
      final var old = (CircuitState) componentData.put(comp, data);
      synchronized (dirtyLock) {
        if (old != null) {
          substates.remove(old);
          old.parentState = null;
        }
        sub.parentState = this;
        substates.add(sub);
        substatesDirty = true;
        dirtyComponents.add(comp);
      }
    } else {
      componentData.put(comp, data);
    }
  }

  public void setData(Component comp, Object data) {
    if (data instanceof CircuitState) {
      // fixme: should never happen?
      System.out.println("fixme: setData with circuitstate... should never happen");
      Thread.dumpStack();
      ((CircuitState) data).parentComp = comp;
    }
    componentData.put(comp, data);
  }

  public void setValue(Location pt, Value val, Component cause, int delay) {
    base.setValue(this, pt, val, cause, delay);
  }

  /** precondition: valuesLock held */
  private void clearFastpathGrid() {
    for (var y = 0; y < FASTPATH_GRID_HEIGHT; y++) {
      for (var x = 0; x < FASTPATH_GRID_WIDTH; x++) {
        fastpathValues[y][x] = null;
      }
    }
  }

  /** for CircuitWires - to set value at point */
  void setValueByWire(Value v, Location[] points, CircuitWires.BusConnection[] connections) {
    for (final var p : points) {
      if (p.x >= 0 && p.y >= 0
          && p.x % 10 == 0 && p.y % 10 == 0
          && p.x < FASTPATH_GRID_WIDTH * 10
          && p.y < FASTPATH_GRID_HEIGHT * 10) {
        synchronized (valuesLock) {
          fastpath(p, v);
        }
      } else {
        synchronized (valuesLock) {
          slowpath(p, v);
        }
      }
      base.locationTouched(this, p);
    }
    for (final var bc : connections) {
      if (bc.isSink || (bc.isBidirectional && !Value.equal(v, bc.drivenValue))) {
        markComponentAsDirty(bc.component);
      }
    }
  }

  /** for CircuitWires - to set value at point */
  void clearValuesByWire() {
    synchronized (valuesLock) {
      slowpathValues.clear(); // slow path
      clearFastpathGrid(); // fast path
    }
  }

  /** precondition: valuesLock held */
  private boolean fastpath(Location p, Value v) {
    final var x = p.x / 10;
    final var y = p.y / 10;
    if (v == Value.NIL) {
      if (fastpathValues[y][x] != null) {
        fastpathValues[y][x] = null;
        return true;
      } else {
        return false;
      }
    } else {
      if (!v.equals(fastpathValues[y][x])) {
        fastpathValues[y][x] = v;
        return true;
      } else {
        return false;
      }
    }
  }

  /** precondition: valuesLock held */
  private boolean slowpath(Location p, Value v) {
    if (v == Value.NIL) {
      final var old = slowpathValues.remove(p);
      return (old != null && old != Value.NIL);
    } else {
      final var old = slowpathValues.put(p, v);
      return !v.equals(old);
    }
  }

  void setWireData(CircuitWires.State data) {
    wireData = data;
  }

  private void markDirtyComponents(Location p, Component[] affected) {
    for (final var comp : affected) {
      markComponentAsDirty(comp);
    }
    if (affected.length > 0) {
      base.locationTouched(this, p);
    }
  }

  boolean toggleClocks(int ticks) {
    var hasClocks = false;
    if (temporaryClock != null) {
      hasClocks |= temporaryClockValidateOrTick(ticks);
    }

    for (Component clock : circuit.getClocks()) {
      hasClocks = true;
      final var dirty = Clock.tick(this, ticks, clock);
      if (dirty) {
        markComponentAsDirty(clock);
        // If simulator is in single step mode, we want to hilight the
        // invalidated components (which are likely Pins, Buttons, or other
        // inputs), so pass this component to the simulator for display.
        proj.getSimulator().addPendingInput(this, clock);
      }
    }

    synchronized (dirtyLock) {
      if (substatesDirty) {
        substatesDirty = false;
        substatesWorking = substates.toArray(substatesWorking);
      }
    }
    for (final var substate : substatesWorking) {
      if (substate == null) break;
      hasClocks |= substate.toggleClocks(ticks);
    }
    return hasClocks;
  }

  private boolean temporaryClockValidateOrTick(int ticks) {
    // temporaryClock.getFactory() will be Pin, normally a 1 bit input
    try {
      final var pin = (Pin) temporaryClock.getFactory();
      final var instance = Instance.getInstanceFor(temporaryClock);
      if (instance == null || !pin.isInputPin(instance) || pin.getWidth(instance).getWidth() != 1) {
        temporaryClock = null;
        return false;
      }
      if (ticks >= 0) {
        final var state = getReusableInstanceState(instance); // OK as we are in the clock update
        final var vOld = pin.getValue(state);
        final var vNew = ticks % 2 == 0 ? Value.FALSE : Value.TRUE;
        if (!vNew.equals(vOld)) {
          pin.driveInputPin(state, vNew);
          markComponentAsDirty(temporaryClock);
          // If simulator is in single step mode, we want to hilight the
          // invalidated components (which are likely Pins, Buttons, or other
          // inputs), so pass this component to the simulator for display.
          proj.getSimulator().addPendingInput(this, temporaryClock);
        }
      }
      return true;

    } catch (ClassCastException e) {
      temporaryClock = null;
      return false;
    }
  }

  private boolean knownClocks;
  private Component temporaryClock;

  public boolean hasKnownClocks() {
    return knownClocks || temporaryClock != null;
  }

  public void markKnownClocks() {
    knownClocks = true;
  }

  public boolean setTemporaryClock(Component clk) {
    temporaryClock = clk;
    return clk == null || temporaryClockValidateOrTick(-1);
  }

  public Component getTemporaryClock() {
    return temporaryClock;
  }

  @Override
  public String toString() {
    return "State" + id + "[" + circuit.getName() + "]";
  }
}
