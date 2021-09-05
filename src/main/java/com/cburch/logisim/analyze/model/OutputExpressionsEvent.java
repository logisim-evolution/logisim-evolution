/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class OutputExpressionsEvent {
  public static final int ALL_VARIABLES_REPLACED = 0;
  public static final int OUTPUT_EXPRESSION = 1;
  public static final int OUTPUT_MINIMAL = 2;

  private final AnalyzerModel model;
  private final int type;
  private final String variable;
  private final Object data;
}
