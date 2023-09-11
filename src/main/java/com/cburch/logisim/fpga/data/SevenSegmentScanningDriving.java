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

public class SevenSegmentScanningDriving {

  public static String getConstrainedDriveMode(char id) {
    return ((id >= SEVEN_SEG_DECODED) && (id <= SEVEN_SEG_SCANNING_ACTIVE_HI)) ? DRIVING_STRINGS[id] : "Unknown";
  }

  public static char getId(String identifier) {
    char result = 0;
    final var thelist = SevenSegmentScanningDriving.getStrings();
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
    return result;
  }

  public static List<String> getDisplayStrings() {
    var result = new LinkedList<String>();
    result.add(S.get(DRIVING_STRINGS[0]));
    result.add(S.get(DRIVING_STRINGS[1]));
    result.add(S.get(DRIVING_STRINGS[2]));
    return result;
  }

  public static final String SEVEN_SEG_SCANNING_MODE = "SevenSegScanningMode";
  public static final char SEVEN_SEG_DECODED = 0;
  public static final char SEVEN_SEG_SCANNING_ACTIVE_LOW = 1;
  public static final char SEVEN_SEG_SCANNING_ACTIVE_HI = 2;
  public static final char UNKNOWN = 255;
  
  public static final String[] DRIVING_STRINGS = {
    "SevenSegDecoded",
    "SevenSegScanningActiveLow",
    "SevenSegScanningActiveHi"
  };
}
