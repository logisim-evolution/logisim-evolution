/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PinActivity {
  public static char getId(String identifier) {
    char result = 0;
    final var thelist = PinActivity.getStrings();
    Iterator<String> iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return Unknown;
  }

  public static List<String> getStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(BEHAVIOR_STRINGS[0]);
    result.add(BEHAVIOR_STRINGS[1]);

    return result;
  }

  public static final String ACTIVITY_ATTRIBUTE_STRING = "ActivityLevel";
  public static final char ACTIVE_LOW = 0;
  public static final char ACTIVE_HIGH = 1;

  public static final char Unknown = 255;

  public static final String[] BEHAVIOR_STRINGS = {"Active low", "Active high"};
}
