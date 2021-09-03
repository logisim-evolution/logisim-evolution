/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;
import lombok.Getter;
import lombok.Setter;

public class MatrixPlacerInfo {

  private final String oldLabel;
  @Getter @Setter private String label;
  @Getter @Setter private int nrOfXCopies = 1;
  @Getter @Setter private int nrOfYCopies = 1;
  @Getter private int displacementX = 1;
  @Getter private int displacementY = 1;
  @Getter private int minimalDisplacementX = 1;
  @Getter private int minimalDisplacementY = 1;

  public MatrixPlacerInfo(String label) {
    this.label = label;
    oldLabel = label;
  }

  void setBounds(Bounds bds) {
    displacementX = minimalDisplacementX = (bds.getWidth() + 9) / 10;
    displacementY = minimalDisplacementY = (bds.getHeight() + 9) / 10;
  }

  void undoLabel() {
    label = oldLabel;
  }

  int getDeltaX() {
    return displacementX * 10;
  }

  void setDeltaX(int value) {
    if (value > 0) displacementX = (value + 9) / 10;
  }

  void setDisplacementX(int value) {
    if (value > 0) displacementX = value;
  }

  int getDeltaY() {
    return displacementY * 10;
  }

  void setDeltaY(int value) {
    if (value > 0) displacementY = (value + 9) / 10;
  }

  void setDisplacementY(int value) {
    if (value > 0) displacementY = value;
  }
}
