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

package com.cburch.logisim.fpga.designrulecheck;

public class BubbleInformationContainer {
  private int MyInBubblesStartIndex;
  private int MyInBubblesEndIndex;
  private int MyInOutBubblesStartIndex;
  private int MyInOutBubblesEndIndex;
  private int MyOutBubblesStartIndex;
  private int MyOutBubblesEndIndex;

  public void BubbleInformation() {
    MyInBubblesStartIndex =
    MyInBubblesEndIndex =
    MyOutBubblesStartIndex =
    MyOutBubblesEndIndex = 
    MyInOutBubblesStartIndex = 
    MyInOutBubblesEndIndex = -1;
  }

  public int GetInOutEndIndex() {
    return MyInOutBubblesEndIndex;
  }

  public int GetInOutStartIndex() {
    return MyInOutBubblesStartIndex;
  }

  public int GetInputEndIndex() {
    return MyInBubblesEndIndex;
  }

  public int GetInputStartIndex() {
    return MyInBubblesStartIndex;
  }

  public int GetOutputEndIndex() {
    return MyOutBubblesEndIndex;
  }

  public int GetOutputStartIndex() {
    return MyOutBubblesStartIndex;
  }

  public boolean HasInOutBubbles() {
    return ((MyInOutBubblesStartIndex >= 0) && (MyInOutBubblesEndIndex >= 0));
  }

  public boolean HasInputBubbles() {
    return ((MyInBubblesStartIndex >= 0) && (MyInBubblesEndIndex >= 0));
  }

  public boolean HasOutputBubbles() {
    return ((MyOutBubblesStartIndex >= 0) && (MyOutBubblesEndIndex >= 0));
  }

  public int NrOfInOutBubbles() {
    if ((MyInOutBubblesStartIndex < 0) || (MyInOutBubblesEndIndex < 0)) return 0;
    return (MyInOutBubblesEndIndex - MyInOutBubblesStartIndex) + 1;
  }

  public int NrOfInputBubbles() {
    if ((MyInBubblesStartIndex < 0) || (MyInBubblesEndIndex < 0)) return 0;
    return (MyInBubblesEndIndex - MyInBubblesStartIndex) + 1;
  }

  public int NrOfOutputBubbles() {
    if ((MyOutBubblesStartIndex < 0) || (MyOutBubblesEndIndex < 0)) return 0;
    return (MyOutBubblesEndIndex - MyOutBubblesStartIndex) + 1;
  }

  public void SetInOutBubblesInformation(int StartIndex, int EndIndex) {
    MyInOutBubblesStartIndex = StartIndex;
    MyInOutBubblesEndIndex = EndIndex;
  }

  public void SetInputBubblesInformation(int StartIndex, int EndIndex) {
    MyInBubblesStartIndex = StartIndex;
    MyInBubblesEndIndex = EndIndex;
  }

  public void SetOutputBubblesInformation(int StartIndex, int EndIndex) {
    MyOutBubblesStartIndex = StartIndex;
    MyOutBubblesEndIndex = EndIndex;
  }
}
