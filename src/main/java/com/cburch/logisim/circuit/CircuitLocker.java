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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CircuitLocker {
  private static class CircuitComparator implements Comparator<Circuit> {
    public int compare(Circuit a, Circuit b) {
      int an = a.getLocker().serialNumber;
      int bn = b.getLocker().serialNumber;
      return an - bn;
    }
  }

  static Map<Circuit, Lock> acquireLocks(CircuitTransaction xn, CircuitMutatorImpl mutator) {
    Map<Circuit, Integer> requests = xn.getAccessedCircuits();
    Map<Circuit, Lock> circuitLocks = new HashMap<Circuit, Lock>();
    // Acquire locks in serial-number order to avoid deadlock
    Circuit[] lockOrder = requests.keySet().toArray(new Circuit[0]);
    Arrays.sort(lockOrder, new CircuitComparator());
    try {
      for (Circuit circ : lockOrder) {
        Integer access = requests.get(circ);
        CircuitLocker locker = circ.getLocker();
        if (access == CircuitTransaction.READ_ONLY) {
          Lock lock = locker.circuitLock.readLock();
          lock.lock();
          circuitLocks.put(circ, lock);
        } else if (access == CircuitTransaction.READ_WRITE) {
          Thread curThread = Thread.currentThread();
          if (locker.mutatingThread == curThread) {; // nothing to do - thread already has lock
          } else {
            Lock lock = locker.circuitLock.writeLock();
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
    Thread curThread = Thread.currentThread();
    for (Map.Entry<Circuit, Lock> entry : locks.entrySet()) {
      Circuit circ = entry.getKey();
      Lock lock = entry.getValue();
      CircuitLocker locker = circ.getLocker();
      if (locker.mutatingThread == curThread) {
        locker.mutatingThread = null;
        locker.mutatingMutator = null;
      }
      lock.unlock();
    }
  }

  private static AtomicInteger NEXT_SERIAL_NUMBER = new AtomicInteger(0);
  private int serialNumber;

  private ReadWriteLock circuitLock;

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

  public static class LockException extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    private Circuit circuit;
    private int serialNumber;
    private transient Thread mutatingThread;
    private CircuitMutatorImpl mutatingMutator;

    public LockException(
        String msg, Circuit circ, int serial, Thread thread, CircuitMutatorImpl mutator) {
      super(msg);
      circuit = circ;
      serialNumber = serial;
      mutatingThread = thread;
      mutatingMutator = mutator;
    }

    public Circuit getCircuit() {
      return circuit;
    }

    public int getSerialNumber() {
      return serialNumber;
    }

    public Thread getMutatingThread() {
      return mutatingThread;
    }

    public CircuitMutatorImpl getCircuitMutator() {
      return mutatingMutator;
    }
  }
}
