/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import lombok.Data;

@Data
class MoveRequest {
  private final MoveGesture moveGesture;
  private final int deltaX;
  private final int deltaY;
}
