/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.util.ArrayList;

public class ComponentMapInformationContainer {

  private int nrOfInputBubbles;
  private int nrOfInOutBubbles;
  private int nrOfOutputBubbles;
  private ArrayList<String> inputBubbleLabels;
  private ArrayList<String> inOutBubbleLabels;
  private ArrayList<String> outputBubbleLabels;

  public ComponentMapInformationContainer(
      int inports,
      int outports,
      int inoutports,
      ArrayList<String> inportLabels,
      ArrayList<String> outportLabels,
      ArrayList<String> inoutportLabels) {
    nrOfInputBubbles = inports;
    nrOfOutputBubbles = outports;
    nrOfInOutBubbles = inoutports;
    inputBubbleLabels = inportLabels;
    outputBubbleLabels = outportLabels;
    inOutBubbleLabels = inoutportLabels;
  }

  public ComponentMapInformationContainer(
      int inports,
      int outports,
      int inoutport) {
    nrOfInputBubbles = inports;
    nrOfOutputBubbles = outports;
    nrOfInOutBubbles = inoutport;
    inputBubbleLabels = null;
    outputBubbleLabels = null;
    inOutBubbleLabels = null;
  }

  @Override
  public ComponentMapInformationContainer clone() {
    @SuppressWarnings("unchecked")
    ComponentMapInformationContainer myClone =
        new ComponentMapInformationContainer(
                nrOfInputBubbles,
                nrOfOutputBubbles,
                nrOfInOutBubbles,
                inputBubbleLabels == null ? null : (ArrayList<String>) inputBubbleLabels.clone(),
                outputBubbleLabels == null ? null : (ArrayList<String>) outputBubbleLabels.clone(),
                inOutBubbleLabels == null ? null : (ArrayList<String>) inOutBubbleLabels.clone());
    return myClone;
  }

  public String getInOutportLabel(int inOutNr) {
    if (inOutBubbleLabels == null) return Integer.toString(inOutNr);
    if (inOutBubbleLabels.size() <= inOutNr) return Integer.toString(inOutNr);
    return inOutBubbleLabels.get(inOutNr);
  }

  public String getInPortLabel(int inputNr) {
    if (inputBubbleLabels == null) return Integer.toString(inputNr);
    if (inputBubbleLabels.size() <= inputNr) return Integer.toString(inputNr);
    return inputBubbleLabels.get(inputNr);
  }

  public int getNrOfInOutPorts() {
    return nrOfInOutBubbles;
  }

  public int getNrOfInPorts() {
    return nrOfInputBubbles;
  }

  public int getNrOfOutPorts() {
    return nrOfOutputBubbles;
  }

  public String getOutPortLabel(int outputNr) {
    if (outputBubbleLabels == null) return Integer.toString(outputNr);
    if (outputBubbleLabels.size() <= outputNr) return Integer.toString(outputNr);
    return outputBubbleLabels.get(outputNr);
  }

  public void setNrOfInOutports(int count, ArrayList<String> labels) {
    nrOfInOutBubbles = count;
    inOutBubbleLabels = labels;
  }

  public void setNrOfInports(int count, ArrayList<String> labels) {
    nrOfInputBubbles = count;
    inputBubbleLabels = labels;
  }

  public void setNrOfOutports(int count, ArrayList<String> labels) {
    nrOfOutputBubbles = count;
    outputBubbleLabels = labels;
  }
}
