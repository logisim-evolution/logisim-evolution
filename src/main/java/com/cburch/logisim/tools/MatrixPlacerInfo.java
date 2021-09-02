/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;

public class MatrixPlacerInfo {

  private final String OldLabel;
  private String SharedLabel;
  private int NrOfXCopies = 1;
  private int NrOfYCopies = 1;
  private int XDisplacement = 1;
  private int YDisplacement = 1;
  private int XDmin = 1;
  private int YDmin = 1;

  public MatrixPlacerInfo(String Label) {
    SharedLabel = Label;
    OldLabel = Label;
  }

  void SetBounds(Bounds bds) {
    XDisplacement = XDmin = (bds.getWidth() + 9) / 10;
    YDisplacement = YDmin = (bds.getHeight() + 9) / 10;
  }

  int getMinimalXDisplacement() {
    return XDmin;
  }

  int getMinimalYDisplacement() {
    return YDmin;
  }

  String GetLabel() {
    return SharedLabel;
  }

  void UndoLabel() {
    SharedLabel = OldLabel;
  }

  void SetLabel(String Lab) {
    SharedLabel = Lab;
  }

  int getNrOfXCopies() {
    return NrOfXCopies;
  }

  void setNrOfXCopies(int val) {
    NrOfXCopies = val;
  }

  int getNrOfYCopies() {
    return NrOfYCopies;
  }

  void setNrOfYCopies(int val) {
    NrOfYCopies = val;
  }

  int GetDeltaX() {
    return XDisplacement * 10;
  }

  void SetDeltaX(int value) {
    if (value > 0) XDisplacement = (value + 9) / 10;
  }

  void setXDisplacement(int value) {
    if (value > 0) XDisplacement = value;
  }

  int getXDisplacement() {
    return XDisplacement;
  }

  int GetDeltaY() {
    return YDisplacement * 10;
  }

  void SetDeltaY(int value) {
    if (value > 0) YDisplacement = (value + 9) / 10;
  }

  void setYisplacement(int value) {
    if (value > 0) YDisplacement = value;
  }

  int getYDisplacement() {
    return YDisplacement;
  }
}
