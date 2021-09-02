/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.util.Iterator;
import java.util.LinkedList;

public class IoStandards {
  public static String GetConstraintedIoStandard(char id) {
    if ((id > DEFAULT_STANDARD) && (id <= LVTTL)) {
      return Behavior_strings[id];
    }
    return "";
  }

  public static char getId(String identifier) {
    char result = 0;
    LinkedList<String> thelist = IoStandards.getStrings();
    Iterator<String> iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return UNKNOWN;
  }

  public static LinkedList<String> getStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(Behavior_strings[0]);
    result.add(Behavior_strings[1]);
    result.add(Behavior_strings[2]);
    result.add(Behavior_strings[3]);
    result.add(Behavior_strings[4]);
    result.add(Behavior_strings[5]);
    result.add(Behavior_strings[6]);

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

  public static final String[] Behavior_strings = {
    "Default", "LVCMOS12", "LVCMOS15", "LVCMOS18", "LVCMOS25", "LVCMOS33", "LVTTL"
  };
}
