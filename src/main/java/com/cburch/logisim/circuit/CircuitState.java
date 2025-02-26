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
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
            subStates.remove(subState);
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
          if (base != null) base.checkComponentEnds(CircuitState.this, comp);
          dirtyComponents.remove(comp);
        }
      } else if (action == CircuitEvent.ACTION_CLEAR) {
        /* Whole circuit was cleared */
        temporaryClock = null;
        knownClocks = false;
        subStates.clear();
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
        dirtyComponents.clear();
        dirtyPoints.clear();
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
            subStates.remove(sub);
          }
        }
      }
    }
  }

  private final MyCircuitListener myCircuitListener = new MyCircuitListener();
  private Propagator base = null; // base of tree of CircuitStates
  private final Project proj; // project where circuit liespr
  private final Circuit circuit; // circuit being simulated

  private CircuitState parentState = null; // parent in tree of CircuitStates
  private Component parentComp = null; // subcircuit component containing this
  // state
  private HashSet<CircuitState> subStates = new HashSet<>();

  private CircuitWires.State wireData = null;
  private final HashMap<Component, Object> componentData = new HashMap<>();
  private final Map<Location, Value> values = new HashMap<>();
  private CopyOnWriteArraySet<Component> dirtyComponents = new CopyOnWriteArraySet<>();
  private final CopyOnWriteArraySet<Location> dirtyPoints = new CopyOnWriteArraySet<>();
  final HashMap<Location, SetData> causes = new HashMap<>();

  private static int lastId = 0;
  private final int id = lastId++;

  public CircuitState(Project proj, Circuit circuit) {
    this.proj = proj;
    this.circuit = circuit;
    circuit.addCircuitListener(myCircuitListener);
  }

  @Override
  public CircuitState clone() {
    return cloneState();
  }

  public CircuitState cloneState() {
    final var ret = new CircuitState(proj, circuit);
    ret.copyFrom(this, new Propagator(ret));
    ret.parentComp = null;
    ret.parentState = null;
    return ret;
  }

  public boolean containsKey(Location pt) {
    return values.containsKey(pt);
  }

  private void copyFrom(CircuitState src, Propagator base) {
    this.base = base;
    this.parentComp = src.parentComp;
    this.parentState = src.parentState;
    final var substateData = new HashMap<CircuitState, CircuitState>();
    this.subStates = new HashSet<>();
    for (final var oldSub : src.subStates) {
      final var newSub = new CircuitState(src.proj, oldSub.circuit);
      newSub.copyFrom(oldSub, base);
      newSub.parentState = this;
      this.subStates.add(newSub);
      substateData.put(oldSub, newSub);
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
    this.values.putAll(src.values);
    this.dirtyComponents.addAll(src.dirtyComponents);
    this.dirtyPoints.addAll(src.dirtyPoints);
    if (src.wireData != null) {
      this.wireData = circuit.wires.newState(this); // all buses will be marked as dirty
    }
  }

  public void drawOscillatingPoints(ComponentDrawContext context) {
    if (base != null) base.drawOscillatingPoints(context);
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
    if (base == null) {
      base = new Propagator(this);
      markAllComponentsDirty();
    }
    return base;
  }

  Component getSubcircuit() {
    return parentComp;
  }

  public Set<CircuitState> getSubStates() { // returns Set of CircuitStates
    return subStates;
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

  //
  // private methods
  //
  private void markAllComponentsDirty() {
    dirtyComponents.addAll(circuit.getNonWires());
  }

  public void markComponentAsDirty(Component comp) {
    try {
      dirtyComponents.add(comp);
    } catch (RuntimeException e) {
      final var set = new CopyOnWriteArraySet<Component>();
      set.add(comp);
      dirtyComponents = set;
    }
  }

  public void markComponentsDirty(Collection<Component> comps) {
    dirtyComponents.addAll(comps);
  }

  public void markPointAsDirty(Location pt) {
    dirtyPoints.add(pt);
  }

  void processDirtyComponents() {
    if (!dirtyComponents.isEmpty()) {
      // This seeming wasted copy is to avoid ConcurrentModifications
      // if we used an iterator instead.
      Object[] toProcess;
      RuntimeException firstException = null;
      for (var tries = 4; true; tries--) {
        try {
          toProcess = dirtyComponents.toArray();
          break;
        } catch (RuntimeException e) {
          if (firstException == null) firstException = e;
          if (tries == 0) {
            toProcess = new Object[0];
            dirtyComponents = new CopyOnWriteArraySet<>();
            throw firstException;
          }
        }
      }
      dirtyComponents.clear();
      for (final var compObj : toProcess) {
        if (compObj instanceof Component comp) {
          comp.propagate(this);
          if (comp.getFactory() instanceof Pin && parentState != null) {
            // should be propagated in superstate
            parentComp.propagate(parentState);
          }
        }
      }
    }

    final var subs = new CircuitState[subStates.size()];
    for (final var substate : subStates.toArray(subs)) {
      substate.processDirtyComponents();
    }
  }

  void processDirtyPoints() {
    final var dirty = new HashSet<>(dirtyPoints);
    dirtyPoints.clear();
    if (circuit.wires.isMapVoided()) {
      for (var i = 3; i >= 0; i--) {
        try {
          dirty.addAll(circuit.wires.points.getSplitLocations());
          break;
        } catch (ConcurrentModificationException e) {
          // try again...
          try {
            Thread.sleep(1);
          } catch (InterruptedException ignored) {
          }
          if (i == 0) e.printStackTrace();
        }
      }
    }
    if (!dirty.isEmpty()) {
      circuit.wires.propagate(this, dirty);
    }

    final var subs = new CircuitState[subStates.size()];
    for (final var substate : subStates.toArray(subs)) {
      /* TODO: Analyze why this bug happens, e.g. a substate that is null! */
      if (substate != null) substate.processDirtyPoints();
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
    dirtyComponents.clear();
    dirtyPoints.clear();
    causes.clear();
    markAllComponentsDirty();

    for (CircuitState sub : subStates) {
      sub.reset();
    }
  }

  public void setData(Component comp, Object data) {
    if (data instanceof CircuitState newState) {
      final var oldState = (CircuitState) componentData.get(comp);
      if (oldState != newState) {
        // There's something new going on with this subcircuit.
        // Maybe the subcircuit is new, or perhaps it's being
        // removed.
        if (oldState != null && oldState.parentComp == comp) {
          // it looks like it's being removed
          subStates.remove(oldState);
          oldState.parentState = null;
          oldState.parentComp = null;
          oldState.reset();
        }
        if (newState != null && newState.parentState != this) {
          // this is the first time I've heard about this CircuitState
          subStates.add(newState);
          newState.base = this.base;
          newState.parentState = this;
          newState.parentComp = comp;
          newState.markAllComponentsDirty();
        }
      }
    } else {
      if (componentData.get(comp) instanceof ComponentDataGuiProvider)
        ((ComponentDataGuiProvider) componentData.get(comp)).destroy();

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

      if (found && base != null) base.locationTouched(this, p);
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

    final var subs = new CircuitState[subStates.size()];
    for (final var substate : subStates.toArray(subs))
      ret |= substate.toggleClocks(ticks);
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
