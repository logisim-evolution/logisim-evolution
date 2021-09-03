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
import lombok.Data;
import lombok.Getter;

@Data
public class KeyConfigurationEvent {
  public static final int KEY_PRESSED = 0;
  public static final int KEY_RELEASED = 1;
  public static final int KEY_TYPED = 2;

  private final int type;
  private final AttributeSet attributeSet;
  private final KeyEvent keyEvent;
  private final Object data;
  private boolean consumed;

  public void consume() {
    consumed = true;
  }
}
