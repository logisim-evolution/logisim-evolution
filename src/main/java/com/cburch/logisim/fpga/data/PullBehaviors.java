/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.util.LinkedList;
import java.util.List;

public class PullBehaviors {
  public static String getConstrainedPullString(char id) {
    return switch (id) {
      case PULL_UP -> "PULLUP";
      case PULL_DOWN -> "PULLDOWN";
      default -> "";
    };
  }
  
  public static String getPullString(char id) {
    if (id == FLOAT || id > PULL_DOWN) return null;
    return switch (id) {
      case PULL_UP -> "UP";
      case PULL_DOWN -> "DOWN";
      default -> "NONE";
    };
  }

  public static char getId(String identifier) {
    char result = 0;
    final var thelist = PullBehaviors.getStrings();
    final var iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return PullBehaviors.UNKNOWN;
  }

  public static List<String> getStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(BEHAVIOR_STRINGS[0]);
    result.add(BEHAVIOR_STRINGS[1]);
    result.add(BEHAVIOR_STRINGS[2]);

    return result;
  }

  public static final String PULL_ATTRIBUTE_STRING = "FPGAPinPullBehavior";
  public static final char FLOAT = 0;
  public static final char PULL_UP = 1;
  public static final char PULL_DOWN = 2;

  public static final char UNKNOWN = 255;

  public static final String[] BEHAVIOR_STRINGS = {"Float", "Pull Up", "Pull Down"};
}
