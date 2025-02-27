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
  private final DecimalFormat formatter;
  private Simulator simulator;
  private long tickCount = 0;
  private long startTime;
  private boolean useKiloHertz = false;
  private double previousFrequency = 0.0;
  private long elapsedTimeSinceLastUnitUpdate = 0;
  static final int NANOSECONDS_PER_SECONDS = 1_000_000_000;
  static final int UNIT_UPDATE_THRESHOLD_NANOSECONDS = NANOSECONDS_PER_SECONDS / 2;
  static final int TICKS_THRESHOLD_BEFORE_HISTORY_WEIGHT_REDUCTION = 1000;
  static final int WEIGHT_REDUCTION_TICKS_COUNT = TICKS_THRESHOLD_BEFORE_HISTORY_WEIGHT_REDUCTION / 2;

  public TickCounter() {
    clear();
    formatter = new DecimalFormat("0.00");
    formatter.setRoundingMode(RoundingMode.HALF_UP);
  }

  public void clear() {
    // If we know the requested frequency, let's initialize the counts to this frequency.
    // It provides a nicer effect at low frequencies, and doesn't hurt at high frequencies.
    if (simulator != null) {
      final var tickPeriodNanoseconds = NANOSECONDS_PER_SECONDS / simulator.getTickFrequency();
      tickCount = 12; // We'll set the frequency as if it happened during 12 ticks already.
      startTime = System.nanoTime() - (long) (tickCount * tickPeriodNanoseconds);
    } else {
      tickCount = 0;
      startTime = System.nanoTime();
    }
  }

  public String getTickRate() {
    // Don't compute the clock frequency if simulation is manual.
    if (simulator == null || !simulator.isAutoTicking()) {
      return "";
    }

    final var currentFrequency = simulator.getTickFrequency();

    // Reset history when the user changes the desired simulation frequency.
    if (previousFrequency != currentFrequency) {
      previousFrequency = currentFrequency;
      clear();
    }

    final var elapsedTime = System.nanoTime() - startTime;

    // If we didn't have any elapsed time we can't compute a frequency.
    if (elapsedTime == 0) {
      return "";
    }

    // If we didn't have any ticks we can't compute a frequency.
    if (tickCount < 1) {
      return "";
    }

    final var ticksPerNanoseconds = (double) tickCount / elapsedTime;
    final var fullCyclesPerSeconds = NANOSECONDS_PER_SECONDS / 2.0 * ticksPerNanoseconds; // 2 ticks per cycles
    elapsedTimeSinceLastUnitUpdate += elapsedTime;

    // If time has come, update the frequency unit.
    if (elapsedTimeSinceLastUnitUpdate > UNIT_UPDATE_THRESHOLD_NANOSECONDS) {
      useKiloHertz = (fullCyclesPerSeconds > 1000.0);
      elapsedTimeSinceLastUnitUpdate = 0;
    }

    // If we accumulated a lot of ticks then lets reduce the weight of the past.
    if (tickCount > TICKS_THRESHOLD_BEFORE_HISTORY_WEIGHT_REDUCTION) {
      tickCount -= WEIGHT_REDUCTION_TICKS_COUNT;
      final var nanoseconds = WEIGHT_REDUCTION_TICKS_COUNT / ticksPerNanoseconds;
      startTime += (long) nanoseconds;
    }

    if (useKiloHertz) {
      return S.get("tickRateKHz", formatter.format(fullCyclesPerSeconds / 1000.0));
    } else {
      return S.get("tickRateHz", formatter.format(fullCyclesPerSeconds));
    }
  }

  public void simulatorStateChanged(Simulator.Event e) {
    simulator = e.getSource();
    clear();
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    simulator = e.getSource();
    clear();
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    if (e.didTick() > 0) {
      simulator = e.getSource();
      tickCount += e.didTick();
    }
  }
}
