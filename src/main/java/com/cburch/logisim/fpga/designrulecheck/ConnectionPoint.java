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

public class ConnectionPoint {

  private Net myOwnNet;
  private Byte myOwnNetBitIndex;
  private int myChildsPortIndex;
  private final Component myComp;

  public ConnectionPoint(Component comp) {
    myOwnNet = null;
    myOwnNetBitIndex = -1;
    myChildsPortIndex = -1;
    myComp = comp;
  }

  public Component getComp() {
    return myComp;
  }

  public int getChildsPortIndex() {
    return myChildsPortIndex;
  }

  public Net getParentNet() {
    return myOwnNet;
  }

  public Byte getParentNetBitIndex() {
    return myOwnNetBitIndex;
  }

  public void setChildsPortIndex(int index) {
    myChildsPortIndex = index;
  }

  public void setParentNet(Net connectedNet, Byte bitIndex) {
    myOwnNet = connectedNet;
    myOwnNetBitIndex = bitIndex;
  }
}
