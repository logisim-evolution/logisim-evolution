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
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.log.ClockSource;
import com.cburch.logisim.gui.log.ComponentSelector;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class Simulator {

  public static class Event {
    private final Simulator source;
    private final boolean didTick;
    private final boolean didSingleStep;
    private final boolean didPropagate;

    public Event(Simulator src, boolean t, boolean s, boolean p) {
      source = src;
      didTick = t;
      didSingleStep = s;
      didPropagate = p;
    }

    public Simulator getSource() {
      return source;
    }

    public boolean didTick() {
      return didTick;
    }

    public boolean didSingleStep() {
      return didSingleStep;
    }

    public boolean didPropagate() {
      return didPropagate;
    }
  }

  public interface Listener {
    void simulatorReset(Event e);

    default boolean wantProgressEvents() {
      return false;
    }

    default void propagationInProgress(Event e) {}

    void propagationCompleted(Event e);

    void simulatorStateChanged(Event e);
  }

  // This thread keeps track of the current stepPoints (when running in step
  // mode), and it invokes various Propagator methods:
  //
  //     propagator.reset() -- clears all signal values
  //     propagator.toggleClocks() -- toggles clock components
  //     propagator.propagate() -- auto-propagates until signals are stable
  //     propagator.step(stepPoints) -- propagates a single step
  //     propagator.isPending() -- checks if more signal changes are pending
  //
  // The thread will invoked these in response to various events:
  //
  // [auto-tick]   If autoTicking is on and autoPropagation is on, the thread
  //               periodically wakes up and invokes toggleClocks() then
  //               propagate().
  //
  // [manual-tick] If the User/GUI requests a tick happen and autoPropagation is
  //               on, the thread wakes up and invokes toggleClocks() then
  //               propagate(). If autoPropagation is off, thread will wake up
  //               and call toggleClocks() then step().
  //
  // [nudge]       If the user makes a circuit change, the thread wakes up and
  //               invokes propagate() or step().
  //
  // [reset]       If the User/GUI requests a reset, the thread wakes up and
  //               invokes reset() and maybe also propagate().
  //
  // [single-step] If the User/GUI requests a single-step propagation (this
  //               only happens when autoTicking is off), the thread wakes up
  //               and invokes step(). If if autoTicking is on and signals are
  //               stable, then toggleClocks() is also called before step().
  private static class SimThread extends UniquelyNamedThread {

    private final Simulator sim;
    private long lastTick = System.nanoTime();

    // NOTE: These variables must only be accessed with lock held.
    private Propagator propagator = null;
    private boolean isAutoPropagating = true;
    private boolean isAutoTicking = false;
    private double autoTickFreq = 1.0; // Hz
    private long autoTickNanos = Math.round(1e9 / (2 * autoTickFreq));
    private int manualTicksRequested = 0;
    private int manualStepsRequested = 0;
    private boolean isNudgeRequested = false;
    private boolean isResetRequested = false;
    private boolean isComplete = false;
    private boolean isOops = false;

    // This last one should be made thread-safe, but it isn't for now.
    private final PropagationPoints stepPoints = new PropagationPoints();

    SimThread(Simulator s) {
      super("SimThread");
      sim = s;
    }

    synchronized Propagator getPropagator() {
      return propagator;
    }

    synchronized boolean isExceptionEncountered() {
      return isOops;
    }

    synchronized boolean isAutoTicking() {
      return isAutoTicking;
    }

    synchronized boolean isAutoPropagating() {
      return isAutoPropagating;
    }

    synchronized double getTickFrequency() {
      return autoTickFreq;
    }

    synchronized void drawStepPoints(ComponentDrawContext context) {
      if (!isAutoPropagating) stepPoints.draw(context);
    }

    synchronized void drawPendingInputs(ComponentDrawContext context) {
      if (!isAutoPropagating)
        stepPoints.drawPendingInputs(context);
    }

    synchronized void addPendingInput(CircuitState state, Component comp) {
      stepPoints.addPendingInput(state, comp);
    }

    synchronized String getSingleStepMessage() {
      return isAutoPropagating ? "" : stepPoints.getSingleStepMessage();
    }

    synchronized boolean setPropagator(Propagator value) {
      if (propagator == value)
        return false;
      propagator = value;
      manualTicksRequested = 0;
      manualStepsRequested = 0;
      notifyAll();
      return true;
    }

    synchronized boolean setAutoPropagation(boolean value) {
      if (isAutoPropagating == value)
        return false;
      isAutoPropagating = value;
      if (isAutoPropagating)
        manualStepsRequested = 0; // manual steps not allowed in autoPropagating mode
      else
        isNudgeRequested = false; // nudges not allowed in single-step mode
      notifyAll();
      return true;
    }

    synchronized boolean setAutoTicking(boolean value) {
      if (isAutoTicking == value)
        return false;
      isAutoTicking = value;
      notifyAll();
      return true;
    }

    synchronized boolean setTickFrequency(double freq) {
      if (autoTickFreq == freq)
        return false;
      autoTickFreq = freq;
      autoTickNanos = freq <= 0 ? 0 : Math.round(1e9 / (2 * autoTickFreq));
      notifyAll();
      return true;
    }

    synchronized void requestStep() {
      manualStepsRequested++;
      isAutoPropagating = false;
      notifyAll();
    }

    synchronized void requestTick(int count) {
      manualTicksRequested += count;
      notifyAll();
    }

    synchronized void requestReset() {
      isResetRequested = true;
      manualTicksRequested = 0;
      manualStepsRequested = 0;
      notifyAll();
    }

    synchronized boolean requestNudge() {
      if (!isAutoPropagating)
        return false;
      isNudgeRequested = true;
      notifyAll();
      return true;
    }

    synchronized void requestShutDown() {
      isComplete = true;
      notifyAll();
    }

    private boolean loop() {

      Propagator prop = null;
      boolean doReset = false;
      boolean doNudge = false;
      boolean doTick = false;
      boolean doTickIfStable = false;
      boolean doStep = false;
      boolean doProp = false;
      long now = 0;

      synchronized (this) {
        boolean ready = false;
        do {
          if (isComplete) return false;

          prop = propagator;
          now = System.nanoTime();

          if (isResetRequested) {
            isResetRequested = false;
            doReset = true;
            doProp = isAutoPropagating;
            ready = true;
          }
          if (isNudgeRequested) {
            isNudgeRequested = false;
            doNudge = true;
            ready = true;
          }
          if (manualStepsRequested > 0) {
            manualStepsRequested--;
            doTickIfStable = isAutoTicking;
            doStep = true;
            ready = true;
          }

          if (manualTicksRequested > 0) {
            // variable is decremented below
            doTick = true;
            doProp = isAutoPropagating;
            doStep = !isAutoPropagating;
            ready = true;
          }

          long delta = 0;
          if (isAutoTicking && isAutoPropagating && autoTickNanos > 0) {
            // see if it is time to do an auto-tick
            long deadline = lastTick + autoTickNanos;
            delta = deadline - now;
            if (delta <= 0) {
              doTick = true;
              doProp = true;
              ready = true;
            }
          }

          if (!ready) {
            // LockSupport.parkNanos(delta);
            try {
              if (delta > 0) wait(delta / 1000000, (int) (delta % 1000000));
              else wait();
            } catch (InterruptedException ignored) {
              // yes, we swallow the interrupt
            }
          }
        } while (!ready);

        isOops = false;
      }

      var oops = false;
      var ticked = false;
      var stepped = false;
      var propagated = false;
      var hasClocks = true;

      if (doReset)
        try {
          stepPoints.clear();
          if (prop != null) prop.reset();
          sim.fireSimulatorReset(); // todo: fixme: ack, wrong thread!
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }

      if (doTick || (doTickIfStable && prop != null && !prop.isPending())) {
        lastTick = now;
        ticked = true;
        if (prop != null) hasClocks = prop.toggleClocks();
      }

      if (doProp || doNudge)
        try {
          propagated = doProp;
          // todo: need to fire events in here for chrono fine grained
          final var p = sim.getPropagationListener();
          final var evt = p == null ? null : new Event(sim, false, false, false);
          stepPoints.clear();
          if (prop != null)
            propagated |=
                prop.propagate(p, evt); // todo: need to fire events in here for chrono fine grained
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }

      if (doStep)
        try {
          stepped = true;
          stepPoints.clear();
          if (prop != null) prop.step(stepPoints);
          if (prop == null || !prop.isPending()) propagated = true;
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }

      final var osc = prop != null && prop.isOscillating();

      var clockDied = false;
      synchronized (this) {
        isOops = oops;
        if (osc) {
          isAutoPropagating = false;
          isNudgeRequested = false;
        }
        if (ticked && manualTicksRequested > 0) manualTicksRequested--;
        if (isAutoTicking && !hasClocks) {
          isAutoTicking = false;
          clockDied = true;
        }
      }

      // We report nudges, but we report them as no-ops, unless they were
      // accompanied by a tick, step, or propagate. That allows for a repaint in
      // some components.
      if (ticked || stepped || propagated || doNudge)
        sim.firePropagationCompleted(
            ticked, stepped && !propagated, propagated); // FIXME ack, wrong thread!
      if (clockDied) sim.fireSimulatorStateChanged(); // FIXME ack, wrong thread!

      return true;
    }

    @Override
    public void run() {
      for (; ; ) {
        try {
          if (!loop()) return;
        } catch (Throwable e) {
          e.printStackTrace();
          synchronized (this) {
            isOops = true;
            isAutoPropagating = false;
            isAutoTicking = false;
            manualTicksRequested = 0;
            manualStepsRequested = 0;
            isNudgeRequested = false;
          }
          // FIXME we shall include at least exception message, otherwise it's too general error and is useless for debugging
          SwingUtilities.invokeLater(
              () ->
                  OptionPane.showMessageDialog(
                      null, "The simulator has crashed. Save your work and restart Logisim."));
        }
      }
    }
  }

  //
  // Everything below here is invoked and accessed only by the User/GUI thread.
  //

  private final SimThread simThread;

  // listeners is protected by a lock because simThread calls the _fire*()
  // methods, but the gui thread can call add/removeSimulateorListener() at any
  // time. Really, the _fire*() methods should be done on the gui thread, I
  // suspect.
  private final ArrayList<Listener> listeners = new ArrayList<>();
  private final Object lock = new Object();

  public Simulator() {
    simThread = new SimThread(this);

    try {
      simThread.setPriority(simThread.getPriority() - 1);
    } catch (IllegalArgumentException | SecurityException ignored) {
      // do nothing;
    }

    simThread.start();

    setTickFrequency(AppPreferences.TICK_FREQUENCY.get());
  }

  public void addSimulatorListener(Listener l) {
    synchronized (lock) {
      listeners.add(l);
    }
  }

  public void removeSimulatorListener(Listener l) {
    synchronized (lock) {
      listeners.remove(l);
    }
  }

  public void drawStepPoints(ComponentDrawContext context) {
    simThread.drawStepPoints(context);
  }

  public void drawPendingInputs(ComponentDrawContext context) {
    simThread.drawPendingInputs(context);
  }

  public String getSingleStepMessage() {
    return simThread.getSingleStepMessage();
  }

  public void addPendingInput(CircuitState state, Component comp) {
    simThread.addPendingInput(state, comp);
  }

  private ArrayList<Listener> copyListeners() {
    ArrayList<Listener> copy;
    synchronized (lock) {
      copy = new ArrayList<>(listeners);
    }
    return copy;
  }

  // called from simThread, but probably should not be
  private void fireSimulatorReset() {
    final var e = new Event(this, false, false, false);
    for (final var l : copyListeners())
      l.simulatorReset(e);
  }

  //called from simThread, but probably should not be
  private void firePropagationCompleted(boolean t, boolean s, boolean p) {
    final var e = new Event(this, t, s, p);
    for (final var l : copyListeners())
      l.propagationCompleted(e);
  }

  // called from simThread (via Propagator.propagate()), but probably should not be
  // void _firePropagationInProgress() {
  //   Event e = new Event(this, false, false, false);
  //   for (Listener l : copyListeners())
  //     l.propagationInProgress(e);
  // }
  // called from simThread, but probably should not be
  private Listener getPropagationListener() {
    Listener p = null;
    for (final var l : copyListeners()) {
      if (l.wantProgressEvents()) {
        if (p != null) throw new IllegalStateException("only one chronogram listener supported");
        p = l;
      }
    }
    return p;
  }

  // called only from gui thread, but need copy here anyway because listeners
  // can add/remove from listeners list?
  private void fireSimulatorStateChanged() {
    final var e = new Event(this, false, false, false);
    for (final var l : copyListeners()) l.simulatorStateChanged(e);
  }

  public double getTickFrequency() {
    return simThread.getTickFrequency();
  }

  public boolean isExceptionEncountered() {
    return simThread.isExceptionEncountered();
  }

  public boolean isOscillating() {
    Propagator prop = simThread.getPropagator();
    return prop != null && prop.isOscillating();
  }

  public CircuitState getCircuitState() {
    final var prop = simThread.getPropagator();
    return prop == null ? null : prop.getRootState();
  }

  public boolean isAutoPropagating() {
    return simThread.isAutoPropagating();
  }

  public boolean isAutoTicking() {
    return simThread.isAutoTicking();
  }

  public void setCircuitState(CircuitState state) {
    if (simThread.setPropagator(state == null ? null : state.getPropagator()))
      fireSimulatorStateChanged();
  }

  public void setAutoPropagation(boolean value) {
    if (simThread.setAutoPropagation(value))
      fireSimulatorStateChanged();
  }

  public void setAutoTicking(boolean value) {
    if (value && !ensureClocks())
      return;
    if (simThread.setAutoTicking(value))
      fireSimulatorStateChanged();
  }

  public void setTickFrequency(double freq) {
    if (simThread.setTickFrequency(freq))
      fireSimulatorStateChanged();
  }

  public void step() {
    simThread.requestStep();
  }

  public void tick(int count) {
    if (!ensureClocks()) return;
    simThread.requestTick(count);
  }

  // User/GUI manually requests a reset
  public void reset() {
    simThread.requestReset();
  }

  // Circuit changed, nudge the signals if needed to fix any pending changes
  public boolean nudge() {
    return simThread.requestNudge();
  }

  public void shutDown() {
    simThread.requestShutDown();
  }

  private boolean ensureClocks() {
    final var cs = getCircuitState();
    if (cs == null) return false;
    if (cs.hasKnownClocks()) return true;
    final var circ = cs.getCircuit();
    final var clocks = ComponentSelector.findClocks(circ);
    if (clocks != null && clocks.size() >= 1) {
      cs.markKnownClocks();
      return true;
    }

    final var clk = ClockSource.doClockDriverDialog(circ);
    if (clk == null || !cs.setTemporaryClock(clk))
      return false;
    fireSimulatorStateChanged();
    return true;
  }

}
