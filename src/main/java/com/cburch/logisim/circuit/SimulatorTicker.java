/**
 * This file is part of logisim-evolution.
 *
 * <p>Logisim-evolution is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>Logisim-evolution is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with
 * logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Original code by Carl Burch (http://www.cburch.com), 2011. Subsequent modifications by: +
 * College of the Holy Cross http://www.holycross.edu + Haute École Spécialisée Bernoise/Berner
 * Fachhochschule http://www.bfh.ch + Haute École du paysage, d'ingénierie et d'architecture de
 * Genève http://hepia.hesge.ch/ + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 */
package com.cburch.logisim.circuit;

import com.cburch.logisim.util.UniquelyNamedThread;

class SimulatorTicker extends UniquelyNamedThread {
  private Simulator.PropagationManager manager;
  private int ticksPerTickPhase;
  private long nanosPerTickPhase;

  private boolean shouldTick;
  private int ticksPending;
  private boolean complete;

  public SimulatorTicker(Simulator.PropagationManager manager) {
    super("SimulationTicker");
    this.manager = manager;
    ticksPerTickPhase = 1;
    nanosPerTickPhase = (long) 1e9;
    shouldTick = false;
    ticksPending = 0;
    complete = false;
  }

  @Override
  public void run() {
    long lastTick = System.nanoTime();
    while (true) {
      boolean curShouldTick = shouldTick;
      long nanos = nanosPerTickPhase;
      int ticks = ticksPerTickPhase;
      try {
        synchronized (this) {
          curShouldTick = shouldTick;
          nanos = nanosPerTickPhase;
          ticks = ticksPerTickPhase;
          while (!curShouldTick && ticksPending == 0 && !complete) {
            wait();
            curShouldTick = shouldTick;
            nanos = nanosPerTickPhase;
            ticks = ticksPerTickPhase;
          }
        }
      } catch (InterruptedException e) {
      }

      if (complete) break;

      int toTick;
      long now = System.nanoTime();
      if (curShouldTick && now - lastTick >= nanos) {
        toTick = ticks;
      } else {
        toTick = ticksPending;
      }

      if (toTick > 0) {
        lastTick = now;
        for (int i = 0; i < toTick; i++) {
          manager.requestTick();
        }
        synchronized (this) {
          if (ticksPending > toTick) ticksPending -= toTick;
          else ticksPending = 0;
        }
        // we fire tickCompleted in this thread so that other
        // objects (in particular the repaint process) can slow
        // the thread down.
      }

      try {
        long nextTick = lastTick + nanos;
        long totalWaitNanos = nextTick - System.nanoTime();
        if (totalWaitNanos < 0) {
          long waitMillis = (long) (totalWaitNanos / 1e6);
          int waitNanos = (int) (totalWaitNanos - (long) (waitMillis * 1e6));
          if (waitMillis > 100) {
            waitMillis = 100;
            waitNanos = 0;
          }
          if (waitNanos < 0) waitNanos = 0;
          if (waitMillis > 0 || waitNanos > 0) Thread.sleep(waitMillis, waitNanos);
        }
      } catch (InterruptedException e) {
      }
    }
  }

  synchronized void setAwake(boolean value) {
    shouldTick = value;
    if (shouldTick) notifyAll();
  }

  public synchronized void setTickFrequency(long nanos, int ticks) {
    nanosPerTickPhase = nanos;
    ticksPerTickPhase = ticks;
  }

  public synchronized void shutDown() {
    complete = true;
    notifyAll();
  }

  public synchronized void tick(int count) {
    ticksPending += count;
    notifyAll();
  }
}
