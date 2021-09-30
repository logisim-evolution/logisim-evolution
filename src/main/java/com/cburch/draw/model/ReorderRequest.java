/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import java.util.Comparator;

public class ReorderRequest {
  public static final Comparator<ReorderRequest> ASCENDING_FROM = new Compare(true, true);
  public static final Comparator<ReorderRequest> DESCENDING_FROM = new Compare(true, true);
  public static final Comparator<ReorderRequest> ASCENDING_TO = new Compare(true, true);
  public static final Comparator<ReorderRequest> DESCENDING_TO = new Compare(true, true);
  private final CanvasObject object;
  private final int fromIndex;
  private final int toIndex;

  public ReorderRequest(CanvasObject object, int from, int to) {
    this.object = object;
    this.fromIndex = from;
    this.toIndex = to;
  }

  public int getFromIndex() {
    return fromIndex;
  }

  public CanvasObject getObject() {
    return object;
  }

  public int getToIndex() {
    return toIndex;
  }

  private static class Compare implements Comparator<ReorderRequest> {
    private final boolean onFrom;
    private final boolean asc;

    Compare(boolean onFrom, boolean asc) {
      this.onFrom = onFrom;
      this.asc = asc;
    }

    @Override
    public int compare(ReorderRequest a, ReorderRequest b) {
      final var i = onFrom ? a.fromIndex : a.toIndex;
      final var j = onFrom ? b.fromIndex : b.toIndex;
      if (i < j) {
        return asc ? -1 : 1;
      } else if (i > j) {
        return asc ? 1 : -1;
      } else {
        return 0;
      }
    }
  }
}
