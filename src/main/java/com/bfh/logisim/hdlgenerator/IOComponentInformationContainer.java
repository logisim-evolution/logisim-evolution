/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.hdlgenerator;

import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;

public class IOComponentInformationContainer {

	private int NrOfInputBubbles;
	private int NrOfInOutBubbles;
	private int NrOfOutputBubbles;
	private ArrayList<String> InputBubbleLabels;
	private ArrayList<String> InOutBubbleLabels;
	private ArrayList<String> OutputBubbleLabels;
	private FPGAIOInformationContainer.IOComponentTypes MainMapType;
	private ArrayList<FPGAIOInformationContainer.IOComponentTypes> AlternateMapTypes;

	public IOComponentInformationContainer(int inports, int outports,
			int inoutports, ArrayList<String> inportLabels,
			ArrayList<String> outportLabels, ArrayList<String> inoutportLabels,
			FPGAIOInformationContainer.IOComponentTypes MapType) {
		NrOfInputBubbles = inports;
		NrOfOutputBubbles = outports;
		NrOfInOutBubbles = inoutports;
		InputBubbleLabels = inportLabels;
		OutputBubbleLabels = outportLabels;
		InOutBubbleLabels = inoutportLabels;
		MainMapType = MapType;
		AlternateMapTypes = new ArrayList<FPGAIOInformationContainer.IOComponentTypes>();
	}

	public IOComponentInformationContainer(int inports, int outports,
			int inoutport, FPGAIOInformationContainer.IOComponentTypes MapType) {
		NrOfInputBubbles = inports;
		NrOfOutputBubbles = outports;
		NrOfInOutBubbles = inoutport;
		InputBubbleLabels = null;
		OutputBubbleLabels = null;
		InOutBubbleLabels = null;
		MainMapType = MapType;
		AlternateMapTypes = new ArrayList<FPGAIOInformationContainer.IOComponentTypes>();
	}

	public void AddAlternateMapType(
			FPGAIOInformationContainer.IOComponentTypes map) {
		AlternateMapTypes.add(map);
	}

	public IOComponentInformationContainer clone() {
		IOComponentInformationContainer Myclone = new IOComponentInformationContainer(
				NrOfInputBubbles, NrOfOutputBubbles, NrOfInOutBubbles,
				InputBubbleLabels, OutputBubbleLabels, InOutBubbleLabels,
				MainMapType);
		for (FPGAIOInformationContainer.IOComponentTypes Alt : AlternateMapTypes) {
			Myclone.AddAlternateMapType(Alt);
		}
		return Myclone;
	}

	public FPGAIOInformationContainer.IOComponentTypes GetAlternateMapType(
			int id) {
		if (id >= AlternateMapTypes.size()) {
			return FPGAIOInformationContainer.IOComponentTypes.Unknown;
		} else {
			return AlternateMapTypes.get(id);
		}
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
			return AlternateMapTypes.get(0).name() + Integer.toString(inputNr);
		}
		if (InputBubbleLabels.size() <= inputNr) {
			return Integer.toString(inputNr);
		}
		return InputBubbleLabels.get(inputNr);
	}

	public FPGAIOInformationContainer.IOComponentTypes GetMainMapType() {
		return MainMapType;
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
			return AlternateMapTypes.get(0).name() + Integer.toString(outputNr);
		}
		if (OutputBubbleLabels.size() <= outputNr) {
			return Integer.toString(outputNr);
		}
		return OutputBubbleLabels.get(outputNr);
	}

	public boolean HasAlternateMapTypes() {
		return !AlternateMapTypes.isEmpty();
	}

	public void setNrOfInOutports(int nb, ArrayList<String> labels) {
		NrOfInOutBubbles = nb;
		InOutBubbleLabels = labels;
	}

	public void setNrOfInports(int nb, ArrayList<String> labels) {
		NrOfInputBubbles = nb;
		InputBubbleLabels = labels;
	}
}
