/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import lombok.Getter;
import lombok.Setter;

public class InstanceDataSingleton implements InstanceData, Cloneable {

  @Getter @Setter private Object value;

  public InstanceDataSingleton(Object value) {
    this.value = value;
  }

  @Override
  public InstanceDataSingleton clone() {
    try {
      return (InstanceDataSingleton) super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
}
