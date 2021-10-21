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

public class Net {
  private final Set<Location> myPoints = new HashSet<>();
  private final Set<String> tunnelNames = new HashSet<>();
  private final Set<Wire> segments = new HashSet<>();
  private int nrOfBits;
  private Net myParent;
  private Boolean requiresToBeRoot;
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
    myPoints.add(loc);
  }

  public Net(Location loc, int width) {
    cleanup();
    myPoints.add(loc);
    nrOfBits = width;
  }

  public void add(Wire segment) {
    myPoints.add(segment.getEnd0());
    myPoints.add(segment.getEnd1());
    segments.add(segment);
  }

  public Set<Wire> getWires() {
    return segments;
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

  public int getBitWidth() {
    return nrOfBits;
  }

  private void cleanup() {
    myPoints.clear();
    segments.clear();
    tunnelNames.clear();
    nrOfBits = 0;
    myParent = null;
    requiresToBeRoot = false;
    inheritedBits.clear();
    sourceList.clear();
    sinkList.clear();
    sourceNetsList.clear();
    sinkNetsList.clear();
  }

  public boolean contains(Location point) {
    return myPoints.contains(point);
  }

  public boolean containsTunnel(String tunnelName) {
    return tunnelNames.contains(tunnelName);
  }

  public void forceRootNet() {
    myParent = null;
    requiresToBeRoot = true;
    inheritedBits.clear();
  }

  public byte getBit(byte bit) {
    if ((bit < 0) || (bit >= inheritedBits.size()) || isRootNet()) return -1;
    return inheritedBits.get(bit);
  }

  public Net getParent() {
    return myParent;
  }

  public Set<Location> getPoints() {
    return this.myPoints;
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
    for (var i = 0; i < nrOfBits; i++) ret |= sourceList.get(i).size() > 1;
    return ret;
  }

  public boolean hasSinks() {
    var ret = false;
    for (var i = 0; i < nrOfBits; i++) {
      ret |= sinkList.get(i).size() > 0;
    }
    return ret;
  }

  public Set<ConnectionPoint> getSinks() {
    final var sinks = new HashSet<ConnectionPoint>();
    for (var i = 0; i < nrOfBits; i++) sinks.addAll(sinkList.get(i).getAll());
    return sinks;
  }

  public boolean hasSource() {
    var ret = false;
    for (var i = 0; i < nrOfBits; i++) {
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
    for (var i = 0; i < nrOfBits; i++) {
      sourceList.add(new ConnectionPointArray());
      sinkList.add(new ConnectionPointArray());
      sourceNetsList.add(new ConnectionPointArray());
      sinkNetsList.add(new ConnectionPointArray());
    }
  }

  public boolean isBus() {
    return nrOfBits > 1;
  }

  public boolean isEmpty() {
    return myPoints.isEmpty();
  }

  public boolean isForcedRootNet() {
    return requiresToBeRoot;
  }

  public boolean isRootNet() {
    return (myParent == null) || requiresToBeRoot;
  }

  public boolean merge(Net theNet) {
    if (theNet.getBitWidth() == nrOfBits) {
      myPoints.addAll(theNet.getPoints());
      segments.addAll(theNet.getWires());
      tunnelNames.addAll(theNet.getTunnelNames());
      return true;
    }
    return false;
  }

  public boolean setWidth(int width) {
    if ((nrOfBits > 0) && (width != nrOfBits)) return false;
    nrOfBits = width;
    return true;
  }

  public boolean setParent(Net parent) {
    if (requiresToBeRoot) return false;
    if (parent == null) return false;
    if (myParent != null) return false;
    myParent = parent;
    return true;
  }

  public Set<String> getTunnelNames() {
    return this.tunnelNames;
  }
}
