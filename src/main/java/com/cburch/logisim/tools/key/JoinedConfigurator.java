/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import lombok.val;

public class JoinedConfigurator implements KeyConfigurator, Cloneable {
  public static JoinedConfigurator create(KeyConfigurator a, KeyConfigurator b) {
    return new JoinedConfigurator(new KeyConfigurator[] {a, b});
  }

  public static JoinedConfigurator create(KeyConfigurator[] configs) {
    return new JoinedConfigurator(configs);
  }

  private KeyConfigurator[] handlers;

  private JoinedConfigurator(KeyConfigurator[] handlers) {
    this.handlers = handlers;
  }

  @Override
  public JoinedConfigurator clone() {
    JoinedConfigurator ret;
    try {
      ret = (JoinedConfigurator) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
    val len = this.handlers.length;
    ret.handlers = new KeyConfigurator[len];
    for (var i = 0; i < len; i++) {
      ret.handlers[i] = this.handlers[i].clone();
    }
    return ret;
  }

  @Override
  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    val hs = handlers;
    if (event.isConsumed()) {
      return null;
    }
    for (val h : hs) {
      val result = h.keyEventReceived(event);
      if (result != null || event.isConsumed()) {
        return result;
      }
    }
    return null;
  }
}
