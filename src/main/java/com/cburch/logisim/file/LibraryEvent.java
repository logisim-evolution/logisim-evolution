/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.tools.Library;

public class LibraryEvent {
  public static final int ADD_TOOL = 0;
  public static final int REMOVE_TOOL = 1;
  public static final int MOVE_TOOL = 2;
  public static final int ADD_LIBRARY = 3;
  public static final int REMOVE_LIBRARY = 4;
  public static final int SET_MAIN = 5;
  public static final int SET_NAME = 6;
  public static final int DIRTY_STATE = 7;

  private final Library source;
  private final int action;
  private final Object data;

  LibraryEvent(Library source, int action, Object data) {
    this.source = source;
    this.action = action;
    this.data = data;
  }

  public int getAction() {
    return action;
  }

  public Object getData() {
    return data;
  }

  public Library getSource() {
    return source;
  }
}
