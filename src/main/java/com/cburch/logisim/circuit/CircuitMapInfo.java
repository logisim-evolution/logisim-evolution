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
import java.util.List;

public class CircuitMapInfo {

  private BoardRectangle rect;
  private Long constValue;
  private int pinid = -1;
  private int ioid = -1;
  private boolean oldMapFormat = true;
  private MapComponent myMap;
  private ArrayList<CircuitMapInfo> pinmaps;

  public CircuitMapInfo() {
    rect = null;
    constValue = null;
  }

  public CircuitMapInfo(BoardRectangle rect) {
    this.rect = rect;
    constValue = null;
  }

  public CircuitMapInfo(Long val) {
    this.rect = null;
    constValue = val;
  }

  public CircuitMapInfo(MapComponent map) {
    oldMapFormat = false;
    myMap = map;
  }

  public CircuitMapInfo(int sourceId, int ioId, int xpos, int ypos) {
    pinid = sourceId;
    ioid = ioId;
    rect = new BoardRectangle(xpos, ypos, 1, 1);
  }

  public CircuitMapInfo(int x, int y) {
    oldMapFormat = false;
    rect = new BoardRectangle(x, y, 1, 1);
  }

  public void addPinMap(CircuitMapInfo map) {
    if (pinmaps == null) {
      pinmaps = new ArrayList<>();
      oldMapFormat = false;
    }
    pinmaps.add(map);
  }

  public void addPinMap(int x, int y, int loc) {
    if (pinmaps == null) {
      pinmaps = new ArrayList<>();
      oldMapFormat = false;
    }
    int sloc = pinmaps.size();
    pinmaps.add(new CircuitMapInfo(sloc, loc, x, y));
  }

  public boolean isSinglePin() {
    return pinid >= 0;
  }

  public int getPinId() {
    return pinid;
  }

  public int getIoId() {
    return ioid;
  }

  public BoardRectangle getRectangle() {
    return rect;
  }

  public Long getConstValue() {
    return constValue;
  }

  public boolean isOpen() {
    return rect == null && constValue == null && oldMapFormat;
  }

  public boolean isConst() {
    return rect == null && constValue != null;
  }

  public boolean isOldFormat() {
    return oldMapFormat;
  }

  public MapComponent getMap() {
    return myMap;
  }

  public List<CircuitMapInfo> getPinMaps() {
    return pinmaps;
  }
}
