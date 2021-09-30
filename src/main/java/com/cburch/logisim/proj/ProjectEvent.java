/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.Tool;

// NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
// getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
// that in future, but for now it looks stupid in this file only.
public record ProjectEvent(int getAction, Project getProject, Object getOldData, Object getData) {

  public static final int ACTION_SET_FILE = 0; // change file
  public static final int ACTION_SET_CURRENT = 1; // change current
  public static final int ACTION_SET_TOOL = 2; // change tool
  public static final int ACTION_SELECTION = 3; // selection alterd
  public static final int ACTION_SET_STATE = 4; // circuit state changed
  public static final int ACTION_START = 5; // action about to start
  public static final int ACTION_COMPLETE = 6; // action has completed
  public static final int ACTION_MERGE = 7; // one action has been appended to
  // another
  public static final int UNDO_START = 8; // undo about to start
  public static final int UNDO_COMPLETE = 9; // undo has completed
  public static final int REPAINT_REQUEST = 10; // canvas should be repainted
  public static final int REDO_START = 11;
  public static final int REDO_COMPLETE = 12;

  public ProjectEvent(int action, Project project) {
    this(action, project, null, null);
  }

  public ProjectEvent(int action, Project project, Object data) {
    // FIXME: I'd move oldData to the end of argument list, so all the constructors
    // would retain the same argument order, just adding new fields at the end.
    this(action, project, null, data);
  }

  // convenience methods
  public LogisimFile getLogisimFile() {
    return getProject.getLogisimFile();
  }

  public Circuit getCircuit() {
    return getProject.getCurrentCircuit();
  }

  public Tool getTool() {
    return getProject.getTool();
  }

}
