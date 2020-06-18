/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.data;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.DefaultListModel;

public class MapListModel extends DefaultListModel<MapListModel.MapInfo> {
	
  public class MapInfo extends Object {
    private int pinNr;
    private MapComponent map;
    
    public MapInfo(int pin , MapComponent map) {
      pinNr = pin;
      this.map = map;
    }
    
    public int getPin() { return pinNr; }
    public MapComponent getMap() { return map; }
    
    @Override
    public String toString() {
      return map.getDisplayString(pinNr);
    }
  }

  private static final long serialVersionUID = 1L;
  private boolean mappedList = false;
  private Map<ArrayList<String>, MapComponent> myMappableResources;
  private ArrayList<MapInfo> myItems;
  
  public MapListModel(boolean mappedList , Map<ArrayList<String>, MapComponent> myMappableResources) {
    this.mappedList = mappedList;
    this.myMappableResources = myMappableResources;
    rebuild();
  }
  
  public void rebuild() {
	int oldsize = 0;
	if (myItems == null) myItems = new ArrayList<MapInfo>();
	else {
	  oldsize = myItems.size();
	  myItems.clear();
	}
    for (ArrayList<String> key : myMappableResources.keySet()) {
      MapComponent map = myMappableResources.get(key);
      if (mappedList) {
        if (map.isCompleteMap(false)) {
          int idx = getInsertionPoint(map);
          myItems.add(idx,new MapInfo(-1,map));
        } else {
          int idx = getInsertionPoint(map);
          for (int i = map.getNrOfPins()-1 ; i >= 0  ; i--) {
            if (map.isMapped(i)) myItems.add(idx,new MapInfo(i,map));
          }
        }
      } else {
        if (map.isNotMapped()) {
          int idx = getInsertionPoint(map);
          myItems.add(idx,new MapInfo(-1,map));
        } else {
          int idx = getInsertionPoint(map);
          for (int i = map.getNrOfPins()-1 ; i >= 0  ; i--) {
            if (!map.isMapped(i)) {
              myItems.add(idx,new MapInfo(i,map));
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
    int idx = 0;
    while (idx < myItems.size() && myItems.get(idx).getMap().getDisplayString(-1).
            compareToIgnoreCase(map.getDisplayString(-1)) < 0) idx++;
    return idx;
  }
  
  @Override
  public int getSize() { return myItems.size(); }
  
  @Override
  public boolean isEmpty() { return myItems.isEmpty(); }
  
  @Override 
  public int size() { return myItems.size(); }
  
  @Override
  public MapInfo elementAt(int idx) {
    if (idx < 0 || idx >= myItems.size()) return null;
    return myItems.get(idx);
  }
  
  @Override
  public MapInfo getElementAt(int idx) { return elementAt(idx); }
}
