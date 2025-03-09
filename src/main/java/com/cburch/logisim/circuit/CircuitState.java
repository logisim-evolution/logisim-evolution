/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

// import com.cburch.logisim.circuit.Propagator.DrivenValue;
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

// CircuitState holds the simulation state of a Circuit (or Subcircuit), i.e.
// the values being carried along all wires and buses, along with the
// InstanceData for all components embedded in the circuit. Most of the
// dynamically-computed data is actually in CircuitWires. In here there is
// mostly just a few pointers to other data structures and the dirty lists
// (lists of locations or components that need to be recomputed).
//
// Note: Each CircuitState belongs to (at most) one Propagator. Some of the
// members in here more properly belong to Propagator (or, vice versa, some of
// the functionality in Propagator could equally well be in here.
public class CircuitState implements InstanceData {

  private class MyCircuitListener implements CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();

      if (action == CircuitEvent.ACTION_ADD) {
        /* Component was added */
        // Nothing to do: CircuitWires.BundleMap will be voided, causing
        // everything to be marked dirty.
        Component comp = (Component) event.getData();
        // System.out.println("added comp " + comp);
        // if (comp instanceof Wire) {
        //   Wire w = (Wire) comp;
        //   markPointAsDirty(w.getEnd0(), null);
        //   markPointAsDirty(w.getEnd1(), null);
        // } else {
        //   markComponentAsDirty(comp);
        // }
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
          // Nothing to do: CircuitWires.BundleMap will be voided, causing
          // everything to be marked dirty.
          // markPointAsDirty(w.getEnd0(), null);
          // markPointAsDirty(w.getEnd1(), null);
          // System.out.println("removed wire " + comp);
        } else {
          // Nothing else to do: CircuitWires.BundleMap will be voided, causing
          // everything to be marked dirty.
          // Propagator.checkComponentEnds(CircuitState.this, comp);
          // System.out.println("removed comp " + comp);
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
          if (componentData.get(comp) instanceof ComponentDataGuiProvider dataGuiProvider)
            dataGuiProvider.destroy();
          else if (componentData.get(comp) instanceof CircuitState circuitState) {
            circuitState.reset();
          }
        }
        componentData.clear();
        synchronized (valuesLock) {
          slowpath_values.clear(); // slow path
          clearFastpathGrid(); // fast path
        }
        synchronized (dirtyLock) {
          dirtyComponents.clear();
          dirtyPoints.clear();
          // dirtyPointVals.clear();
          substates.clear();
          substatesWorking = new CircuitState[0];
          substatesDirty = true;
        }
        // slowpath_drivers.clear();
      } else if (action == CircuitEvent.ACTION_INVALIDATE) {
        final var comp = (Component) event.getData();
        markComponentAsDirty(comp);
        // If simulator is in single step mode, we want to hilight the
        // invalidated components (which are likely Pins, Buttons, or other
        // inputs), so pass this component to the simulator for display.
        proj.getSimulator().addPendingInput(CircuitState.this, comp);
        // TODO detemine if this should really be missing if (base != null) base.checkComponentEnds(CircuitState.this, comp);
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
              setData(repl, compState);
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
  private Propagator base = null; // inherited from base of tree of CircuitStates
  private final Project proj; // project containing this circuit
  private final Circuit circuit; // circuit being simulated

  private CircuitState parentState = null; // parent in tree of CircuitStates
  private Component parentComp = null; // subcircuit component containing this
  // state

  private CircuitWires.State wireData = null;
  private final HashMap<Component, Object> componentData = new HashMap<>();

  private static final int FASTPATH_GRID_WIDTH = 200;
  private static final int FASTPATH_GRID_HEIGHT = 200;

  // slowpath_values and fastpath_values store values resulting from propagation
  // *within* this circuit, i.e. the outputs of componnents in this circuit
  // together with the values carried on wires and buses in this circuit. When
  // components embedded in this circuit are called upon to re-calculate /
  // propagate, the components will call getValue() to pick out values from
  // these data structures. These are the values you would see if you stick a
  // probe at some location on the circuit sheet.
  Map<Location, Value> slowpath_values = new HashMap<>(); // protected by valuesLock
  Value[][] fastpath_values = new Value[FASTPATH_GRID_HEIGHT][FASTPATH_GRID_WIDTH]; // protected by valuesLock

  // slowpath_drivers and fastpass_drivers store {component,value} pairs for each
  // component that is currently emitting a value *into* this circuit, i.e.
  // the values that sources/drivers are putting out and that will ultimately
  // get propagated along wires and busses to other points in the circuit. These
  // are essentially the values you would see at the outputs of components if
  // you somehow froze all their inputs then removed all wires, splitters,
  // tunnels, and other connectivity within the circuit so that each component's
  // outputs could be observed in isolation.
  // HashMap<Location, DrivenValue> slowpath_drivers = new HashMap<>(); // used by Propagator, protected by valuesLock
  // DrivenValue[][] fastpath_drivers = new DrivenValue[FASTPATH_GRID_HEIGHT][FASTPATH_GRID_WIDTH]; // used by Propagator, protected by valuesLock

  Object valuesLock = new Object();

  // HashSet<Propagator.ComponentPoint> visited = new HashSet<>(); // used by Propagator
  // int visitedNonce; // used by Propagator;
  // The visited member holds the set of every [component,loc] pair (where the
  // component is among those in this circuit) that has been visited during the
  // current iteration of Propagator.stepInternal().

  //private CopyOnWriteArraySet<Component> dirtyComponents = new CopyOnWriteArraySet<>();
  //private HashSet<Component> dirtyComponents = new HashSet<>(); // protected by dirtyLock
  private ArrayList<Component> dirtyComponents = new ArrayList<>(); // protected by dirtyLock
  //private final CopyOnWriteArraySet<Location> dirtyPoints = new CopyOnWriteArraySet<>();
  //private final HashSet<Location> dirtyPoints = new HashSet<>();
  // private ArrayList<Location> dirtyPoints = new ArrayList<>(); // protected by dirtyLock
  // private ArrayList<Value> dirtyPointVals = new ArrayList<>(); // protected by dirtyLock
  private ArrayList<Propagator.SimulatorEvent> dirtyPoints = new ArrayList<>(); // protected by dirtyLock
  private HashSet<CircuitState> substates = new HashSet<>(); // protected by dirtyLock
  private Object dirtyLock = new Object();

  private static int lastId = 0;
  private final int id = lastId++;

  public CircuitState(Project proj, Circuit circuit, Propagator prop) {
    this.proj = proj;
    this.circuit = circuit;
    this.base = prop != null ? prop : new Propagator(this);
    circuit.addCircuitListener(myCircuitListener);
    markAllComponentsDirty();
  }

  @Override
  public CircuitState clone() {
    return cloneAsNewRootState();
  }

  public static CircuitState createRootState(Project proj, Circuit circuit) {
    return new CircuitState(proj, circuit, null /* make new Propagator */);
  }

  public CircuitState cloneAsNewRootState() {
    final var ret = new CircuitState(proj, circuit, null);
    ret.copyFrom(this);
    ret.parentComp = null;
    ret.parentState = null;
    return ret;
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
        final var newSub = CircuitState.createRootState(src.proj, oldSub.circuit);
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
        if (newValue != null) this.componentData.put(key, newValue);
        else this.componentData.remove(key);
      } else {
        final var newValue = (oldValue instanceof ComponentState state) ? state.clone() : oldValue;
        this.componentData.put(key, newValue);
      }
    }
    // Propagator.copyDrivenValues(this, src);
    // note: we don't bother with our this.valuesLock here: it isn't needed
    // (b/c no other threads have a reference to this yet), and to avoid the
    // possibility of deadlock (though that shouldn't happen either since no
    // other threads have references to this yet).
    this.slowpath_values.clear(); // slow path
    synchronized (src.valuesLock) {
      this.slowpath_values.putAll(src.slowpath_values); // slow path
      for (int y = 0; y < FASTPATH_GRID_HEIGHT; y++) { // fast path
        System.arraycopy(src.fastpath_values[y], 0,
            this.fastpath_values[y], 0, FASTPATH_GRID_WIDTH);
      }
    }
    synchronized (src.dirtyLock) {
      // note: we don't bother with our this.dirtyLock here: it isn't needed
      // (b/c no other threads have a reference to this yet), and to avoid the
      // possibility of deadlock (though that shouldn't happen either since no
      // other threads have references to this yet).
      this.dirtyComponents.addAll(src.dirtyComponents);
      this.dirtyPoints.addAll(src.dirtyPoints);
      // this.dirtyPointVals.addAll(src.dirtyPointVals);
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

  private InstanceStateImpl reusableInstanceState = new InstanceStateImpl(this, null);

  // FIXME: wtf?
  public InstanceState getInstanceState(Component comp) {
    final var factory = comp.getFactory();
    if (factory instanceof InstanceFactory) {
      if (comp != ((InstanceComponent) comp).getInstance().getComponent())
        throw new IllegalStateException("instanceComponent.getInstance().getComponent() is wrong");
      reusableInstanceState.repurpose(this, comp);
      return reusableInstanceState;
    }
    throw new RuntimeException("getInstanceState requires instance component");
  }

  // FIXME: wtf?
  public InstanceState getInstanceState(Instance instance) {
    final var factory = instance.getFactory();
    if (factory instanceof InstanceFactory) {
      reusableInstanceState.repurpose(this, instance.getComponent());
      return reusableInstanceState;
    }
    throw new RuntimeException("getInstanceState() requires instance component");
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
    Value v = null;
    if (p.x % 10 == 0 && p.y % 10 == 0
        && p.x < FASTPATH_GRID_WIDTH * 10
        && p.y < FASTPATH_GRID_HEIGHT * 10) {
      // fast path
      int x = p.x / 10;
      int y = p.y / 10;
      synchronized (valuesLock) {
        v = fastpath_values[y][x];
        if (v == null)
          v = CircuitWires.getBusValue(this, p);
      }
    } else {
      // slow path
      synchronized (valuesLock) {
        v = slowpath_values.get(p);
        if (v == null)
          v = CircuitWires.getBusValue(this, p);
      }
    }
    return v != null ? v : Value.createUnknown(circuit.getWidth(p));
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

  void markPointAsDirty(Propagator.SimulatorEvent ev) { // Location pt, Component cause, Value drivenValue) {
    synchronized (dirtyLock) {
      dirtyPoints.add(ev);
      // dirtyPoints.add(pt);
      // dirtyPointVals.add(newVal);
    }
  }

  ArrayList<Component> dirtyComponentsWorking = new ArrayList<>();
  void processDirtyComponents() {
    if (!dirtyComponentsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyComponentsWorking not empty");
    }
    synchronized (dirtyLock) {
      ArrayList<Component> other = dirtyComponents;
      dirtyComponents = dirtyComponentsWorking; // dirtyComponents is now empty
      dirtyComponentsWorking = other; // working set is now ready to process
      if (substatesDirty) {
        substatesDirty = false;
        substatesWorking = substates.toArray(substatesWorking);
      }
    }
    for (Component comp : dirtyComponentsWorking) {
      comp.propagate(this);
      // pin values also get propagated to parent state
      if (comp.getFactory() instanceof Pin && parentState != null) {
        parentComp.propagate(parentState);
      }
    }
    dirtyComponentsWorking.clear();
    for (CircuitState substate : substatesWorking) {
      if (substate == null) break;
      substate.processDirtyComponents();
    }
  }

  // private ArrayList<Location> dirtyPointsWorking = new ArrayList<>();
  // private ArrayList<Value> dirtyPointValsWorking = new ArrayList<>();
  private ArrayList<Propagator.SimulatorEvent> dirtyPointsWorking = new ArrayList<>();
  private CircuitState[] substatesWorking = new CircuitState[0];
  private boolean substatesDirty = true;

  void processDirtyPoints() {
    if (!dirtyPointsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyPointsWorking not empty");
    }
    synchronized (dirtyLock) {
      ArrayList<Propagator.SimulatorEvent> other = dirtyPoints;
      // ArrayList<Value> otherVals = dirtyPointVals;
      dirtyPoints = dirtyPointsWorking; // dirtyPoints is now empty
      // dirtyPointVals = dirtyPointValsWorking; // dirtyPointVals is now empty
      dirtyPointsWorking = other; // working set is now ready to process
      // dirtyPointValsWorking = otherVals; // working set is now ready to process
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
    //
    // if (circuit.wires.isMapVoided()) {
    //   // Note: this is a stopgap hack until we figure out the cause of the
    //   // concurrent modification exception.
    //   for (int i = 3; i >= 0; i--) {
    //     try {
    //       dirtyPointsWorking.addAll(circuit.wires.points.getAllLocations());
    //       break;
    //     } catch (ConcurrentModificationException e) {
    //       System.out.printf("warning: concurrent exception upon voided map (tries left %d)\n", i);
    //       // try again...
    //       try {
    //         Thread.sleep(1);
    //       } catch (InterruptedException e2) {
    //         // Yes, swallow the interrupt -- if simulator thread is interrupted
    //         // while it is in here, we want to keep going. The simulator thread
    //         // uses interrupts only for cancelling its own sleep/wait calls, not
    //         // this sleep call.
    //       }
    //       if (i == 0)
    //         e.printStackTrace();
    //     }
    //   }
    // }
    // if (!dirtyPointsWorking.isEmpty()) {
    // circuit.wires.propagate(this, dirtyPointsWorking, dirtyPointValsWorking);
    circuit.wires.propagate(this, dirtyPointsWorking);
    dirtyPointsWorking.clear();
    // dirtyPointValsWorking.clear();
    //}

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
        if (componentData.get(comp) instanceof ComponentDataGuiProvider guiProvider)
          guiProvider.destroy();
        if (componentData.get(comp) instanceof TelnetServer telnetServer)
          telnetServer.deleteAll();
        // it.remove(); ktt1: clear out the state instead of removing the key to
        // prevent concurrent modification error
        componentData.put(comp, null);
      }
    }
    synchronized (valuesLock) {
      slowpath_values.clear(); // slow path
      clearFastpathGrid(); // fast path
    }
    synchronized (dirtyLock) {
      dirtyComponents.clear();
      dirtyPoints.clear();
      // dirtyPointVals.clear();
      for (CircuitState sub : substates) {
        sub.reset();
      }
    }
    // slowpath_drivers.clear();
    markAllComponentsDirty();
  }

  public CircuitState createCircuitSubstateFor(Component comp, Circuit circ) {
    CircuitState oldState = (CircuitState) componentData.get(comp);
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
    CircuitState newState = new CircuitState(proj, circ, base);
    synchronized (dirtyLock) {
      substates.add(newState);
      substatesDirty = true;
    }
    newState.parentState = this;
    newState.parentComp = comp;
    componentData.put(comp, newState);
    return newState;
  }

  public void setData(Component comp, Object data) {
    if (data instanceof CircuitState newState) {
      // fixme: should never happen?
      System.out.println("fixme: setData with circuitstate... should never happen");
    }
    componentData.put(comp, data);
  }

  public void setValue(Location pt, Value val, Component cause, int delay) {
    base.setValue(this, pt, val, cause, delay);
  }

  private void clearFastpathGrid() { // precondition: valuesLock held
    for (int y = 0; y < FASTPATH_GRID_HEIGHT; y++)
      for (int x = 0; x < FASTPATH_GRID_WIDTH; x++)
        fastpath_values[y][x] = null;
  }

  // for CircuitWires - to set value at point
  void setValueByWire(Value v, Location[] points, CircuitWires.BusConnection[] connections) {
    for (Location p : points) {
      if (p.x % 10 == 0 && p.y % 10 == 0
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
    for (CircuitWires.BusConnection bc : connections) {
      if (bc.isSink || (bc.isBidirectional && !Value.equal(v, bc.drivenValue)))
        markComponentAsDirty(bc.component);
    }
  }

  // for CircuitWires - to set value at point
  void clearValuesByWire() {
    synchronized (valuesLock) {
      slowpath_values.clear(); // slow path
      clearFastpathGrid(); // fast path
    }
  }

  // // for CircuitWires - to set value at point where there is no bus, just a
  // // bunch of components
  // void setValueByWire(Value v, Location p) {
  //   boolean changed;
  //   if (p.x % 10 == 0 && p.y % 10 == 0
  //       && p.x < FASTPATH_GRID_WIDTH*10
  //       && p.y < FASTPATH_GRID_HEIGHT*10) {
  //     synchronized (valuesLock) {
  //       changed = fastpath(p, v);
  //     }
  //   } else {
  //     synchronized (valuesLock) {
  //       changed = slowpath(p, v);
  //     }
  //   }
  //   if (changed)
  //     markDirtyComponentsAt(p);
  // }

  private boolean fastpath(Location p, Value v) { // precondition: valuesLock held
    int x = p.x / 10;
    int y = p.y / 10;
    if (v == Value.NIL) {
      if (fastpath_values[y][x] != null) {
        fastpath_values[y][x] = null;
        return true;
      } else {
        return false;
      }
    } else {
      if (!v.equals(fastpath_values[y][x])) {
        fastpath_values[y][x] = v;
        return true;
      } else {
        return false;
      }
    }
  }

  private boolean slowpath(Location p, Value v) { // precondition: valuesLock held
    if (v == Value.NIL) {
      Object old = slowpath_values.remove(p);
      return (old != null && old != Value.NIL);
    } else {
      Object old = slowpath_values.put(p, v);
      return !v.equals(old);
    }
  }

  // private void markDirtyComponentsAt(Location p) {
  //   boolean found = false;
  //   for (Component comp : circuit.getComponents(p)) {
  //     if (!(comp instanceof Wire) && !(comp instanceof Splitter)) {
  //       found = true;
  //       markComponentAsDirty(comp);
  //     }
  //   }
  //   // NOTE: this will cause a double-propagation on components
  //   // whose outputs have just changed.
  //   // FIXME: huh?
  //   if (found)
  //     base.locationTouched(this, p);
  // }

  // private void markDirtyComponents(Location p, Component[] affected) {
  //   for (Component comp : affected)
  //     markComponentAsDirty(comp);
  //   // NOTE: this will cause a double-propagation on components
  //   // whose outputs have just changed.
  //   // FIXME: huh?
  //   if (affected.length > 0)
  //     base.locationTouched(this, p);
  // }

  void setWireData(CircuitWires.State data) {
    wireData = data;
  }

  private void markDirtyComponents(Location p, Component[] affected) {
    for (Component comp : affected)
      markComponentAsDirty(comp);
    // NOTE: this will cause a double-propagation on components
    // whose outputs have just changed.
    // FIXME: huh?
    if (affected.length > 0)
      base.locationTouched(this, p);
  }

  boolean toggleClocks(int ticks) {
    var hasClocks = false;
    if (temporaryClock != null)
      hasClocks |= temporaryClockValidateOrTick(ticks);

    for (Component clock : circuit.getClocks()) {
      hasClocks = true;
      boolean dirty = Clock.tick(this, ticks, clock);
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
        final var state = getInstanceState(instance);
        Value vOld = pin.getValue(state);
        Value vNew = ticks % 2 == 0 ? Value.FALSE : Value.TRUE;
        if (!vNew.equals(vOld)) {
          pin.setValue(state, vNew);
          // state.fireInvalidated();
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
