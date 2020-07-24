/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
  private StringBuffer lastRow;
  private int row;
  private boolean sendStdout;

  public TtyState(int rows, int cols) {
    lastClock = Value.UNKNOWN;
    rowData = new String[rows - 1];
    colCount = cols;
    lastRow = new StringBuffer(cols);
    sendStdout = false;
    clear();
  }

  public void add(char c) {
    if (sendStdout) {
      TtyInterface.sendFromTty(c);
    }

    int lastLength = lastRow.length();
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
    Value ret = lastClock;
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
    int oldRows = rowData.length + 1;
    if (rows != oldRows) {
      String[] newData = new String[rows - 1];
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

    int oldCols = colCount;
    if (cols != oldCols) {
      colCount = cols;
      if (cols < oldCols) { // will need to trim any long rows
        for (int i = 0; i < rows - 1; i++) {
          String s = rowData[i];
          if (s.length() > cols) rowData[i] = s.substring(0, cols);
        }
        if (lastRow.length() > cols) {
          lastRow.delete(cols, lastRow.length());
        }
      }
    }
  }
}
