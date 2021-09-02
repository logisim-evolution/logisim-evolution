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

import java.util.Iterator;
import java.util.LinkedList;

public class DriveStrength {
  public static String GetContraintedDriveStrength(char id) {
    if ((id > DEFAULT_STENGTH) && (id <= DRIVE_24)) {
      return BEHAVIOR_STRINGS[id].replace(" mA", " ");
    }
    return "";
  }

  public static char getId(String identifier) {
    char result = 0;
    LinkedList<String> thelist = DriveStrength.getStrings();
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
  public static char DRIVE_2 = 1;
  public static char DRIVE_4 = 2;
  public static char DRIVE_8 = 3;
  public static char DRIVE_16 = 4;
  public static final char DRIVE_24 = 5;

  public static final char UNKNOWN = 255;

  public static final String[] BEHAVIOR_STRINGS = {
    "Default", "2 mA", "4 mA", "8 mA", "16 mA", "24 mA"
  };
}
