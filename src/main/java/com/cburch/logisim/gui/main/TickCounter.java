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
  private final String [] spaces = {"", " ", "  ", "   ", "    "};
  private long fullTickCount = 0;
  private long startTime;
  private long tickTime;
  private Object lock = new Object(); // lock for fullTickCount, startTime, tickTime
  private boolean autoTicking = false;
  private double requestedClockFrequency;
  private long processedTickCount = 0;
  private boolean useKiloHertz = false;
  static final int NANOSECONDS_PER_SECONDS = 1_000_000_000;

  public TickCounter() {
    String pattern = ".";
    for (int i = 0; i < 4; i++) {
      formatterWithDigits[i] = new DecimalFormat(pattern);
      formatterWithDigits[i].setRoundingMode(RoundingMode.HALF_UP);
      pattern += "0";
    }
  }

  public void clear(Simulator simulator) {
    synchronized (lock) {
      fullTickCount = -1;
      startTime = 0;
    }
    autoTicking = simulator.isAutoTicking();
    requestedClockFrequency = simulator.getTickFrequency() / 2.0;
    processedTickCount = 0;
  }

  public double getFullCyclesPerSecond() {
    long started;
    long fullCount;
    long timeOfTick;
    synchronized (lock) {
      started = startTime;
      fullCount = fullTickCount;
      timeOfTick = tickTime;
    }

    // Don't compute the clock frequency if simulation is manual.
    if (!autoTicking || fullTickCount < 1) {
      return requestedClockFrequency;
    }
    final var elapsedTime = timeOfTick - started;
    // If we didn't have any elapsed time we can't compute a frequency.
    if (elapsedTime == 0) {
      return requestedClockFrequency;
    }

    var tickCount = fullCount - processedTickCount;

    // If we didn't have any ticks we can't compute a frequency.
    if (tickCount < 1) {
      return requestedClockFrequency;
    }

    final var ticksPerNanosecond = (double) tickCount / elapsedTime;
    final var ticksPerSecond = ticksPerNanosecond * NANOSECONDS_PER_SECONDS;
    final var fullCyclesPerSecond = ticksPerSecond / 2.0; // 2 ticks per cycle

    // If we accumulated a lot of ticks or time then lets reduce the weight of the past.
    var thresholdForWeightReduction = fullCyclesPerSecond > 50 ? fullCyclesPerSecond : 50;
    if (tickCount > thresholdForWeightReduction) {
      var weightReductionTickCount = tickCount / 2; // reduce history by half
      processedTickCount += weightReductionTickCount;
      final var nanoseconds = weightReductionTickCount / ticksPerNanosecond;
      // We can modify startTime here because simThread only sets it to nanoTime when fullTickCount is -1.
      startTime += (long) nanoseconds;
    }
    return fullCyclesPerSecond;
  }

  public String getTickRate() {
    if (!autoTicking) {
      return "";
    }
    var fullCyclesPerSecond = getFullCyclesPerSecond();

    // update the frequency unit if needed
    if (fullCyclesPerSecond > 999.5) useKiloHertz = true;
    if (fullCyclesPerSecond < 900.0) useKiloHertz = false;

    // display 3 significant digits of the frequency
    final var unitsKey = useKiloHertz ? "tickRateKHz" : "tickRateHz";
    final var displayNum = useKiloHertz ? fullCyclesPerSecond / 1000.0 : fullCyclesPerSecond;
    final var fractionalDigits = displayNum < 0.9995 ? 3 : displayNum < 9.995 ? 2 : displayNum < 99.95 ? 1 : 0;
    var display = formatterWithDigits[fractionalDigits].format(displayNum);
    return S.get(unitsKey, display);
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
      synchronized (lock) {
        tickTime = System.nanoTime();
        if (fullTickCount == -1) {
          startTime = tickTime;
        }
        fullTickCount++;
      }
    }
  }
}
