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
    if ((id >= LedDefault) && (id <= RgbColumnScanning)) {
      return Driving_strings[id];
    }
    return "Unknown";
  }

  public static char getId(String identifier) {
    char result = 0;
    LinkedList<String> thelist = LedArrayDriving.getStrings();
    Iterator<String> iter = thelist.iterator();
    result = 0;
    while (iter.hasNext()) {
      if (iter.next().equals(identifier)) return result;
      result++;
    }
    return Unknown;
  }

  public static LinkedList<String> getStrings() {
    LinkedList<String> result = new LinkedList<>();

    result.add(Driving_strings[0]);
    result.add(Driving_strings[1]);
    result.add(Driving_strings[2]);
    result.add(Driving_strings[3]);
    result.add(Driving_strings[4]);
    result.add(Driving_strings[5]);
    return result;
  }

  public static LinkedList<String> getDisplayStrings() {
    LinkedList<String> result = new LinkedList<>();
    result.add(S.get(Driving_strings[0]));
    result.add(S.get(Driving_strings[1]));
    result.add(S.get(Driving_strings[2]));
    result.add(S.get(Driving_strings[3]));
    result.add(S.get(Driving_strings[4]));
    result.add(S.get(Driving_strings[5]));
    return result;
  }

  public static final String LedArrayDriveString = "LedArrayDriveMode";
  public static final char LedDefault = 0;
  public static final char LedRowScanning = 1;
  public static final char LedColumnScanning = 2;
  public static final char RgbDefault = 3;
  public static final char RgbRowScanning = 4;
  public static final char RgbColumnScanning = 5;

  public static final char Unknown = 255;

  public static final String[] Driving_strings = {
    "LedDefault", "LedRowScanning", "LedColumnScanning", "RgbDefault", "RgbRowScanning", "RgbColScanning"
  };
}
