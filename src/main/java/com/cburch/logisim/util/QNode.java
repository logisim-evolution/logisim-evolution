/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

public class QNode implements Comparable<QNode> {
  public final int timeKey, serialNumber;
  QNode left, right;

  public QNode(int timeKey, int serialNumber) {
    this.timeKey = timeKey;
    this.serialNumber = serialNumber;
  }

  @Override
  public int compareTo(QNode other) {
    // Yes, these subtractions may overflow. This is intentional, as it
    // avoids potential wraparound problems as the counters increment.
    int ret = timeKey - other.timeKey;
    if (ret == 0) ret = serialNumber - other.serialNumber;
    return ret;
  }

}
