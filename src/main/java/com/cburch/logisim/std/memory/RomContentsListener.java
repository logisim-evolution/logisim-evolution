/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

class RomContentsListener implements HexModelListener {
  private static class Change extends Action {
    private final RomContentsListener source;
    private final MemContents contents;
    private final long start;
    private final long[] oldValues;
    private final long[] newValues;
    private boolean completed = true;

    Change(
        RomContentsListener source,
        MemContents contents,
        long start,
        long[] oldValues,
        long[] newValues) {
      this.source = source;
      this.contents = contents;
      this.start = start;
      this.oldValues = oldValues;
      this.newValues = newValues;
    }

    @Override
    public Action append(Action other) {
      if (other instanceof Change o) {
        final var oEnd = o.start + o.newValues.length;
        final var end = start + newValues.length;
        if (oEnd >= start && end >= o.start) {
          final var nStart = Math.min(start, o.start);
          final var nEnd = Math.max(end, oEnd);
          final var nOld = new long[(int) (nEnd - nStart)];
          final var nNew = new long[(int) (nEnd - nStart)];
          System.arraycopy(o.oldValues, 0, nOld, (int) (o.start - nStart), o.oldValues.length);
          System.arraycopy(oldValues, 0, nOld, (int) (start - nStart), oldValues.length);
          System.arraycopy(newValues, 0, nNew, (int) (start - nStart), newValues.length);
          System.arraycopy(o.newValues, 0, nNew, (int) (o.start - nStart), o.newValues.length);
          return new Change(source, contents, nStart, nOld, nNew);
        }
      }
      return super.append(other);
    }

    @Override
    public void doIt(Project proj) {
      if (!completed) {
        completed = true;
        try {
          source.setEnabled(false);
          contents.set(start, newValues);
        } finally {
          source.setEnabled(true);
        }
      }
    }

    @Override
    public String getName() {
      return S.get("romChangeAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      if (other instanceof Change o) {
        final var oEnd = o.start + o.newValues.length;
        final var end = start + newValues.length;
        if (oEnd >= start && end >= o.start) return true;
      }
      return super.shouldAppendTo(other);
    }

    @Override
    public void undo(Project proj) {
      if (completed) {
        completed = false;
        try {
          source.setEnabled(false);
          contents.set(start, oldValues);
        } finally {
          source.setEnabled(true);
        }
      }
    }
  }

  final Project proj;
  boolean enabled = true;

  RomContentsListener(Project proj) {
    this.proj = proj;
  }

  @Override
  public void bytesChanged(HexModel source, long start, long numBytes, long[] oldValues) {
    if (enabled && proj != null && oldValues != null) {
      // this change needs to be logged in the undo log
      final var newValues = new long[oldValues.length];
      for (var i = 0; i < newValues.length; i++) {
        newValues[i] = source.get(start + i);
      }
      proj.doAction(new Change(this, (MemContents) source, start, oldValues, newValues));
    }
  }

  @Override
  public void metainfoChanged(HexModel source) {
    // ignore - this can only come from an already-registered
    // action
  }

  void setEnabled(boolean value) {
    enabled = value;
  }
}
