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

package com.cburch.logisim.circuit;

import java.util.ArrayList;

import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.MapComponent;

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
    
    public CircuitMapInfo(int sourceId, int ioId, int xpos , int ypos) {
      pinid = sourceId;
      ioid = ioId;
      rect = new BoardRectangle(xpos,ypos,1,1);
    }
    
    public CircuitMapInfo(int x , int y) {
      oldMapFormat = false;
      rect = new BoardRectangle(x,y,1,1);
    }
    
    public void addPinMap(CircuitMapInfo map) {
      if (pinmaps == null) {
        pinmaps = new ArrayList<CircuitMapInfo>();
        oldMapFormat = false;
      }
      pinmaps.add(map);
    }
    
    public void addPinMap(int x , int y , int loc) {
      if (pinmaps == null) {
        pinmaps = new ArrayList<CircuitMapInfo>();
        oldMapFormat = false;
      }
      int sloc = pinmaps.size();
      pinmaps.add(new CircuitMapInfo(sloc,loc,x,y));
    }
      
    public boolean isSinglePin() { return pinid >= 0; }
    public int getPinId() { return pinid; }
    public int getIOId() { return ioid; }
    public BoardRectangle getRectangle() { return rect; }
    public Long getConstValue() { return constValue; }
    public boolean isOpen() { return rect==null && constValue == null && oldMapFormat; }
    public boolean isConst() { return rect==null && constValue != null; }
    public boolean isOldFormat() { return oldMapFormat; }
    public MapComponent getMap() { return myMap; }
    public ArrayList<CircuitMapInfo> getPinMaps() { return pinmaps; }
}
