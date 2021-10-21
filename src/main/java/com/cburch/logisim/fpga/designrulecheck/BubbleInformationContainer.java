/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import lombok.Getter;

public class BubbleInformationContainer {
  @Getter private int inBubblesStartIndex;
  @Getter private int inBubblesEndIndex;
  @Getter private int inOutBubblesStartIndex;
  @Getter private int inOutBubblesEndIndex;
  @Getter private int outBubblesStartIndex;
  @Getter private int outBubblesEndIndex;

  public boolean hasInOutBubbles() {
    return ((inOutBubblesStartIndex >= 0) && (inOutBubblesEndIndex >= 0));
  }

  public boolean hasInputBubbles() {
    return ((inBubblesStartIndex >= 0) && (inBubblesEndIndex >= 0));
  }

  public boolean hasOutputBubbles() {
    return ((outBubblesStartIndex >= 0) && (outBubblesEndIndex >= 0));
  }

  public int nrOfInOutBubbles() {
    if ((inOutBubblesStartIndex < 0) || (inOutBubblesEndIndex < 0)) return 0;
    return (inOutBubblesEndIndex - inOutBubblesStartIndex) + 1;
  }

  public int nrOfInputBubbles() {
    if ((inBubblesStartIndex < 0) || (inBubblesEndIndex < 0)) return 0;
    return (inBubblesEndIndex - inBubblesStartIndex) + 1;
  }

  public int nrOfOutputBubbles() {
    if ((outBubblesStartIndex < 0) || (outBubblesEndIndex < 0)) return 0;
    return (outBubblesEndIndex - outBubblesStartIndex) + 1;
  }

  public void setInOutBubblesInformation(int startIndex, int endIndex) {
    inOutBubblesStartIndex = startIndex;
    inOutBubblesEndIndex = endIndex;
  }

  public void setInputBubblesInformation(int startIndex, int endIndex) {
    inBubblesStartIndex = startIndex;
    inBubblesEndIndex = endIndex;
  }

  public void setOutputBubblesInformation(int startIndex, int endIndex) {
    outBubblesStartIndex = startIndex;
    outBubblesEndIndex = endIndex;
  }
}
