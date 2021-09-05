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

package com.cburch.logisim.fpga.designrulecheck;

public class BubbleInformationContainer {
  private int myInBubblesStartIndex;
  private int myInBubblesEndIndex;
  private int myInOutBubblesStartIndex;
  private int myInOutBubblesEndIndex;
  private int myOutBubblesStartIndex;
  private int myOutBubblesEndIndex;

  public void BubbleInformation() {
    myInBubblesStartIndex =
        myInBubblesEndIndex =
            myOutBubblesStartIndex =
                myOutBubblesEndIndex = myInOutBubblesStartIndex = myInOutBubblesEndIndex = -1;
  }

  public int getInOutEndIndex() {
    return myInOutBubblesEndIndex;
  }

  public int getInOutStartIndex() {
    return myInOutBubblesStartIndex;
  }

  public int getInputEndIndex() {
    return myInBubblesEndIndex;
  }

  public int getInputStartIndex() {
    return myInBubblesStartIndex;
  }

  public int getOutputEndIndex() {
    return myOutBubblesEndIndex;
  }

  public int getOutputStartIndex() {
    return myOutBubblesStartIndex;
  }

  public boolean hasInOutBubbles() {
    return ((myInOutBubblesStartIndex >= 0) && (myInOutBubblesEndIndex >= 0));
  }

  public boolean hasInputBubbles() {
    return ((myInBubblesStartIndex >= 0) && (myInBubblesEndIndex >= 0));
  }

  public boolean hasOutputBubbles() {
    return ((myOutBubblesStartIndex >= 0) && (myOutBubblesEndIndex >= 0));
  }

  public int nrOfInOutBubbles() {
    if ((myInOutBubblesStartIndex < 0) || (myInOutBubblesEndIndex < 0)) return 0;
    return (myInOutBubblesEndIndex - myInOutBubblesStartIndex) + 1;
  }

  public int nrOfInputBubbles() {
    if ((myInBubblesStartIndex < 0) || (myInBubblesEndIndex < 0)) return 0;
    return (myInBubblesEndIndex - myInBubblesStartIndex) + 1;
  }

  public int nrOfOutputBubbles() {
    if ((myOutBubblesStartIndex < 0) || (myOutBubblesEndIndex < 0)) return 0;
    return (myOutBubblesEndIndex - myOutBubblesStartIndex) + 1;
  }

  public void setInOutBubblesInformation(int startIndex, int endIndex) {
    myInOutBubblesStartIndex = startIndex;
    myInOutBubblesEndIndex = endIndex;
  }

  public void setInputBubblesInformation(int startIndex, int endIndex) {
    myInBubblesStartIndex = startIndex;
    myInBubblesEndIndex = endIndex;
  }

  public void setOutputBubblesInformation(int startIndex, int EndIndex) {
    myOutBubblesStartIndex = startIndex;
    myOutBubblesEndIndex = EndIndex;
  }
}
