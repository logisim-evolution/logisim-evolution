/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import java.util.List;
import java.util.Random;

public class TestBase {

  public static final int DEFAULT_RANDOM_STRING_LENGTH_MIN = 16;
  public static final int DEFAULT_RANDOM_STRING_LENGTH_MAX = 32;

  /**
   * Function to generate a random string of specified length.
   *
   * @param length Numbers of characters to be generated.
   * @param includeDigits If True, returned string will also contain digits
   * @return Random string.
   */
  protected String getRandomString(int length, boolean includeDigits) {
    // chose a Character random from this String
    final var allowedCharsBuilder = new StringBuilder("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    allowedCharsBuilder.append(allowedCharsBuilder.toString().toLowerCase());
    if (includeDigits) allowedCharsBuilder.append("0123456789");
    final var allowedChars = allowedCharsBuilder.toString();

    // create StringBuilder size of AlphaNumericString
    final var sb = new StringBuilder(length);

    for (var i = 0; i < length; i++) {
      final var index = (int) (allowedChars.length() * Math.random());
      sb.append(allowedChars.charAt(index));
    }
    return sb.toString();
  }

  protected String getRandomString(boolean includeDigits) {
    final var len =
        getRandomInt(DEFAULT_RANDOM_STRING_LENGTH_MIN, DEFAULT_RANDOM_STRING_LENGTH_MAX);
    return getRandomString(len, includeDigits);
  }

  protected String getRandomString(int length) {
    return getRandomString(length, true);
  }

  protected String getRandomString() {
    final var len =
        getRandomInt(DEFAULT_RANDOM_STRING_LENGTH_MIN, DEFAULT_RANDOM_STRING_LENGTH_MAX);
    return getRandomString(len);
  }

  protected static int getRandomInt(int min, int max) {
    if (min >= max) throw new IllegalArgumentException("Max must be greater than Min.");
    return (new Random()).nextInt((max - min) + 1) + min;
  }

  /** Dumps content of given list to stdout. */
  protected void printList(List<String> list) {
    for (final var line : list) {
      System.out.println(line);
    }
  }
} // End of TestBase
