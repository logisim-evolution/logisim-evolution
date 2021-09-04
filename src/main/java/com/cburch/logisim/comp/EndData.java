/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import lombok.Getter;

public class EndData {
  public static final int INPUT_ONLY = 1;
  public static final int OUTPUT_ONLY = 2;
  public static final int INPUT_OUTPUT = 3;

  @Getter private final Location location;
  @Getter private final BitWidth width;
  @Getter private final int type;
  @Getter private final boolean exclusive;

  public EndData(Location location, BitWidth width, int type) {
    this(location, width, type, type == OUTPUT_ONLY);
  }

  public EndData(Location location, BitWidth width, int type, boolean exclusive) {
    this.location = location;
    this.width = width;
    this.type = type;    // i_o
    this.exclusive = exclusive;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof EndData)) return false;
    if (other == this) return true;
    final var o = (EndData) other;
    return o.location.equals(this.location)
        && o.width.equals(this.width)
        && o.type == this.type
        && o.exclusive == this.exclusive;
  }

  public boolean isInput() {
    return (type & INPUT_ONLY) != 0;
  }

  public boolean isOutput() {
    return (type & OUTPUT_ONLY) != 0;
  }
}
