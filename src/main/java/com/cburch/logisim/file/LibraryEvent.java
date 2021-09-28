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

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record LibraryEvent(Library getSource, int getAction, Object getData) {

  public static final int ADD_TOOL = 0;
  public static final int REMOVE_TOOL = 1;
  public static final int MOVE_TOOL = 2;
  public static final int ADD_LIBRARY = 3;
  public static final int REMOVE_LIBRARY = 4;
  public static final int SET_MAIN = 5;
  public static final int SET_NAME = 6;
  public static final int DIRTY_STATE = 7;

}
