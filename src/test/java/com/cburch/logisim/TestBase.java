/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
