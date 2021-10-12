/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

public class BubbleInformationContainer {
  private int myInBubblesStartIndex;
  private int myInBubblesEndIndex;
  private int myInOutBubblesStartIndex;
  private int myInOutBubblesEndIndex;
  private int myOutBubblesStartIndex;
  private int myOutBubblesEndIndex;

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

  public void setOutputBubblesInformation(int startIndex, int endIndex) {
    myOutBubblesStartIndex = startIndex;
    myOutBubblesEndIndex = endIndex;
  }
}
