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

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class TruthTable {

  private static final Entry DEFAULT_ENTRY = Entry.DONT_CARE;

  private MyListener myListener = new MyListener();
  private List<TruthTableListener> listeners = new ArrayList<TruthTableListener>();

  private AnalyzerModel model;
  private ArrayList<Row> rows = new ArrayList<>(); // visible input rows
  private ArrayList<Entry[]> columns = new ArrayList<>(); // output columns
  private static final CompareInputs sortByInputs = new CompareInputs();

  private class Row implements Iterable<Integer> {
    // todo: probably more efficient to store this in baseIdx/dcMask format.
    Entry inputs[];

    Row(int idx, int numInputs, int mask) {
      inputs = new Entry[numInputs];
      for (int i = numInputs - 1; i >= 0; i--) {
        inputs[i] = (mask & 1) == 0 ? (idx & 1) == 0 ? Entry.ZERO : Entry.ONE : Entry.DONT_CARE;
        idx = idx >> 1;
        mask = mask >> 1;
      }
    }

    Row(Entry entries[], int numInputs) {
      inputs = new Entry[numInputs];
      for (int i = 0; i < numInputs; i++) inputs[i] = entries[i];
    }

    public int baseIndex() {
      int idx = 0;
      for (int i = 0; i < inputs.length; i++) idx = (idx << 1) | (inputs[i] == Entry.ONE ? 1 : 0);
      return idx;
    }

    public int dcMask() {
      int mask = 0;
      for (int i = 0; i < inputs.length; i++)
        mask = (mask << 1) | (inputs[i] == Entry.DONT_CARE ? 1 : 0);
      return mask;
    }

    public int duplicity() {
      int count = 1;
      for (int i = 0; i < inputs.length; i++) count *= (inputs[i] == Entry.DONT_CARE ? 2 : 1);
      return count;
    }

    @Override
    public String toString() {
      String s = "row[";
      for (int i = 0; i < inputs.length; i++) {
        if (i != 0) s += " ";
        s += inputs[i].getDescription();
      }
      s += "]";
      s += " dup=" + duplicity();
      s += String.format(" base=%x dcmask=%x", baseIndex(), dcMask());
      return s;
    }

    public String toBitString(List<Var> vars) {
      String s = null;
      int i = 0;
      for (Var v : vars) {
        if (s == null) s = "";
        else s += " ";
        for (int j = 0; j < v.width; j++) s += inputs[i++].toBitString();
      }
      return s;
    }

    public boolean contains(int idx) {
      return (idx & ~dcMask()) == baseIndex();
    }

    public boolean contains(Row other) {
      return contains(other.baseIndex()) && (other.dcMask() & ~dcMask()) == 0;
    }

    public boolean intersects(Row other) {
      int dc = dcMask() | other.dcMask();
      return (other.baseIndex() & ~dc) == (baseIndex() & ~dc);
    }

    public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {
        int base = baseIndex();
        int mask = dcMask();
        int nbits = inputs.length;
        int count = duplicity();
        int iter = 0;

        @Override
        public boolean hasNext() {
          return iter < count;
        }

        @Override
        public Integer next() {
          int add = iter;
          int keep = 0;
          for (int b = 0; b < nbits; b++) {
            if ((mask & (1 << b)) == 0) add = ((add & ~keep) << 1) | (add & keep);
            keep |= (1 << b);
          }
          iter++;
          return base | add;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  public static class CompareInputs implements Comparator<Row> {
    public int compare(Row r1, Row r2) {
      return r1.baseIndex() - r2.baseIndex();
    }
  }

  private void initRows() {
    int inputs = getInputColumnCount();
    int n = getRowCount();
    rows.clear();
    rows.ensureCapacity(n);
    for (int i = 0; i < n; i++) rows.add(new Row(i, inputs, 0));
  }

  private void initColumns() {
    int outputs = getOutputColumnCount();
    columns.clear();
    columns.ensureCapacity(outputs);
    for (int i = 0; i < outputs; i++) columns.add(null /* created lazily */);
  }

  public TruthTable(AnalyzerModel model) {
    this.model = model;
    initRows();
    initColumns();
    model.getInputs().addVariableListListener(myListener);
    model.getOutputs().addVariableListListener(myListener);
  }

  public void expandVisibleRows() {
    if (getVisibleRowCount() == getRowCount()) return;
    initRows();
    fireRowsChanged();
  }

  public void compactVisibleRows() {
    SortedMap<Implicant, String> partition = Implicant.computePartition(model);
    rows.clear();
    initColumns();
    int ni = getInputColumnCount();
    int no = getOutputColumnCount();
    for (Map.Entry<Implicant, String> it : partition.entrySet()) {
      Implicant imp = it.getKey();
      String val = it.getValue();
      Row r = new Row(imp.values, ni, imp.unknowns);
      rows.add(r);
      for (int col = 0; col < no; col++) {
        Entry value = Entry.parse("" + val.charAt(col));
        Entry[] column = columns.get(col);
        if (column == null && value == DEFAULT_ENTRY) continue;
        else if (column == null) column = getOutputColumn(col);
        for (Integer idx : r) {
          column[idx] = value;
        }
      }
    }
    fireRowsChanged();
    for (int col = 0; col < no; col++) {
      if (columns.get(col) != null) fireCellsChanged(col);
    }
  }

  public void setOutputColumn(int col, Entry[] values) {
    if (values.length != getRowCount()) throw new IllegalArgumentException("bad column length");
    Entry[] oldValues = columns.set(col, values);
    if (oldValues == values) return;
    // Expand rows as dictated by column inconsistencies
    boolean rowsChanged = false;
    for (int i = rows.size() - 1; i >= 0; i--) {
      Row r = rows.get(i);
      int base = r.baseIndex();
      Entry v = values[base];
      boolean split = true;
      while (split) {
        split = false;
        for (Integer idx : r) {
          if (v != values[idx]) {
            splitRow(r, idx);
            rowsChanged = true;
            split = true;
            break;
          }
        }
      }
    }
    if (rowsChanged) fireRowsChanged();
    fireCellsChanged(col);
  }

  void splitRow(Row r, int idx) {
    int base = r.baseIndex();
    if (idx == base || !r.contains(idx)) throw new IllegalArgumentException("bad row split");
    int diff = idx ^ base;
    int n = r.duplicity();
    if (n <= 1) throw new IllegalStateException("row duplicity should be at least 2");
    Row splits = new Row(base, r.inputs.length, diff);
    int m = 0;
    rows.remove(r);
    for (Integer other : splits) {
      Row s = new Row(other, r.inputs.length, r.dcMask() & ~diff);
      m += s.duplicity();
      int pos = Collections.binarySearch(rows, s, sortByInputs);
      if (pos < 0) rows.add(-pos - 1, s);
      else throw new IllegalStateException("unexpected row split");
    }
    if (m != n) throw new IllegalStateException("assertion failed in row split");
  }

  public Entry getVisibleOutputEntry(int row, int col) {
    Row r = rows.get(row);
    int idx = r.baseIndex();
    return getOutputEntry(idx, col);
  }

  public Entry getOutputEntry(int idx, int col) {
    if (idx < 0 || col < 0) return DEFAULT_ENTRY;
    Entry[] column = columns.get(col);
    return (column == null ? DEFAULT_ENTRY : idx < column.length ? column[idx] : DEFAULT_ENTRY);
  }

  public String getVisibleOutputs(int row) {
    Row r = rows.get(row);
    int idx = r.baseIndex();
    String s = "";
    for (Entry[] column : columns)
      s += (column == null ? DEFAULT_ENTRY : column[idx]).getDescription();
    return s;
  }

  public Entry getVisibleInputEntry(int row, int col) {
    Row r = rows.get(row);
    return r.inputs[col];
  }

  public int getVisibleRowDcMask(int row) {
    Row r = rows.get(row);
    return r.dcMask();
  }

  public int getVisibleRowIndex(int row) {
    Row r = rows.get(row);
    return r.baseIndex();
  }

  public Iterable<Integer> getVisibleRowIndexes(int row) {
    Row r = rows.get(row);
    return r;
  }

  public Entry getInputEntry(int idx, int col) {
    if (idx < 0 || idx >= getRowCount()) throw new IndexOutOfBoundsException("bad row index");
    int inputs = getInputColumnCount();
    if (col < 0 || col >= inputs) throw new IndexOutOfBoundsException("bad input column index");
    return isInputSet(idx, col, inputs) ? Entry.ONE : Entry.ZERO;
  }

  public static boolean isInputSet(int idx, int col, int inputs) {
    return (idx & (1 << (inputs - col - 1))) != 0;
  }

  public Entry[] getOutputColumn(int col) {
    Entry[] column = columns.get(col);
    if (column == null) {
      if (col < 0 || col >= getOutputColumnCount())
        throw new IndexOutOfBoundsException("bad output column index");
      column = new Entry[getRowCount()];
      Arrays.fill(column, DEFAULT_ENTRY);
      columns.set(col, column);
    }
    return column;
  }

  private boolean identicalOutputs(int idx1, int idx2) {
    if (idx1 == idx2) return true;
    for (int col = 0; col < columns.size(); col++) {
      Entry[] column = columns.get(col);
      if (column == null) continue;
      if (column[idx1] != column[idx2]) return false;
    }
    return true;
  }

  private void mergeOutputs(int idx1, int idx2, boolean changed[]) {
    if (idx1 == idx2) return;
    for (int col = 0; col < columns.size(); col++) {
      Entry[] column = columns.get(col);
      if (column == null) continue;
      if (column[idx1] != column[idx2]) {
        column[idx2] = column[idx1];
        changed[col] = true;
      }
    }
  }

  private boolean setDontCare(Row r, int dc, boolean force, boolean changed[]) {
    Row rNew = new Row(r.baseIndex(), r.inputs.length, r.dcMask() | dc);
    int base = rNew.baseIndex();
    if (!force) {
      for (Integer idx : rNew) {
        if (!identicalOutputs(base, idx)) return false;
      }
    }
    for (int i = 0; i < rows.size(); i++) {
      Row s = rows.get(i);
      if (!rNew.intersects(s)) continue;
      if (rNew.contains(s)) {
        for (Integer idx : s) mergeOutputs(base, idx, changed);
        rows.remove(i);
      } else {
        // find a bit we can flip in s so it doesn't conflict
        int pos;
        for (pos = s.inputs.length - 1; pos >= 0; pos--) {
          if (s.inputs[pos] == Entry.DONT_CARE && rNew.inputs[pos] != Entry.DONT_CARE) break;
        }
        if (pos < 0) throw new IllegalStateException("failed row merge");
        int bit = (1 << (s.inputs.length - 1 - pos));
        splitRow(s, s.baseIndex() ^ bit);
      }
      i--; // back up, may need a second split
    }
    int pos = Collections.binarySearch(rows, rNew, sortByInputs);
    if (pos < 0) rows.add(-pos - 1, rNew);
    else throw new IllegalStateException("failed row merge");
    return true;
  }

  public boolean setVisibleInputEntry(int row, int col, Entry value, boolean force) {
    Row r = rows.get(row);
    if (r.inputs[col] == value) return false;
    int dc = (1 << (r.inputs.length - 1 - col));
    if (value == Entry.DONT_CARE) {
      boolean changed[] = new boolean[columns.size()];
      if (!setDontCare(r, dc, force, changed)) return false;
      fireRowsChanged();
      for (int ocol = 0; ocol < columns.size(); ocol++) {
        if (changed[ocol]) fireCellsChanged(ocol);
      }
      return true;
    } else if (value == Entry.ONE || value == Entry.ZERO) {
      if (r.inputs[col] != Entry.DONT_CARE) return false;
      splitRow(r, r.baseIndex() | dc);
      fireRowsChanged();
      return true;
    } else {
      throw new IllegalArgumentException("invalid input entry");
    }
  }

  public void setVisibleOutputEntry(int row, int col, Entry value) {
    Row r = rows.get(row);
    Entry[] column = columns.get(col);
    if (column == null && value == DEFAULT_ENTRY) return;
    else if (column == null) column = getOutputColumn(col);
    boolean changed = false;
    for (Integer idx : r) {
      if (column[idx] == value) continue;
      changed = true;
      column[idx] = value;
    }
    if (changed) fireCellsChanged(col);
  }

  Row findRow(int idx) {
    for (int i = rows.size() - 1; i >= 0; i--) {
      Row r = rows.get(i);
      if (r.contains(idx)) return r;
    }
    throw new IllegalStateException("missing row");
  }

  public int findVisibleRowContaining(int idx) {
    for (int i = rows.size() - 1; i >= 0; i--) {
      Row r = rows.get(i);
      if (r.contains(idx)) return i;
    }
    throw new IllegalStateException("missing row");
  }

  public void setVisibleRows(ArrayList<Entry[]> newEntries, boolean force) {
    int ni = getInputColumnCount();
    int no = getOutputColumnCount();
    ArrayList<Row> newRows = new ArrayList<>(newEntries.size());
    for (Entry values[] : newEntries) {
      if (values.length != ni + no) throw new IllegalArgumentException("wrong column count");
      newRows.add(new Row(values, ni));
    }
    // check that newRows has no intersections
    List<Var> ivars = getInputVariables();
    int taken[] = new int[getRowCount()];
    for (int i = 0; i < newRows.size(); i++) {
      Row r = newRows.get(i);
      for (Integer idx : r) {
        if (taken[idx] != 0 && !force) {
          throw new IllegalArgumentException(
              String.format(
                  "Some inputs are repeated."
                      + " For example, rows %d and %d have overlapping input values %s and %s.",
                  taken[idx],
                  i + 1,
                  newRows.get(taken[idx] - 1).toBitString(ivars),
                  r.toBitString(ivars)));
        } else if (taken[idx] != 0) {
          // todo: split row
          throw new IllegalArgumentException(
              "Sorry, this error can't yet be fixed. Eliminate duplicate rows then try again.");
        } else {
          taken[idx] = i + 1;
        }
      }
    }
    // check that newRows covers all possible cases
    for (int i = 0; i < getRowCount(); i++) {
      if (taken[i] == 0 && !force) {
        throw new IllegalArgumentException(
            String.format(
                "Some inputs are missing." + " For example, there is no row for input %s.",
                new Row(i, ni, 0).toBitString(ivars)));
      } else if (taken[i] == 0) {
        newRows.add(new Row(i, ni, 0));
      }
    }

    Collections.sort(newRows, sortByInputs);
    rows.clear();
    rows = newRows;
    initColumns();

    for (Entry values[] : newEntries) {
      Row r = new Row(values, ni);
      for (int col = 0; col < no; col++) {
        Entry value = values[ni + col];
        Entry[] column = columns.get(col);
        if (column == null && value == DEFAULT_ENTRY) continue;
        else if (column == null) column = getOutputColumn(col);
        for (Integer idx : r) {
          column[idx] = value;
        }
      }
    }
    fireRowsChanged();
    for (int col = 0; col < no; col++) {
      if (columns.get(col) != null) fireCellsChanged(col);
    }
  }

  public void setOutputEntry(int idx, int col, Entry value) {
    Entry[] column = columns.get(col);
    if (column == null && value == DEFAULT_ENTRY) return;
    else if (column == null) column = getOutputColumn(col);
    if (column[idx] == value) return;
    column[idx] = value;
    Row r = findRow(idx);
    if (r.duplicity() > 1) {
      splitRow(r, idx);
      fireRowsChanged();
    }
    fireCellsChanged(col);
  }

  private class MyListener implements VariableListListener {

    public void listChanged(VariableListEvent event) {
      if (event.getSource() == model.getInputs()) {
        inputsChanged(event);
        for (int col = 0; col < columns.size(); col++) {
          Entry[] column = columns.get(col);
          if (column == null) continue;
          column = inputsChangedForOutput(column, event);
          columns.set(col, column);
        }
        fireRowsChanged();
      } else {
        outputsChanged(event);
      }
      fireStructureChanged(event);
    }

    private void outputsChanged(VariableListEvent event) {
      Var v = event.getVariable();
      int action = event.getType();
      if (action == VariableListEvent.ALL_REPLACED) {
        initColumns();
      } else if (action == VariableListEvent.ADD) {
        int bitIndex = event.getBitIndex();
        for (int b = v.width - 1; b >= 0; b--) columns.add(bitIndex - b, null /* lazily created */);
      } else if (action == VariableListEvent.MOVE) {
        int delta = event.getBitIndex();
        int newIndex = getOutputIndex(v.bitName(0));
        if (delta > 0) {
          for (int b = 0; b < v.width; b++) {
            Entry[] column = columns.remove(newIndex - delta - b);
            columns.add(newIndex - b, column);
          }
        } else if (delta < 0) {
          for (int b = v.width - 1; b >= 0; b--) {
            Entry[] column = columns.remove(newIndex - delta - b);
            columns.add(newIndex - b, column);
          }
        }
      } else if (action == VariableListEvent.REMOVE) {
        int bitIndex = event.getBitIndex();
        for (int b = 0; b < v.width; b++) columns.remove(bitIndex - b);
      } else if (action == VariableListEvent.REPLACE) {
        Var oldVar = v;
        int bitIndex = event.getBitIndex();
        Var newVar = getOutputVariable(event.getIndex());
        int lost = oldVar.width - newVar.width;
        int pos = bitIndex + 1 - oldVar.width;
        if (lost > 0) {
          while (lost-- != 0) columns.remove(pos);
        } else if (lost < 0) {
          while (lost++ != 0) columns.add(pos, null /* lazily created */);
        }
      }
    }

    private void inputsChanged(VariableListEvent event) {
      Var v = event.getVariable();
      int action = event.getType();
      if (action == VariableListEvent.ADD) {
        int bitIndex = event.getBitIndex();
        int oldCount = getInputColumnCount() - v.width;
        for (int b = v.width - 1; b >= 0; b--) addInput(bitIndex - b, oldCount++);
      } else if (action == VariableListEvent.REMOVE) {
        int bitIndex = event.getBitIndex();
        int oldCount = getInputColumnCount() + v.width;
        for (int b = 0; b < v.width; b++) removeInput(bitIndex - b, oldCount--);
      } else if (action == VariableListEvent.MOVE) {
        int delta = event.getBitIndex();
        int newIndex = getInputIndex(v.bitName(0));
        if (delta > 0) {
          for (int b = 0; b < v.width; b++) moveInput(newIndex - delta - b, newIndex - b);
        } else if (delta < 0) {
          for (int b = v.width - 1; b >= 0; b--) moveInput(newIndex - delta - b, newIndex - b);
        }
      } else if (action == VariableListEvent.REPLACE) {
        Var oldVar = v;
        int bitIndex = event.getBitIndex();
        Var newVar = getInputVariable(event.getIndex());
        int lost = oldVar.width - newVar.width;
        int oldCount = getInputColumnCount() + lost;
        int pos = bitIndex + 1 - oldVar.width;
        if (lost > 0) {
          while (lost-- != 0) removeInput(pos, oldCount--);
        } else if (lost < 0) {
          while (lost++ != 0) addInput(pos, oldCount++);
        }
      } else if (action == VariableListEvent.ALL_REPLACED) {
        initRows();
      }
    }

    private void moveInput(int oldIndex, int newIndex) {
      int inputs = getInputColumnCount();
      oldIndex = inputs - 1 - oldIndex;
      newIndex = inputs - 1 - newIndex;
      int allMask = (1 << inputs) - 1;
      int sameMask =
          allMask
              ^ ((1 << (1 + Math.max(oldIndex, newIndex))) - 1)
              ^ ((1 << Math.min(oldIndex, newIndex)) - 1); // bits that don't change
      int moveMask = 1 << oldIndex; // bit that moves
      int moveDist = Math.abs(newIndex - oldIndex);
      boolean moveLeft = newIndex > oldIndex;
      int blockMask = allMask ^ sameMask ^ moveMask; // bits that move by one
      ArrayList<Row> ret = new ArrayList<>(2 * rows.size());
      for (Row r : rows) {
        int i = r.baseIndex();
        int dc = r.dcMask();
        int idx0;
        int dc0;
        if (moveLeft) {
          idx0 = (i & sameMask) | ((i & moveMask) << moveDist) | ((i & blockMask) >> 1);
          dc0 = (dc & sameMask) | ((dc & moveMask) << moveDist) | ((dc & blockMask) >> 1);
        } else {
          idx0 = (i & sameMask) | ((i & moveMask) >> moveDist) | ((i & blockMask) << 1);
          dc0 = (dc & sameMask) | ((dc & moveMask) >> moveDist) | ((dc & blockMask) << 1);
        }
        ret.add(new Row(idx0, inputs, dc0));
      }
      Collections.sort(ret, sortByInputs);
      rows = ret;
    }

    private void addInput(int index, int oldCount) {
      // add another Entry column to each row.input
      ArrayList<Row> ret = new ArrayList<>(2 * rows.size());
      for (Row r : rows) {
        int i = r.baseIndex();
        int dc = r.dcMask();
        int b = 1 << (oldCount - index); // _0001000
        int mask = b - 1; // _0000111
        int idx0 = ((i & ~mask) << 1) | 0 | (i & mask); // xxxx0yyy
        int dc0 = ((dc & ~mask) << 1) | 0 | (dc & mask); // wwww0zzz
        ret.add(new Row(idx0 | 0, oldCount + 1, dc0)); // xxxx0yyy
        ret.add(new Row(idx0 | b, oldCount + 1, dc0)); // xxxx1yyy
      }
      Collections.sort(ret, sortByInputs);
      rows = ret;
    }

    private void removeInput(int index, int oldCount) {
      // force an Entry column of each row.input to 'x', then remove it
      int b = (1 << (oldCount - 1 - index)); // _0001000
      boolean changed[] = new boolean[columns.size()];
      for (int i = 0; i < rows.size(); i++) {
        Row r = rows.get(i);
        if (r.inputs[index] == Entry.DONT_CARE) continue;
        setDontCare(r, b, true, changed);
      }
      int mask = b - 1; // _0000111
      ArrayList<Row> ret = new ArrayList<>(rows.size());
      for (Row r : rows) {
        int i = r.baseIndex();
        int dc = r.dcMask();
        int idx0 = ((i >> 1) & ~mask) | (i & mask); // __xxxyyy
        int dc0 = ((dc >> 1) & ~mask) | (dc & mask); // wwww0zzz
        ret.add(new Row(idx0, oldCount - 1, dc0));
      }
      Collections.sort(ret, sortByInputs);
      rows = ret;
    }

    private Entry[] inputsChangedForOutput(Entry[] column, VariableListEvent event) {
      Var v = event.getVariable();
      int action = event.getType();
      if (action == VariableListEvent.ADD) {
        int bitIndex = event.getBitIndex();
        int oldCount = getInputColumnCount() - v.width;
        for (int b = v.width - 1; b >= 0; b--)
          column = addInputForOutput(column, bitIndex - b, oldCount++);
      } else if (action == VariableListEvent.REMOVE) {
        int bitIndex = event.getBitIndex();
        int oldCount = getInputColumnCount() + v.width;
        for (int b = 0; b < v.width; b++)
          column = removeInputForOutput(column, bitIndex - b, oldCount--);
      } else if (action == VariableListEvent.MOVE) {
        int delta = event.getBitIndex();
        int newIndex = getInputIndex(v.bitName(0));
        if (delta > 0) {
          for (int b = 0; b < v.width; b++)
            column = moveInputForOutput(column, newIndex - delta - b, newIndex - b);
        } else if (delta < 0) {
          for (int b = v.width - 1; b >= 0; b--)
            column = moveInputForOutput(column, newIndex - delta - b, newIndex - b);
        }
      } else if (action == VariableListEvent.REPLACE) {
        Var oldVar = v;
        int bitIndex = event.getBitIndex();
        Var newVar = getInputVariable(event.getIndex());
        int lost = oldVar.width - newVar.width;
        int oldCount = getInputColumnCount() + lost;
        int pos = bitIndex + 1 - oldVar.width;
        if (lost > 0) {
          while (lost-- != 0) column = removeInputForOutput(column, pos, oldCount--);
        } else if (lost < 0) {
          while (lost++ != 0) column = addInputForOutput(column, pos, oldCount++);
        }
      }
      return column;
    }

    private Entry[] moveInputForOutput(Entry[] old, int oldIndex, int newIndex) {
      int inputs = getInputColumnCount();
      oldIndex = inputs - 1 - oldIndex;
      newIndex = inputs - 1 - newIndex;
      Entry[] ret = new Entry[old.length];
      int sameMask =
          (old.length - 1)
              ^ ((1 << (1 + Math.max(oldIndex, newIndex))) - 1)
              ^ ((1 << Math.min(oldIndex, newIndex)) - 1); // bits that don't change
      int moveMask = 1 << oldIndex; // bit that moves
      int moveDist = Math.abs(newIndex - oldIndex);
      boolean moveLeft = newIndex > oldIndex;
      int blockMask = (old.length - 1) ^ sameMask ^ moveMask; // bits that move by one
      for (int i = 0; i < old.length; i++) {
        int j; // new index
        if (moveLeft) {
          j = (i & sameMask) | ((i & moveMask) << moveDist) | ((i & blockMask) >> 1);
        } else {
          j = (i & sameMask) | ((i & moveMask) >> moveDist) | ((i & blockMask) << 1);
        }
        ret[j] = old[i];
      }
      return ret;
    }

    private Entry[] removeInputForOutput(Entry[] old, int index, int oldCount) {
      Entry[] ret = new Entry[old.length / 2];
      int j = 0;
      int mask = 1 << (oldCount - 1 - index);
      for (int i = 0; i < old.length; i++) {
        if ((i & mask) == 0) {
          Entry e0 = old[i];
          Entry e1 = old[i | mask];
          ret[j++] = (e0 == e1 ? e0 : Entry.DONT_CARE);
        }
      }
      return ret;
    }

    private Entry[] addInputForOutput(Entry[] old, int index, int oldCount) {
      Entry[] ret = new Entry[2 * old.length];
      int b = 1 << (oldCount - index); // _0001000
      int mask = b - 1; // _0000111
      for (int i = 0; i < old.length; i++) {
        ret[((i & ~mask) << 1) | 0 | (i & mask)] = old[i]; // xxxx0yyy
        ret[((i & ~mask) << 1) | b | (i & mask)] = old[i]; // xxxx1yyy
      }
      return ret;
    }
  }

  public void addTruthTableListener(TruthTableListener l) {
    listeners.add(l);
  }

  public void removeTruthTableListener(TruthTableListener l) {
    listeners.remove(l);
  }

  private void fireRowsChanged() {
    TruthTableEvent event = new TruthTableEvent(this, null);
    for (TruthTableListener l : listeners) {
      l.rowsChanged(event);
    }
  }

  private void fireCellsChanged(int col) {
    TruthTableEvent event = new TruthTableEvent(this, col);
    for (TruthTableListener l : listeners) {
      l.cellsChanged(event);
    }
  }

  private void fireStructureChanged(VariableListEvent cause) {
    TruthTableEvent event = new TruthTableEvent(this, cause);
    for (TruthTableListener l : listeners) {
      l.structureChanged(event);
    }
  }

  public String getInputHeader(int col) {
    return model.getInputs().bits.get(col);
  }

  public String getOutputHeader(int col) {
    return model.getOutputs().bits.get(col);
  }

  public int getInputIndex(String input) {
    return model.getInputs().bits.indexOf(input);
  }

  public int getOutputIndex(String output) {
    return model.getOutputs().bits.indexOf(output);
  }

  public List<Var> getInputVariables() {
    return model.getInputs().vars;
  }

  public List<Var> getOutputVariables() {
    return model.getOutputs().vars;
  }

  public Var getInputVariable(int index) {
    return model.getInputs().vars.get(index);
  }

  public Var getOutputVariable(int index) {
    return model.getOutputs().vars.get(index);
  }

  public int getInputColumnCount() {
    return model.getInputs().bits.size();
  }

  public int getOutputColumnCount() {
    return model.getOutputs().bits.size();
  }

  public int getRowCount() {
    return 1 << model.getInputs().bits.size();
  }

  public int getVisibleRowCount() {
    return rows.size();
  }
}
