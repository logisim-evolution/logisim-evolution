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

public class SimpleDRCContainer {

  public static final int LEVEL_NORMAL = 1;
  public static final int LEVEL_SEVERE = 2;
  public static final int LEVEL_FATAL = 3;
  public static final int MARK_NONE = 0;
  public static final int MARK_INSTANCE = 1;
  public static final int MARK_LABEL = 2;
  public static final int MARK_WIRE = 4;

  private final String message;
  private final int severityLevel;
  private Set<Object> drcComponents;
  private Circuit myCircuit;
  private final int markType;
  private int listNumber;
  private final boolean suppressCount;

  public SimpleDRCContainer(String message, int level) {
    this.message = message;
    this.severityLevel = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressCount = false;
  }

  public SimpleDRCContainer(String message, int level, boolean supressCount) {
    this.message = message;
    this.severityLevel = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressCount = supressCount;
  }

  public SimpleDRCContainer(Object message, int level) {
    this.message = message.toString();
    this.severityLevel = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressCount = false;
  }

  public SimpleDRCContainer(Object message, int level, boolean supressCount) {
    this.message = message.toString();
    this.severityLevel = level;
    this.markType = MARK_NONE;
    this.listNumber = 0;
    this.suppressCount = supressCount;
  }

  public SimpleDRCContainer(Circuit circ, Object message, int level, int markMask) {
    this.message = message.toString();
    this.severityLevel = level;
    this.myCircuit = circ;
    this.markType = markMask;
    this.listNumber = 0;
    this.suppressCount = false;
  }

  public SimpleDRCContainer(Circuit circ, Object message, int level, int markMask, boolean supressCount) {
    this.message = message.toString();
    this.severityLevel = level;
    this.myCircuit = circ;
    this.markType = markMask;
    this.listNumber = 0;
    this.suppressCount = supressCount;
  }

  @Override
  public String toString() {
    return message;
  }

  public int getSeverity() {
    return severityLevel;
  }

  public boolean isDrcInfoPresent() {
    if (drcComponents == null || myCircuit == null) return false;
    return !drcComponents.isEmpty();
  }

  public Circuit getCircuit() {
    return myCircuit;
  }

  public boolean hasCircuit() {
    return (myCircuit != null);
  }

  public void addMarkComponent(Object comp) {
    if (drcComponents == null) drcComponents = new HashSet<>();
    drcComponents.add(comp);
  }

  public void addMarkComponents(Set<?> set) {
    if (drcComponents == null) drcComponents = new HashSet<>();
    drcComponents.addAll(set);
  }

  public void setListNumber(int number) {
    listNumber = number;
  }

  public boolean getSupressCount() {
    return suppressCount;
  }

  public int getListNumber() {
    return listNumber;
  }

  public void markComponents() {
    if (!isDrcInfoPresent()) return;
    for (final var obj : drcComponents) {
      if (obj instanceof Wire) {
        final var wire = (Wire) obj;
        if ((markType & MARK_WIRE) != 0) {
          wire.SetDRCHighlight(true);
        }
      } else if (obj instanceof Splitter) {
        final var split = (Splitter) obj;
        if ((markType & MARK_INSTANCE) != 0) {
          split.SetMarked(true);
        }
      } else if (obj instanceof InstanceComponent) {
        final var comp = (InstanceComponent) obj;
        if ((markType & MARK_INSTANCE) != 0) comp.markInstance();
        if ((markType & MARK_LABEL) != 0) comp.markLabel();
      } else {
      }
    }
  }

  public void clearMarks() {
    if (!isDrcInfoPresent()) return;
    for (final var obj : drcComponents) {
      if (obj instanceof Wire) {
        final var wire = (Wire) obj;
        if ((markType & MARK_WIRE) != 0) {
          wire.SetDRCHighlight(false);
        }
      } else if (obj instanceof Splitter) {
        final var split = (Splitter) obj;
        if ((markType & MARK_INSTANCE) != 0) {
          split.SetMarked(false);
        }
      } else if (obj instanceof InstanceComponent) {
        final var comp = (InstanceComponent) obj;
        comp.clearMarks();
      }
    }
  }
}
