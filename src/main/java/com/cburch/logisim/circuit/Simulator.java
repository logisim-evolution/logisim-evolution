/**
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

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.util.ArrayList;

public class Simulator {

  class PropagationManager extends UniquelyNamedThread {

    private Propagator propagator = null;
    private PropagationPoints stepPoints = new PropagationPoints();
    private volatile int ticksRequested = 0;
    private volatile int stepsRequested = 0;
    private volatile boolean resetRequested = false;
    private volatile boolean propagateRequested = false;
    private volatile boolean complete = false;

    private void doTick() {
      synchronized (this) {
        ticksRequested--;
      }
      propagator.tick();
    }

    public PropagationManager() {
      super("PropagationManager");
    }

    public Propagator getPropagator() {
      return propagator;
    }

    public synchronized void requestPropagate() {
      if (!propagateRequested) {
        propagateRequested = true;
        notifyAll();
      }
    }

    public synchronized void requestReset() {
      if (!resetRequested) {
        resetRequested = true;
        notifyAll();
      }
    }

    public synchronized void requestTick() {
      if (ticksRequested < 16) {
        ticksRequested++;
      }
      notifyAll();
    }

    @Override
    public void run() {
      while (!complete) {
        try {
          synchronized (this) {
            while (!complete
                && !propagateRequested
                && !resetRequested
                && ticksRequested == 0
                && stepsRequested == 0) {
              try {
                wait();
              } catch (InterruptedException e) {
              }
            }
          }

          if (resetRequested) {
            resetRequested = false;
            if (propagator != null) {
              propagator.reset();
            }
            firePropagationCompleted();
            propagateRequested |= isRunning;
          }
          // TODO: fix unsynchronized access to shared variables
          if (propagateRequested || ticksRequested > 0 || stepsRequested > 0) {
            boolean ticked = false;
            if (isRunning) {
              stepPoints.clear();
              stepsRequested = 0;
              if (propagator == null) {
                propagateRequested = false;
                ticksRequested = 0;
              } else {
                ticked = ticksRequested > 0;
                if (ticked) {
                  doTick();
                }
                do {
                  propagateRequested = false;
                  try {
                    exceptionEncountered = false;
                    if (propagator != null) propagator.propagate();
                  } catch (UnsupportedOperationException thr) {
                    thr.printStackTrace();
                    exceptionEncountered = true;
                    setIsRunning(false);
                  } catch (Exception thr) {
                    thr.printStackTrace();
                    exceptionEncountered = true;
                    setIsRunning(false);
                  }
                } while (propagateRequested);
                if (isOscillating()) {
                  setIsRunning(false);
                  ticksRequested = 0;
                  propagateRequested = false;
                }
              }
            } else if (stepsRequested > 0) {
                if (ticksRequested > 0 || (isTicking && !propagateRequested)) {
                  ticksRequested = 1;
                  doTick();
                }
                propagateRequested = false;
                synchronized (this) {
                  stepsRequested--;
                }
                exceptionEncountered = false;
                try {
                  stepPoints.clear();
                  propagator.step(stepPoints);
                  propagateRequested |= propagator.isPending();
                } catch (Exception thr) {
                  thr.printStackTrace();
                  exceptionEncountered = true;
              }
            }
            if (ticked) {
              fireTickCompleted();
            }
            firePropagationCompleted();
          }
        } catch (Throwable e) {
          e.printStackTrace();
          exceptionEncountered = true;
          setIsRunning(false);
          javax.swing.SwingUtilities.invokeLater(
              new Runnable() {
                public void run() {
                  OptionPane.showMessageDialog(
                      null, "The simulator has crashed. Save your work and restart Logisim.");
                }
              });
        }
      }
    }

    public void setPropagator(Propagator value) {
      propagator = value;
    }

    public synchronized void shutDown() {
      complete = true;
      notifyAll();
    }
  }

  private boolean isRunning = true;
  private boolean isTicking = false;
  private boolean exceptionEncountered = false;
  private double tickFrequency = 1.0;
  private PropagationManager manager;
  private SimulatorTicker ticker;
  private ArrayList<SimulatorListener> listeners = new ArrayList<SimulatorListener>();

  public Simulator() {
    manager = new PropagationManager();
    ticker = new SimulatorTicker(manager);

    try {
      manager.setPriority(manager.getPriority() - 1);
      ticker.setPriority(ticker.getPriority() - 1);
    } catch (SecurityException e) {
    } catch (IllegalArgumentException e) {
    }

    manager.start();
    ticker.start();

    tickFrequency = 0.0;
    setTickFrequency(AppPreferences.TICK_FREQUENCY.get().doubleValue());
  }

  public void addSimulatorListener(SimulatorListener l) {
    listeners.add(l);
  }

  public void drawStepPoints(ComponentDrawContext context) {
    manager.stepPoints.draw(context);
  }

  void firePropagationCompleted() {
    SimulatorEvent e = new SimulatorEvent(this);
    for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
      l.propagationCompleted(e);
    }
  }

  void fireSimulatorStateChanged() {
    SimulatorEvent e = new SimulatorEvent(this);
    for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
      l.simulatorStateChanged(e);
    }
  }

  void fireTickCompleted() {
    SimulatorEvent e = new SimulatorEvent(this);
    for (SimulatorListener l : new ArrayList<SimulatorListener>(listeners)) {
      l.tickCompleted(e);
    }
  }

  public CircuitState getCircuitState() {
    Propagator prop = manager.getPropagator();
    return prop == null ? null : prop.getRootState();
  }

  public double getTickFrequency() {
    return tickFrequency;
  }

  public boolean isExceptionEncountered() {
    return exceptionEncountered;
  }

  public boolean isOscillating() {
    Propagator prop = manager.getPropagator();
    return prop != null && prop.isOscillating();
  }

  public boolean isRunning() {
    return isRunning;
  }

  public boolean isTicking() {
    return isTicking;
  }

  public void removeSimulatorListener(SimulatorListener l) {
    listeners.remove(l);
  }

  private void renewTickerAwake() {
    ticker.setAwake(isRunning && isTicking && tickFrequency > 0);
  }

  public void requestPropagate() {
    manager.requestPropagate();
  }

  public void requestReset() {
    manager.requestReset();
  }

  public void setCircuitState(CircuitState state) {
    manager.setPropagator(state == null ? null : state.getPropagator());
    renewTickerAwake();
  }

  public void setIsRunning(boolean value) {
    if (isRunning != value) {
      isRunning = value;
      renewTickerAwake();
      /*
       * DEBUGGING - comment out: if (!value) flushLog(); //
       */
      fireSimulatorStateChanged();
    }
  }

  public void setIsTicking(boolean value) {
    if (isTicking != value) {
      isTicking = value;
      renewTickerAwake();
      fireSimulatorStateChanged();
    }
  }

  //TODO: convert half-cycle frequency to full-cycle frequency
  public void setTickFrequency(double freq) {
    if (tickFrequency != freq) {
      int millis = (int) Math.round(1000 / freq);
      int ticks;
      if (millis > 0) {
        ticks = 1;
      } else {
        millis = 1;
        ticks = (int) Math.round(freq / 1000);
      }

      tickFrequency = freq;
      ticker.setTickFrequency(millis, ticks);
      renewTickerAwake();
      fireSimulatorStateChanged();
    }
  }

  public void shutDown() {
    ticker.shutDown();
    manager.shutDown();
  }

  public void step() {
    synchronized (manager) {
      manager.stepsRequested++;
      manager.notifyAll();
    }
  }

  public void tick(int count) {
	    ticker.tick(count);
  }
}
