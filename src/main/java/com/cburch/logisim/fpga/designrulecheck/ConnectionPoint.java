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
import lombok.Getter;
import lombok.Setter;

public class ConnectionPoint {

  @Getter private Net parentNet;
  @Getter private Byte parentNetBitIndex;
  @Getter @Setter private int childsPortIndex;
  @Getter private final Component comp;

  public ConnectionPoint(Component component) {
    parentNet = null;
    parentNetBitIndex = -1;
    childsPortIndex = -1;
    comp = component;
  }

  public void setParentNet(Net connectedNet, Byte bitIndex) {
    parentNet = connectedNet;
    parentNetBitIndex = bitIndex;
  }

}
