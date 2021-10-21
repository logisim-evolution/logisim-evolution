/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.instance.InstanceComponent;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public class SimpleDrcContainer {

  public static final int LEVEL_NORMAL = 1;
  public static final int LEVEL_SEVERE = 2;
  public static final int LEVEL_FATAL = 3;
  public static final int MARK_NONE = 0;
  public static final int MARK_INSTANCE = 1;
  public static final int MARK_LABEL = 2;
  public static final int MARK_WIRE = 4;

  private final String message;
  @Getter private final int severity;
  private Set<Object> drcComponents;
  @Getter private Circuit circuit;
  private final int markType;
  @Getter @Setter private int listNumber;
  @Getter private final boolean suppressionCountEnabled;

  public SimpleDrcContainer(String message, int level) {
    this.message = message;
    this.severity = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressionCountEnabled = false;
  }

  public SimpleDrcContainer(String message, int level, boolean suppressionCountEnabled) {
    this.message = message;
    this.severity = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressionCountEnabled = suppressionCountEnabled;
  }

  public SimpleDrcContainer(Object message, int level) {
    this.message = message.toString();
    this.severity = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressionCountEnabled = false;
  }

  public SimpleDrcContainer(Object message, int level, boolean suppressionCountEnabled) {
    this.message = message.toString();
    this.severity = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressionCountEnabled = suppressionCountEnabled;
  }

  public SimpleDrcContainer(Circuit circ, Object message, int level, int markMask) {
    this.message = message.toString();
    this.severity = level;
    this.circuit = circ;
    this.markType = markMask;
    this.listNumber = 0;
    this.suppressionCountEnabled = false;
  }

  public SimpleDrcContainer(Circuit circ, Object message, int level, int markMask, boolean suppressionCountEnabled) {
    this.message = message.toString();
    this.severity = level;
    this.circuit = circ;
    this.markType = markMask;
    this.listNumber = 0;
    this.suppressionCountEnabled = suppressionCountEnabled;
  }

  @Override
  public String toString() {
    return message;
  }

  public boolean isDrcInfoPresent() {
    if (drcComponents == null || circuit == null) return false;
    return !drcComponents.isEmpty();
  }

  public boolean hasCircuit() {
    return (circuit != null);
  }

  public void addMarkComponent(Object comp) {
    if (drcComponents == null) drcComponents = new HashSet<>();
    drcComponents.add(comp);
  }

  public void addMarkComponents(Set<?> set) {
    if (drcComponents == null) drcComponents = new HashSet<>();
    drcComponents.addAll(set);
  }

  public void markComponents() {
    if (!isDrcInfoPresent()) return;
    for (final var obj : drcComponents) {
      if (obj instanceof Wire wire) {
        if ((markType & MARK_WIRE) != 0) {
          wire.setDrcHighlighted(true);
        }
      } else if (obj instanceof Splitter split) {
        if ((markType & MARK_INSTANCE) != 0) {
          split.setMarked(true);
        }
      } else if (obj instanceof InstanceComponent comp) {
        if ((markType & MARK_INSTANCE) != 0) comp.markInstance();
        if ((markType & MARK_LABEL) != 0) comp.markLabel();
      } else {
        // FIXME: ???
      }
    }
  }

  public void clearMarks() {
    if (!isDrcInfoPresent()) return;
    for (final var obj : drcComponents) {
      if (obj instanceof Wire wire) {
        if ((markType & MARK_WIRE) != 0) {
          wire.setDrcHighlighted(false);
        }
      } else if (obj instanceof Splitter split) {
        if ((markType & MARK_INSTANCE) != 0) {
          split.setMarked(false);
        }
      } else if (obj instanceof InstanceComponent comp) {
        comp.clearMarks();
      }
    }
  }
}
