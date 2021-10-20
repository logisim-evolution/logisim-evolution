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
import lombok.Getter;
import lombok.Setter;

public class CsvParameter {
  @Getter @Setter private char quote = TruthtableCsvFile.DEFAULT_QUOTE;
  @Getter @Setter private char seperator = TruthtableCsvFile.DEFAULT_SEPARATOR;
  @Getter @Setter private boolean valid = false;
}
