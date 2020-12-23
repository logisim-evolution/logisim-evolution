/**
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
    if ((id > DefaulStength) && (id <= Drive_24)) {
      return Behavior_strings[id].replace(" mA", " ");
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
    return Unknown;
  }

  public static LinkedList<String> getStrings() {
    LinkedList<String> result = new LinkedList<String>();

    result.add(Behavior_strings[0]);
    result.add(Behavior_strings[1]);
    result.add(Behavior_strings[2]);
    result.add(Behavior_strings[3]);
    result.add(Behavior_strings[4]);
    result.add(Behavior_strings[5]);

    return result;
  }

  public static String DriveAttributeString = "FPGAPinDriveStrength";
  public static char DefaulStength = 0;
  public static char Drive_2 = 1;
  public static char Drive_4 = 2;
  public static char Drive_8 = 3;
  public static char Drive_16 = 4;
  public static char Drive_24 = 5;

  public static char Unknown = 255;

  public static String[] Behavior_strings = {"Default", "2 mA", "4 mA", "8 mA", "16 mA", "24 mA"};
}
