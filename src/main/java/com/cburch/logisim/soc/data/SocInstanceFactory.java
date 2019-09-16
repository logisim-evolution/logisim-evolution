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

package com.cburch.logisim.soc.data;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.StringGetter;

public abstract class SocInstanceFactory extends InstanceFactory {
  
  public static final int SocUnknown = 0;
  public static final int SocMaster = 1;
  public static final int SocSlave = 2;
  public static final int SocBus = 4;
  public static final int SocSniffer = 8;
  
  private int myType = SocUnknown;
  
  public SocInstanceFactory(String name, StringGetter displayName, int type) {
    super(name, displayName);
    myType = type;
  }
  
  @Override
  public void paintInstance(InstancePainter painter) {}

  @Override
  public void propagate(InstanceState state) {}
  
  @Override
  public boolean isSocComponent() { return true ; }
  
  public int getSocType() {
    return myType;
  }
  
  public boolean isSocSlave() {
    return (myType & SocSlave) != 0;
  }
  
  public boolean isSocSniffer() {
    return (myType & SocSniffer) != 0;
  }
	  
  public boolean isSocBus() {
    return (myType & SocBus) != 0;
  }
		  
  public boolean isSocMaster() {
    return (myType & SocMaster) != 0;
  }
			  
  public boolean isSocUnknown() {
    return myType == SocUnknown;
  }
				  
  public abstract SocBusSlaveInterface getSlaveInterface(AttributeSet attrs);
  
  public abstract SocBusSnifferInterface getSnifferInterface(AttributeSet attrs);
  
  public abstract SocProcessorInterface getProcessorInterface(AttributeSet attrs);
}
