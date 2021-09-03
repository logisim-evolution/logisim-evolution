/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Simulator;

class TickCounter implements Simulator.Listener {
  private static final int QUEUE_LENGTH = 1000;

  private final long[] queueTimes;
  private final double[] queueRates;
  private int queueStart;
  private int queueSize;
  private double tickFrequency;

  public TickCounter() {
    queueTimes = new long[QUEUE_LENGTH];
    queueRates = new double[QUEUE_LENGTH];
    queueSize = 0;
  }

  public void clear() {
    queueSize = 0;
  }

  public String getTickRate() {
    int size = queueSize;
    if (size <= 1) {
      return "";
    } else {
      int maxSize = queueTimes.length;
      int start = queueStart;
      int end = start + size - 1;
      if (end >= maxSize) {
        end -= maxSize;
      }
      double rate = queueRates[end];
      if (rate <= 0 || rate == Double.MAX_VALUE) {
        return "";
      } else {
        // Figure out the minimum over the previous 100 readings, and
        // base our rounding off of that. This is meant to provide some
        // stability in the rounding - we don't want the result to
        // oscillate rapidly between 990 Hz and 1 KHz - it's better for
        // it to oscillate between 990 Hz and 1005 Hz.
        int baseLen = size;
        if (baseLen > 100) baseLen = 100;
        int baseStart = end - baseLen + 1;
        double min = rate;
        if (baseStart < 0) {
          baseStart += maxSize;
          for (int i = baseStart + maxSize; i < maxSize; i++) {
            double x = queueRates[i];
            if (x < min) min = x;
          }
          for (int i = 0; i < end; i++) {
            double x = queueRates[i];
            if (x < min) min = x;
          }
        } else {
          for (int i = baseStart; i < end; i++) {
            double x = queueRates[i];
            if (x < min) min = x;
          }
        }
        if (min < 0.9 * rate) min = rate;

        // report the full-cycle frequency, not the half-cycle tick rate
        min /= 2;
        rate /= 2;

        if (min >= 1000.0) {
          return S.get("tickRateKHz", roundString(rate / 1000.0, min / 1000.0));
        } else {
          return S.get("tickRateHz", roundString(rate, min));
        }
      }
    }
  }

  public void updateSimulator(Simulator.Event e) {
    Simulator sim = e.getSource();
    if (!sim.isAutoTicking()) {
      queueSize = 0;
    }
  }

  private String roundString(double val, double min) {
    // round so we have only three significant digits
    int i = 0; // invariant: a = 10^i
    double a = 1.0; // invariant: a * bm == min, a is power of 10
    double bm = min;
    double bv = val;
    if (bm >= 1000) {
      while (bm >= 1000) {
        i++;
        a *= 10;
        bm /= 10;
        bv /= 10;
      }
    } else {
      while (bm < 100) {
        i--;
        a /= 10;
        bm *= 10;
        bv *= 10;
      }
    }

    // Examples:
    // 2.34: i = -2, a = .2, b = 234
    // 20.1: i = -1, a = .1, b = 201

    if (i >= 0) { // nothing after decimal point
      return "" + (int) Math.round(a * Math.round(bv));
    } else { // keep some after decimal point
      return String.format("%." + (-i) + "f", a * bv);
    }
  }

  public void simulatorStateChanged(Simulator.Event e) {
    updateSimulator(e);
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    updateSimulator(e);
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    if (e.didTick()) {
      Simulator sim = e.getSource();
      if (!sim.isAutoTicking()) {
        queueSize = 0;
      } else {
        double freq = sim.getTickFrequency();
        if (freq != tickFrequency) {
          queueSize = 0;
          tickFrequency = freq;
        }

        int curSize = queueSize;
        int maxSize = queueTimes.length;
        int start = queueStart;
        int end;
        if (curSize < maxSize) { // new sample is added into queue
          end = start + curSize;
          if (end >= maxSize) {
            end -= maxSize;
          }
          curSize++;
          queueSize = curSize;
        } else { // new sample replaces oldest value in queue
          end = queueStart;
          if (end + 1 >= maxSize) {
            queueStart = 0;
          } else {
            queueStart = end + 1;
          }
        }
        long startTime = queueTimes[start];
        long endTime = System.currentTimeMillis();
        double rate;
        if (startTime == endTime || curSize <= 1) {
          rate = Double.MAX_VALUE;
        } else {
          rate = 1000.0 * (curSize - 1) / (endTime - startTime);
        }
        queueTimes[end] = endTime;
        queueRates[end] = rate;
      }
    }
  }
}
