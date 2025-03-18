/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.HashSet;

class WireThread {
  private WireThread representative;
  private HashSet<BundlePosition> tempBundlePositions = new HashSet<>();
  public int steps; // will be set when BundleMap is done being constructed
  public WireBundle[] bundle; // will be set when BundleMap is done being constructed
  public int[] position; // will be set when BundleMap is done being constructed

  private static class BundlePosition {
    int pos;
    WireBundle b;
    BundlePosition(int pos, WireBundle b) {
      this.pos = pos;
      this.b = b;
    }
  }

  WireThread() {
    representative = this;
  }

  void addBundlePosition(int pos, WireBundle b) {
    tempBundlePositions.add(new BundlePosition(pos, b));
  }

  void finishConstructing() {
    if (tempBundlePositions == null) return;
    steps = tempBundlePositions.size();
    bundle = new WireBundle[steps];
    position = new int[steps];
    int i = 0;
    for (BundlePosition bp : tempBundlePositions) {
      bundle[i] = bp.b;
      position[i] = bp.pos;
      i++;
    }
    tempBundlePositions = null;
  }

  WireThread getRepresentative() {
    WireThread ret = this;
    if (ret.representative != ret) {
      do
        ret = ret.representative;
      while (ret.representative != ret);
      this.representative = ret;
    }
    return ret;
  }

  void unite(WireThread other) {
    WireThread us = this.getRepresentative();
    WireThread them = other.getRepresentative();
    if (us != them) {
      us.representative = them;
    }
  }
}
