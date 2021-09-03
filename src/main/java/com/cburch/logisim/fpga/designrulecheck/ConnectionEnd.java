/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.comp.Component;
import java.util.ArrayList;

public class ConnectionEnd {

  private final boolean isOutput;
  private final Byte nrOfBits;
  private final ArrayList<ConnectionPoint> myConnections;

  public ConnectionEnd(boolean isOutputEnd, Byte nrOfBits, Component comp) {
    this.isOutput = isOutputEnd;
    this.nrOfBits = nrOfBits;
    this.myConnections = new ArrayList<>();
    for (byte i = 0; i < nrOfBits; i++) {
      myConnections.add(new ConnectionPoint(comp));
    }
  }

  public ConnectionPoint get(Byte bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= nrOfBits)) return null;
    return myConnections.get(bitIndex);
  }

  public boolean isOutputEnd() {
    return isOutput;
  }

  public int getNrOfBits() {
    return nrOfBits;
  }

  public boolean setChildPortIndex(Net connectedNet, Byte bitIndex, int portIndex) {
    if ((bitIndex < 0) || (bitIndex >= nrOfBits)) return false;
    final var Connection = myConnections.get(bitIndex);
    if (Connection == null) return false;
    Connection.setChildsPortIndex(portIndex);
    return true;
  }

  public boolean setConnection(ConnectionPoint Connection, Byte BitIndex) {
    if ((BitIndex < 0) || (BitIndex >= nrOfBits)) return false;
    myConnections.set(BitIndex, Connection);
    return true;
  }
}
