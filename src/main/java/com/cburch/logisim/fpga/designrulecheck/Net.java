/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

public class Net {
  @Getter private final Set<Location> points = new HashSet<>();
  @Getter private final Set<String> tunnelNames = new HashSet<>();
  @Getter private final Set<Wire> wires = new HashSet<>();
  @Getter private int bitWidth;
  @Getter private Net parent;
  /**
   * Tells if it is required to be root
   */
  @Getter private boolean forcedRootNet;
  private final ArrayList<Byte> inheritedBits = new ArrayList<>();
  private final ArrayList<ConnectionPointArray> sourceList = new ArrayList<>();
  private final ArrayList<ConnectionPointArray> sinkList = new ArrayList<>();
  private final ArrayList<ConnectionPointArray> sourceNetsList = new ArrayList<>();
  private final ArrayList<ConnectionPointArray> sinkNetsList = new ArrayList<>();

  public Net() {
    cleanup();
  }

  public Net(Location loc) {
    cleanup();
    points.add(loc);
  }

  public Net(Location loc, int width) {
    cleanup();
    points.add(loc);
    bitWidth = width;
  }

  public void add(Wire segment) {
    points.add(segment.getEnd0());
    points.add(segment.getEnd1());
    wires.add(segment);
  }

  public boolean addParentBit(byte bitId) {
    if (bitId < 0) return false;
    inheritedBits.add(bitId);
    return true;
  }

  public boolean addSink(int bitIndex, ConnectionPoint sink) {
    if ((bitIndex < 0) || (bitIndex >= sinkList.size())) return false;
    sinkList.get(bitIndex).add(sink);
    return true;
  }

  public boolean addSinkNet(int bitIndex, ConnectionPoint sinkNet) {
    if ((bitIndex < 0) || (bitIndex >= sinkNetsList.size())) return false;
    sinkNetsList.get(bitIndex).add(sinkNet);
    return true;
  }

  public boolean addSource(int bitIndex, ConnectionPoint source) {
    if ((bitIndex < 0) || (bitIndex >= sourceList.size())) return false;
    sourceList.get(bitIndex).add(source);
    return true;
  }

  public boolean addSourceNet(int bitIndex, ConnectionPoint sourceNet) {
    if ((bitIndex < 0) || (bitIndex >= sourceNetsList.size())) return false;
    sourceNetsList.get(bitIndex).add(sourceNet);
    return true;
  }

  public void addTunnel(String tunnelName) {
    tunnelNames.add(tunnelName);
  }

  private void cleanup() {
    points.clear();
    wires.clear();
    tunnelNames.clear();
    bitWidth = 0;
    parent = null;
    forcedRootNet = false;
    inheritedBits.clear();
    sourceList.clear();
    sinkList.clear();
    sourceNetsList.clear();
    sinkNetsList.clear();
  }

  public boolean contains(Location point) {
    return points.contains(point);
  }

  public boolean containsTunnel(String tunnelName) {
    return tunnelNames.contains(tunnelName);
  }

  public void forceRootNet() {
    parent = null;
    forcedRootNet = true;
    inheritedBits.clear();
  }

  public byte getBit(byte bit) {
    if ((bit < 0) || (bit >= inheritedBits.size()) || isRootNet()) return -1;
    return inheritedBits.get(bit);
  }

  public List<ConnectionPoint> getSinkNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= sinkNetsList.size())) return new ArrayList<>();
    return sinkNetsList.get(bitIndex).getAll();
  }

  public List<ConnectionPoint> getSourceNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= sourceNetsList.size())) return new ArrayList<>();
    return sourceNetsList.get(bitIndex).getAll();
  }

  public void cleanupSourceNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= sourceNetsList.size())) return;
    final var oldconns = sourceNetsList.get(bitIndex).getAll();
    if (oldconns.size() > 1) {
      final var point = oldconns.get(0);
      sourceNetsList.get(bitIndex).clear();
      sourceNetsList.get(bitIndex).add(point);
    }
  }

  public boolean hasBitSinks(int bitid) {
    return (bitid < 0 || bitid >= sinkList.size()) ? false : sinkList.get(bitid).size() > 0;
  }

  public List<ConnectionPoint> getBitSinks(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= sourceNetsList.size()))
      return new ArrayList<>();
    return new ArrayList<>(sinkList.get(bitIndex).getAll());
  }

  public List<ConnectionPoint> getBitSources(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= sourceNetsList.size())) return null;
    return sourceList.get(bitIndex).getAll();
  }

  public boolean hasBitSource(int bitid) {
    return (bitid < 0 || bitid >= sourceList.size()) ? false : sourceList.get(bitid).size() > 0;
  }

  public boolean hasShortCircuit() {
    var ret = false;
    for (var i = 0; i < bitWidth; i++) ret |= sourceList.get(i).size() > 1;
    return ret;
  }

  public boolean hasSinks() {
    var ret = false;
    for (var i = 0; i < bitWidth; i++) {
      ret |= sinkList.get(i).size() > 0;
    }
    return ret;
  }

  public Set<ConnectionPoint> getSinks() {
    final var sinks = new HashSet<ConnectionPoint>();
    for (var i = 0; i < bitWidth; i++) sinks.addAll(sinkList.get(i).getAll());
    return sinks;
  }

  public boolean hasSource() {
    var ret = false;
    for (var i = 0; i < bitWidth; i++) {
      ret |= sourceList.get(i).size() > 0;
    }
    return ret;
  }

  public boolean hasTunnel() {
    return tunnelNames.size() != 0;
  }

  public void initializeSourceSinks() {
    sourceList.clear();
    sinkList.clear();
    sourceNetsList.clear();
    sinkNetsList.clear();
    for (var i = 0; i < bitWidth; i++) {
      sourceList.add(new ConnectionPointArray());
      sinkList.add(new ConnectionPointArray());
      sourceNetsList.add(new ConnectionPointArray());
      sinkNetsList.add(new ConnectionPointArray());
    }
  }

  public boolean isBus() {
    return bitWidth > 1;
  }

  public boolean isEmpty() {
    return points.isEmpty();
  }

  public boolean isRootNet() {
    return (parent == null) || forcedRootNet;
  }

  public boolean merge(Net theNet) {
    if (theNet.getBitWidth() == bitWidth) {
      points.addAll(theNet.getPoints());
      wires.addAll(theNet.getWires());
      tunnelNames.addAll(theNet.getTunnelNames());
      return true;
    }
    return false;
  }

  public boolean setWidth(int width) {
    if ((bitWidth > 0) && (width != bitWidth)) return false;
    bitWidth = width;
    return true;
  }

  public boolean setParent(Net parent) {
    if (forcedRootNet) return false;
    if (parent == null) return false;
    if (this.parent != null) return false;
    this.parent = parent;
    return true;
  }

}
