/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.util.UniquelyNamedThread;

public final class ConnectorThread extends UniquelyNamedThread {

  private static final ConnectorThread INSTANCE = new ConnectorThread();

  private final Object lock;
  private transient boolean overrideRequest;
  private MoveRequest nextRequest;
  private MoveRequest processingRequest;

  private ConnectorThread() {
    super("tools-move-ConnectorThread");
    lock = new Object();
    overrideRequest = false;
    nextRequest = null;
  }

  public static void enqueueRequest(MoveRequest req, boolean priority) {
    synchronized (INSTANCE.lock) {
      if (!req.equals(INSTANCE.processingRequest)) {
        INSTANCE.nextRequest = req;
        INSTANCE.overrideRequest = priority;
        INSTANCE.lock.notifyAll();
      }
    }
  }

  public static boolean isOverrideRequested() {
    return INSTANCE.overrideRequest;
  }

  static {
    INSTANCE.start();
  }

  public boolean isAbortRequested() {
    return overrideRequest;
  }

  @Override
  public void run() {
    while (true) {
      MoveRequest req;
      boolean wasOverride;
      synchronized (lock) {
        processingRequest = null;
        while (nextRequest == null) {
          try {
            lock.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
        }
        req = nextRequest;
        wasOverride = overrideRequest;
        nextRequest = null;
        overrideRequest = false;
        processingRequest = req;
      }

      try {
        final var result = Connector.computeWires(req);
        if (result != null) {
          final var gesture = req.getMoveGesture();
          gesture.notifyResult(req, result);
        }
      } catch (Exception t) {
        t.printStackTrace();
        if (wasOverride) {
          final var result =
              new MoveResult(req, new ReplacementMap(), req.getMoveGesture().getConnections(), 0);
          req.getMoveGesture().notifyResult(req, result);
        }
      }
    }
  }
}
