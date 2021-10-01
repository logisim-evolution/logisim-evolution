/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import java.util.ArrayList;
import java.util.List;

class ConnectionPointArray {

  private final ArrayList<ConnectionPoint> myConnections;

  public ConnectionPointArray() {
    myConnections = new ArrayList<>();
  }

  public void add(ConnectionPoint connection) {
    myConnections.add(connection);
  }

  public void clear() {
    myConnections.clear();
  }

  public List<ConnectionPoint> getAll() {
    return myConnections;
  }

  public int size() {
    return myConnections.size();
  }
}
