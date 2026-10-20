/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.AttributeSet;
import java.awt.event.KeyEvent;

public class KeyConfigurationEvent {
  public static final int KEY_PRESSED = 0;
  public static final int KEY_RELEASED = 1;
  public static final int KEY_TYPED = 2;

  private final int type;
  private final AttributeSet attrs;
  private final KeyEvent event;
  private final Object data;
  private boolean consumed;

  public KeyConfigurationEvent(int type, AttributeSet attrs, KeyEvent event, Object data) {
    this.type = type;
    this.attrs = attrs;
    this.event = event;
    this.data = data;
    this.consumed = false;
  }

  public void consume() {
    consumed = true;
  }

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public Object getData() {
    return data;
  }

  public KeyEvent getKeyEvent() {
    return event;
  }

  public int getType() {
    return type;
  }

  public boolean isConsumed() {
    return consumed;
  }
}
