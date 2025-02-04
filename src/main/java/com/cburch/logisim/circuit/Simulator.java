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
import com.cburch.logisim.gui.log.ClockSource;
import com.cburch.logisim.gui.log.ComponentSelector;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;
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

  public static interface StatusListener {
    public void simulatorReset(Event e);
    public void simulatorStateChanged(Event e);
  }

  public static interface Listener extends StatusListener {
    public void propagationCompleted(Event e);
  }

  public static interface ProgressListener extends Listener {
    public boolean wantsProgressEvents();
    public void propagationInProgress(Event e);
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

    private ReentrantLock simStateLock = new ReentrantLock();
    private Condition simStateUpdated = simStateLock.newCondition();
    // NOTE: These variables must only be accessed with lock held.
    private Propagator propagator = null;
    private boolean autoPropagating = true;
    private boolean autoTicking = false;
    private double autoTickFreq = 1.0; // Hz
    private int smoothingFactor = 1; // for WEMA
    private long autoTickNanos = Math.round(1.0e9 / autoTickFreq);
    private int manualTicksRequested = 0;
    private int manualStepsRequested = 0;
    private boolean nudgeRequested = false;
    private boolean resetRequested = false;
    private boolean complete = false;
    private double avgTickNanos = -1.0;

    // These are copies of some of the above variables that can be read without
    // the lock if synchronization with other variables is not needed.
    private volatile Propagator propagatorUnsynchronized = null;
    private volatile boolean autoPropagatingUnsynchronized = true;
    private volatile boolean autoTickingUnsynchronized = false;
    private volatile double autoTickFreqUnsynchronized = 1.0; // Hz

    // These next ones are written only by the simulation thread, and read by
    // the repaining thread. They can be read without locks as they do not need
    // to be kept consistent with other variables.
    private volatile boolean exceptionEncountered = false;
    private volatile boolean oscillating = false;

    // This last one should be made thread-safe, but it isn't for now.
    private final PropagationPoints stepPoints = new PropagationPoints();

    // lastTick is used only within loop() by a single thread.
    // No synchronization needed.
    private long lastTick = System.nanoTime(); // time of last propagation start

    SimThread(Simulator s) {
      super("SimThread");
      sim = s;
    }

    Propagator getPropagatorUnsynchronized() {
      return propagatorUnsynchronized;
    }

    boolean isAutoTickingUnsynchronized() {
      return autoTickingUnsynchronized;
    }

    boolean isAutoPropagatingUnsynchronized() {
      return autoPropagatingUnsynchronized;
    }

    double getTickFrequencyUnsynchronized() {
      return autoTickFreqUnsynchronized;
    }

    void drawStepPoints(ComponentDrawContext context) {
      if (!autoPropagatingUnsynchronized) {
        stepPoints.draw(context);
      }
    }

    void drawPendingInputs(ComponentDrawContext context) {
      if (!autoPropagatingUnsynchronized) {
        stepPoints.drawPendingInputs(context);
      }
    }

    void addPendingInput(CircuitState state, Component comp) {
      if (!autoPropagatingUnsynchronized) {
        stepPoints.addPendingInput(state, comp);
      }
    }

    synchronized String getSingleStepMessage() {
      return autoPropagatingUnsynchronized ? "" : stepPoints.getSingleStepMessage();
    }

    boolean setPropagator(Propagator prop) {
      var smoothFactor = 1;
      if (prop != null) {
        final var opts = prop.getRootState().getProject().getOptions();
        //smoothFactor = opts.getAttributeSet().getValue(Options.ATTR_SIM_SMOOTH); #TODO: implement smooth factor
        if (smoothFactor < 1) {
          smoothFactor = 1;
        }
      }
      simStateLock.lock();
      try {
        if (propagator == prop) {
          return false;
        }
        propagator = prop;
        propagatorUnsynchronized = prop;
        smoothingFactor = smoothFactor;
        manualTicksRequested = 0;
        manualStepsRequested = 0;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
        return true;
      } finally {
        simStateLock.unlock();
      }
    }

    boolean setAutoPropagation(boolean value) {
      simStateLock.lock();
      try {
        if (autoPropagating == value) {
          return false;
        }
        autoPropagating = value;
        autoPropagatingUnsynchronized = value;
        if (autoPropagating) {
          manualStepsRequested = 0;
        } else {
          nudgeRequested = false;
        }
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
        return true;
      } finally {
        simStateLock.unlock();
      }
    }

    boolean setAutoTicking(boolean value) {
      simStateLock.lock();
      try {
        if (autoTicking == value) {
          return false;
        }
        autoTicking = value;
        autoTickingUnsynchronized = value;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
        return true;
      } finally {
        simStateLock.unlock();
      }
    }

    boolean setTickFrequency(double freq) {
      simStateLock.lock();
      try {
        if (autoTickFreq == freq) {
          return false;
        }
        autoTickFreq = freq;
        autoTickFreqUnsynchronized = freq;
        autoTickNanos = freq <= 0 ? 0 : Math.round(1.0e9 / autoTickFreq);
        avgTickNanos = -1.0;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
        return true;
      } finally {
        simStateLock.unlock();
      }
    }

    void requestStep() {
      simStateLock.lock();
      try {
        manualStepsRequested++;
        autoPropagating = false;
        autoPropagatingUnsynchronized = false;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
      } finally {
        simStateLock.unlock();
      }
    }

    void requestTick(int count) {
      simStateLock.lock();
      try {
        manualTicksRequested += count;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
      } finally {
        simStateLock.unlock();
      }
    }

    void requestReset() {
      simStateLock.lock();
      try {
        resetRequested = true;
        manualTicksRequested = 0;
        manualStepsRequested = 0;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
      } finally {
        simStateLock.unlock();
      }
    }

    boolean requestNudge() {
      simStateLock.lock();
      try {
        if (!autoPropagating) {
          return false;
        }
        nudgeRequested = true;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
        return true;
      } finally {
        simStateLock.unlock();
      }
    }

    void requestShutDown() {
      simStateLock.lock();
      try {
        complete = true;
        if (Thread.currentThread() != this) {
          simStateUpdated.signalAll();
        }
      } finally {
        simStateLock.unlock();
      }
    }

    private boolean loop() {

      Propagator prop = null;
      var doReset = false;
      var doNudge = false;
      var doTick = false;
      var doTickIfStable = false;
      var doStep = false;
      var doProp = false;
      var now = 0L;

      simStateLock.lock();

      try {
        var ready = false;
        do {
          if (complete) {
            return false;
          }

          prop = propagator;
          now = System.nanoTime();

          if (resetRequested) {
            resetRequested = false;
            doReset = true;
            doProp = autoPropagating;
            ready = true;
          } else if (nudgeRequested) {
            nudgeRequested = false;
            doNudge = true;
            ready = true;
          } else if (manualStepsRequested > 0) {
            manualStepsRequested--;
            doTickIfStable = autoTicking;
            doStep = true;
            ready = true;
          } else if (manualTicksRequested > 0) {
            // variable is decremented below
            doTick = true;
            doProp = autoPropagating;
            doStep = !autoPropagating;
            ready = true;
          } else {
            if (autoTicking && autoPropagating && autoTickNanos > 0) {
              // see if it is time to do an auto-tick
              final var smooth = smoothingFactor;
              final var lastNanos = now - lastTick;
              if (avgTickNanos <= 0) {
                avgTickNanos = autoTickNanos;
                doTick = true;
                doProp = true;
                ready = true;
              } else {
                final var avg = ((smooth - 1.0) / smooth) * avgTickNanos + (1.0 / smooth) * lastNanos;
                final var deadline = lastTick + autoTickNanos - (long) ((smooth - 1) * (avgTickNanos - autoTickNanos));
                final var delta = deadline - now;
                if (delta <= 1000) {
                  avgTickNanos = avg;
                  doTick = true;
                  doProp = true;
                  ready = true;
                } else if (delta < 1000000) {
                  simStateLock.unlock();
                  try {
                    var time = 0L;
                    do {
                      time = System.nanoTime();
                    } while (time < deadline);
                  } finally {
                    simStateLock.lock();
                  }
                } else {
                  try {
                    simStateUpdated.awaitNanos(delta);
                  } catch (InterruptedException e) {
                    // Do Nothing
                  }
                }
              }
            } else {
              avgTickNanos = -1.0;
              try {
                simStateUpdated.await();
              } catch (InterruptedException e) {
                // Do Nothing
              }
            }
          }
        } while (!ready);
      } finally {
        simStateLock.unlock();
      }

      // DEBUGGING
      // System.out.printf("%d nudge %s tick %s prop %s step %s\n", cnt++, doNudge, doTick, doProp,
      // doStep);

      exceptionEncountered = false;

      var oops = false;
      var osc = false;
      var ticked = false;
      var stepped = false;
      var propagated = false;
      var hasClocks = true;

      if (doReset) {
        try {
          stepPoints.clear();
          if (prop != null) {
            prop.reset();
          }
          sim.fireSimulatorReset(); // TODO: fixme: ack, wrong thread!
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }
      }

      if (doTick || (doTickIfStable && prop != null && !prop.isPending())) {
        lastTick = now;
        ticked = true;
        if (prop != null) {
          hasClocks = prop.toggleClocks();
        }
      }

      if (doProp || doNudge) {
        try {
          propagated = doProp;
          final var listener = sim.progressListener;
          final var evt = listener == null ? null : new Event(sim, false, false, false);
          stepPoints.clear();
          if (prop != null) {
            propagated |= prop.propagate(listener, evt);
          }
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }
      }

      if (doStep) {
        try {
          stepped = true;
          stepPoints.clear();
          if (prop != null) {
            prop.step(stepPoints);
          }
          if (prop == null || !prop.isPending()) {
            propagated = true;
          }
        } catch (Exception err) {
          oops = true;
          err.printStackTrace();
        }
      }

      osc = prop != null && prop.isOscillating();

      var clockDied = false;
      exceptionEncountered = oops;
      oscillating = osc;
      simStateLock.lock();
      try {
        if (osc) {
          autoPropagating = false;
          autoPropagatingUnsynchronized = false;
          nudgeRequested = false;
        }
        if (ticked && manualTicksRequested > 0) {
          manualTicksRequested--;
        }
        if (autoTicking && !hasClocks) {
          autoTicking = false;
          autoTickingUnsynchronized = false;
          clockDied = true;
        }
      } finally {
        simStateLock.unlock();
      }

      // We report nudges, but we report them as no-ops, unless they were
      // accompanied by a tick, step, or propagate. That allows for a repaint in
      // some components.
      if (ticked || stepped || propagated || doNudge) {
        sim.firePropagationCompleted(ticked, stepped && !propagated, propagated); // FIXME: ack, wrong thread!
      }
      if (clockDied) {
        sim.fireSimulatorStateChanged(); // FIXME: ack, wrong thread!
      }
      return true;
    }

    @Override
    public void run() {
      for (;;) {
        try {
          if (!loop()) {
            return;
          }
        } catch (Throwable e) {
          e.printStackTrace();
          exceptionEncountered = true;
          simStateLock.lock();
          try {
            autoPropagating = false;
            autoPropagatingUnsynchronized = false;
            autoTicking = false;
            autoTickingUnsynchronized = false;
            manualTicksRequested = 0;
            manualStepsRequested = 0;
            nudgeRequested = false;
          } finally {
            simStateLock.unlock();
          }
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                JOptionPane.showMessageDialog(null, "The simulator crashed. Save your work and restart Logisim.");
              }
              // TODO: Hardcoded String
            });
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
  private final ArrayList<StatusListener> statusListeners = new ArrayList<>();
  private ArrayList<Listener> activityListeners = new ArrayList<>();
  private volatile ProgressListener progressListener = null;
  private final Object lock = new Object();
  private volatile int numListeners = 0;
  private volatile Listener[] listeners = new Listener[10];

  public Simulator() {
    simThread = new SimThread(this);

    try {
      simThread.setPriority(simThread.getPriority() - 1);
    } catch (IllegalArgumentException | SecurityException ignored) {
      // do nothing
    }

    simThread.start();

    setTickFrequency(AppPreferences.TICK_FREQUENCY.get());
  }

  public void addSimulatorListener(StatusListener listener) {
    if (listener instanceof Listener) {
      synchronized (lock) {
        statusListeners.add(listener);
        activityListeners.add((Listener) listener);
        if (numListeners >= 0) {
          if (numListeners >= listeners.length) {
            final var newArray = new Listener[2 * listeners.length];
            for (var idx = 0; idx < numListeners; idx++) {
              newArray[idx] = listeners[idx];
            }
            listeners = newArray;
          }
          listeners[numListeners] = (Listener) listener;
          numListeners++;
        }
        if (listener instanceof ProgressListener) {
          if (progressListener != null) {
            throw new IllegalStateException("only one chronogram listener supported");
          }
          progressListener = (ProgressListener) listener;
        }
      }
    } else {
      synchronized (lock) {
        statusListeners.add(listener);
      }
    }
  }

  public void removeSimulatorListener(StatusListener listener) {
    if (listener instanceof Listener) {
      synchronized (lock) {
        if (listener == progressListener) {
          progressListener = null;
        }
        statusListeners.remove(listener);
        activityListeners.remove((Listener) listener);
        numListeners = -1;
      }
    } else {
      synchronized (lock) {
        statusListeners.remove(listener);
      }
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

  private ArrayList<StatusListener> copyStatusListeners() {
    ArrayList<StatusListener> copy;
    synchronized (lock) {
      copy = new ArrayList<>(statusListeners);
    }
    return copy;
  }

  // called from simThread, but probably should not be
  private void fireSimulatorReset() {
    final var event = new Event(this, false, false, false);
    for (final var listener : copyStatusListeners()) {
      listener.simulatorReset(event);
    }
  }

  // called from simThread, but probably should not be
  private void firePropagationCompleted(boolean t, boolean s, boolean p) {
    final var event = new Event(this, t, s, p);
    var nrListeners = numListeners;
    if (nrListeners < 0) {
      synchronized (lock) {
        nrListeners = activityListeners.size();
        if (nrListeners > listeners.length) {
          listeners = new Listener[2 * nrListeners];
        }
        for (var idx = 0; idx < nrListeners; idx++) {
          listeners[idx] = activityListeners.get(idx);
        }
        for (var idx = nrListeners; idx < listeners.length; idx++) {
          listeners[idx] = null;
        }
        numListeners = nrListeners;
      }
    }
    if (nrListeners == 0) {
      return;
    }
    for (var idx = 0; idx < nrListeners; idx++) {
      listeners[idx].propagationCompleted(event);
    }
  }

  // called only from gui thread, but need copy here anyway because listeners
  // can add/remove from listeners list?
  private void fireSimulatorStateChanged() {
    final var event = new Event(this, false, false, false);
    for (final var listener : copyStatusListeners()) {
      listener.simulatorStateChanged(event);
    }
  }

  public double getTickFrequency() {
    return simThread.getTickFrequencyUnsynchronized();
  }

  public boolean isExceptionEncountered() {
    return simThread.exceptionEncountered;
  }

  public boolean isOscillating() {
    return simThread.oscillating;
  }

  public CircuitState getCircuitState() {
    final var prop = simThread.getPropagatorUnsynchronized();
    return prop == null ? null : prop.getRootState();
  }

  public boolean isAutoPropagating() {
    return simThread.isAutoPropagatingUnsynchronized();
  }

  public boolean isAutoTicking() {
    return simThread.isAutoTickingUnsynchronized();
  }

  public void setCircuitState(CircuitState state) {
    if (simThread.setPropagator(state == null ? null : state.getPropagator()))
      fireSimulatorStateChanged();
  }

  public void setAutoPropagation(boolean value) {
    if (simThread.setAutoPropagation(value)) fireSimulatorStateChanged();
  }

  public void setAutoTicking(boolean value) {
    if (value && !ensureClocks()) return;
    if (simThread.setAutoTicking(value)) fireSimulatorStateChanged();
  }

  public void setTickFrequency(double freq) {
    if (simThread.setTickFrequency(freq)) fireSimulatorStateChanged();
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
    if (cs == null) {
      return false;
    }
    if (cs.hasKnownClocks()) {
      return true;
    }
    final var circ = cs.getCircuit();
    final var clocks = ComponentSelector.findClocks(circ);
    if (CollectionUtil.isNotEmpty(clocks)) {
      cs.markKnownClocks();
      return true;
    }

    final var clk = ClockSource.doClockDriverDialog(circ);
    if (clk == null || !cs.setTemporaryClock(clk)) return false;

    fireSimulatorStateChanged();
    return true;
  }
}
