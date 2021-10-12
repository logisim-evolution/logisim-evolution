/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public interface InstanceState {
  void fireInvalidated();

  AttributeSet getAttributeSet();

  <E> E getAttributeValue(Attribute<E> attr);

  InstanceData getData();

  InstanceFactory getFactory();

  Instance getInstance();

  int getPortIndex(Port port);

  Value getPortValue(int portIndex);

  Project getProject();

  int getTickCount();

  boolean isCircuitRoot();

  boolean isPortConnected(int portIndex);

  void setData(InstanceData value);

  void setPort(int portIndex, Value value, int delay);
}
