/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

public class ComponentEvent {
  private final Component source;
  private final Object oldData;
  private final Object newData;

  public ComponentEvent(Component source) {
    this(source, null, null);
  }

  public ComponentEvent(Component source, Object oldData, Object newData) {
    this.source = source;
    this.oldData = oldData;
    this.newData = newData;
  }

  public Object getData() {
    return newData;
  }

  public Object getOldData() {
    return oldData;
  }

  public Component getSource() {
    return source;
  }
}
