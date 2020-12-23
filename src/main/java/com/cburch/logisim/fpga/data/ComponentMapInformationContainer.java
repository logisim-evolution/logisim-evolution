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

  public ComponentMapInformationContainer clone() {
    @SuppressWarnings("unchecked")
	ComponentMapInformationContainer Myclone =
        new ComponentMapInformationContainer(
            NrOfInputBubbles,
            NrOfOutputBubbles,
            NrOfInOutBubbles,
            InputBubbleLabels == null ? null : (ArrayList<String>)InputBubbleLabels.clone(),
            OutputBubbleLabels == null ? null : (ArrayList<String>)OutputBubbleLabels.clone(),
            InOutBubbleLabels == null ? null : (ArrayList<String>)InOutBubbleLabels.clone());
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
  
  public void setNrOfOutports(int nb , ArrayList<String> labels) {
    NrOfOutputBubbles = nb;
    OutputBubbleLabels = labels;
  }
}
