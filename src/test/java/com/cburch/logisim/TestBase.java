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

package com.cburch.logisim;

import java.util.Random;

public class TestBase {

  private static final int DEFAULT_RANDOM_STRING_LENGTH = 16;

  /**
   * Function to generate a random string of specified length.
   *
   * @param length Numbers of characters to be generated.
   * @return Random string.
   */
  protected String getRandomString(int length) {
    // chose a Character random from this String
    final var AlphaNumericString =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

    // create StringBuffer size of AlphaNumericString
    final var sb = new StringBuilder(length);

    for (var i = 0; i < length; i++) {
      int index = (int) (AlphaNumericString.length() * Math.random());
      sb.append(AlphaNumericString.charAt(index));
    }
    return sb.toString();
  }

  protected String getRandomString() {
    return getRandomString(DEFAULT_RANDOM_STRING_LENGTH);
  }

  protected static int getRandomInt(int min, int max) {
    if (min >= max) throw new IllegalArgumentException("Max must be greater than Min.");

    return (new Random()).nextInt((max - min) + 1) + min;
  }
}
