/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

public class InstanceDataSingleton implements InstanceData, Cloneable {
  private Object value;

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

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
}
