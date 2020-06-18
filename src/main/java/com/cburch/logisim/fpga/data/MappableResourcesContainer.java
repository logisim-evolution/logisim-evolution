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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMapInfo;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.proj.ProjectActions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappableResourcesContainer {

  static final Logger logger = LoggerFactory.getLogger(MappableResourcesContainer.class);

  private Circuit myCircuit;
  private BoardInformation currentUsedBoard;
  private IOComponentsInformation IOcomps;
  private Map<ArrayList<String>, MapComponent> myMappableResources;
  private List<FPGAIOInformationContainer> myIOComponents;

  /*
   * We differentiate two notation for each component, namely: 1) The display
   * name: "LED: /LED1". This name can be augmented with alternates, e.g. a
   * 7-segment display could either be: "SEVENSEGMENT: /DS1" or
   * "SEVENSEGMENT: /DS1#Segment_A",etc.
   *
   * 2) The map name: "FPGA4U:/LED1" or "FPGA4U:/DS1#Segment_A", etc.
   *
   * The MappedList keeps track of the display names.
   */
  public MappableResourcesContainer(BoardInformation CurrentBoard, 
                                    Circuit circ) {
    currentUsedBoard = CurrentBoard;
    myCircuit = circ;
    ArrayList<String> BoardId = new ArrayList<String>();
    BoardId.add(CurrentBoard.getBoardName());
    myIOComponents = new ArrayList<FPGAIOInformationContainer>();
    for (FPGAIOInformationContainer io : currentUsedBoard.GetAllComponents()) {
      try {
        FPGAIOInformationContainer clone = (FPGAIOInformationContainer) io.clone();
        clone.setMapMode();
        myIOComponents.add(clone);
      } catch (CloneNotSupportedException e) { continue; }
    }
    updateMapableComponents();
    circ.setBoardMap(CurrentBoard.getBoardName(), this);
  }
  
/*
 * Here we define the new structure of MappableResourcesContainer that allows for more features 
 * and has less complexity; being compatible with the old version
 */
  public IOComponentsInformation getIOComponentInformation() {
    if (IOcomps == null) {
      IOcomps = new IOComponentsInformation(null,true);
      for (FPGAIOInformationContainer io : myIOComponents) IOcomps.addComponent(io, 1);
       /* TODO: build-up info */
    }
    return IOcomps;
  }
  
  public Map<ArrayList<String>, MapComponent> getMappableResources() { return myMappableResources; }
  
  public void destroyIOComponentInformation() { 
    IOcomps.clear();
    IOcomps = null;
  }
  
  public String getToplevelName() { return myCircuit.getName(); }

  public BoardInformation getBoardInformation() {
    return currentUsedBoard;
  }

  public void save() {
    ProjectActions.doSave(myCircuit.getProject());
  }
  
  public void updateMapableComponents() {
    HashSet<ArrayList<String>> cur = new HashSet<ArrayList<String>>(); 
    if (myMappableResources == null)
      myMappableResources = new HashMap<ArrayList<String>, MapComponent>();
    else
      cur.addAll(myMappableResources.keySet());
    ArrayList<String> BoardId = new ArrayList<String>();
    BoardId.add(currentUsedBoard.getBoardName());
    Map<ArrayList<String>, NetlistComponent> newMappableResources = 
           myCircuit.getNetList().GetMappableResources(BoardId, true);
    for (ArrayList<String> key : newMappableResources.keySet()) {
      if (cur.contains(key)) {
        MapComponent comp = myMappableResources.get(key);
        if (!comp.equalsType(newMappableResources.get(key))) {
          comp.unmap();
          myMappableResources.put(key, new MapComponent(key,newMappableResources.get(key)));
        } else {
          MapComponent newMap = new MapComponent(key,newMappableResources.get(key));
          newMap.copyMapFrom(comp);
          myMappableResources.put(key, newMap);
        }
        cur.remove(key);
      } else {
        myMappableResources.put(key, new MapComponent(key,newMappableResources.get(key)));
      }
    }
    for (ArrayList<String>key : cur) {
      myMappableResources.get(key).unmap();
      myMappableResources.remove(key);
    }
  }
  
  public void tryMap(String mapKey, CircuitMapInfo cmap) {
    ArrayList<String> key = getHierarchyName(mapKey);
    if (!myMappableResources.containsKey(key)) return;
    if (mapKey.contains("#")) myMappableResources.get(key).tryMap(mapKey, cmap, myIOComponents);
    else  myMappableResources.get(key).tryMap(cmap,myIOComponents);
  }
  
  public void tryMap(String mapKey, BoardRectangle rect) {
    tryMap(mapKey, new CircuitMapInfo(rect));
  }
  
  public Map<String,CircuitMapInfo> getCircuitMap() {
    int id = 0;
    HashMap<String,CircuitMapInfo> result = new HashMap<String,CircuitMapInfo>(); 
    for (ArrayList<String> key : myMappableResources.keySet()) {
      result.put(Integer.toString(id++), new CircuitMapInfo(myMappableResources.get(key)));
    }
    return result;
  }
  
  public void unMapAll() {
    for (ArrayList<String> key : myMappableResources.keySet())
      myMappableResources.get(key).unmap();
  }
  
  private ArrayList<String> getHierarchyName(String mapKey) {
    String[] split1 = mapKey.split(" ");
    String hier = split1[split1.length-1];
    String[] split2 = hier.split("#");
    ArrayList<String> result = new ArrayList<String>();
    result.add(currentUsedBoard.getBoardName());
    for (String key : split2[0].split("/")) 
      if (!key.isEmpty()) result.add(key);
    return result;
  }
  
  public void markChanged() {
    myCircuit.getProject().setForcedDirty();
  }
  
  public boolean isCompletelyMapped() {
boolean result = true;
    for (ArrayList<String> key : myMappableResources.keySet()) {
      MapComponent map = myMappableResources.get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++)
        result &= map.isMapped(i);
    }
    return result;
  }
  
  public ArrayList<String> GetMappedIOPinNames() {
    ArrayList<String> result = new ArrayList<String>();
    for (ArrayList<String> key : myMappableResources.keySet()) {
      MapComponent map = myMappableResources.get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        if (!map.isIO(i)) continue;
        if (map.isBoardMapped(i)) {
          StringBuffer sb = new StringBuffer();
          if (map.isExternalInverted(i)) sb.append("n_");
          sb.append(map.getHdlString(i));
          result.add(sb.toString());
        }
      }
    }
    return result;
  }

  public ArrayList<String> GetMappedInputPinNames() {
    ArrayList<String> result = new ArrayList<String>();
    for (ArrayList<String> key : myMappableResources.keySet()) {
      MapComponent map = myMappableResources.get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        if (!map.isInput(i)) continue;
        if (map.isBoardMapped(i)) {
          StringBuffer sb = new StringBuffer();
          if (map.isExternalInverted(i)) sb.append("n_");
          sb.append(map.getHdlString(i));
          result.add(sb.toString());
        }
      }
    }
    return result;
  }

  public ArrayList<String> GetMappedOutputPinNames() {
    ArrayList<String> result = new ArrayList<String>();
    for (ArrayList<String> key : myMappableResources.keySet()) {
      MapComponent map = myMappableResources.get(key);
      for (int i = 0 ; i < map.getNrOfPins() ; i++) {
        if (!map.isOutput(i)) continue;
        if (map.isBoardMapped(i)) {
          StringBuffer sb = new StringBuffer();
          if (map.isExternalInverted(i)) sb.append("n_");
          sb.append(map.getHdlString(i));
          result.add(sb.toString());
        }
      }
    }
    return result;
  }
}
