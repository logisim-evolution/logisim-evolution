/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.Map;

public abstract class CircuitTransaction {
  public static final Integer READ_ONLY = 1;
  public static final Integer READ_WRITE = 2;

  public final CircuitTransactionResult execute() {
    final var mutator = new CircuitMutatorImpl();
    final var locks = CircuitLocker.acquireLocks(this, mutator);
    CircuitTransactionResult result;
    try {
      try {
        this.run(mutator);
      } catch (CircuitLocker.LockException e) {
        System.out.println("*** Circuit Lock Bug Diagnostics ***");
        System.out.println("This thread: " + Thread.currentThread());
        System.out.println("owns " + locks.size() + " locks, as follows:");
        for (final var entry : locks.entrySet()) {
          final var circuit = entry.getKey();
          final var lock = entry.getValue();
          System.out.printf(
              "  circuit \"%s\" [lock serial: %d] with lock %s\n",
              circuit.getName(), circuit.getLocker().getSerialNumber(), lock);
        }
        System.out.println("attempted to access without a lock:");
        System.out.printf(
            "  circuit \"%s\" [lock serial: %d/%d]\n",
            e.getCircuit().getName(),
            e.getSerialNumber(),
            e.getCircuit().getLocker().getSerialNumber());
        System.out.println("  owned by thread: " + e.getMutatingThread());
        System.out.println("  with mutator: " + e.getCircuitMutator());
        throw e;
      }

      // Let the port locations of each subcircuit's appearance be
      // updated to reflect the changes - this needs to happen before
      // wires are repaired because it could lead to some wires being
      // split
      final var modified = mutator.getModifiedCircuits();
      for (final var circuit : modified) {
        final var circMutator = circuit.getLocker().getMutator();
        if (circMutator == mutator) {
          final var repl = mutator.getReplacementMap(circuit);
          if (repl != null) {
            final var pins = circuit.getAppearance().getCircuitPins();
            pins.transactionCompleted(repl);
          }
        }
      }

      // Now go through each affected circuit and repair its wires
      for (final var circuit : modified) {
        final var circMutator = circuit.getLocker().getMutator();
        if (circMutator == mutator) {
          final var repair = new WireRepair(circuit);
          repair.run(mutator);
        } else {
          // this is a transaction executed within a transaction -
          // wait to repair wires until overall transaction is done
          circMutator.markModified(circuit);
        }
      }

      result = new CircuitTransactionResult(mutator);
      for (final var circuit : result.getModifiedCircuits()) {
        circuit.fireEvent(CircuitEvent.TRANSACTION_DONE, result);
      }
    } finally {
      CircuitLocker.releaseLocks(locks);
    }
    return result;
  }

  protected abstract Map<Circuit, Integer> getAccessedCircuits();

  protected abstract void run(CircuitMutator mutator);
}
