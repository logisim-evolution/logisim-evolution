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

package com.cburch.logisim.circuit;

import com.cburch.logisim.circuit.Propagator.SetData;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
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
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();

      /* Component was added */
      if (action == CircuitEvent.ACTION_ADD) {
        Component comp = (Component) event.getData();
        if (comp instanceof Wire) {
          Wire w = (Wire) comp;
          markPointAsDirty(w.getEnd0());
          markPointAsDirty(w.getEnd1());
        } else {
          markComponentAsDirty(comp);
        }
      }

      /* Component was removed */
      else if (action == CircuitEvent.ACTION_REMOVE) {
        Component comp = (Component) event.getData();
        if (comp == temporaryClock)
          temporaryClock = null;
        if (comp.getFactory() instanceof Clock) {
          knownClocks = false; // just in case, will be recomputed by simulator
        }
        if (comp.getFactory() instanceof SubcircuitFactory) {
          knownClocks = false; // just in case, will be recomputed by simulator
          // disconnect from tree
          CircuitState substate = (CircuitState) getData(comp);
          if (substate != null && substate.parentComp == comp) {
            substates.remove(substate);
            substate.parentState = null;
            substate.parentComp = null;
            substate.reset();
          }
        } else if (getData(comp) != null && getData(comp) instanceof ComponentDataGuiProvider) 
          ((ComponentDataGuiProvider)getData(comp)).destroy();
        if (comp instanceof Wire) {
          Wire w = (Wire) comp;
          markPointAsDirty(w.getEnd0());
          markPointAsDirty(w.getEnd1());
        } else {
          if (base != null) base.checkComponentEnds(CircuitState.this, comp);
          dirtyComponents.remove(comp);
        }
      }

      /* Whole circuit was cleared */
      else if (action == CircuitEvent.ACTION_CLEAR) {
        temporaryClock = null;
        knownClocks = false;
        substates.clear();
        wireData = null;
        for (Component c : componentData.keySet()) {
          if (componentData.get(c) != null && componentData.get(c) instanceof ComponentDataGuiProvider)
            ((ComponentDataGuiProvider)componentData.get(c)).destroy();
          else if (componentData.get(c) instanceof CircuitState) {
            ((CircuitState)componentData.get(c)).reset();
          }
        }
        componentData.clear();
        values.clear();
        dirtyComponents.clear();
        dirtyPoints.clear();
        causes.clear();
      }

      else if (action == CircuitEvent.ACTION_INVALIDATE) {
        Component comp = (Component) event.getData();
        markComponentAsDirty(comp);
        // If simulator is in single step mode, we want to hilight the
        // invalidated components (which are likely Pins, Buttons, or other
        // inputs), so pass this component to the simulator for display.
        proj.getSimulator().addPendingInput(CircuitState.this, comp);
        // TODO detemine if this should really be missing if (base !=
        // null) base.checkComponentEnds(CircuitState.this, comp);
      } else if (action == CircuitEvent.TRANSACTION_DONE) {
        ReplacementMap map = event.getResult().getReplacementMap(circuit);
        if (map == null) return;
        for (Component comp : map.getRemovals()) {
          Object compState = componentData.remove(comp);
          if (compState != null) continue; 
          Class<?> compFactory = comp.getFactory().getClass();
          boolean found = false;
          for (Component repl : map.getReplacementsFor(comp)) {
            if (repl.getFactory().getClass() == compFactory) {
              found = true;
              setData(repl, compState);
              break;
            }
          }
          if (!found && compState instanceof RamState) Ram.closeHexFrame((RamState) compState);
          if (!found && compState instanceof CircuitState) {
            CircuitState sub = (CircuitState) compState;
            sub.parentState = null;
            substates.remove(sub);
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
  private HashSet<CircuitState> substates = new HashSet<>();

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
    CircuitState ret = new CircuitState(proj, circuit);
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
    HashMap<CircuitState, CircuitState> substateData = new HashMap<>();
    this.substates = new HashSet<>();
    for (CircuitState oldSub : src.substates) {
      CircuitState newSub = new CircuitState(src.proj, oldSub.circuit);
      newSub.copyFrom(oldSub, base);
      newSub.parentState = this;
      this.substates.add(newSub);
      substateData.put(oldSub, newSub);
    }
    for (Component key : src.componentData.keySet()) {
      Object oldValue = src.componentData.get(key);
      if (oldValue instanceof CircuitState) {
        Object newValue = substateData.get(oldValue);
        if (newValue != null) this.componentData.put(key, newValue);
        else this.componentData.remove(key);
      } else {
        Object newValue;
        if (oldValue instanceof ComponentState) {
          newValue = ((ComponentState) oldValue).clone();
        } else {
          newValue = oldValue;
        }
        this.componentData.put(key, newValue);
      }
    }
    for (Location key : src.causes.keySet()) {
      Propagator.SetData oldValue = src.causes.get(key);
      Propagator.SetData newValue = oldValue.cloneFor(this);
      this.causes.put(key, newValue);
    }
    if (src.wireData != null) {
      this.wireData = (CircuitWires.State) src.wireData.clone();
    }
    this.values.putAll(src.values);
    this.dirtyComponents.addAll(src.dirtyComponents);
    this.dirtyPoints.addAll(src.dirtyPoints);
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
    Propagator.SetData cause_list = causes.get(p);
    return Propagator.computeValue(cause_list);
  }

  public Object getData(Component comp) {
    return componentData.get(comp);
  }

  public InstanceState getInstanceState(Component comp) {
    Object factory = comp.getFactory();
    if (factory instanceof InstanceFactory) {
      return ((InstanceFactory) factory).createInstanceState(this, comp);
    } else {
      throw new RuntimeException("getInstanceState requires instance component");
    }
  }

  public InstanceState getInstanceState(Instance instance) {
    Object factory = instance.getFactory();
    if (factory instanceof InstanceFactory) {
      return ((InstanceFactory) factory).createInstanceState(this, instance);
    } else {
      throw new RuntimeException("getInstanceState requires instance component");
    }
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

  public Set<CircuitState> getSubstates() { // returns Set of CircuitStates
    return substates;
  }

  public Value getValue(Location pt) {
    Value ret = values.get(pt);
    if (ret != null) return ret;

    BitWidth wid = circuit.getWidth(pt);
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
      CopyOnWriteArraySet<Component> set = new CopyOnWriteArraySet<>();
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
      for (int tries = 4; true; tries--) {
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
      for (Object compObj : toProcess) {
        if (compObj instanceof Component) {
          Component comp = (Component) compObj;
          comp.propagate(this);
          if (comp.getFactory() instanceof Pin && parentState != null) {
            // should be propagated in superstate
            parentComp.propagate(parentState);
          }
        }
      }
    }

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs)) {
      substate.processDirtyComponents();
    }
  }

  void processDirtyPoints() {
    HashSet<Location> dirty = new HashSet<>(dirtyPoints);
    dirtyPoints.clear();
    if (circuit.wires.isMapVoided()) {
      for (int i = 3; i >= 0; i--) {
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

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs)) {
      /* TODO: Analyze why this bug happens, e.g. a substate that is null! */
      if (substate != null) substate.processDirtyPoints();
    }
  }

  void reset() {
    temporaryClock = null;
    wireData = null;
    for (Component comp : componentData.keySet()) {
      if (comp.getFactory() instanceof Ram) {
        Ram ram = (Ram) comp.getFactory();
        boolean remove = ram.reset(this, Instance.getInstanceFor(comp));
        if (remove)
          componentData.put(comp, null);
      } else if (comp.getFactory() instanceof Buzzer) {
        Buzzer.StopBuzzerSound(comp, this);
      } else if (!(comp.getFactory() instanceof SubcircuitFactory)) {
        if (componentData.get(comp) instanceof ComponentDataGuiProvider)
          ((ComponentDataGuiProvider) componentData.get(comp)).destroy();
    /*  it.remove(); ktt1: clear out the state instead of removing the key to prevent concurrent
        modification error */
        componentData.put(comp, null);
      }
    }
    values.clear();
    dirtyComponents.clear();
    dirtyPoints.clear();
    causes.clear();
    markAllComponentsDirty();

    for (CircuitState sub : substates) {
      sub.reset();
    }
  }

  public void setData(Component comp, Object data) {
    if (data instanceof CircuitState) {
      CircuitState oldState = (CircuitState) componentData.get(comp);
      CircuitState newState = (CircuitState) data;
      if (oldState != newState) {
        // There's something new going on with this subcircuit.
        // Maybe the subcircuit is new, or perhaps it's being
        // removed.
        if (oldState != null && oldState.parentComp == comp) {
          // it looks like it's being removed
          substates.remove(oldState);
          oldState.parentState = null;
          oldState.parentComp = null;
          oldState.reset();
        }
        if (newState != null && newState.parentState != this) {
          // this is the first time I've heard about this CircuitState
          substates.add(newState);
          newState.base = this.base;
          newState.parentState = this;
          newState.parentComp = comp;
          newState.markAllComponentsDirty();
        }
      }
    } else {
      if (componentData.get(comp)!= null && componentData.get(comp) instanceof ComponentDataGuiProvider)
        ((ComponentDataGuiProvider)componentData.get(comp)).destroy();
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
      Object old = values.remove(p);
      changed = (old != null && old != Value.NIL);
    } else {
      Object old = values.put(p, v);
      changed = !v.equals(old);
    }
    if (changed) {
      boolean found = false;
      for (Component comp : circuit.getComponents(p)) {
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
    boolean ret = false;
    if (temporaryClock != null)
      ret |= temporaryClockValidateOrTick(ticks);

    for (Component clock : circuit.getClocks())
      ret |= Clock.tick(this, ticks, clock);

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs)) 
      ret |= substate.toggleClocks(ticks);
    return ret;
  }

  private boolean temporaryClockValidateOrTick(int ticks) {
    // temporaryClock.getFactory() will be Pin, normally a 1 bit input
    Pin pin;
    try {
      pin = (Pin)temporaryClock.getFactory();
    } catch (ClassCastException e) {
      temporaryClock = null;
      return false;
    }
    Instance i = Instance.getInstanceFor(temporaryClock);
    if (i == null || !pin.isInputPin(i) || pin.getWidth(i).getWidth() != 1) {
      temporaryClock = null;
      return false;
    }
    if (ticks >= 0) {
      InstanceState state = getInstanceState(i);
      // Value v = pin.getValue(state);
      pin.setValue(state, ticks%2==0 ? Value.FALSE : Value.TRUE);
      state.fireInvalidated();
    }
    return true;
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
    return clk == null ? true : temporaryClockValidateOrTick(-1);
  }

  public Component getTemporaryClock() {
    return temporaryClock;
  }

  @Override
  public String toString() {
    return "State" + id + "[" + circuit.getName() + "]";
  }
}
