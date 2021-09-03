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

  private int NrOfInputBubbles;
  private int NrOfInOutBubbles;
  private int NrOfOutputBubbles;
  private ArrayList<String> InputBubbleLabels;
  private ArrayList<String> InOutBubbleLabels;
  private ArrayList<String> OutputBubbleLabels;

  public ComponentMapInformationContainer(
      int inports,
      int outports,
      int inoutports,
      ArrayList<String> inportLabels,
      ArrayList<String> outportLabels,
      ArrayList<String> inoutportLabels) {
    NrOfInputBubbles = inports;
    NrOfOutputBubbles = outports;
    NrOfInOutBubbles = inoutports;
    InputBubbleLabels = inportLabels;
    OutputBubbleLabels = outportLabels;
    InOutBubbleLabels = inoutportLabels;
  }

  public ComponentMapInformationContainer(
      int inports,
      int outports,
      int inoutport) {
    NrOfInputBubbles = inports;
    NrOfOutputBubbles = outports;
    NrOfInOutBubbles = inoutport;
    InputBubbleLabels = null;
    OutputBubbleLabels = null;
    InOutBubbleLabels = null;
  }

  @Override
  public ComponentMapInformationContainer clone() {
    @SuppressWarnings("unchecked")
    ComponentMapInformationContainer Myclone =
        new ComponentMapInformationContainer(
            NrOfInputBubbles,
            NrOfOutputBubbles,
            NrOfInOutBubbles,
            InputBubbleLabels == null ? null : (ArrayList<String>) InputBubbleLabels.clone(),
            OutputBubbleLabels == null ? null : (ArrayList<String>) OutputBubbleLabels.clone(),
            InOutBubbleLabels == null ? null : (ArrayList<String>) InOutBubbleLabels.clone());
    return Myclone;
  }

  public String GetInOutportLabel(int inoutNr) {
    if (InOutBubbleLabels == null) {
      return Integer.toString(inoutNr);
    }
    if (InOutBubbleLabels.size() <= inoutNr) {
      return Integer.toString(inoutNr);
    }
    return InOutBubbleLabels.get(inoutNr);
  }

  public String GetInportLabel(int inputNr) {
    if (InputBubbleLabels == null) {
      return Integer.toString(inputNr);
    }
    if (InputBubbleLabels.size() <= inputNr) {
      return Integer.toString(inputNr);
    }
    return InputBubbleLabels.get(inputNr);
  }

  public int GetNrOfInOutports() {
    return NrOfInOutBubbles;
  }

  public int GetNrOfInports() {
    return NrOfInputBubbles;
  }

  public int GetNrOfOutports() {
    return NrOfOutputBubbles;
  }

  public String GetOutportLabel(int outputNr) {
    if (OutputBubbleLabels == null) {
      return Integer.toString(outputNr);
    }
    if (OutputBubbleLabels.size() <= outputNr) {
      return Integer.toString(outputNr);
    }
    return OutputBubbleLabels.get(outputNr);
  }

  public void setNrOfInOutports(int nb, ArrayList<String> labels) {
    NrOfInOutBubbles = nb;
    InOutBubbleLabels = labels;
  }

  public void setNrOfInports(int nb, ArrayList<String> labels) {
    NrOfInputBubbles = nb;
    InputBubbleLabels = labels;
  }

  public void setNrOfOutports(int nb, ArrayList<String> labels) {
    NrOfOutputBubbles = nb;
    OutputBubbleLabels = labels;
  }
}
