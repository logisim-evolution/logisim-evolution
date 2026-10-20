/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.std.wiring.Clock;
import java.util.ArrayList;
import java.util.List;

public class ClockSourceContainer {

  final ArrayList<Component> sources;
  boolean requiresFpgaGlobalClock;

  public ClockSourceContainer() {
    sources = new ArrayList<>();
    requiresFpgaGlobalClock = false;
  }

  public void clear() {
    sources.clear();
    requiresFpgaGlobalClock = false;
  }

  private boolean equals(Component comp1, Component comp2) {
    if (comp1.getAttributeSet().getValue(Clock.ATTR_PHASE).intValue()
        != comp2.getAttributeSet().getValue(Clock.ATTR_PHASE).intValue()) return false;
    if (comp1.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue()
        != comp2.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue()) {
      return false;
    }
    return comp1.getAttributeSet().getValue(Clock.ATTR_LOW).intValue()
        == comp2.getAttributeSet().getValue(Clock.ATTR_LOW).intValue();
  }

  public int getClockId(Component comp) {
    if (!(comp.getFactory() instanceof Clock)) {
      return -1;
    }
    for (final var clock : sources) {
      if (equals(comp, clock)) {
        return sources.indexOf(clock);
      }
    }
    sources.add(comp);
    return sources.indexOf(comp);
  }

  public int getNrofSources() {
    return sources.size();
  }

  public List<Component> getSources() {
    return sources;
  }

  public boolean getRequiresFpgaGlobalClock() {
    return requiresFpgaGlobalClock;
  }

  public void setRequiresFpgaGlobalClock() {
    requiresFpgaGlobalClock = true;
  }
}
