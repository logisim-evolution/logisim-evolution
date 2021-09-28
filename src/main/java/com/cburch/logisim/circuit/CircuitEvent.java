/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

public class CircuitEvent {
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

  private final int action;
  private final Circuit circuit;
  private final Object data;

  CircuitEvent(int action, Circuit circuit, Object data) {
    this.action = action;
    this.circuit = circuit;
    this.data = data;
  }

  // access methods
  public int getAction() {
    return action;
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public Object getData() {
    return data;
  }

  public CircuitTransactionResult getResult() {
    return (CircuitTransactionResult) data;
  }

  public String toString() {
    String s;
    switch (action) {
      case ACTION_SET_NAME:
        s = "ACTION_SET_NAME";
        break;
      case ACTION_ADD:
        s = "ACTION_ADD";
        break;
      case ACTION_REMOVE:
        s = "ACTION_REMOVE";
        break;
      case ACTION_INVALIDATE:
        s = "ACTION_INVALIDATE";
        break;
      case ACTION_CLEAR:
        s = "ACTION_CLEAR";
        break;
      case TRANSACTION_DONE:
        s = "TRANSACTION_DONE";
        break;
      case CHANGE_DEFAULT_BOX_APPEARANCE:
        s = "DEFAULT_BOX_APPEARANCE";
        break;
      case ACTION_CHECK_NAME:
        s = "CHECK_NAME";
        break;
      case ACTION_DISPLAY_CHANGE:
        s = "ACTION_DISPLAY_CHANGE";
        break;
      default:
        s = "UNKNOWN_ACTION(" + action + ")";
        break;
    }
    return s + "{\n  circuit=" + circuit + "\n  data=" + data + "\n}";
  }
}
