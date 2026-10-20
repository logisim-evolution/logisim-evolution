/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.Collection;

public class CircuitTransactionResult {
  private final CircuitMutatorImpl mutator;

  CircuitTransactionResult(CircuitMutatorImpl mutator) {
    this.mutator = mutator;
  }

  public Collection<Circuit> getModifiedCircuits() {
    return mutator.getModifiedCircuits();
  }

  public ReplacementMap getReplacementMap(Circuit circuit) {
    final var ret = mutator.getReplacementMap(circuit);
    return ret == null ? new ReplacementMap() : ret;
  }

  public CircuitTransaction getReverseTransaction() {
    return mutator.getReverseTransaction();
  }

  @Override
  public String toString() {
    final var s = new StringBuilder("CircuitTransactionResult affecting...");
    for (final var c : getModifiedCircuits()) {
      s.append("\n    - circuit ").append(c).append(" with replacements...");
      s.append("\n").append(getReplacementMap(c));
    }
    return s.toString();
  }
}
