/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.util.LineBuffer;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record CircuitEvent(int getAction, Circuit getCircuit, Object getData) {

  public static final int ACTION_SET_NAME = 0; // name changed
  public static final int ACTION_ADD = 1; // component added
  public static final int ACTION_REMOVE = 2; // component removed
  //  public static final int ACTION_CHANGE = 3; // component changed
  public static final int ACTION_INVALIDATE = 4; // component invalidated (pin types changed)
  public static final int ACTION_CLEAR = 5; // entire circuit cleared
  public static final int TRANSACTION_DONE = 6;
  public static final int CHANGE_DEFAULT_BOX_APPEARANCE = 7;
  public static final int ACTION_CHECK_NAME = 8;
  public static final int ACTION_DISPLAY_CHANGE = 9; // viewed/haloed status change

  public CircuitTransactionResult getResult() {
    return (CircuitTransactionResult) getData;
  }

  @Override
  public String toString() {
    final var s = switch (getAction) {
      case ACTION_SET_NAME -> "ACTION_SET_NAME";
      case ACTION_ADD -> "ACTION_ADD";
      case ACTION_REMOVE -> "ACTION_REMOVE";
      case ACTION_INVALIDATE -> "ACTION_INVALIDATE";
      case ACTION_CLEAR -> "ACTION_CLEAR";
      case TRANSACTION_DONE -> "TRANSACTION_DONE";
      case CHANGE_DEFAULT_BOX_APPEARANCE -> "DEFAULT_BOX_APPEARANCE";
      case ACTION_CHECK_NAME -> "CHECK_NAME";
      case ACTION_DISPLAY_CHANGE -> "ACTION_DISPLAY_CHANGE";
      default -> "UNKNOWN_ACTION(" + getAction + ")";
    };
    return LineBuffer.format("{{1}}{\n  circuit={{2}}\n  data={{3}}\n}", s, getCircuit, getData);
  }

}
