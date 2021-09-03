/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    if (MyComponents == null) MyComponents = new LinkedList<>();
    else MyComponents.clear();
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
    ArrayList<BoardRectangle> result = new ArrayList<>();
    for (FPGAIOInformationContainer comp : MyComponents) {
      if (comp.GetType().equals(type)) {
        if (!type.equals(IOComponentTypes.DIPSwitch) || nrOfPins <= comp.getNrOfPins()) {
          if (!type.equals(IOComponentTypes.PortIO) || nrOfPins <= comp.getNrOfPins()) {
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
