/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.start.TtyInterface;
import com.cburch.logisim.instance.InstanceData;
import java.util.Arrays;

class TtyState implements InstanceData, Cloneable {
  private Value lastClock;
  private String[] rowData;
  private int colCount;
  private final StringBuilder lastRow;
  private int row;
  private boolean sendStdout;

  public TtyState(int rows, int cols) {
    lastClock = Value.UNKNOWN;
    rowData = new String[rows - 1];
    colCount = cols;
    lastRow = new StringBuilder(cols);
    sendStdout = false;
    clear();
  }

  public void add(char c) {
    if (sendStdout) {
      TtyInterface.sendFromTty(c);
    }

    final var lastLength = lastRow.length();
    switch (c) {
      case 12: // control-L
        row = 0;
        lastRow.delete(0, lastLength);
        Arrays.fill(rowData, "");
        break;
      case '\b': // backspace
        if (lastLength > 0) lastRow.delete(lastLength - 1, lastLength);
        break;
      case '\n':
      case '\r': // newline
        commit();
        break;
      default:
        if (!Character.isISOControl(c)) {
          if (lastLength == colCount) commit();
          lastRow.append(c);
        }
    }
  }

  public void clear() {
    Arrays.fill(rowData, "");
    lastRow.delete(0, lastRow.length());
    row = 0;
  }

  @Override
  public TtyState clone() {
    try {
      TtyState ret = (TtyState) super.clone();
      ret.rowData = this.rowData.clone();
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  private void commit() {
    if (row >= rowData.length) {
      System.arraycopy(rowData, 1, rowData, 0, rowData.length - 1);
      rowData[row - 1] = lastRow.toString();
    } else {
      rowData[row] = lastRow.toString();
      row++;
    }
    lastRow.delete(0, lastRow.length());
  }

  public int getCursorColumn() {
    return lastRow.length();
  }

  public int getCursorRow() {
    return row;
  }

  public String getRowString(int index) {
    if (index < row) return rowData[index];
    else if (index == row) return lastRow.toString();
    else return "";
  }

  public Value setLastClock(Value newClock) {
    final var ret = lastClock;
    lastClock = newClock;
    return ret;
  }

  public void setSendStdout(boolean value) {
    sendStdout = value;
  }

  public int getNrRows() {
    return rowData.length + 1;
  }

  public int getNrCols() {
    return colCount;
  }

  public void updateSize(int rows, int cols) {
    final var oldRows = rowData.length + 1;
    if (rows != oldRows) {
      final var newData = new String[rows - 1];
      if (rows > oldRows // rows have been added,
          || row < rows - 1) { // or rows removed but filled rows fit
        System.arraycopy(rowData, 0, newData, 0, row);
        Arrays.fill(newData, row, rows - 1, "");
      } else { // rows removed, and some filled rows must go
        System.arraycopy(rowData, row - rows + 1, newData, 0, rows - 1);
        row = rows - 1;
      }
      rowData = newData;
    }

    final var oldCols = colCount;
    if (cols != oldCols) {
      colCount = cols;
      if (cols < oldCols) { // will need to trim any long rows
        for (int i = 0; i < rows - 1; i++) {
          final var s = rowData[i];
          if (s.length() > cols) rowData[i] = s.substring(0, cols);
        }
        if (lastRow.length() > cols) {
          lastRow.delete(cols, lastRow.length());
        }
      }
    }
  }
}
