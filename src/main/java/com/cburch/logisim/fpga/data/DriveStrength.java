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

public class DriveStrength {
  public static String getConstrainedDriveStrength(char id) {
    if ((id > DEFAULT_STENGTH) && (id <= DRIVE_24)) {
      return BEHAVIOR_STRINGS[id].replace(" mA", " ");
    }
    return "";
  }

  public static String getDriveString(char id) {
    if (id == DEFAULT_STENGTH || id > DRIVE_24) return null;
    return SIMPLE_STRINGS[id];
  }

  public static char getId(String identifier) {
    char result = 0;
    final var theList = DriveStrength.getStrings();
    final var iter = theList.iterator();
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

    return result;
  }

  public static final String DRIVE_ATTRIBUTE_STRING = "FPGAPinDriveStrength";
  public static final char DEFAULT_STENGTH = 0;
  public static final char DRIVE_2 = 1;
  public static final char DRIVE_4 = 2;
  public static final char DRIVE_8 = 3;
  public static final char DRIVE_16 = 4;
  public static final char DRIVE_24 = 5;

  public static final char UNKNOWN = 255;

  public static final String[] BEHAVIOR_STRINGS = {
    "Default", "2 mA", "4 mA", "8 mA", "16 mA", "24 mA"
  };

  public static final String[] SIMPLE_STRINGS = {
    "0", "2", "4", "8", "16", "24"
  };
}
