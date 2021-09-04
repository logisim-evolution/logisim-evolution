/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;

public class CircuitLocker {
  private static class CircuitComparator implements Comparator<Circuit> {
    @Override
    public int compare(Circuit a, Circuit b) {
      val an = a.getLocker().serialNumber;
      val bn = b.getLocker().serialNumber;
      return an - bn;
    }
  }

  static Map<Circuit, Lock> acquireLocks(CircuitTransaction xn, CircuitMutatorImpl mutator) {
    final var requests = xn.getAccessedCircuits();
    final var circuitLocks = new HashMap<Circuit, Lock>();
    // Acquire locks in serial-number order to avoid deadlock
    final var lockOrder = requests.keySet().toArray(new Circuit[0]);
    Arrays.sort(lockOrder, new CircuitComparator());
    try {
      for (final var circ : lockOrder) {
        final var access = requests.get(circ);
        final var locker = circ.getLocker();
        if (access == CircuitTransaction.READ_ONLY) {
          final var lock = locker.circuitLock.readLock();
          lock.lock();
          circuitLocks.put(circ, lock);
        } else if (access == CircuitTransaction.READ_WRITE) {
          final var curThread = Thread.currentThread();
          if (locker.mutatingThread == curThread) { // nothing to do - thread already has lock
          } else {
            final var lock = locker.circuitLock.writeLock();
            lock.lock();
            circuitLocks.put(circ, lock);
            locker.mutatingThread = Thread.currentThread();
            if (mutator == null) {
              mutator = new CircuitMutatorImpl();
            }
            locker.mutatingMutator = mutator;
          }
        }
      }
    } catch (RuntimeException t) {
      releaseLocks(circuitLocks);
      throw t;
    }
    return circuitLocks;
  }

  static void releaseLocks(Map<Circuit, Lock> locks) {
    final var curThread = Thread.currentThread();
    for (val entry : locks.entrySet()) {
      final var circ = entry.getKey();
      final var lock = entry.getValue();
      final var locker = circ.getLocker();
      if (locker.mutatingThread == curThread) {
        locker.mutatingThread = null;
        locker.mutatingMutator = null;
      }
      lock.unlock();
    }
  }

  private static final AtomicInteger NEXT_SERIAL_NUMBER = new AtomicInteger(0);
  private final int serialNumber;

  private final ReadWriteLock circuitLock;

  private transient Thread mutatingThread;

  private CircuitMutatorImpl mutatingMutator;

  CircuitLocker() {
    serialNumber = NEXT_SERIAL_NUMBER.getAndIncrement();
    circuitLock = new ReentrantReadWriteLock();
    mutatingThread = null;
    mutatingMutator = null;
  }

  public int getSerialNumber() {
    return serialNumber;
  }

  void checkForWritePermission(String operationName, Circuit circuit) {
    if (mutatingThread != Thread.currentThread()) {
      throw new LockException(
          operationName + " outside transaction",
          circuit,
          serialNumber,
          mutatingThread,
          mutatingMutator);
    }
  }

  void execute(CircuitTransaction xn) {
    if (mutatingThread == Thread.currentThread()) {
      xn.run(mutatingMutator);
    } else {
      xn.execute();
    }
  }

  CircuitMutatorImpl getMutator() {
    return mutatingMutator;
  }

  public boolean hasWriteLock() {
    return mutatingThread == Thread.currentThread();
  }

  // Can't use @Data annotation, because there's no way to pass callSuper value.
  @Getter
  @ToString
  @EqualsAndHashCode(callSuper = true)
  public static class LockException extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    private final Circuit circuit;
    private final int serialNumber;
    private final transient Thread mutatingThread;
    private final CircuitMutatorImpl circuitMutator;  // mutating mutator

    // Not sure Lombok's generated constructor would call super() propertly (if ever), so keep ours
    public LockException(String msg, Circuit circ, int serial, Thread thread, CircuitMutatorImpl mutator) {
      super(msg);
      circuit = circ;
      serialNumber = serial;
      mutatingThread = thread;
      circuitMutator = mutator;
    }
  } // end of LockException class

}
