/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class netlistComponent {

  private final int nrOfEnds;
  private final Component compReference;
  private final ArrayList<ConnectionEnd> endEnds;
  final ComponentMapInformationContainer myMapInformation;
  private Map<List<String>, BubbleInformationContainer> globalIds;
  private BubbleInformationContainer localId;
  private boolean isGatedInstance;

  public netlistComponent(Component ref) {
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
      List<String> hierarchyName,
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

  public List<ConnectionPoint> getConnections(Net rootNet, byte bitIndex, boolean isOutput) {
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

  public BubbleInformationContainer getGlobalBubbleId(List<String> hierarchyName) {
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

  public boolean hasConnection(Net rootNet, byte bitIndex) {
    for (final var search : endEnds) {
      for (byte bit = 0; bit < search.getNrOfBits(); bit++) {
        final var connection = search.get(bit);
        if (connection.getParentNet() == rootNet && connection.getParentNetBitIndex() == bitIndex) {
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
