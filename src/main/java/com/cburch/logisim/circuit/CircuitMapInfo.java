/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.MapComponent;
import java.util.ArrayList;
import lombok.Getter;

public class CircuitMapInfo {

  @Getter private BoardRectangle rectangle;
  @Getter private Long constValue;
  @Getter private int pinId = -1;
  @Getter private int ioId = -1;
  @Getter private boolean oldFormat = true;   // old map format
  @Getter private MapComponent map;
  @Getter private ArrayList<CircuitMapInfo> pinMaps;

  public CircuitMapInfo() {
    rectangle = null;
    constValue = null;
  }

  public CircuitMapInfo(BoardRectangle rect) {
    this.rectangle = rect;
    constValue = null;
  }

  public CircuitMapInfo(Long val) {
    this.rectangle = null;
    constValue = val;
  }

  public CircuitMapInfo(MapComponent map) {
    oldFormat = false;
    this.map = map;
  }

  public CircuitMapInfo(int sourceId, int ioId, int xpos, int ypos) {
    pinId = sourceId;
    this.ioId = ioId;
    rectangle = new BoardRectangle(xpos, ypos, 1, 1);
  }

  public CircuitMapInfo(int x, int y) {
    oldFormat = false;
    rectangle = new BoardRectangle(x, y, 1, 1);
  }

  public void addPinMap(CircuitMapInfo map) {
    if (pinMaps == null) {
      pinMaps = new ArrayList<>();
      oldFormat = false;
    }
    pinMaps.add(map);
  }

  public void addPinMap(int x, int y, int loc) {
    if (pinMaps == null) {
      pinMaps = new ArrayList<>();
      oldFormat = false;
    }
    int sloc = pinMaps.size();
    pinMaps.add(new CircuitMapInfo(sloc, loc, x, y));
  }

  public boolean isSinglePin() {
    return pinId >= 0;
  }

  public boolean isOpen() {
    return rectangle == null && constValue == null && oldFormat;
  }

  public boolean isConst() {
    return rectangle == null && constValue != null;
  }
}
