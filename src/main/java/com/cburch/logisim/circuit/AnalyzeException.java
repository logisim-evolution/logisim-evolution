/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

public class AnalyzeException extends Exception {
  public static class CannotHandle extends AnalyzeException {
    private static final long serialVersionUID = 1L;

    public CannotHandle(String reason) {
      super(S.get("analyzeCannotHandleError", reason));
    }
  }

  public static class Circular extends AnalyzeException {
    private static final long serialVersionUID = 1L;

    public Circular() {
      super(S.get("analyzeCircularError"));
    }
  }

  public static class Conflict extends AnalyzeException {
    private static final long serialVersionUID = 1L;

    public Conflict() {
      super(S.get("analyzeConflictError"));
    }
  }

  private static final long serialVersionUID = 1L;

  public AnalyzeException() {}

  public AnalyzeException(String message) {
    super(message);
  }
}
