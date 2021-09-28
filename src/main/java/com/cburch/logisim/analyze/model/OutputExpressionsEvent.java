/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

public class OutputExpressionsEvent {
  public static final int ALL_VARIABLES_REPLACED = 0;
  public static final int OUTPUT_EXPRESSION = 1;
  public static final int OUTPUT_MINIMAL = 2;

  private final AnalyzerModel model;
  private final int type;
  private final String variable;
  private final Object data;

  public OutputExpressionsEvent(AnalyzerModel model, int type, String variable, Object data) {
    this.model = model;
    this.type = type;
    this.variable = variable;
    this.data = data;
  }

  public Object getData() {
    return data;
  }

  public AnalyzerModel getModel() {
    return model;
  }

  public int getType() {
    return type;
  }

  public String getVariable() {
    return variable;
  }
}
