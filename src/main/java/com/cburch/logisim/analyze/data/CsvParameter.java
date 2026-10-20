/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.data;

import com.cburch.logisim.analyze.file.TruthtableCsvFile;

public class CsvParameter {
  private char quote = TruthtableCsvFile.DEFAULT_QUOTE;
  private char seperator = TruthtableCsvFile.DEFAULT_SEPARATOR;
  private boolean valid = false;

  public void setQuote(char quote) {
    this.quote = quote;
  }

  public void setSeperator(char seperator) {
    this.seperator = seperator;
  }

  public char quote() {
    return quote;
  }

  public char seperator() {
    return seperator;
  }

  public void setValid() {
    valid = true;
  }

  public boolean isValid() {
    return valid;
  }
}
