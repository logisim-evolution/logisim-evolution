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

public class ConnectionPoint {

  private Net myOwnNet;
  private Byte myOwnNetBitIndex;
  private int myChildsPortIndex;
  private final Component myComp;

  public ConnectionPoint(Component comp) {
    myOwnNet = null;
    myOwnNetBitIndex = -1;
    myChildsPortIndex = -1;
    myComp = comp;
  }

  public Component getComp() {
    return myComp;
  }

  public int getChildsPortIndex() {
    return myChildsPortIndex;
  }

  public Net getParentNet() {
    return myOwnNet;
  }

  public Byte getParentNetBitIndex() {
    return myOwnNetBitIndex;
  }

  public void setChildsPortIndex(int index) {
    myChildsPortIndex = index;
  }

  public void setParentNet(Net connectedNet, Byte bitIndex) {
    myOwnNet = connectedNet;
    myOwnNetBitIndex = bitIndex;
  }

}
