/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.concurrent.CopyOnWriteArraySet;
import lombok.Getter;

class WireThread {
  private WireThread parent;
  @Getter private final CopyOnWriteArraySet<CircuitWires.ThreadBundle> bundles =
      new CopyOnWriteArraySet<>();

  public WireThread() {
    parent = this;
  }

  public WireThread find() {
    var ret = this;
    if (ret.parent != ret) {
      do ret = ret.parent;
      while (ret.parent != ret);
      this.parent = ret;
    }
    return ret;
  }

  void unite(WireThread other) {
    final var group = this.find();
    final var group2 = other.find();
    if (group != group2) group.parent = group2;
  }
}
