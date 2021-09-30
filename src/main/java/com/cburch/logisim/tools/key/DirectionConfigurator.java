/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import java.awt.event.KeyEvent;

public class DirectionConfigurator implements KeyConfigurator, Cloneable {
  private final Attribute<?> attr;
  private final int modsEx;

  public DirectionConfigurator(Attribute<?> attr, int modifiersEx) {
    this.attr = attr;
    this.modsEx = modifiersEx;
  }

  @Override
  public DirectionConfigurator clone() {
    try {
      return (DirectionConfigurator) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }

  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    if (event.getType() == KeyConfigurationEvent.KEY_PRESSED) {
      final var e = event.getKeyEvent();
      if (e.getModifiersEx() == modsEx) {
        Direction value = null;
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
            value = Direction.NORTH;
            break;
          case KeyEvent.VK_DOWN:
            value = Direction.SOUTH;
            break;
          case KeyEvent.VK_LEFT:
            value = Direction.WEST;
            break;
          case KeyEvent.VK_RIGHT:
            value = Direction.EAST;
            break;
          default:
            // nothing
            break;
        }
        if (value != null) {
          event.consume();
          return new KeyConfigurationResult(event, attr, value);
        }
      }
    }
    return null;
  }
}
