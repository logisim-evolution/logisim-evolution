/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;

class ClipboardContents {
  static final ClipboardContents EMPTY = new ClipboardContents(Collections.emptySet(), null, null);

  @Getter private final Collection<CanvasObject> elements;
  @Getter private final Location anchorLocation;
  @Getter private final Direction anchorFacing;

  public ClipboardContents(Collection<CanvasObject> onClipboard, Location anchorLocation, Direction anchorFacing) {
    this.elements = java.util.List.copyOf(onClipboard);
    this.anchorLocation = anchorLocation;
    this.anchorFacing = anchorFacing;
  }
}
