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

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NetlistComponent {

  private final int nrOfEnds;
  private final Component compReference;
  private final ArrayList<ConnectionEnd> endEnds;
  final ComponentMapInformationContainer myMapInformation;
  private Map<ArrayList<String>, BubbleInformationContainer> globalIds;
  private BubbleInformationContainer localId;
  private boolean isGatedInstance;

  public NetlistComponent(Component ref) {
    isGatedInstance = false;
    nrOfEnds = ref.getEnds().size();
    compReference = ref;
    endEnds = new ArrayList<>();
    for (var i = 0; i < ref.getEnds().size(); i++) {
      endEnds.add(new ConnectionEnd(ref.getEnd(i).isOutput(), (byte) ref.getEnd(i).getWidth().getWidth(), ref));
    }
    if (ref.getAttributeSet().containsAttribute(StdAttr.MAPINFO)) {
      myMapInformation = ref.getAttributeSet().getValue(StdAttr.MAPINFO).clone();
    } else {
      if (ref.getFactory() instanceof Pin) {
        final var nrOfBits = ref.getEnd(0).getWidth().getWidth();
        if (ref.getEnd(0).isInput() && ref.getEnd(0).isOutput()) {
          myMapInformation = new ComponentMapInformationContainer(0, 0, nrOfBits);
        } else if (ref.getEnd(0).isInput()) {
          myMapInformation = new ComponentMapInformationContainer(0, nrOfBits, 0);
        } else {
          myMapInformation = new ComponentMapInformationContainer(nrOfBits, 0, 0);
        }
      } else {
        myMapInformation = null;
      }
    }
    globalIds = null;
    localId = null;
  }

  public void addGlobalBubbleId(
      ArrayList<String> hierarchyName,
      int inputBubblesStartId,
      int nrOfInputBubbles,
      int outputBubblesStartId,
      int nrOfOutputBubbles,
      int inOutBubblesStartId,
      int nrOfInOutBubbles) {
    if ((nrOfInputBubbles == 0) && (nrOfOutputBubbles == 0) && (nrOfInOutBubbles == 0)) {
      return;
    }
    if (globalIds == null) {
      globalIds = new HashMap<>();
    }
    final var thisInfo = new BubbleInformationContainer();
    if (nrOfInputBubbles > 0) {
      thisInfo.setInputBubblesInformation(inputBubblesStartId, inputBubblesStartId + nrOfInputBubbles - 1);
    }
    if (nrOfInOutBubbles > 0) {
      thisInfo.setInOutBubblesInformation(inOutBubblesStartId, inOutBubblesStartId + nrOfInOutBubbles - 1);
    }
    if (nrOfOutputBubbles > 0) {
      thisInfo.setOutputBubblesInformation(outputBubblesStartId, outputBubblesStartId + nrOfOutputBubbles - 1);
    }
    globalIds.put(hierarchyName, thisInfo);
  }

  public boolean isEndConnected(int index) {
    if ((index < 0) || (index >= nrOfEnds)) {
      return false;
    }
    var isConnected = false;
    final var ThisEnd = endEnds.get(index);
    for (var i = 0; i < ThisEnd.getNrOfBits(); i++) {
      isConnected |= (ThisEnd.get((byte) i).getParentNet() != null);
    }
    return isConnected;
  }

  public boolean isEndInput(int index) {
    if ((index < 0) || (index >= nrOfEnds)) {
      return false;
    }
    return compReference.getEnd(index).isInput();
  }

  public Component getComponent() {
    return compReference;
  }

  public byte getConnectionBitIndex(Net rootNet, byte bitIndex) {
    for (final var search : endEnds) {
      for (byte bit = 0; bit < search.getNrOfBits(); bit++) {
        final var connection = search.get(bit);
        if (connection.getParentNet() == rootNet && connection.getParentNetBitIndex() == bitIndex) {
          return bit;
        }
      }
    }
    return -1;
  }

  public ArrayList<ConnectionPoint> getConnections(Net rootNet, byte bitIndex, boolean isOutput) {
    final var connections = new ArrayList<ConnectionPoint>();
    for (final var search : endEnds) {
      for (byte bit = 0; bit < search.getNrOfBits(); bit++) {
        final var connection = search.get(bit);
        if (connection.getParentNet() == rootNet
            && connection.getParentNetBitIndex() == bitIndex
            && search.isOutputEnd() == isOutput) {
          connections.add(connection);
        }
      }
    }
    return connections;
  }

  public ConnectionEnd getEnd(int index) {
    if ((index < 0) || (index >= nrOfEnds)) {
      return null;
    }
    return endEnds.get(index);
  }

  public BubbleInformationContainer getGlobalBubbleId(ArrayList<String> hierarchyName) {
    return (globalIds == null) ? null : globalIds.getOrDefault(hierarchyName, null);
  }

  public ComponentMapInformationContainer getMapInformationContainer() {
    return myMapInformation;
  }

  public int getLocalBubbleInOutEndId() {
    return (localId == null) ? 0 : localId.getInOutEndIndex();
  }

  public int getLocalBubbleInOutStartId() {
    return (localId == null) ? 0 : localId.getInOutStartIndex();
  }

  public int getLocalBubbleInputEndId() {
    return (localId == null) ? 0 : localId.getInputEndIndex();
  }

  public int getLocalBubbleInputStartId() {
    return (localId == null) ? 0 : localId.getInputStartIndex();
  }

  public int getLocalBubbleOutputEndId() {
    return (localId == null) ? 0 : localId.getOutputEndIndex();
  }

  public int getLocalBubbleOutputStartId() {
    return (localId == null) ? 0 : localId.getOutputStartIndex();
  }

  public boolean hasConnection(Net RootNet, byte BitIndex) {
    for (final var search : endEnds) {
      for (byte bit = 0; bit < search.getNrOfBits(); bit++) {
        final var connection = search.get(bit);
        if (connection.getParentNet() == RootNet && connection.getParentNetBitIndex() == BitIndex) {
          return true;
        }
      }
    }
    return false;
  }

  public int nrOfEnds() {
    return nrOfEnds;
  }

  public boolean setEnd(int index, ConnectionEnd End) {
    if ((index < 0) || (index >= nrOfEnds)) {
      return false;
    }
    endEnds.set(index, End);
    return true;
  }

  public void setLocalBubbleID(
      int inputBubblesStartId,
      int nrOfInputBubbles,
      int outputBubblesStartId,
      int nrOfOutputBubbles,
      int inOutBubblesStartId,
      int nrOfInOutBubbles) {
    if (localId == null) {
      localId = new BubbleInformationContainer();
    }
    if (nrOfInputBubbles > 0) {
      localId.setInputBubblesInformation(inputBubblesStartId, inputBubblesStartId + nrOfInputBubbles - 1);
    }
    if (nrOfInOutBubbles > 0) {
      localId.setInOutBubblesInformation(inOutBubblesStartId, inOutBubblesStartId + nrOfInOutBubbles - 1);
    }
    if (nrOfOutputBubbles > 0) {
      localId.setOutputBubblesInformation(outputBubblesStartId, outputBubblesStartId + nrOfOutputBubbles - 1);
    }
  }

  public boolean isGatedInstance() {
    return isGatedInstance;
  }

  public void setIsGatedInstance() {
    isGatedInstance = true;
  }
}
