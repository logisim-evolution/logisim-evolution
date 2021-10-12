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

  @Override
  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    if (event.getType() == KeyConfigurationEvent.KEY_PRESSED) {
      final var e = event.getKeyEvent();
      if (e.getModifiersEx() == modsEx) {
        Direction value = switch (e.getKeyCode()) {
          case KeyEvent.VK_UP -> Direction.NORTH;
          case KeyEvent.VK_DOWN -> Direction.SOUTH;
          case KeyEvent.VK_LEFT -> Direction.WEST;
          case KeyEvent.VK_RIGHT -> Direction.EAST;
          default -> null;
        };
        if (value != null) {
          event.consume();
          return new KeyConfigurationResult(event, attr, value);
        }
      }
    }
    return null;
  }
}
