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

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TickCounter implements Simulator.Listener {
  private final DecimalFormat [] formatterWithDigits = new DecimalFormat[4];
  static final long NANOSECONDS_PER_SECONDS = 1_000_000_000;
  static final long [] hertzValue = {1, 1000, 1000000};
  static final double [] hertzMinValue = {0.0, 999.5, 999500.0};
  static final String [] hertzKey = {"tickRateHz", "tickRateKHz", "tickRateMHz"};
  static final int HZ = 0, KHZ = 1, MHZ = 2; // index constants for hertz arrays

  private HistoryData historyData = new HistoryData(false, 1.0);
  private final Object historyLock = new Object(); // Lock for manipulation of history data.
  private final PropagateData propagateData = new PropagateData();

  public TickCounter() {
    String pattern = ".";
    for (int i = 0; i < 4; i++) {
      formatterWithDigits[i] = new DecimalFormat(pattern);
      formatterWithDigits[i].setRoundingMode(RoundingMode.HALF_UP);
      pattern += "0";
    }
  }

  public void clear(Simulator simulator) {
    synchronized (historyLock) {
      propagateData.clear();
      historyData = new HistoryData(simulator.isAutoTicking(), simulator.getTickFrequency() / 2.0);
    }
  }

  public String getTickRate() {
    double fullCyclesPerSecond;
    synchronized (historyLock) {
      if (!historyData.autoTicking) {
        return "";
      }
      fullCyclesPerSecond = historyData.getFullCyclesPerSecond();
    }

    // display 3 significant digits of the frequency in appropriate units.
    final var units = hertzMinValue[MHZ] <= fullCyclesPerSecond ? MHZ
        : hertzMinValue[KHZ] <= fullCyclesPerSecond ? KHZ
        : HZ;
    final var displayNum = fullCyclesPerSecond / hertzValue[units];
    final var fractionalDigits = displayNum < 0.9995 ? 3 : displayNum < 9.995 ? 2 : displayNum < 99.95 ? 1 : 0;
    var display = formatterWithDigits[fractionalDigits].format(displayNum);
    return S.get(hertzKey[units], display);
  }

  @Override
  public void simulatorStateChanged(Simulator.Event e) {
    clear(e.getSource());
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    clear(e.getSource());
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    if (e.didTick() && e.getSource().isAutoTicking()) {
      propagateData.propagateCompleted(System.nanoTime());
    }
  }

  /** HistoryData holds information for calculating clock speed. */
  private class HistoryData {
    private final boolean autoTicking;
    private final double requestedClockFrequency;
    private final long[] propagationValues = new long[3]; // to avoid allocation while holding lock
    private long startTime = 0; // updated to move smoothing window
    private long processedTickCount = 0; // ticks no longer in smoothing window

    public HistoryData(boolean ticking, double requestedFrequency) {
      autoTicking = ticking;
      requestedClockFrequency = requestedFrequency;
    }

    /** Caller must hold historyLock. */
    public double getFullCyclesPerSecond() {
      propagateData.getValues(propagationValues);
      final var fullTickCount = propagationValues[0];
      final var tickTime = propagationValues[1];
      final var propStartTime = propagationValues[2];

      if (!autoTicking || fullTickCount < 1) {
        return requestedClockFrequency; // Not enough ticks to calculate frequency.
      }
      if (startTime == 0) {
        startTime = propStartTime;
      }
      final var elapsedTime = tickTime - startTime;
      if (elapsedTime == 0) {
        return requestedClockFrequency; // No time has elapsed.
      }
      final var tickCount = fullTickCount - processedTickCount;
      if (tickCount < 1) {
        return requestedClockFrequency; // Should not happen. Can't calculate frequency.
      }

      final var ticksPerNanosecond = (double) tickCount / elapsedTime;
      final var ticksPerSecond = ticksPerNanosecond * NANOSECONDS_PER_SECONDS;
      final var fullCyclesPerSecond = ticksPerSecond / 2.0; // 2 ticks per cycle

      // If we accumulated a lot of ticks or time then lets reduce the weight of the past.
      final var thresholdForWeightReduction = fullCyclesPerSecond > 50 ? fullCyclesPerSecond : 50;
      if (tickCount > thresholdForWeightReduction) {
        final var weightReductionTickCount = tickCount / 2; // reduce history by half
        processedTickCount += weightReductionTickCount;
        final var nanoseconds = weightReductionTickCount / ticksPerNanosecond;
        startTime += (long) nanoseconds; // advance startTime to reflect weightReduction.
      }
      return fullCyclesPerSecond;
    }
  }

  /** PropagateData holds data from propagation completion. */
  private static class PropagateData {
    private long fullTickCount = -1;
    private long tickTime = 0;
    private long startTime = 0; // set when fullTickCount is 0.

    /** Initializes values */
    public synchronized void clear() {
      fullTickCount = -1;
      tickTime = startTime = 0;
    }

    /**
     * Fills in values with fullTickCount, tickTime, and startTime.
     *
     * @param values should be an array of at least 3 long elements.
     */
    public synchronized void getValues(long [] values) {
      values[0] = fullTickCount;
      values[1] = tickTime;
      values[2] = startTime;
    }

    /**
     * Updates data for end of propagation cycle at the given time.
     *
     * @param nanoTime the time at which the cycle ended.
     */
    public synchronized void propagateCompleted(long nanoTime) {
      final var thisTick = fullTickCount + 1;
      tickTime = nanoTime;
      fullTickCount = thisTick;
      if (thisTick == 0) startTime = nanoTime;
    }
  }
}
