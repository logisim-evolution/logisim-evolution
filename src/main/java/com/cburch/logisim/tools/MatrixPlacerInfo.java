/*
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
