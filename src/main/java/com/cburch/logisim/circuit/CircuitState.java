/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.circuit.Propagator.SetData;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.TelnetServer;
import com.cburch.logisim.std.io.extra.Buzzer;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamState;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CircuitState implements InstanceData {

  private class MyCircuitListener implements CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();

      if (action == CircuitEvent.ACTION_ADD) {
        /* Component was added */
        final var comp = (Component) event.getData();
        if (comp instanceof Wire wire) {
          markPointAsDirty(wire.getEnd0());
          markPointAsDirty(wire.getEnd1());
        } else {
          markComponentAsDirty(comp);
        }
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
          final var subState = (CircuitState) getData(comp);
          if (subState != null && subState.parentComp == comp) {
            synchronized (dirtyLock) {
              substates.remove(subState);
            }
            subState.parentState = null;
            subState.parentComp = null;
            subState.reset();
          }
        } else if (getData(comp) instanceof ComponentDataGuiProvider guiProvider) {
          guiProvider.destroy();
        }
        if (comp instanceof Wire w) {
          markPointAsDirty(w.getEnd0());
          markPointAsDirty(w.getEnd1());
        } else {
          Propagator.checkComponentEnds(CircuitState.this, comp);
          synchronized (dirtyLock) {
            dirtyComponents.remove(comp);
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
        values.clear();
        synchronized (dirtyLock) {
          dirtyComponents.clear();
          dirtyPoints.clear();
          substates.clear();
          substatesWorking = new CircuitState[0];
        }
        causes.clear();
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
  private final Map<Location, Value> values = new HashMap<>();
  final HashMap<Location, SetData> causes = new HashMap<>();
  //private CopyOnWriteArraySet<Component> dirtyComponents = new CopyOnWriteArraySet<>();
  private HashSet<Component> dirtyComponents = new HashSet<>(); // protected by dirtyLock
  //private final CopyOnWriteArraySet<Location> dirtyPoints = new CopyOnWriteArraySet<>();
  //private final HashSet<Location> dirtyPoints = new HashSet<>();
  private ArrayList<Location> dirtyPoints = new ArrayList<>(); // protected by dirtyLock
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

  public boolean containsKey(Location pt) {
    return values.containsKey(pt);
  }

  private void copyFrom(CircuitState src) {
    this.parentComp = src.parentComp;
    this.parentState = src.parentState;
    final var substateData = new HashMap<CircuitState, CircuitState>();
    this.substates = new HashSet<>();
    synchronized (src.dirtyLock) {
      // note: we don't bother with our this.dirtyLock here: it isn't needed
      // (b/c no other threads have a reference to his yet), and to avoid the
      // possibility of deadlock (though  that shouldn't happen either since no
      // other threads have references to this yet).
      for (final var oldSub : src.substates) {
        final var newSub = CircuitState.createRootState(src.proj, oldSub.circuit);
        newSub.copyFrom(oldSub);
        newSub.parentState = this;
        this.substates.add(newSub);
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
    for (final var key : src.causes.keySet()) {
      final var oldValue = src.causes.get(key);
      final var newValue = oldValue.cloneFor(this);
      this.causes.put(key, newValue);
    }
    this.values.clear();
    this.values.putAll(src.values);
    synchronized (src.dirtyLock) {
      // note: we don't bother with our this.dirtyLock here: it isn't needed
      // (b/c no other threads have a reference to his yet), and to avoid the
      // possibility of deadlock (though  that shouldn't happen either since no
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

  Value getComponentOutputAt(Location p) {
    // for CircuitWires - to get values, ignoring wires' contributions
    final var causeList = causes.get(p);
    return Propagator.computeValue(causeList);
  }

  public Object getData(Component comp) {
    return componentData.get(comp);
  }

  public InstanceState getInstanceState(Component comp) {
    final var factory = comp.getFactory();
    if (factory instanceof InstanceFactory instanceFactory) {
      return instanceFactory.createInstanceState(this, comp);
    }
    throw new RuntimeException("getInstanceState requires instance component");
  }

  public InstanceState getInstanceState(Instance instance) {
    final var factory = instance.getFactory();
    if (factory instanceof InstanceFactory) {
      return factory.createInstanceState(this, instance);
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

  public Set<CircuitState> getSubStates() { // returns Set of CircuitStates
    return substates;
  }

  public Value getValue(Location pt) {
    final var ret = values.get(pt);
    if (ret != null) return ret;

    final var wid = circuit.getWidth(pt);
    return Value.createUnknown(wid);
  }

  Value getValueByWire(Location p) {
    return values.get(p);
  }

  CircuitWires.State getWireData() {
    return wireData;
  }

  //
  // methods for other classes within package
  //
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

  void markPointAsDirty(Location pt) {
    synchronized (dirtyLock) {
      dirtyPoints.add(pt);
    }
  }

  HashSet<Component> dirtyComponentsWorking = new HashSet<>();
  void processDirtyComponents() {
    if (!dirtyComponentsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyComponentsWorking not empty");
    }
    synchronized (dirtyLock) {
      HashSet<Component> other = dirtyComponents;
      dirtyComponents = dirtyComponentsWorking; // dirtyComponents is now empty
      dirtyComponentsWorking = other; // working set is now ready to process
      substatesWorking = substates.toArray(substatesWorking);
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

  private ArrayList<Location> dirtyPointsWorking = new ArrayList<>();
  private CircuitState[] substatesWorking = new CircuitState[0];

  void processDirtyPoints() {
    if (!dirtyPointsWorking.isEmpty()) {
      throw new IllegalStateException("INTERNAL ERROR: dirtyPointWorking not empty");
    }
    synchronized (dirtyLock) {
      ArrayList<Location> other = dirtyPoints;
      dirtyPoints = dirtyPointsWorking; // dirtyPoints is now empty
      dirtyPointsWorking = other; // working set is now ready to process
      substatesWorking = substates.toArray(substatesWorking);
    }
    if (circuit.wires.isMapVoided()) {
      for (var i = 3; i >= 0; i--) {
        try {
          dirtyPointsWorking.addAll(circuit.wires.points.getSplitLocations());
          break;
        } catch (ConcurrentModificationException e) {
          System.out.printf("warning: concurrent exception upon voided map (tries left %d)\n", i);
          // try again...
          try {
            Thread.sleep(1);
          } catch (InterruptedException ignored) {
          }
          if (i == 0) e.printStackTrace();
        }
      }
    }
    if (!dirtyPointsWorking.isEmpty()) {
      circuit.wires.propagate(this, dirtyPointsWorking);
      dirtyPointsWorking.clear();
    }

    for (final var substate : substatesWorking) {
      if (substate == null) break;
      substate.processDirtyPoints();
    }
  }

  void reset() {
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
    values.clear();
    synchronized (dirtyLock) {
      dirtyComponents.clear();
      dirtyPoints.clear();
    }
    causes.clear();
    markAllComponentsDirty();
  }

  public CircuitState createCircuitSubstateFor(Component comp, Circuit circ) {
    CircuitState oldState = (CircuitState) componentData.get(comp);
    if (oldState != null && oldState.parentComp == comp) {
      // fixme: Does this ever happen?
      System.out.println("fixme: removed stale circuitstate... should never happen");
      synchronized (dirtyLock) {
        substates.remove(oldState);
      }
      oldState.parentState = null;
      oldState.parentComp = null;
    }
    CircuitState newState = new CircuitState(proj, circ, base);
    synchronized (dirtyLock) {
      substates.add(newState);
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
    if (base != null) base.setValue(this, pt, val, cause, delay);
  }

  void setValueByWire(Location p, Value v) {
    // for CircuitWires - to set value at point
    boolean changed;
    if (v == Value.NIL) {
      final var old = values.remove(p);
      changed = (old != null && old != Value.NIL);
    } else {
      final var old = values.put(p, v);
      changed = !v.equals(old);
    }
    if (changed) {
      var found = false;
      for (final var comp : circuit.getComponents(p)) {
        if (!(comp instanceof Wire) && !(comp instanceof Splitter)) {
          found = true;
          markComponentAsDirty(comp);
        }
      }
      // NOTE: this will cause a double-propagation on components
      // whose outputs have just changed.

      if (found) base.locationTouched(this, p);
    }
  }

  void setWireData(CircuitWires.State data) {
    wireData = data;
  }

  boolean toggleClocks(int ticks) {
    var ret = false;
    if (temporaryClock != null)
      ret |= temporaryClockValidateOrTick(ticks);

    for (final var clock : circuit.getClocks())
      ret |= Clock.tick(this, ticks, clock);

    final var subs = new CircuitState[substates.size()];
    synchronized (dirtyLock) {
      substatesWorking = substates.toArray(substatesWorking);
    }
    for (final var substate : substatesWorking) {
      if (substate == null) break;
      ret |= substate.toggleClocks(ticks);
    }
    return ret;
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
        pin.setValue(state, ticks % 2 == 0 ? Value.FALSE : Value.TRUE);
        state.fireInvalidated();
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
