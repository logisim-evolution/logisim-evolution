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
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import java.lang.ref.WeakReference;
//import java.util.PriorityQueue;
import java.util.Random;

public class Propagator {
  static class ComponentPoint {
    final Component cause;
    final Location loc;

    public ComponentPoint(Component cause, Location loc) {
      this.cause = cause;
      this.loc = loc;
    }

    @Override
    public boolean equals(Object other) {
      return (other instanceof ComponentPoint o)
             ? this.cause.equals(o.cause) && this.loc.equals(o.loc)
             : false;
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
    public void attributeListChanged(AttributeEvent e) {
      // do nothing
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      final var p = prop.get();
      if (p == null) {
        e.getSource().removeAttributeListener(this);
      } else if (e.getAttribute().equals(Options.ATTR_SIM_RAND)) {
        p.updateRandomness();
      } else if (e.getAttribute().equals(Options.ATTR_SIM_LIMIT)) {
        p.updateSimLimit();
      }
    }
  }

  static class DrivenValue {
    DrivenValue next; // linked list
    final Component driver;
    Value val;
    DrivenValue(Component c, Value v) {
      driver = c;
      val = v;
    }
  }

  private static class SimulatorEvent extends SplayQueue.Node
      implements Comparable<SimulatorEvent> {

    final int time;
    int serialNumber; // used to make the times unique
    final CircuitState state; // state of circuit containing component
    final Location loc; // the location at which value is emitted
    final Component cause; // component emitting the value
    Value val; // value being emitted

    private SimulatorEvent(int time, int serialNumber,
                           CircuitState state, Location loc, Component cause, Value val) {
      super(((long) time << 32) | (serialNumber & 0xFFFFFFFFL));
      this.time = time;
      this.serialNumber = serialNumber;
      this.state = state;
      this.cause = cause;
      this.loc = loc;
      this.val = val;
    }

    public SimulatorEvent cloneFor(CircuitState newState) {
      final var newProp = newState.getPropagator();
      final var dtime = newProp.clock - state.getPropagator().clock;
      SimulatorEvent ret = new SimulatorEvent(time + dtime,
          newProp.eventSerialNumber++, newState, loc, cause, val);
      return ret;
    }

    @Override
    public int compareTo(SimulatorEvent o) {
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

  // This one is only used to initialize  TODO: can we eliminate this... is it used only when initializing BundleMap?
  static Value getDrivenValueAt(CircuitState circState, Location p) {
    // for CircuitWires - to get values, ignoring wires' contributions
    DrivenValue vals;
    synchronized (circState.valuesLock) {
      vals = circState.slowpath_drivers.get(p);
    }
    return computeValue(vals);
  }

  static Value computeValue(DrivenValue vals) {
    if (vals == null)
      return Value.NIL;
    Value ret = vals.val;
    for (DrivenValue v = vals.next; v != null; v = v.next)
      ret = ret.combine(v.val);
    return ret;
  }

  static void copyDrivenValues(CircuitState dest, CircuitState src) {
    // note: we don't bother with our this.valuesLock here: it isn't needed
    // (b/c no other threads have a reference to this yet), and to avoid the
    // possibility of deadlock (though that shouldn't happen either since no
    // other threads have references to this yet).
    dest.slowpath_drivers.clear();
    synchronized (src.valuesLock)  {
      for (Location loc : src.slowpath_drivers.keySet()) {
        DrivenValue v = src.slowpath_drivers.get(loc);
        DrivenValue n = new DrivenValue(v.driver, v.val);
        dest.slowpath_drivers.put(loc, n);
        while (v.next != null) {
          n.next = new DrivenValue(v.next.driver, v.next.val);
          v = v.next;
          n = n.next;
        }
      }
    }
  }

  private final CircuitState root; // root of state tree

  /** The number of clock cycles to let pass before deciding that the circuit is oscillating. */
  private volatile int simLimit;

  /**
   * On average, one out of every 2**simRandomShift propagations through a component is delayed one
   * step more than the component requests. This noise is intended to address some circuits that
   * would otherwise oscillate within Logisim (though they wouldn't oscillate in practice).
   */
  private volatile int simRandomShift;

  // private PriorityQueue<SimulatorEvent> toProcess = new PriorityQueue<>();
  private SplayQueue<SimulatorEvent> toProcess = new SplayQueue<>();
  //private LinkedQueue<SimulatorEvent> toProcess = new LinkedQueue<>();
  private int clock = 0;
  private boolean isOscillating = false;
  private boolean oscAdding = false;
  private PropagationPoints oscPoints = new PropagationPoints();
  private int halfClockCycles = 0;
  private final Random noiseSource = new Random();
  private int noiseCount = 0;

  private int eventSerialNumber = 0;
  static int lastId = 0;

  final int id = lastId++;

  public Propagator(CircuitState root) {
    this.root = root;
    final var l = new Listener(this);
    root.getProject().getOptions().getAttributeSet().addAttributeListener(l);
    updateRandomness();
    updateSimLimit();
  }

  // precondition: state.valuesLock held
  private static DrivenValue addCause(CircuitState state, DrivenValue head,
                                      Location loc, Component cause, Value val) {
    if (val == null) // actually, it should be removed
      return removeCause(state, head, loc, cause);

    // first check whether this is change of previous info
    for (DrivenValue n = head; n != null; n = n.next) {
      if (n.driver == cause) {
        n.val = val;
        return head;
      }
    }

    // otherwise, insert into list of causes
    DrivenValue n = new DrivenValue(cause, val);
    if (head == null) {
      head = n;
      state.slowpath_drivers.put(loc, head);
    } else {
      n.next = head.next;
      head.next = n;
    }

    return head;
  }

  public void drawOscillatingPoints(ComponentDrawContext context) {
    if (isOscillating) oscPoints.draw(context);
  }

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

  void locationTouched(CircuitState state, Location loc) {
    if (oscAdding) oscPoints.add(state, loc);
  }

  public boolean propagate() {
    return propagate(null, null);
  }

  /**
   * Safe to call from sim thread
   */
  public boolean propagate(Simulator.ProgressListener propListener, Simulator.Event propEvent) {
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

  // precondition: state.valuesLock held
  private static DrivenValue removeCause(CircuitState state, DrivenValue head,
                                         Location loc, Component cause) {
    if (head == null)
      return null;

    if (head.driver == cause) {
      head = head.next;
      if (head == null)
        state.slowpath_drivers.remove(loc);
      else
        state.slowpath_drivers.put(loc, head);
    } else {
      DrivenValue prev = head;
      DrivenValue cur = head.next;
      while (cur != null) {
        if (cur.driver == cause) {
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
    toProcess.add(new SimulatorEvent(clock + delay, eventSerialNumber, state, pt, cause, val));
    /*
     * DEBUGGING - comment out Simulator.log(clock + ": set " + pt + " in "
     * + state + " to " + val + " by " + cause + " after " + delay); //
     */

    eventSerialNumber++;
  }

  /**
   * Safe to call from sim thread
   */
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

  int visitedNonce = 1;
  private void stepInternal(PropagationPoints changedPoints) {
    if (toProcess.isEmpty()) return;

    // update clock
    clock = toProcess.peek().time;
    visitedNonce++; // used to ensure a fresh circuitState.visited set.

    // propagate all values for this clock tick
    while (true) {
      SimulatorEvent ev = toProcess.peek();
      if (ev == null || ev.time != clock) break;
      toProcess.remove();
      CircuitState state = ev.state;

      // if it's already handled for this clock tick, continue
      if (state.visitedNonce != visitedNonce) {
        // first time visiting this circuitState during this call to stepInternal
        state.visitedNonce = visitedNonce;
        state.visited.clear();
      }
      if (!state.visited.add(new ComponentPoint(ev.cause, ev.loc)))
        continue; // this component+loc change has already been handled

      // DEBUGGING
      // System.out.printf("%s: proc %s in %s to %s by %s\n",
      //     ev.time, ev.loc, ev.state, ev.val, ev.cause);

      if (changedPoints != null) changedPoints.add(state, ev.loc);

      // change the information about value
      Value oldVal, newVal;
      synchronized (state.valuesLock) {
        DrivenValue oldHead = state.slowpath_drivers.get(ev.loc);
        oldVal = computeValue(oldHead);
        DrivenValue newHead = addCause(state, oldHead, ev.loc, ev.cause, ev.val);
        newVal = computeValue(newHead);
      }

      // if the value at point has changed, propagate it
      if (!newVal.equals(oldVal)) {
        state.markPointAsDirty(ev.loc, newVal);
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
    final var rand = opts.getAttributeSet().getValue(Options.ATTR_SIM_RAND);
    final var val = rand;
    var logVal = 0;
    while ((1 << logVal) < val) logVal++;
    simRandomShift = logVal;
  }

  private void updateSimLimit() {
    final var opts = root.getProject().getOptions();
    final var lim = opts.getAttributeSet().getValue(Options.ATTR_SIM_LIMIT);
    simLimit = lim;
  }
}
