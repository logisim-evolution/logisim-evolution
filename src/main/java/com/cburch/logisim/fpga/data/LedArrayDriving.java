/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
