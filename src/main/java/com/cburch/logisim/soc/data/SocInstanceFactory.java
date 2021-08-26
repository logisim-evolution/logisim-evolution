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

package com.cburch.logisim.soc.data;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.StringGetter;

public abstract class SocInstanceFactory extends InstanceFactory {

  public static final int SOC_UNKNOWN = 0;
  public static final int SOC_MASTER = 1;
  public static final int SOC_SLAVE = 2;
  public static final int SOC_BUS = 4;
  public static final int SOC_SNIFFER = 8;

  private int myType = SOC_UNKNOWN;

  public SocInstanceFactory(String name, StringGetter displayName, int type) {
    super(name, displayName);
    myType = type;
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    // no-op implementation
  }

  @Override
  public void propagate(InstanceState state) {
    // no-op implementation
  }

  @Override
  public boolean isSocComponent() {
    return true;
  }

  public int getSocType() {
    return myType;
  }

  public boolean isSocSlave() {
    return (myType & SOC_SLAVE) != 0;
  }

  public boolean isSocSniffer() {
    return (myType & SOC_SNIFFER) != 0;
  }

  public boolean isSocBus() {
    return (myType & SOC_BUS) != 0;
  }

  public boolean isSocMaster() {
    return (myType & SOC_MASTER) != 0;
  }

  public boolean isSocUnknown() {
    return myType == SOC_UNKNOWN;
  }

  public abstract SocBusSlaveInterface getSlaveInterface(AttributeSet attrs);

  public abstract SocBusSnifferInterface getSnifferInterface(AttributeSet attrs);

  public abstract SocProcessorInterface getProcessorInterface(AttributeSet attrs);
}
