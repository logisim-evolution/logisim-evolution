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

package com.bfh.logisim.designrulecheck;

import static com.cburch.logisim.std.io.PortIO.ATTR_SIZE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.wiring.Pin;

public class NetlistComponent {

	private int nr_of_ends;
	private Component CompReference;
	private ArrayList<ConnectionEnd> Ends;
	IOComponentInformationContainer MyIOInformation;
	private Map<ArrayList<String>, BubbleInformationContainer> GlobalIds;
	private BubbleInformationContainer LocalId;
	private Map<String, BoardRectangle> BoardMaps;
	private Map<ArrayList<String>, Boolean> AlternateMapEnabled;
	private Map<ArrayList<String>, Boolean> AlternateMapLocked;
	private Map<String, String> CurrentMapType;

	public NetlistComponent(Component Ref) {
		nr_of_ends = Ref.getEnds().size();
		CompReference = Ref;
		Ends = new ArrayList<ConnectionEnd>();
		for (int i = 0; i < Ref.getEnds().size(); i++) {
			Ends.add(new ConnectionEnd(Ref.getEnd(i).isOutput(), (byte) Ref
					.getEnd(i).getWidth().getWidth(),Ref));
		}
		if (Ref.getFactory().getIOInformation() != null) {
			MyIOInformation = Ref.getFactory().getIOInformation().clone();
			if (Ref.getFactory() instanceof PortIO) {
				MyIOInformation.setNrOfInOutports(Ref.getAttributeSet()
						.getValue(ATTR_SIZE), PortIO.GetLabels(Ref
						.getAttributeSet().getValue(ATTR_SIZE)));
			}
		} else {
			if (Ref.getFactory() instanceof Pin) {
				int NrOfBits = Ref.getEnd(0).getWidth().getWidth();
				FPGAIOInformationContainer.IOComponentTypes MainType = (NrOfBits > 1) ? FPGAIOInformationContainer.IOComponentTypes.Bus
						: FPGAIOInformationContainer.IOComponentTypes.Pin;
				if (Ref.getEnd(0).isInput() && Ref.getEnd(0).isOutput()) {
					MyIOInformation = new IOComponentInformationContainer(0, 0,
							NrOfBits, MainType);
					if (NrOfBits > 1) {
						MyIOInformation
								.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
					}
					MyIOInformation
							.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.PortIO);
				} else if (Ref.getEnd(0).isInput()) {
					MyIOInformation = new IOComponentInformationContainer(0,
							NrOfBits, 0, MainType);
					if (NrOfBits > 1) {
						MyIOInformation
								.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
					}
					MyIOInformation
							.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.LED);
				} else {
					MyIOInformation = new IOComponentInformationContainer(
							NrOfBits, 0, 0, MainType);
					if (NrOfBits > 1) {
						MyIOInformation
								.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
					}
					MyIOInformation
							.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Button);
				}
			} else {
				MyIOInformation = null;
			}
		}
		GlobalIds = null;
		LocalId = null;
		BoardMaps = new HashMap<String, BoardRectangle>();
		AlternateMapEnabled = new HashMap<ArrayList<String>, Boolean>();
		AlternateMapLocked = new HashMap<ArrayList<String>, Boolean>();
		CurrentMapType = new HashMap<String, String>();
	}

	public void AddGlobalBubbleID(ArrayList<String> HierarchyName,
			int InputBubblesStartId, int NrOfInputBubbles,
			int OutputBubblesStartId, int NrOfOutputBubbles,
			int InOutBubblesStartId, int NrOfInOutBubbles) {
		if ((NrOfInputBubbles == 0) && (NrOfOutputBubbles == 0)
				&& (NrOfInOutBubbles == 0)) {
			return;
		}
		if (GlobalIds == null) {
			GlobalIds = new HashMap<ArrayList<String>, BubbleInformationContainer>();
		}
		BubbleInformationContainer thisInfo = new BubbleInformationContainer();
		if (NrOfInputBubbles > 0) {
			thisInfo.SetInputBubblesInformation(InputBubblesStartId,
					InputBubblesStartId + NrOfInputBubbles - 1);
		}
		if (NrOfInOutBubbles > 0) {
			thisInfo.SetInOutBubblesInformation(InOutBubblesStartId,
					InOutBubblesStartId + NrOfInOutBubbles - 1);
		}
		if (NrOfOutputBubbles > 0) {
			thisInfo.SetOutputBubblesInformation(OutputBubblesStartId,
					OutputBubblesStartId + NrOfOutputBubbles - 1);
		}
		GlobalIds.put(HierarchyName, thisInfo);
	}

	public void addMap(String MapName, BoardRectangle map, String MapType) {
		BoardMaps.put(MapName, map);
		CurrentMapType.put(MapName, MapType);
	}

	public boolean AlternateMappingEnabled(ArrayList<String> key) {
		if (!AlternateMapEnabled.containsKey(key)) {
			AlternateMapEnabled.put(key, MyIOInformation.GetMainMapType()
					.equals(FPGAIOInformationContainer.IOComponentTypes.Bus));
			AlternateMapLocked.put(key, MyIOInformation.GetMainMapType()
					.equals(FPGAIOInformationContainer.IOComponentTypes.Bus));
		}
		return AlternateMapEnabled.get(key);
	}

	public boolean AlternateMappingIsLocked(ArrayList<String> key) {
		if (!AlternateMapLocked.containsKey(key)) {
			AlternateMapLocked.put(key, MyIOInformation.GetMainMapType()
					.equals(FPGAIOInformationContainer.IOComponentTypes.Bus));
		}
		return AlternateMapLocked.get(key);
	}

	public boolean EndIsConnected(int index) {
		if ((index < 0) || (index >= nr_of_ends)) {
			return false;
		}
		boolean isConnected = false;
		ConnectionEnd ThisEnd = Ends.get(index);
		for (int i = 0; i < ThisEnd.NrOfBits(); i++) {
			isConnected |= (ThisEnd.GetConnection((byte) i).GetParrentNet() != null);
		}
		return isConnected;
	}
	
	public boolean EndIsInput(int index) {
		if ((index < 0) || (index >= nr_of_ends)) {
			return false;
		}
		return CompReference.getEnd(index).isInput();
	}

	public Component GetComponent() {
		return CompReference;
	}

	public byte GetConnectionBitIndex(Net RootNet, byte BitIndex) {
		for (ConnectionEnd search : Ends) {
			for (byte bit = 0; bit < search.NrOfBits(); bit++) {
				ConnectionPoint connection = search.GetConnection(bit);
				if (connection.GetParrentNet() == RootNet
						&& connection.GetParrentNetBitIndex() == BitIndex) {
					return bit;
				}
			}
		}
		return -1;
	}

	public ArrayList<ConnectionPoint> GetConnections(Net RootNet,
			byte BitIndex, boolean IsOutput) {
		ArrayList<ConnectionPoint> Connections = new ArrayList<ConnectionPoint>();
		for (ConnectionEnd search : Ends) {
			for (byte bit = 0; bit < search.NrOfBits(); bit++) {
				ConnectionPoint connection = search.GetConnection(bit);
				if (connection.GetParrentNet() == RootNet
						&& connection.GetParrentNetBitIndex() == BitIndex
						&& search.IsOutputEnd() == IsOutput) {
					Connections.add(connection);
				}
			}
		}
		return Connections;
	}

	public ConnectionEnd getEnd(int index) {
		if ((index < 0) || (index >= nr_of_ends)) {
			return null;
		}
		return Ends.get(index);
	}

	public BubbleInformationContainer GetGlobalBubbleId(
			ArrayList<String> HierarchyName) {
		if (GlobalIds.containsKey(HierarchyName)) {
			return GlobalIds.get(HierarchyName);
		} else {
			return null;
		}
	}

	public IOComponentInformationContainer GetIOInformationContainer() {
		return MyIOInformation;
	}

	public int GetLocalBubbleInOutEndId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetInOutEndIndex();
	}

	public int GetLocalBubbleInOutStartId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetInOutStartIndex();
	}

	public int GetLocalBubbleInputEndId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetInputEndIndex();
	}

	public int GetLocalBubbleInputStartId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetInputStartIndex();
	}

	public int GetLocalBubbleOutputEndId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetOutputEndIndex();
	}

	public int GetLocalBubbleOutputStartId() {
		if (LocalId == null) {
			return 0;
		}
		return LocalId.GetOutputStartIndex();
	}

	public BoardRectangle getMap(String MapName) {
		return BoardMaps.get(MapName);
	}

	public String getMapType(String MapName) {
		return CurrentMapType.get(MapName);
	}

	public boolean hasConnection(Net RootNet, byte BitIndex) {
		for (ConnectionEnd search : Ends) {
			for (byte bit = 0; bit < search.NrOfBits(); bit++) {
				ConnectionPoint connection = search.GetConnection(bit);
				if (connection.GetParrentNet() == RootNet
						&& connection.GetParrentNetBitIndex() == BitIndex) {
					return true;
				}
			}
		}
		return false;
	}

	public void LockAlternateMapping(ArrayList<String> key) {
		AlternateMapLocked.put(key, true);
	}

	public int NrOfEnds() {
		return nr_of_ends;
	}

	public void removeMap(String MapName) {
		BoardMaps.remove(MapName);
		CurrentMapType.remove(MapName);
	}

	public boolean setEnd(int index, ConnectionEnd End) {
		if ((index < 0) || (index >= nr_of_ends)) {
			return false;
		}
		Ends.set(index, End);
		return true;
	}

	public void SetLocalBubbleID(int InputBubblesStartId, int NrOfInputBubbles,
			int OutputBubblesStartId, int NrOfOutputBubbles,
			int InOutBubblesStartId, int NrOfInOutBubbles) {
		if (LocalId == null) {
			LocalId = new BubbleInformationContainer();
		}
		if (NrOfInputBubbles > 0) {
			LocalId.SetInputBubblesInformation(InputBubblesStartId,
					InputBubblesStartId + NrOfInputBubbles - 1);
		}
		if (NrOfInOutBubbles > 0) {
			LocalId.SetInOutBubblesInformation(InOutBubblesStartId,
					InOutBubblesStartId + NrOfInOutBubbles - 1);
		}
		if (NrOfOutputBubbles > 0) {
			LocalId.SetOutputBubblesInformation(OutputBubblesStartId,
					OutputBubblesStartId + NrOfOutputBubbles - 1);
		}
	}

	public void ToggleAlternateMapping(ArrayList<String> key) {
		boolean newIsLocked = MyIOInformation.GetMainMapType().equals(
				FPGAIOInformationContainer.IOComponentTypes.Bus);
		if (AlternateMapLocked.containsKey(key)) {
			if (AlternateMapLocked.get(key)) {
				return;
			}
		} else {
			AlternateMapLocked.put(key, newIsLocked);
			if (newIsLocked) {
				return;
			}
		}
		if (!AlternateMapEnabled.containsKey(key)) {
			AlternateMapEnabled.put(key, true);
		}
		if (AlternateMapEnabled.get(key)) {
			AlternateMapEnabled.put(key, false);
		} else {
			if (MyIOInformation.HasAlternateMapTypes()) {
				AlternateMapEnabled.put(key, true);
			}
		}
	}

	public void UnlockAlternateMapping(ArrayList<String> key) {
		if (!MyIOInformation.GetMainMapType().equals(
				FPGAIOInformationContainer.IOComponentTypes.Bus)) {
			AlternateMapLocked.put(key, false);
		}
	}

}
