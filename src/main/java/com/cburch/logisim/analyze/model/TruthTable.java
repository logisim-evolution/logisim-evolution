/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TruthTable {

  private static final Entry DEFAULT_ENTRY = Entry.DONT_CARE;

  private final MyListener myListener = new MyListener();
  private final List<TruthTableListener> listeners = new ArrayList<>();

  private final AnalyzerModel model;
  private ArrayList<Row> rows = new ArrayList<>(); // visible input rows
  private final ArrayList<Entry[]> columns = new ArrayList<>(); // output columns
  private static final CompareInputs sortByInputs = new CompareInputs();

  private static class Row implements Iterable<Integer> {
    // todo: probably more efficient to store this in baseIdx/dcMask format.
    final Entry[] inputs;

    Row(int idx, int numInputs, int mask) {
      inputs = new Entry[numInputs];
      for (var i = numInputs - 1; i >= 0; i--) {
        inputs[i] = (mask & 1) == 0 ? (idx & 1) == 0 ? Entry.ZERO : Entry.ONE : Entry.DONT_CARE;
        idx = idx >> 1;
        mask = mask >> 1;
      }
    }

    Row(Entry[] entries, int numInputs) {
      inputs = new Entry[numInputs];
      System.arraycopy(entries, 0, inputs, 0, numInputs);
    }

    public int baseIndex() {
      var idx = 0;
      for (final var input : inputs) idx = (idx << 1) | (input == Entry.ONE ? 1 : 0);
      return idx;
    }

    public int dcMask() {
      var mask = 0;
      for (final var input : inputs) mask = (mask << 1) | (input == Entry.DONT_CARE ? 1 : 0);
      return mask;
    }

    public int duplicity() {
      int count = 1;
      for (Entry input : inputs)
        count *= (input == Entry.DONT_CARE ? 2 : 1);
      return count;
    }

    @Override
    public String toString() {
      final var s = new StringBuilder("row[");
      for (var i = 0; i < inputs.length; i++) {
        if (i != 0) s.append(" ");
        s.append(inputs[i].getDescription());
      }
      s.append("]");
      s.append(" dup=").append(duplicity());
      s.append(String.format(" base=%x dcmask=%x", baseIndex(), dcMask()));
      return s.toString();
    }

    public String toBitString(List<Var> vars) {
      final var s = new StringBuilder();
      var i = 0;
      for (final var variable : vars) {
        s.append(" ");
        for (var j = 0; j < variable.width; j++) s.append(inputs[i++].toBitString());
      }
      return s.toString();
    }

    public boolean contains(int idx) {
      return (idx & ~dcMask()) == baseIndex();
    }

    public boolean contains(Row other) {
      return contains(other.baseIndex()) && (other.dcMask() & ~dcMask()) == 0;
    }

    public boolean intersects(Row other) {
      final var dc = dcMask() | other.dcMask();
      return (other.baseIndex() & ~dc) == (baseIndex() & ~dc);
    }

    @Override
    public Iterator<Integer> iterator() {
      return new Iterator<>() {
        final int base = baseIndex();
        final int mask = dcMask();
        final int nbits = inputs.length;
        final int count = duplicity();
        int iter = 0;

        @Override
        public boolean hasNext() {
          return iter < count;
        }

        @Override
        public Integer next() {
          var add = iter;
          var keep = 0;
          for (var b = 0; b < nbits; b++) {
            if ((mask & (1 << b)) == 0) {
              add = ((add & ~keep) << 1) | (add & keep);
            }
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
    @Override
    public int compare(Row r1, Row r2) {
      return r1.baseIndex() - r2.baseIndex();
    }
  }

  private void initRows() {
    final var inputs = getInputColumnCount();
    final var n = getRowCount();
    rows.clear();
    rows.ensureCapacity(n);
    for (int i = 0; i < n; i++) rows.add(new Row(i, inputs, 0));
  }

  private void initColumns() {
    final var outputs = getOutputColumnCount();
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
    final var partition = Implicant.computePartition(model);
    rows.clear();
    initColumns();
    final var ni = getInputColumnCount();
    final var no = getOutputColumnCount();
    for (final var it : partition.entrySet()) {
      final var imp = it.getKey();
      final var val = it.getValue();
      final var r = new Row(imp.values, ni, imp.unknowns);
      rows.add(r);
      for (var col = 0; col < no; col++) {
        final var value = Entry.parse("" + val.charAt(col));
        var column = columns.get(col);
        if (column == null && value == DEFAULT_ENTRY) continue;
        else if (column == null) column = getOutputColumn(col);
        for (Integer idx : r) {
          column[idx] = value;
        }
      }
    }
    fireRowsChanged();
    for (var col = 0; col < no; col++) {
      if (columns.get(col) != null) fireCellsChanged(col);
    }
  }

  public void setOutputColumn(int col, Entry[] values) {
    if (values.length != getRowCount()) throw new IllegalArgumentException("bad column length");
    final var oldValues = columns.set(col, values);
    if (oldValues == values) return;
    // Expand rows as dictated by column inconsistencies
    var rowsChanged = false;
    for (var i = rows.size() - 1; i >= 0; i--) {
      final var r = rows.get(i);
      final var base = r.baseIndex();
      final var v = values[base];
      var split = true;
      while (split) {
        split = false;
        for (final var idx : r) {
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
    final var base = r.baseIndex();
    if (idx == base || !r.contains(idx)) throw new IllegalArgumentException("bad row split");
    final var diff = idx ^ base;
    final var n = r.duplicity();
    if (n <= 1) throw new IllegalStateException("row duplicity should be at least 2");
    final var splits = new Row(base, r.inputs.length, diff);
    var m = 0;
    rows.remove(r);
    for (final var other : splits) {
      final var s = new Row(other, r.inputs.length, r.dcMask() & ~diff);
      m += s.duplicity();
      int pos = Collections.binarySearch(rows, s, sortByInputs);
      if (pos < 0) rows.add(-pos - 1, s);
      else throw new IllegalStateException("unexpected row split");
    }
    if (m != n) throw new IllegalStateException("assertion failed in row split");
  }

  public Entry getVisibleOutputEntry(int row, int col) {
    final var r = rows.get(row);
    final var idx = r.baseIndex();
    return getOutputEntry(idx, col);
  }

  public Entry getOutputEntry(int idx, int col) {
    if (idx < 0 || col < 0) return DEFAULT_ENTRY;
    final var column = columns.get(col);
    return (column == null ? DEFAULT_ENTRY : idx < column.length ? column[idx] : DEFAULT_ENTRY);
  }

  public String getVisibleOutputs(int row) {
    final var r = rows.get(row);
    final var idx = r.baseIndex();
    final var s = new StringBuilder();
    for (Entry[] column : columns) {
      s.append((column == null ? DEFAULT_ENTRY : column[idx]).getDescription());
    }
    return s.toString();
  }

  public Entry getVisibleInputEntry(int row, int col) {
    final var r = rows.get(row);
    return r.inputs[col];
  }

  public int getVisibleRowDcMask(int row) {
    final var r = rows.get(row);
    return r.dcMask();
  }

  public int getVisibleRowIndex(int row) {
    final var r = rows.get(row);
    return r.baseIndex();
  }

  public Iterable<Integer> getVisibleRowIndexes(int row) {
    return rows.get(row);
  }

  public Entry getInputEntry(int idx, int col) {
    if (idx < 0 || idx >= getRowCount()) throw new IndexOutOfBoundsException("bad row index");
    final var inputs = getInputColumnCount();
    if (col < 0 || col >= inputs) throw new IndexOutOfBoundsException("bad input column index");
    return isInputSet(idx, col, inputs) ? Entry.ONE : Entry.ZERO;
  }

  public static boolean isInputSet(int idx, int col, int inputs) {
    return (idx & (1 << (inputs - col - 1))) != 0;
  }

  public Entry[] getOutputColumn(int col) {
    var column = columns.get(col);
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
    for (final var column : columns) {
      if (column == null) continue;
      if (column[idx1] != column[idx2]) return false;
    }
    return true;
  }

  private void mergeOutputs(int idx1, int idx2, boolean[] changed) {
    if (idx1 == idx2) return;
    for (var col = 0; col < columns.size(); col++) {
      final var column = columns.get(col);
      if (column == null) continue;
      if (column[idx1] != column[idx2]) {
        column[idx2] = column[idx1];
        changed[col] = true;
      }
    }
  }

  private boolean setDontCare(Row r, int dc, boolean force, boolean[] changed) {
    final var newRow = new Row(r.baseIndex(), r.inputs.length, r.dcMask() | dc);
    final var base = newRow.baseIndex();
    if (!force) {
      for (final var idx : newRow) {
        if (!identicalOutputs(base, idx)) return false;
      }
    }
    for (var i = 0; i < rows.size(); i++) {
      final var row = rows.get(i);
      if (!newRow.intersects(row)) continue;
      if (newRow.contains(row)) {
        for (final var idx : row) mergeOutputs(base, idx, changed);
        rows.remove(i);
      } else {
        // find a bit we can flip in s so it doesn't conflict
        int pos;
        for (pos = row.inputs.length - 1; pos >= 0; pos--) {
          if (row.inputs[pos] == Entry.DONT_CARE && newRow.inputs[pos] != Entry.DONT_CARE) break;
        }
        if (pos < 0) throw new IllegalStateException("failed row merge");
        int bit = (1 << (row.inputs.length - 1 - pos));
        splitRow(row, row.baseIndex() ^ bit);
      }
      i--; // back up, may need a second split
    }
    final var pos = Collections.binarySearch(rows, newRow, sortByInputs);
    if (pos < 0) rows.add(-pos - 1, newRow);
    else throw new IllegalStateException("failed row merge");
    return true;
  }

  public boolean setVisibleInputEntry(int row, int col, Entry value, boolean force) {
    final var r = rows.get(row);
    if (r.inputs[col] == value) return false;
    final var dc = (1 << (r.inputs.length - 1 - col));
    if (value == Entry.DONT_CARE) {
      final var changed = new boolean[columns.size()];
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
    final var r = rows.get(row);
    var column = columns.get(col);
    if (column == null && value == DEFAULT_ENTRY) return;
    else if (column == null) column = getOutputColumn(col);
    var changed = false;
    for (Integer idx : r) {
      if (column[idx] == value) continue;
      changed = true;
      column[idx] = value;
    }
    if (changed) fireCellsChanged(col);
  }

  Row findRow(int idx) {
    for (var i = rows.size() - 1; i >= 0; i--) {
      final var r = rows.get(i);
      if (r.contains(idx)) return r;
    }
    throw new IllegalStateException("missing row");
  }

  public int findVisibleRowContaining(int idx) {
    for (var i = rows.size() - 1; i >= 0; i--) {
      final var r = rows.get(i);
      if (r.contains(idx)) return i;
    }
    throw new IllegalStateException("missing row");
  }

  public void setVisibleRows(List<Entry[]> newEntries, boolean force) {
    final var ni = getInputColumnCount();
    final var no = getOutputColumnCount();
    final var newRows = new ArrayList<Row>(newEntries.size());
    for (final var values : newEntries) {
      if (values.length != ni + no) throw new IllegalArgumentException("wrong column count");
      newRows.add(new Row(values, ni));
    }
    // check that newRows has no intersections
    final var ivars = getInputVariables();
    final var taken = new int[getRowCount()];
    for (int i = 0; i < newRows.size(); i++) {
      final var r = newRows.get(i);
      for (final var idx : r) {
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
    for (var i = 0; i < getRowCount(); i++) {
      if (taken[i] == 0 && !force) {
        throw new IllegalArgumentException(
            String.format("Some inputs are missing." + " For example, there is no row for input %s.",
                new Row(i, ni, 0).toBitString(ivars)));
      } else if (taken[i] == 0) {
        newRows.add(new Row(i, ni, 0));
      }
    }

    newRows.sort(sortByInputs);
    rows.clear();
    rows = newRows;
    initColumns();

    for (Entry[] values : newEntries) {
      final var r = new Row(values, ni);
      for (var col = 0; col < no; col++) {
        final var value = values[ni + col];
        var column = columns.get(col);
        if (column == null && value == DEFAULT_ENTRY) continue;
        else if (column == null) column = getOutputColumn(col);
        for (final var idx : r) column[idx] = value;
      }
    }
    fireRowsChanged();
    for (int col = 0; col < no; col++) {
      if (columns.get(col) != null) fireCellsChanged(col);
    }
  }

  public void setOutputEntry(int idx, int col, Entry value) {
    var column = columns.get(col);
    if (column == null && value == DEFAULT_ENTRY) return;
    else if (column == null) column = getOutputColumn(col);
    if (column[idx] == value) return;
    column[idx] = value;
    final var r = findRow(idx);
    if (r.duplicity() > 1) {
      splitRow(r, idx);
      fireRowsChanged();
    }
    fireCellsChanged(col);
  }

  private class MyListener implements VariableListListener {

    @Override
    public void listChanged(VariableListEvent event) {
      if (event.getSource() == model.getInputs()) {
        inputsChanged(event);
        for (var col = 0; col < columns.size(); col++) {
          var column = columns.get(col);
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
      final var v = event.getVariable();
      final var action = event.getType();
      if (action == VariableListEvent.ALL_REPLACED) {
        initColumns();
      } else if (action == VariableListEvent.ADD) {
        final var bitIndex = event.getBitIndex();
        for (int b = v.width - 1; b >= 0; b--) columns.add(bitIndex - b, null /* lazily created */);
      } else if (action == VariableListEvent.MOVE) {
        final var delta = event.getBitIndex();
        final var newIndex = getOutputIndex(v.bitName(0));
        if (delta > 0) {
          for (var b = 0; b < v.width; b++) {
            final var column = columns.remove(newIndex - delta - b);
            columns.add(newIndex - b, column);
          }
        } else if (delta < 0) {
          for (var b = v.width - 1; b >= 0; b--) {
            final var column = columns.remove(newIndex - delta - b);
            columns.add(newIndex - b, column);
          }
        }
      } else if (action == VariableListEvent.REMOVE) {
        final var bitIndex = event.getBitIndex();
        for (var b = 0; b < v.width; b++) columns.remove(bitIndex - b);
      } else if (action == VariableListEvent.REPLACE) {
        final var bitIndex = event.getBitIndex();
        final var newVar = getOutputVariable(event.getIndex());
        var lost = v.width - newVar.width;
        final var pos = bitIndex + 1 - v.width;
        if (lost > 0) {
          while (lost-- != 0) columns.remove(pos);
        } else if (lost < 0) {
          while (lost++ != 0) columns.add(pos, null /* lazily created */);
        }
      }
    }

    private void inputsChanged(VariableListEvent event) {
      final var v = event.getVariable();
      final var action = event.getType();
      if (action == VariableListEvent.ADD) {
        final var bitIndex = event.getBitIndex();
        var oldCount = getInputColumnCount() - v.width;
        for (var b = v.width - 1; b >= 0; b--) addInput(bitIndex - b, oldCount++);
      } else if (action == VariableListEvent.REMOVE) {
        final var bitIndex = event.getBitIndex();
        var oldCount = getInputColumnCount() + v.width;
        for (var b = 0; b < v.width; b++) removeInput(bitIndex - b, oldCount--);
      } else if (action == VariableListEvent.MOVE) {
        final var delta = event.getBitIndex();
        final var newIndex = getInputIndex(v.bitName(0));
        if (delta > 0) {
          for (int b = 0; b < v.width; b++) moveInput(newIndex - delta - b, newIndex - b);
        } else if (delta < 0) {
          for (int b = v.width - 1; b >= 0; b--) moveInput(newIndex - delta - b, newIndex - b);
        }
      } else if (action == VariableListEvent.REPLACE) {
        final var bitIndex = event.getBitIndex();
        final var newVar = getInputVariable(event.getIndex());
        var lost = v.width - newVar.width;
        var oldCount = getInputColumnCount() + lost;
        final var pos = bitIndex + 1 - v.width;
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
      final var inputs = getInputColumnCount();
      oldIndex = inputs - 1 - oldIndex;
      newIndex = inputs - 1 - newIndex;
      final var allMask = (1 << inputs) - 1;
      final var sameMask =
          allMask
              ^ ((1 << (1 + Math.max(oldIndex, newIndex))) - 1)
              ^ ((1 << Math.min(oldIndex, newIndex)) - 1); // bits that don't change
      final var moveMask = 1 << oldIndex; // bit that moves
      final var moveDist = Math.abs(newIndex - oldIndex);
      final var moveLeft = newIndex > oldIndex;
      final var blockMask = allMask ^ sameMask ^ moveMask; // bits that move by one
      ArrayList<Row> ret = new ArrayList<>(2 * rows.size());
      for (final var row : rows) {
        final var i = row.baseIndex();
        final var dc = row.dcMask();
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
      ret.sort(sortByInputs);
      rows = ret;
    }

    private void addInput(int index, int oldCount) {
      // add another Entry column to each row.input
      final var ret = new ArrayList<Row>(2 * rows.size());
      for (final var row : rows) {
        final var i = row.baseIndex();
        final var dc = row.dcMask();
        final var b = 1 << (oldCount - index); // _0001000
        var mask = b - 1; // _0000111
        final var idx0 = ((i & ~mask) << 1) | 0 | (i & mask); // xxxx0yyy
        final var dc0 = ((dc & ~mask) << 1) | 0 | (dc & mask); // wwww0zzz
        ret.add(new Row(idx0 | 0, oldCount + 1, dc0)); // xxxx0yyy
        ret.add(new Row(idx0 | b, oldCount + 1, dc0)); // xxxx1yyy
      }
      ret.sort(sortByInputs);
      rows = ret;
    }

    private void removeInput(int index, int oldCount) {
      // force an Entry column of each row.input to 'x', then remove it
      final var b = (1 << (oldCount - 1 - index)); // _0001000
      final var changed = new boolean[columns.size()];
      // loop rows by index to avoid java.util.ConcurrentModificationException
      //noinspection ForLoopReplaceableByForEach
      for (var i = 0; i < rows.size(); ++i) {
        final var r = rows.get(i);
        if (r.inputs[index] == Entry.DONT_CARE) continue;
        setDontCare(r, b, true, changed); // mutates row
      }
      final var mask = b - 1; // _0000111
      final var ret = new ArrayList<Row>(rows.size());
      for (final var r : rows) {
        final var i = r.baseIndex();
        final var dc = r.dcMask();
        final var idx0 = ((i >> 1) & ~mask) | (i & mask); // __xxxyyy
        final var dc0 = ((dc >> 1) & ~mask) | (dc & mask); // wwww0zzz
        ret.add(new Row(idx0, oldCount - 1, dc0));
      }
      ret.sort(sortByInputs);
      rows = ret;
    }

    private Entry[] inputsChangedForOutput(Entry[] column, VariableListEvent event) {
      final var v = event.getVariable();
      final var action = event.getType();
      if (action == VariableListEvent.ADD) {
        var bitIndex = event.getBitIndex();
        var oldCount = getInputColumnCount() - v.width;
        for (int b = v.width - 1; b >= 0; b--)
          column = addInputForOutput(column, bitIndex - b, oldCount++);
      } else if (action == VariableListEvent.REMOVE) {
        final var bitIndex = event.getBitIndex();
        var oldCount = getInputColumnCount() + v.width;
        for (int b = 0; b < v.width; b++)
          column = removeInputForOutput(column, bitIndex - b, oldCount--);
      } else if (action == VariableListEvent.MOVE) {
        final var delta = event.getBitIndex();
        final var newIndex = getInputIndex(v.bitName(0));
        if (delta > 0) {
          for (int b = 0; b < v.width; b++)
            column = moveInputForOutput(column, newIndex - delta - b, newIndex - b);
        } else if (delta < 0) {
          for (var b = v.width - 1; b >= 0; b--)
            column = moveInputForOutput(column, newIndex - delta - b, newIndex - b);
        }
      } else if (action == VariableListEvent.REPLACE) {
        final var bitIndex = event.getBitIndex();
        final var newVar = getInputVariable(event.getIndex());
        var lost = v.width - newVar.width;
        var oldCount = getInputColumnCount() + lost;
        final var pos = bitIndex + 1 - v.width;
        if (lost > 0) {
          while (lost-- != 0) column = removeInputForOutput(column, pos, oldCount--);
        } else if (lost < 0) {
          while (lost++ != 0) column = addInputForOutput(column, pos, oldCount++);
        }
      }
      return column;
    }

    private Entry[] moveInputForOutput(Entry[] old, int oldIndex, int newIndex) {
      final var inputs = getInputColumnCount();
      oldIndex = inputs - 1 - oldIndex;
      newIndex = inputs - 1 - newIndex;
      final var ret = new Entry[old.length];
      final var sameMask =
          (old.length - 1)
              ^ ((1 << (1 + Math.max(oldIndex, newIndex))) - 1)
              ^ ((1 << Math.min(oldIndex, newIndex)) - 1); // bits that don't change
      final var moveMask = 1 << oldIndex; // bit that moves
      final var moveDist = Math.abs(newIndex - oldIndex);
      final var moveLeft = newIndex > oldIndex;
      final var blockMask = (old.length - 1) ^ sameMask ^ moveMask; // bits that move by one
      for (var i = 0; i < old.length; i++) {
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
      var ret = new Entry[old.length / 2];
      var j = 0;
      final var mask = 1 << (oldCount - 1 - index);
      for (var i = 0; i < old.length; i++) {
        if ((i & mask) == 0) {
          Entry e0 = old[i];
          Entry e1 = old[i | mask];
          ret[j++] = (e0 == e1 ? e0 : Entry.DONT_CARE);
        }
      }
      return ret;
    }

    private Entry[] addInputForOutput(Entry[] old, int index, int oldCount) {
      final var ret = new Entry[2 * old.length];
      final var b = 1 << (oldCount - index); // _0001000
      final var mask = b - 1; // _0000111
      for (var i = 0; i < old.length; i++) {
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
    final var event = new TruthTableEvent(this, null);
    for (TruthTableListener l : listeners) {
      l.rowsChanged(event);
    }
  }

  private void fireCellsChanged(int col) {
    final var event = new TruthTableEvent(this, col);
    for (TruthTableListener l : listeners) {
      l.cellsChanged(event);
    }
  }

  private void fireStructureChanged(VariableListEvent cause) {
    final var event = new TruthTableEvent(this, cause);
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
