/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import static com.cburch.logisim.fpga.Strings.S;

import java.util.LinkedList;
import java.util.List;

public class LedArrayDriving {

  public static String getConstrainedDriveMode(char id) {
    return ((id >= LED_DEFAULT) && (id <= RGB_COLUMN_SCANNING)) ? DRIVING_STRINGS[id] : "Unknown";
  }

  public static char getId(String identifier) {
    char result = 0;
    final var thelist = LedArrayDriving.getStrings();
    for (String s : thelist) {
      if (s.equals(identifier)) return result;
      result++;
    }
    return UNKNOWN;
  }

  public static List<String> getStrings() {
    var result = new LinkedList<String>();
    result.add(DRIVING_STRINGS[0]);
    result.add(DRIVING_STRINGS[1]);
    result.add(DRIVING_STRINGS[2]);
    result.add(DRIVING_STRINGS[3]);
    result.add(DRIVING_STRINGS[4]);
    result.add(DRIVING_STRINGS[5]);
    return result;
  }

  public static List<String> getDisplayStrings() {
    var result = new LinkedList<String>();
    result.add(S.get(DRIVING_STRINGS[0]));
    result.add(S.get(DRIVING_STRINGS[1]));
    result.add(S.get(DRIVING_STRINGS[2]));
    result.add(S.get(DRIVING_STRINGS[3]));
    result.add(S.get(DRIVING_STRINGS[4]));
    result.add(S.get(DRIVING_STRINGS[5]));
    return result;
  }

  public static final String LED_ARRAY_DRIVE_STRING = "LedArrayDriveMode";
  public static final char LED_DEFAULT = 0;
  public static final char LED_ROW_SCANNING = 1;
  public static final char LED_COLUMN_SCANNING = 2;
  public static final char RGB_DEFAULT = 3;
  public static final char RGB_ROW_SCANNING = 4;
  public static final char RGB_COLUMN_SCANNING = 5;

  public static final char UNKNOWN = 255;

  public static final String[] DRIVING_STRINGS = {
    "LedDefault",
    "LedRowScanning",
    "LedColumnScanning",
    "RgbDefault",
    "RgbRowScanning",
    "RgbColScanning"
  };
}
