/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import com.cburch.logisim.instance.InstanceComponent;

public interface SocBusSlaveInterface {

  boolean canHandleTransaction(SocBusTransaction trans);
  
  void handleTransaction(SocBusTransaction trans);
  
  Integer getStartAddress();
  Integer getMemorySize();
  String getName();
  void registerListener(SocBusSlaveListener l);
  void removeListener(SocBusSlaveListener l);
  InstanceComponent getComponent();
}
