/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
