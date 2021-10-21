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
import lombok.Getter;
import lombok.Setter;

public class ClockSourceContainer {

  @Getter final ArrayList<Component> sources;
  @Getter @Setter boolean fpgaGlobalClockRequired;

  public ClockSourceContainer() {
    sources = new ArrayList<>();
    fpgaGlobalClockRequired = false;
  }

  public void clear() {
    sources.clear();
    fpgaGlobalClockRequired = false;
  }

  private boolean equals(Component comp1, Component comp2) {
    final var phase1 = comp1.getAttributeSet().getValue(Clock.ATTR_PHASE).intValue();
    final var phase2 = comp2.getAttributeSet().getValue(Clock.ATTR_PHASE).intValue();
    if (phase1 != phase2) return false;

    final var high1 = comp1.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue();
    final var high2 = comp2.getAttributeSet().getValue(Clock.ATTR_HIGH).intValue();
    if (high1 != high2) return false;

    final var low1 = comp1.getAttributeSet().getValue(Clock.ATTR_LOW).intValue();
    final var low2 = comp2.getAttributeSet().getValue(Clock.ATTR_LOW).intValue();
    return low1 == low2;
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

  public int getNrOfSources() {
    return sources.size();
  }

}
