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

import java.util.Iterator;
import java.util.LinkedList;

public class LedArrayDriving {

  public static String GetContraintedDriveMode(char id) {
    if ((id >= LED_DEFAULT) && (id <= RGB_COLUMN_SCANNING)) {
      return DRIVING_STRINGS[id];
    }
    return "Unknown";
  }

  public static char getId(String identifier) {
    char result = 0;
    LinkedList<String> thelist = LedArrayDriving.getStrings();
    Iterator<String> iter = thelist.iterator();
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return UNKNOWN;
  }

  public static LinkedList<String> getStrings() {
    var result = new LinkedList<String>();
    result.add(DRIVING_STRINGS[0]);
    result.add(DRIVING_STRINGS[1]);
    result.add(DRIVING_STRINGS[2]);
    result.add(DRIVING_STRINGS[3]);
    result.add(DRIVING_STRINGS[4]);
    result.add(DRIVING_STRINGS[5]);
    return result;
  }

  public static LinkedList<String> getDisplayStrings() {
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
    "LedDefault", "LedRowScanning", "LedColumnScanning", "RgbDefault", "RgbRowScanning", "RgbColScanning"
  };
}
