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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NetlistComponent {

  private int nr_of_ends;
  private Component CompReference;
  private ArrayList<ConnectionEnd> Ends;
  ComponentMapInformationContainer MyMapInformation;
  private Map<ArrayList<String>, BubbleInformationContainer> GlobalIds;
  private BubbleInformationContainer LocalId;
  private boolean IsGatedInstance;

  public NetlistComponent(Component Ref) {
    IsGatedInstance = false;
    nr_of_ends = Ref.getEnds().size();
    CompReference = Ref;
    Ends = new ArrayList<ConnectionEnd>();
    for (int i = 0; i < Ref.getEnds().size(); i++) {
      Ends.add( new ConnectionEnd( Ref.getEnd(i).isOutput(), 
                (byte) Ref.getEnd(i).getWidth().getWidth(), Ref));
    }
    if (Ref.getAttributeSet().containsAttribute(StdAttr.MAPINFO)) {
      MyMapInformation = Ref.getAttributeSet().getValue(StdAttr.MAPINFO).clone();
    } else {
      if (Ref.getFactory() instanceof Pin) {
        int NrOfBits = Ref.getEnd(0).getWidth().getWidth();
        if (Ref.getEnd(0).isInput() && Ref.getEnd(0).isOutput()) {
          MyMapInformation = new ComponentMapInformationContainer(0, 0, NrOfBits);
        } else if (Ref.getEnd(0).isInput()) {
          MyMapInformation = new ComponentMapInformationContainer(0, NrOfBits, 0);
        } else {
          MyMapInformation = new ComponentMapInformationContainer(NrOfBits, 0, 0);
        }
      } else {
        MyMapInformation = null;
      }
    }
    GlobalIds = null;
    LocalId = null;
  }

  public void AddGlobalBubbleID(
      ArrayList<String> HierarchyName,
      int InputBubblesStartId,
      int NrOfInputBubbles,
      int OutputBubblesStartId,
      int NrOfOutputBubbles,
      int InOutBubblesStartId,
      int NrOfInOutBubbles) {
    if ((NrOfInputBubbles == 0) && (NrOfOutputBubbles == 0) && (NrOfInOutBubbles == 0)) {
      return;
    }
    if (GlobalIds == null) {
      GlobalIds = new HashMap<ArrayList<String>, BubbleInformationContainer>();
    }
    BubbleInformationContainer thisInfo = new BubbleInformationContainer();
    if (NrOfInputBubbles > 0) {
      thisInfo.SetInputBubblesInformation(
          InputBubblesStartId, InputBubblesStartId + NrOfInputBubbles - 1);
    }
    if (NrOfInOutBubbles > 0) {
      thisInfo.SetInOutBubblesInformation(
          InOutBubblesStartId, InOutBubblesStartId + NrOfInOutBubbles - 1);
    }
    if (NrOfOutputBubbles > 0) {
      thisInfo.SetOutputBubblesInformation(
          OutputBubblesStartId, OutputBubblesStartId + NrOfOutputBubbles - 1);
    }
    GlobalIds.put(HierarchyName, thisInfo);
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

  public ArrayList<ConnectionPoint> GetConnections(Net RootNet, byte BitIndex, boolean IsOutput) {
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

  public BubbleInformationContainer GetGlobalBubbleId(ArrayList<String> HierarchyName) {
	if (GlobalIds == null) return null;
    if (GlobalIds.containsKey(HierarchyName)) {
      return GlobalIds.get(HierarchyName);
    } else {
      return null;
    }
  }

  public ComponentMapInformationContainer GetMapInformationContainer() {
    return MyMapInformation;
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

  public int NrOfEnds() {
    return nr_of_ends;
  }

  public boolean setEnd(int index, ConnectionEnd End) {
    if ((index < 0) || (index >= nr_of_ends)) {
      return false;
    }
    Ends.set(index, End);
    return true;
  }

  public void SetLocalBubbleID(
      int InputBubblesStartId,
      int NrOfInputBubbles,
      int OutputBubblesStartId,
      int NrOfOutputBubbles,
      int InOutBubblesStartId,
      int NrOfInOutBubbles) {
    if (LocalId == null) {
      LocalId = new BubbleInformationContainer();
    }
    if (NrOfInputBubbles > 0) {
      LocalId.SetInputBubblesInformation(
          InputBubblesStartId, InputBubblesStartId + NrOfInputBubbles - 1);
    }
    if (NrOfInOutBubbles > 0) {
      LocalId.SetInOutBubblesInformation(
          InOutBubblesStartId, InOutBubblesStartId + NrOfInOutBubbles - 1);
    }
    if (NrOfOutputBubbles > 0) {
      LocalId.SetOutputBubblesInformation(
          OutputBubblesStartId, OutputBubblesStartId + NrOfOutputBubbles - 1);
    }
  }

  public boolean IsGatedInstance() { return IsGatedInstance; }
  public void SetIsGatedInstance() { IsGatedInstance = true; }
}
