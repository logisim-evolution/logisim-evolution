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

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Net {
  private Set<Location> MyPoints = new HashSet<Location>();
  private Set<String> TunnelNames = new HashSet<String>();
  private Set<Wire> Segments = new HashSet<Wire>();
  private int nr_of_bits;
  private Net MyParent;
  private Boolean Requires_to_be_root;
  private ArrayList<Byte> InheritedBits = new ArrayList<Byte>();
  private ArrayList<ConnectionPointArray> SourceList = new ArrayList<ConnectionPointArray>();
  private ArrayList<ConnectionPointArray> SinkList = new ArrayList<ConnectionPointArray>();
  private ArrayList<ConnectionPointArray> SourceNetsList = new ArrayList<ConnectionPointArray>();
  private ArrayList<ConnectionPointArray> SinkNetsList = new ArrayList<ConnectionPointArray>();

  public Net() {
    cleanup();
  }

  public Net(Location loc) {
    cleanup();
    MyPoints.add(loc);
  }

  public Net(Location loc, int width) {
    cleanup();
    MyPoints.add(loc);
    nr_of_bits = width;
  }

  public void add(Wire segment) {
    MyPoints.add(segment.getEnd0());
    MyPoints.add(segment.getEnd1());
    Segments.add(segment);
  }

  public Set<Wire> getWires() {
    return Segments;
  }

  public boolean AddParrentBit(byte BitID) {
    if (BitID < 0) return false;
    InheritedBits.add(BitID);
    return true;
  }

  public boolean addSink(int bitIndex, ConnectionPoint Sink) {
    if ((bitIndex < 0) || (bitIndex >= SinkList.size())) return false;
    SinkList.get(bitIndex).AddConnection(Sink);
    return true;
  }

  public boolean addSinkNet(int bitIndex, ConnectionPoint SinkNet) {
    if ((bitIndex < 0) || (bitIndex >= SinkNetsList.size())) return false;
    SinkNetsList.get(bitIndex).AddConnection(SinkNet);
    return true;
  }

  public boolean addSource(int bitIndex, ConnectionPoint Source) {
    if ((bitIndex < 0) || (bitIndex >= SourceList.size())) return false;
    SourceList.get(bitIndex).AddConnection(Source);
    return true;
  }

  public boolean addSourceNet(int bitIndex, ConnectionPoint SourceNet) {
    if ((bitIndex < 0) || (bitIndex >= SourceNetsList.size())) return false;
    SourceNetsList.get(bitIndex).AddConnection(SourceNet);
    return true;
  }

  public void addTunnel(String TunnelName) {
    TunnelNames.add(TunnelName);
  }

  public int BitWidth() {
    return nr_of_bits;
  }

  private void cleanup() {
    MyPoints.clear();
    Segments.clear();
    TunnelNames.clear();
    nr_of_bits = 0;
    MyParent = null;
    Requires_to_be_root = false;
    InheritedBits.clear();
    SourceList.clear();
    SinkList.clear();
    SourceNetsList.clear();
    SinkNetsList.clear();
  }

  public boolean contains(Location point) {
    return MyPoints.contains(point);
  }

  public boolean ContainsTunnel(String TunnelName) {
    return TunnelNames.contains(TunnelName);
  }

  public void ForceRootNet() {
    MyParent = null;
    Requires_to_be_root = true;
    InheritedBits.clear();
  }

  public byte getBit(byte bit) {
    if ((bit < 0) || (bit >= InheritedBits.size()) || IsRootNet()) return -1;
    return InheritedBits.get(bit);
  }

  public Net getParent() {
    return MyParent;
  }

  public Set<Location> getPoints() {
    return this.MyPoints;
  }

  public ArrayList<ConnectionPoint> GetSinkNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= SinkNetsList.size()))
      return new ArrayList<ConnectionPoint>();
    return SinkNetsList.get(bitIndex).GetConnections();
  }

  public ArrayList<ConnectionPoint> GetSourceNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= SourceNetsList.size()))
      return new ArrayList<ConnectionPoint>();
    return SourceNetsList.get(bitIndex).GetConnections();
  }
  
  public void CleanupSourceNets(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= SourceNetsList.size())) return;
    ArrayList<ConnectionPoint> oldconns = SourceNetsList.get(bitIndex).GetConnections();
    if (oldconns.size() > 1) {
      ConnectionPoint point = oldconns.get(0);
      SourceNetsList.get(bitIndex).ClearConnections();
      SourceNetsList.get(bitIndex).AddConnection(point);
    }
    return;
  }

  public boolean hasBitSinks(int bitid) {
    if (bitid < 0 || bitid >= SinkList.size()) return false;
    return SinkList.get(bitid).NrOfConnections() > 0;
  }

  public ArrayList<ConnectionPoint> GetBitSinks(int bitIndex) {
    ArrayList<ConnectionPoint> sinks = new ArrayList<ConnectionPoint>();
    if ((bitIndex < 0) || (bitIndex >= SourceNetsList.size()))
      return new ArrayList<ConnectionPoint>();
    sinks.addAll(SinkList.get(bitIndex).GetConnections());
    return sinks;
  }

  public ArrayList<ConnectionPoint> GetBitSources(int bitIndex) {
    if ((bitIndex < 0) || (bitIndex >= SourceNetsList.size())) return null;
    return SourceList.get(bitIndex).GetConnections();
  }

  public boolean hasBitSource(int bitid) {
    if (bitid < 0 || bitid >= SourceList.size()) return false;
    return SourceList.get(bitid).NrOfConnections() > 0;
  }

  public boolean hasShortCircuit() {
    boolean ret = false;
    for (int i = 0; i < nr_of_bits; i++) ret |= SourceList.get(i).NrOfConnections() > 1;
    return ret;
  }

  public boolean hasSinks() {
    boolean ret = false;
    for (int i = 0; i < nr_of_bits; i++) ret |= SinkList.get(i).NrOfConnections() > 0;
    return ret;
  }

  public Set<ConnectionPoint> GetSinks() {
    Set<ConnectionPoint> sinks = new HashSet<ConnectionPoint>();
    for (int i = 0; i < nr_of_bits; i++) {
      sinks.addAll(SinkList.get(i).GetConnections());
    }
    return sinks;
  }

  public boolean hasSource() {
    boolean ret = false;
    for (int i = 0; i < nr_of_bits; i++) ret |= SourceList.get(i).NrOfConnections() > 0;
    return ret;
  }

  public boolean HasTunnel() {
    return TunnelNames.size() != 0;
  }

  public void InitializeSourceSinks() {
    SourceList.clear();
    SinkList.clear();
    SourceNetsList.clear();
    SinkNetsList.clear();
    for (int i = 0; i < nr_of_bits; i++) {
      SourceList.add(new ConnectionPointArray());
      SinkList.add(new ConnectionPointArray());
      SourceNetsList.add(new ConnectionPointArray());
      SinkNetsList.add(new ConnectionPointArray());
    }
  }

  public boolean isBus() {
    return nr_of_bits > 1;
  }

  public boolean isEmpty() {
    return MyPoints.isEmpty();
  }

  public boolean IsForcedRootNet() {
    return Requires_to_be_root;
  }

  public boolean IsRootNet() {
    return (MyParent == null) || Requires_to_be_root;
  }

  public boolean merge(Net TheNet) {
    if (TheNet.BitWidth() == nr_of_bits) {
      MyPoints.addAll(TheNet.getPoints());
      Segments.addAll(TheNet.getWires());
      TunnelNames.addAll(TheNet.TunnelNames());
      return true;
    }
    return false;
  }

  public boolean setWidth(int Width) {
    if ((nr_of_bits > 0) && (Width != nr_of_bits)) return false;
    nr_of_bits = Width;
    return true;
  }

  public boolean setParent(Net Parrent) {
    if (Requires_to_be_root) return false;
    if (Parrent == null) return false;
    if (MyParent != null) return false;
    MyParent = Parrent;
    return true;
  }

  public Set<String> TunnelNames() {
    return this.TunnelNames;
  }
}
