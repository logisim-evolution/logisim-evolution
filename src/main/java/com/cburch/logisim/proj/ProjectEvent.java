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

public class ProjectEvent {
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

  private final int action;
  private final Project proj;
  private Object oldData;
  private final Object data;

  public ProjectEvent(int action, Project proj) {
    this.action = action;
    this.proj = proj;
    this.data = null;
  }

  public ProjectEvent(int action, Project proj, Object data) {
    this.action = action;
    this.proj = proj;
    this.data = data;
  }

  public ProjectEvent(int action, Project proj, Object old, Object data) {
    this.action = action;
    this.proj = proj;
    this.oldData = old;
    this.data = data;
  }

  // access methods
  public int getAction() {
    return action;
  }

  public Circuit getCircuit() {
    return proj.getCurrentCircuit();
  }

  public Object getData() {
    return data;
  }

  // convenience methods
  public LogisimFile getLogisimFile() {
    return proj.getLogisimFile();
  }

  public Object getOldData() {
    return oldData;
  }

  public Project getProject() {
    return proj;
  }

  public Tool getTool() {
    return proj.getTool();
  }
}
