/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.util.ArrayList;
import java.util.Map;
import javax.swing.DefaultListModel;

public class MapListModel extends DefaultListModel<MapListModel.MapInfo> {

  public static class MapInfo {
    private final int pinNr;
    private final MapComponent map;

    public MapInfo(int pin, MapComponent map) {
      pinNr = pin;
      this.map = map;
    }

    public int getPin() {
      return pinNr;
    }

    public MapComponent getMap() {
      return map;
    }

    @Override
    public String toString() {
      return map.getDisplayString(pinNr);
    }
  }

  private static final long serialVersionUID = 1L;
  private boolean mappedList = false;
  private final Map<ArrayList<String>, MapComponent> myMappableResources;
  private ArrayList<MapInfo> myItems;

  public MapListModel(boolean mappedList, Map<ArrayList<String>, MapComponent> myMappableResources) {
    this.mappedList = mappedList;
    this.myMappableResources = myMappableResources;
    rebuild();
  }

  public void rebuild() {
    var oldsize = 0;
    if (myItems == null) myItems = new ArrayList<>();
    else {
      oldsize = myItems.size();
      myItems.clear();
    }
    for (var key : myMappableResources.keySet()) {
      var map = myMappableResources.get(key);
      if (mappedList) {
        if (map.isCompleteMap(false)) {
          var idx = getInsertionPoint(map);
          myItems.add(idx, new MapInfo(map.getNrOfPins() == 1 ? 0 : -1, map));
        } else {
          var idx = getInsertionPoint(map);
          for (var i = map.getNrOfPins() - 1; i >= 0; i--) {
            if (map.isMapped(i)) myItems.add(idx, new MapInfo(i, map));
          }
        }
      } else {
        if (map.isNotMapped()) {
          var idx = getInsertionPoint(map);
          myItems.add(idx, new MapInfo(map.getNrOfPins() == 1 ? 0 : -1, map));
        } else {
          var idx = getInsertionPoint(map);
          for (var i = map.getNrOfPins() - 1; i >= 0; i--) {
            if (!map.isMapped(i)) {
              myItems.add(idx, new MapInfo(i, map));
            }
          }
        }
      }
    }
    if (oldsize > 0 || !myItems.isEmpty()) {
      fireContentsChanged(this, 0, Math.max(oldsize, myItems.size()));
    }
  }

  private int getInsertionPoint(MapComponent map) {
    if (myItems.isEmpty()) return 0;
    var idx = 0;
    while (idx < myItems.size() && myItems
        .get(idx)
        .getMap()
        .getDisplayString(-1)
        .compareToIgnoreCase(map.getDisplayString(-1)) < 0) idx++;
    return idx;
  }

  @Override
  public int getSize() {
    return myItems.size();
  }

  @Override
  public boolean isEmpty() {
    return myItems.isEmpty();
  }

  @Override
  public int size() {
    return myItems.size();
  }

  @Override
  public MapInfo elementAt(int idx) {
    if (idx < 0 || idx >= myItems.size()) return null;
    return myItems.get(idx);
  }

  @Override
  public MapInfo getElementAt(int idx) {
    return elementAt(idx);
  }
}
