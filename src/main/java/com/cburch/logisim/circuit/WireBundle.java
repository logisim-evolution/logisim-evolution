/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import java.util.HashSet;

class WireBundle {
  private BitWidth width = BitWidth.UNKNOWN;
  private Value pullValue = Value.UNKNOWN;
  private WireBundle parent;
  private Location widthDeterminant = null;
  private boolean isBus_ = false;
  WireThread[] threads = null; // will be set when Connectivity is done being constructed
  Location[] xpoints = null; // will be set when Connectivity is done being constructed
  HashSet<Location> tempPoints = new HashSet<>();
  // bundle
  // hits
  private WidthIncompatibilityData incompatibilityData = null;

  WireBundle(Location p) {
    parent = this;
    tempPoints.add(p);
  }

  void addPullValue(Value val) {
    pullValue = pullValue.combine(val);
  }

  WireBundle find() {
    var ret = this;
    if (ret.parent != ret) {
      do ret = ret.parent;
      while (ret.parent != ret);
      this.parent = ret;
    }
    return ret;
  }

  Value getPullValue() {
    return pullValue;
  }

  BitWidth getWidth() {
    if (incompatibilityData != null) {
      return BitWidth.UNKNOWN;
    } else {
      return width;
    }
  }

  WidthIncompatibilityData getWidthIncompatibilityData() {
    return incompatibilityData;
  }

  boolean isBus() {
    return isBus_;
  }

  void isolate() {
    parent = this;
  }

  boolean isValid() {
    return incompatibilityData == null;
  }

  void setWidth(BitWidth width, Location det) {
    if (width == BitWidth.UNKNOWN) return;
    if (incompatibilityData != null) {
      incompatibilityData.add(det, width);
      return;
    }
    if (this.width != BitWidth.UNKNOWN) {
      if (width.equals(this.width)) {
        isBus_ = width.getWidth() > 1;
        // nothing to do
      } else { // the widths are broken: Create incompatibilityData
        // holding this info
        incompatibilityData = new WidthIncompatibilityData();
        incompatibilityData.add(widthDeterminant, this.width);
        incompatibilityData.add(det, width);
      }
      // the widths match, and the bundle is already set
      return;
    }
    this.width = width;
    this.widthDeterminant = det;
  }

  void unite(WireBundle other) {
    final var group = this.find();
    final var group2 = other.find();
    if (group != group2) group.parent = group2;
  }
}
