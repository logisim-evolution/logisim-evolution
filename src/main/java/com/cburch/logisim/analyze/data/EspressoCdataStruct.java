/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 * 
 * This part of logisim implements (parts of) espresso:
 * 
 * Copyright (c) 1988, 1989, Regents of the University of California.
 * All rights reserved.
 *
 * Use and copying of this software and preparation of derivative works
 * based upon this software are permitted.  However, any distribution of
 * this software or derivative works must include the above copyright
 * notice.
 *
 * This software is made available AS IS, and neither the Electronics
 * Research Laboratory or the University of California make any
 * warranty about the software, its performance or its conformity to
 * any specification.
*/

package com.cburch.logisim.analyze.data;

import java.util.ArrayList;
import java.util.HashSet;

public class EspressoCdataStruct {

  private interface CounterListener {
    void resetCounter();
    void conditionalIncrement(int value);
  }

  private class Counter implements CounterListener {
    private int counterValue;
    private int mask;

    public Counter(int bitIndex) {
      counterValue = 0;
      mask = 1 << bitIndex;
    }

    public int getValue() {
      return counterValue;
    }

    public void resetCounter() {
      counterValue = 0;
    }

    public void conditionalIncrement(int value) {
      if ((value & mask) == 0) return;
      counterValue++;
    }
  }

  private class PartZeroClass {
    private ArrayList<HashSet<CounterListener>> wordCounterListeners;
    private HashSet<CounterListener> allCounters;
    private ArrayList<ArrayList<Counter>> wordCounters;

    public PartZeroClass(EspressoCubeStruct infoCube) {
      final var nrOfWords = infoCube.getNumberOfWords();
      wordCounterListeners = new ArrayList<>();
      allCounters = new HashSet<>();
      wordCounters = new ArrayList<>();
      for (var word = 0; word < nrOfWords; word++) {
        final var wordListen = new HashSet<CounterListener>();
        final var wordCounter = new ArrayList<Counter>();
        for (var bit = 0; bit < EspressoCubeStruct.BPI; bit++) {
          final var counter = new Counter(bit);
          wordListen.add(counter);
          allCounters.add(counter);
          wordCounter.add(counter);
        }
        wordCounterListeners.add(wordListen);
        wordCounters.add(wordCounter);
      }
    }

    public void resetCounters() {
      for (final var counter : allCounters) counter.resetCounter();
    }

    public void conditionalIncrement(int wordId, int value) {
      if ((wordId < 0) || (wordId >= wordCounterListeners.size())) return;
      for (final var counter : wordCounterListeners.get(wordId)) counter.conditionalIncrement(value);
    }

    public int getCounterValue(int wordId, int bitId) {
      if ((wordId < 0) || (wordId >= wordCounters.size())) return 0;
      final var wordXcounter = wordCounters.get(wordId);
      if ((bitId < 0) || bitId >= wordXcounter.size()) return 0;
      return wordXcounter.get(bitId).getValue();
    }

    public int getCounterValue(int index) {
      final var wordId = cubeInfo.whichWord(index);
      final var bitId = cubeInfo.whichBit(index);
      return getCounterValue(wordId, bitId);
    }
  }

  private final EspressoCubeStruct cubeInfo;
  private final PartZeroClass partZero;
  private final ArrayList<Integer> varZero;
  private final ArrayList<Integer> partsActive;
  private final ArrayList<Boolean> isUnate;
  private int varsActive;
  private int varsUnate;
  private int best;

  public EspressoCdataStruct(EspressoCubeStruct cubeInfo) {
    this.cubeInfo = cubeInfo;
    final var nrOfVars = cubeInfo.getNumberOfVariables();
    partZero = new PartZeroClass(cubeInfo);
    varZero = cubeInfo.allocInt(nrOfVars);
    partsActive = cubeInfo.allocInt(nrOfVars);
    isUnate = cubeInfo.allocBool(nrOfVars);
    varsActive = varsUnate = best = 0;
  }

  public void massiveCount(EspressoPset cubeSet) {
    partZero.resetCounters();
    final var cof = cubeSet.getTempSet();
    final var full = cubeInfo.getFullSet();
    for (final var cube : cubeSet.getCover()) {
      for (var wordId = 0; wordId < cube.size(); wordId++) {
        final var wordIndex = wordId + 1;
        final var value = full.get(wordIndex) & ~(cube.get(wordId) | cof.get(wordId));
        if (value != 0) partZero.conditionalIncrement(wordId, value);
      }
    }
    final var nrOfVariables = cubeInfo.getNumberOfVariables();
    varsUnate = varsActive = 0;
    var localBest = -1;
    var mostActive = 0;
    var mostZero = 0;
    var mostBalanced = 32000;
    var active = 0;
    var maxActive = 0;
    for (var variable = 0; variable < nrOfVariables; variable++) {
      // hack, we now that the last variable are the outputs and that we do not have mv-variables
      if (variable < (nrOfVariables - 1)) {
        // here we have an input variable
        final var firstCount = partZero.getCounterValue(variable * 2);
        final var lastCount = partZero.getCounterValue((variable * 2) + 1);
        active = ((firstCount > 0) ? 1 : 0) + ((lastCount > 0) ? 1 : 0);
        varZero.set(variable, firstCount + lastCount);
        maxActive = Math.max(firstCount, lastCount);
      } else {
        // here we have the outputs
        varZero.set(variable, 0);
        active = maxActive = 0;
        final var lastIndex = cubeInfo.getLastPart(variable);
        for (var index = cubeInfo.getFirtPart(variable); index <= lastIndex; index++) {
          final var zeroCount = partZero.getCounterValue(index);
          varZero.set(variable, varZero.get(variable) + zeroCount);
          active += (zeroCount > 0) ? 1 : 0;
          maxActive = Math.max(active, maxActive);
        }
      }
      if (active > mostActive) {
        localBest = variable;
        mostActive = active;
        mostZero = varZero.get(localBest);
        mostBalanced = mostActive;
      } else {
        if (active == maxActive) {
          // secondary condition is to maximize the number zeros
          // for binary variables, this is the same as minimum # of 2's
          if (varZero.get(variable) > mostZero) {
            localBest = variable;
            mostZero = varZero.get(variable);
            mostBalanced = maxActive;
          } else {
            if (varZero.get(variable) == mostZero) {
              if (maxActive < mostBalanced) {
                localBest = variable;
                mostBalanced = maxActive;
              }
            }
          }
        }
      }
      partsActive.set(variable, active);
      isUnate.set(variable, active == 1);
      varsActive += (active > 0) ? 1 : 0;
      varsUnate += (active == 1) ? 1 : 0;
    }
    best = localBest;
  }
}
