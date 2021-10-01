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

public class IoStandards {
  public static String getConstraintedIoStandard(char id) {
    return ((id > DEFAULT_STANDARD) && (id <= LVTTL)) ? BEHAVIOR_STRINGS[id] : "";
  }

  public static char getId(String identifier) {
    char result = 0;
    final var thelist = IoStandards.getStrings();
    final var iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return UNKNOWN;
  }

  public static List<String> getStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(BEHAVIOR_STRINGS[0]);
    result.add(BEHAVIOR_STRINGS[1]);
    result.add(BEHAVIOR_STRINGS[2]);
    result.add(BEHAVIOR_STRINGS[3]);
    result.add(BEHAVIOR_STRINGS[4]);
    result.add(BEHAVIOR_STRINGS[5]);
    result.add(BEHAVIOR_STRINGS[6]);

    return result;
  }

  public static final String IO_ATTRIBUTE_STRING = "FPGAPinIOStandard";
  public static final char DEFAULT_STANDARD = 0;
  public static char LVCMOS12 = 1;
  public static char LVCMOS15 = 2;
  public static char LVCMOS18 = 3;
  public static char LVCMOS25 = 4;
  public static char LVCMOS33 = 5;

  public static final char LVTTL = 6;

  public static final char UNKNOWN = 255;

  public static final String[] BEHAVIOR_STRINGS = {
    "Default", "LVCMOS12", "LVCMOS15", "LVCMOS18", "LVCMOS25", "LVCMOS33", "LVTTL"
  };
}
