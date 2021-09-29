/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.file.ToolbarData;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;

class ToolbarActions {
  private ToolbarActions() {
    // private
  }

  public static Action addSeparator(ToolbarData toolbar, int pos) {
    return new AddSeparator(toolbar, pos);
  }

  public static Action addTool(ToolbarData toolbar, Tool tool) {
    return new AddTool(toolbar, tool);
  }

  public static Action moveTool(ToolbarData toolbar, int src, int dest) {
    return new MoveTool(toolbar, src, dest);
  }

  public static Action removeSeparator(ToolbarData toolbar, int pos) {
    return new RemoveSeparator(toolbar, pos);
  }

  public static Action removeTool(ToolbarData toolbar, int pos) {
    return new RemoveTool(toolbar, pos);
  }

  private static class AddSeparator extends Action {
    final ToolbarData toolbar;
    final int pos;

    AddSeparator(ToolbarData toolbar, int pos) {
      this.toolbar = toolbar;
      this.pos = pos;
    }

    @Override
    public void doIt(Project proj) {
      toolbar.addSeparator(pos);
    }

    @Override
    public String getName() {
      return S.get("toolbarInsertSepAction");
    }

    @Override
    public void undo(Project proj) {
      toolbar.remove(pos);
    }
  }

  private static class AddTool extends Action {
    final ToolbarData toolbar;
    final Tool tool;
    int pos;

    AddTool(ToolbarData toolbar, Tool tool) {
      this.toolbar = toolbar;
      this.tool = tool;
    }

    @Override
    public void doIt(Project proj) {
      pos = toolbar.getContents().size();
      toolbar.addTool(pos, tool);
    }

    @Override
    public String getName() {
      return S.get("toolbarAddAction");
    }

    @Override
    public void undo(Project proj) {
      toolbar.remove(pos);
    }
  }

  private static class MoveTool extends Action {
    final ToolbarData toolbar;
    final int oldpos;
    final int dest;

    MoveTool(ToolbarData toolbar, int oldpos, int dest) {
      this.toolbar = toolbar;
      this.oldpos = oldpos;
      this.dest = dest;
    }

    @Override
    public Action append(Action other) {
      if (other instanceof MoveTool o) {
        if (this.toolbar == o.toolbar && this.dest == o.oldpos) {
          // TODO if (this.oldpos == o.dest) return null;
          return new MoveTool(toolbar, this.oldpos, o.dest);
        }
      }
      return super.append(other);
    }

    @Override
    public void doIt(Project proj) {
      toolbar.move(oldpos, dest);
    }

    @Override
    public String getName() {
      return S.get("toolbarMoveAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      if (other instanceof MoveTool o) {
        return this.toolbar == o.toolbar && o.dest == this.oldpos;
      } else {
        return false;
      }
    }

    @Override
    public void undo(Project proj) {
      toolbar.move(dest, oldpos);
    }
  }

  private static class RemoveSeparator extends Action {
    final ToolbarData toolbar;
    final int pos;

    RemoveSeparator(ToolbarData toolbar, int pos) {
      this.toolbar = toolbar;
      this.pos = pos;
    }

    @Override
    public void doIt(Project proj) {
      toolbar.remove(pos);
    }

    @Override
    public String getName() {
      return S.get("toolbarRemoveSepAction");
    }

    @Override
    public void undo(Project proj) {
      toolbar.addSeparator(pos);
    }
  }

  private static class RemoveTool extends Action {
    final ToolbarData toolbar;
    Object removed;
    final int which;

    RemoveTool(ToolbarData toolbar, int which) {
      this.toolbar = toolbar;
      this.which = which;
    }

    @Override
    public void doIt(Project proj) {
      removed = toolbar.remove(which);
    }

    @Override
    public String getName() {
      return S.get("toolbarRemoveAction");
    }

    @Override
    public void undo(Project proj) {
      if (removed instanceof Tool) {
        toolbar.addTool(which, (Tool) removed);
      } else if (removed == null) {
        toolbar.addSeparator(which);
      }
    }
  }
}
