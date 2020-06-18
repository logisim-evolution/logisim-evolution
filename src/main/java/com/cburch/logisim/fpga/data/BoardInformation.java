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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BoardInformation {

  private LinkedList<FPGAIOInformationContainer> MyComponents;
  private String boardname;
  private BufferedImage BoardPicture;
  public FPGAClass fpga = new FPGAClass();

  public BoardInformation() {
    this.clear();
  }

  public void AddComponent(FPGAIOInformationContainer comp) {
    MyComponents.add(comp);
  }

  public void clear() {
	if (MyComponents == null)
	  MyComponents = new LinkedList<FPGAIOInformationContainer>();
	else
      MyComponents.clear();
    boardname = null;
    fpga.clear();
    BoardPicture = null;
  }
  
  public void setComponents(List<FPGAIOInformationContainer> comps) {
    MyComponents.clear();
    MyComponents.addAll(comps);  
  }

  public LinkedList<FPGAIOInformationContainer> GetAllComponents() {
    return MyComponents;
  }

  public String getBoardName() {
    return boardname;
  }

  public FPGAIOInformationContainer GetComponent(BoardRectangle rect) {
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetRectangle().equals(rect)) {
        return comp;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Map<String, ArrayList<Integer>> GetComponents() {
    Map<String, ArrayList<Integer>> result = new HashMap<>();
    ArrayList<Integer> list = new ArrayList<>();

    int count = 0;
    for (IOComponentTypes type : IOComponentTypes.KnownComponentSet) {
      count = 0;
      for (FPGAIOInformationContainer comp : MyComponents) {
        if (comp.GetType().equals(type)) {
          list.add(count, comp.getNrOfPins());
          count++;
        }
      }
      if (count > 0) {
        result.put(type.toString(), (ArrayList<Integer>) list.clone());
      }
      list.clear();
    }

    return result;
  }

  public String GetComponentType(BoardRectangle rect) {
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetRectangle().equals(rect)) {
        return comp.GetType().toString();
      }
    }
    return IOComponentTypes.Unknown.toString();
  }

  public String getDriveStrength(BoardRectangle rect) {
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetRectangle().equals(rect)) {
        return DriveStrength.GetContraintedDriveStrength(comp.GetDrive());
      }
    }
    return "";
  }

  public BufferedImage GetImage() {
    return BoardPicture;
  }

  public ArrayList<BoardRectangle> GetIoComponentsOfType(
      IOComponentTypes type, int nrOfPins) {
    ArrayList<BoardRectangle> result = new ArrayList<BoardRectangle>();
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetType().equals(type)) {
        if ((!type.equals(IOComponentTypes.DIPSwitch))
            || (type.equals(IOComponentTypes.DIPSwitch) && (nrOfPins <= comp.getNrOfPins()))) {
          if ((!type.equals(IOComponentTypes.PortIO))
              || (type.equals(IOComponentTypes.PortIO) && (nrOfPins <= comp.getNrOfPins()))) {
            result.add(comp.GetRectangle());
          }
        }
      }
    }
    return result;
  }

  public String getIOStandard(BoardRectangle rect) {
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetRectangle().equals(rect)) {
        return IoStandards.GetConstraintedIoStandard(comp.GetIOStandard());
      }
    }
    return "";
  }

  public int GetNrOfDefinedComponents() {
    return MyComponents.size();
  }

  public String getPullBehavior(BoardRectangle rect) {
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetRectangle().equals(rect)) {
        return PullBehaviors.getContraintedPullString(comp.GetPullBehavior());
      }
    }
    return "";
  }

  public void setBoardName(String name) {
    boardname = name;
  }

  public void SetImage(BufferedImage pict) {
    BoardPicture = pict;
  }
}
