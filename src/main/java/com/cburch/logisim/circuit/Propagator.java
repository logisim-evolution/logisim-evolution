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
import com.cburch.logisim.util.QNodeQueue;
import com.cburch.logisim.util.SplayQueue;
import com.cburch.logisim.util.QNode;
import java.lang.ref.WeakReference;
import java.util.PriorityQueue;
import java.util.Random;

public class Propagator {
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

  public static class SimulatorEvent extends QNode
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

  private final CircuitState root; // root of state tree

  /** The number of clock cycles to let pass before deciding that the circuit is oscillating. */
  private volatile int simLimit;

  /**
   * On average, one out of every 2**simRandomShift propagations through a component is delayed one
   * step more than the component requests. This noise is intended to address some circuits that
   * would otherwise oscillate within Logisim (though they wouldn't oscillate in practice).
   */
  private volatile int simRandomShift;

  private class PriorityEventQueue<T extends QNode> extends PriorityQueue<T> implements QNodeQueue<T> {
  }

  // The simulator event queue can be implemented by a PriorityEventQueue,
  // SplayQueue, or LinkedQueue. LinkedQueue seems fastest in practice, though
  // it has poor worst-case performance. SplayQueue should have good
  // expected-case performance, but it seems a bit slower than LinkedQueue
  // for simpler simulations. SplayQueue is best for complex simulations.
  // PriorityEventQueue, using Java's PriorityQueue, seems slightly worse than the
  // others. It is trivial to switch between the implementations, just change the
  // object to a new one of: SplayQueue, LinkedQueue, or PriorityEventQueue.

  private QNodeQueue<SimulatorEvent> toProcess = new SplayQueue<SimulatorEvent>();

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

  private void stepInternal(PropagationPoints changedPoints) {
    if (toProcess.isEmpty()) return;

    // update clock
    clock = toProcess.peek().time;

    // propagate all values for this clock tick
    while (true) {
      SimulatorEvent ev = toProcess.peek();
      if (ev == null || ev.time != clock) break;
      toProcess.remove();
      CircuitState state = ev.state;

      if (changedPoints != null) changedPoints.add(state, ev.loc);

      // if the value at point has changed, propagate it
      state.markPointAsDirty(ev); // ev.loc, ev.cause, ev.val);
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
