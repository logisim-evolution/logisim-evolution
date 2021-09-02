/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import java.util.HashMap;

public class ParallelConfigurator implements KeyConfigurator, Cloneable {
  public static ParallelConfigurator create(KeyConfigurator a, KeyConfigurator b) {
    return new ParallelConfigurator(new KeyConfigurator[] {a, b});
  }

  public static ParallelConfigurator create(KeyConfigurator[] configs) {
    return new ParallelConfigurator(configs);
  }

  private KeyConfigurator[] handlers;

  private ParallelConfigurator(KeyConfigurator[] handlers) {
    this.handlers = handlers;
  }

  @Override
  public ParallelConfigurator clone() {
    ParallelConfigurator ret;
    try {
      ret = (ParallelConfigurator) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
    int len = this.handlers.length;
    ret.handlers = new KeyConfigurator[len];
    for (int i = 0; i < len; i++) {
      ret.handlers[i] = this.handlers[i].clone();
    }
    return ret;
  }

  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    KeyConfigurator[] hs = handlers;
    if (event.isConsumed()) {
      return null;
    }
    KeyConfigurationResult first = null;
    HashMap<Attribute<?>, Object> map = null;
    for (KeyConfigurator h : hs) {
      KeyConfigurationResult result = h.keyEventReceived(event);
      if (result != null) {
        if (first == null) {
          first = result;
        } else if (map == null) {
          map = new HashMap<>(first.getAttributeValues());
          map.putAll(result.getAttributeValues());
        } else {
          map.putAll(result.getAttributeValues());
        }
      }
    }
    if (map != null) {
      return new KeyConfigurationResult(event, map);
    } else {
      return first;
    }
  }
}
