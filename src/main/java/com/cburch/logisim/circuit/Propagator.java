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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

public class Propagator {
  private static class ComponentPoint {
    final Component cause;
    final Location loc;

    public ComponentPoint(Component cause, Location loc) {
      this.cause = cause;
      this.loc = loc;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ComponentPoint)) return false;
      final var o = (ComponentPoint) other;
      return this.cause.equals(o.cause) && this.loc.equals(o.loc);
    }

    @Override
    public int hashCode() {
      return 31 * cause.hashCode() + loc.hashCode();
    }
  }

  private static class Listener implements AttributeListener {
    final WeakReference<Propagator> prop;

    public Listener(Propagator propagator) {
      prop = new WeakReference<>(propagator);
    }

    @Override
    public void attributeListChanged(AttributeEvent e) {}

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final var p = prop.get();
      if (p == null) {
        e.getSource().removeAttributeListener(this);
      } else if (e.getAttribute().equals(Options.ATTR_SIM_RAND)) {
        p.updateRandomness();
      }
    }
  }

  static class SetData implements Comparable<SetData> {
    final int time;
    final int serialNumber;
    final CircuitState state; // state of circuit containing component
    final Component cause; // component emitting the value
    final Location loc; // the location at which value is emitted
    Value val; // value being emitted
    SetData next = null;

    private SetData(
        int time, int serialNumber, CircuitState state, Location loc, Component cause, Value val) {
      this.time = time;
      this.serialNumber = serialNumber;
      this.state = state;
      this.cause = cause;
      this.loc = loc;
      this.val = val;
    }

    public SetData cloneFor(CircuitState newState) {
      final var newProp = newState.getPropagator();
      final var dtime = newProp.clock - state.getPropagator().clock;
      final var ret =
          new SetData(time + dtime, newProp.setDataSerialNumber, newState, loc, cause, val);
      newProp.setDataSerialNumber++;
      if (this.next != null) ret.next = this.next.cloneFor(newState);
      return ret;
    }

    @Override
    public int compareTo(SetData o) {
      // Yes, these subtractions may overflow. This is intentional, as it
      // avoids potential wraparound problems as the counters increment.
      int ret = this.time - o.time;
      if (ret != 0) return ret;
      return this.serialNumber - o.serialNumber;
    }

    @Override
    public String toString() {
      return loc + ":" + val + "(" + cause + ")";
    }
  }

  //
  // static methods
  //
  static Value computeValue(SetData causes) {
    if (causes == null) return Value.NIL;
    var ret = causes.val;
    for (SetData n = causes.next; n != null; n = n.next) {
      ret = ret.combine(n.val);
    }
    return ret;
  }

  private final CircuitState root; // root of state tree

  /** The number of clock cycles to let pass before deciding that the circuit is oscillating. */
  private static final int simLimit = 1000;

  /**
   * On average, one out of every 2**simRandomShift propagations through a component is delayed one
   * step more than the component requests. This noise is intended to address some circuits that
   * would otherwise oscillate within Logisim (though they wouldn't oscillate in practice).
   */
  private volatile int simRandomShift;

  private final PriorityQueue<SetData> toProcess = new PriorityQueue<>();
  private int clock = 0;
  private boolean isOscillating = false;
  private boolean oscAdding = false;
  private PropagationPoints oscPoints = new PropagationPoints();
  private int halfClockCycles = 0;
  private final Random noiseSource = new Random();
  private int noiseCount = 0;

  private int setDataSerialNumber = 0;
  static int lastId = 0;

  final int id = lastId++;

  public Propagator(CircuitState root) {
    this.root = root;
    final var l = new Listener(this);
    root.getProject().getOptions().getAttributeSet().addAttributeListener(l);
    updateRandomness();
  }

  private SetData addCause(CircuitState state, SetData head, SetData data) {
    if (data.val == null) { // actually, it should be removed
      return removeCause(state, head, data.loc, data.cause);
    }

    final var causes = state.causes;

    // first check whether this is change of previous info.
    boolean replaced = false;
    for (SetData n = head; n != null; n = n.next) {
      if (n.cause == data.cause) {
        n.val = data.val;
        replaced = true;
        break;
      }
    }

    // otherwise, insert to list of causes
    if (!replaced) {
      if (head == null) {
        causes.put(data.loc, data);
        head = data;
      } else {
        data.next = head.next;
        head.next = data;
      }
    }

    return head;
  }

  //
  // private methods
  //
  void checkComponentEnds(CircuitState state, Component comp) {
    for (final var end : comp.getEnds()) {
      final var loc = end.getLocation();
      final var oldHead = state.causes.get(loc);
      final var oldVal = computeValue(oldHead);
      final var newHead = removeCause(state, oldHead, loc, comp);
      final var newVal = computeValue(newHead);
      final var wireVal = state.getValueByWire(loc);

      if (!newVal.equals(oldVal) || wireVal != null) {
        state.markPointAsDirty(loc);
      }
      if (wireVal != null) state.setValueByWire(loc, Value.NIL);
    }
  }

  public void drawOscillatingPoints(ComponentDrawContext context) {
    if (isOscillating) oscPoints.draw(context);
  }

  //
  // public methods
  //
  CircuitState getRootState() {
    return root;
  }

  public int getTickCount() {
    return halfClockCycles;
  }

  public boolean isOscillating() {
    return isOscillating;
  }

  boolean isPending() {
    return !toProcess.isEmpty();
  }

  /*
   * TODO for the SimulatorPrototype class void step() { clock++;
   *
   * // propagate all values for this clock tick HashMap visited = new
   * HashMap(); // State -> set of ComponentPoints handled while
   * (!toProcess.isEmpty()) { SetData data; data = (SetData) toProcess.peek();
   * if (data.time != clock) break; toProcess.remove(); CircuitState state =
   * data.state;
   *
   * // if it's already handled for this clock tick, continue HashSet handled
   * = (HashSet) visited.get(state); if (handled != null) { if
   * (!handled.add(new ComponentPoint(data.cause, data.loc))) continue; } else
   * { handled = new HashSet(); visited.put(state, handled); handled.add(new
   * ComponentPoint(data.cause, data.loc)); }
   *
   * if (oscAdding) oscPoints.add(state, data.loc);
   *
   * // change the information about value SetData oldHead = (SetData)
   * state.causes.get(data.loc); Value oldVal = computeValue(oldHead); SetData
   * newHead = addCause(state, oldHead, data); Value newVal =
   * computeValue(newHead);
   *
   * // if the value at point has changed, propagate it if
   * (!newVal.equals(oldVal)) { state.markPointAsDirty(data.loc); } }
   *
   * clearDirtyPoints(); clearDirtyComponents(); }
   */

  void locationTouched(CircuitState state, Location loc) {
    if (oscAdding) oscPoints.add(state, loc);
  }

  public boolean propagate() {
    return propagate(null, null);
  }

  public boolean propagate(Simulator.Listener propListener, Simulator.Event propEvent) {
    oscPoints.clear();
    root.processDirtyPoints();
    root.processDirtyComponents();

    final var oscThreshold = simLimit;
    final var logThreshold = 3 * oscThreshold / 4;
    var iters = 0;
    while (!toProcess.isEmpty()) {
      if (iters > 0 && propListener != null)
        propListener.propagationInProgress(propEvent);
      iters++;

      if (iters < logThreshold) {
        stepInternal(null);
      } else if (iters < oscThreshold) {
        oscAdding = true;
        stepInternal(oscPoints);
      } else {
        isOscillating = true;
        oscAdding = false;
        return true;
      }
    }
    isOscillating = false;
    oscAdding = false;
    oscPoints.clear();
    return iters > 0;
  }

  private SetData removeCause(CircuitState state, SetData head, Location loc, Component cause) {
    final var causes = state.causes;
    if (head == null) {
    } else if (head.cause == cause) {
      head = head.next;
      if (head == null) causes.remove(loc);
      else causes.put(loc, head);
    } else {
      var prev = head;
      var cur = head.next;
      while (cur != null) {
        if (cur.cause == cause) {
          prev.next = cur.next;
          break;
        }
        prev = cur;
        cur = cur.next;
      }
    }
    return head;
  }

  void reset() {
    halfClockCycles = 0;
    toProcess.clear();
    root.reset();
    isOscillating = false;
  }

  //
  // package-protected helper methods
  //
  void setValue(CircuitState state, Location pt, Value val, Component cause, int delay) {
    if (cause instanceof Wire || cause instanceof Splitter) return;
    if (delay <= 0) {
      delay = 1;
    }
    final var randomShift = simRandomShift;
    if (randomShift > 0) { // random noise is turned on
      // multiply the delay by 32 so that the random noise
      // only changes the delay by 3%.
      delay <<= randomShift;
      if (!(cause.getFactory() instanceof SubcircuitFactory)) {
        if (noiseCount > 0) {
          noiseCount--;
        } else {
          delay++;
          noiseCount = noiseSource.nextInt(1 << randomShift);
        }
      }
    }
    toProcess.add(new SetData(clock + delay, setDataSerialNumber, state, pt, cause, val));
    /*
     * DEBUGGING - comment out Simulator.log(clock + ": set " + pt + " in "
     * + state + " to " + val + " by " + cause + " after " + delay); //
     */

    setDataSerialNumber++;
  }

  boolean step(PropagationPoints changedPoints) {
    oscPoints.clear();
    root.processDirtyPoints();
    root.processDirtyComponents();

    if (toProcess.isEmpty()) return false;

    final var oldOsc = oscPoints;
    oscAdding = changedPoints != null;
    oscPoints = changedPoints;
    stepInternal(changedPoints);
    oscAdding = false;
    oscPoints = oldOsc;
    return true;
  }

  private void stepInternal(PropagationPoints changedPoints) {
    if (toProcess.isEmpty()) return;

    // update clock
    clock = toProcess.peek().time;

    // propagate all values for this clock tick
    final var visited = new HashMap<CircuitState, HashSet<ComponentPoint>>();
    while (true) {
      final var data = toProcess.peek();
      if (data == null || data.time != clock) break;
      toProcess.remove();
      final var state = data.state;

      // if it's already handled for this clock tick, continue
      var handled = visited.get(state);
      if (handled != null) {
        if (!handled.add(new ComponentPoint(data.cause, data.loc))) continue;
      } else {
        handled = new HashSet<>();
        visited.put(state, handled);
        handled.add(new ComponentPoint(data.cause, data.loc));
      }

      /*
       * DEBUGGING - comment out Simulator.log(data.time + ": proc " +
       * data.loc + " in " + data.state + " to " + data.val + " by " +
       * data.cause); //
       */

      if (changedPoints != null) changedPoints.add(state, data.loc);

      // change the information about value
      final var oldHead = state.causes.get(data.loc);
      final var oldVal = computeValue(oldHead);
      final var newHead = addCause(state, oldHead, data);
      final var newVal = computeValue(newHead);

      // if the value at point has changed, propagate it
      if (!newVal.equals(oldVal)) {
        state.markPointAsDirty(data.loc);
      }
    }

    root.processDirtyPoints();
    root.processDirtyComponents();
  }

  public boolean toggleClocks() {
    halfClockCycles++;
    return root.toggleClocks(halfClockCycles);
  }

  @Override
  public String toString() {
    return "Prop" + id;
  }

  private void updateRandomness() {
    final var opts = root.getProject().getOptions();
    Object rand = opts.getAttributeSet().getValue(Options.ATTR_SIM_RAND);
    final var val = (Integer) rand;
    var logVal = 0;
    while ((1 << logVal) < val) logVal++;
    simRandomShift = logVal;
  }
}
