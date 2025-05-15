/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.test;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.ValueTable;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;

class TestPanel extends JPanel implements ValueTable.Model {

  static final Color failColor = new Color(0xff9999);
  private static final long serialVersionUID = 1L;
  private final TestFrame testFrame;
  private final ValueTable table;
  private final MyListener myListener = new MyListener();

  public TestPanel(TestFrame frame) {
    this.testFrame = frame;
    table = new ValueTable(getModel() == null ? null : this);
    setLayout(new BorderLayout());
    add(table);
    modelChanged(null, getModel());
  }

  @Override
  public void changeColumnValueRadix(int i) {
    if (i == 0) return;
    TestVector vec = getModel().getVector();
    switch (vec.columnRadix[i - 1]) {
      case 2 -> vec.columnRadix[i - 1] = 10;
      case 10 -> vec.columnRadix[i - 1] = 16;
      default -> vec.columnRadix[i - 1] = 2;
    }
    table.modelChanged();
  }

  @Override
  public int getColumnCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.columnName.length + 1;
  }

  @Override
  public String getColumnName(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? S.get("statusHeader") : vec.columnName[i - 1];
  }

  @Override
  public int getColumnValueRadix(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? 0 : vec.columnRadix[i - 1];
  }

  // ValueTable.Model implementation

  @Override
  public BitWidth getColumnValueWidth(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? null : vec.columnWidth[i - 1];
  }

  Model getModel() {
    return testFrame.getModel();
  }

  @Override
  public int getRowCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.data.size();
  }

  @Override
  public void getRowData(int firstRow, int numRows, ValueTable.Cell[][] rowData) {
    Model model = getModel();
    TestException[] results = model.getResults();
    var numPass = model.getPass();
    var numFail = model.getFail();
    final var vec = model.getVector();
    int columns = vec.columnName.length;
    final var msg = new String[columns];
    final var altdata = new Value[columns];
    final var passMsg = S.get("passStatus");
    final var failMsg = S.get("failStatus");

    for (var i = firstRow; i < firstRow + numRows; i++) {
      final var row = model.sortedIndex(i);
      final var data = vec.data.get(row);
      String rowmsg = null;
      String status = null;
      var failed = false;
      if (row < numPass + numFail) {
        final var err = results[row];
        if (err instanceof FailException failEx) {
          failed = true;
          for (final var e : failEx.getAll()) {
            var col = e.getColumn();
            msg[col] = S.get("expectedValueMessage", e.getExpected().toDisplayString(getColumnValueRadix(col + 1)));
            altdata[col] = e.getComputed();
          }
        } else if (err != null) {
          failed = true;
          rowmsg = err.getMessage();
        }
        status = failed ? failMsg : passMsg;
      }

      rowData[i - firstRow][0] =
          new ValueTable.Cell(status, rowmsg != null ? failColor : null, null, rowmsg);

      for (var col = 0; col < columns; col++) {
        rowData[i - firstRow][col + 1] =
            new ValueTable.Cell(
                altdata[col] != null ? altdata[col] : data[col],
                msg[col] != null ? failColor : null,
                null,
                msg[col]);
        msg[col] = null;
        altdata[col] = null;
      }
    }
  }

  public void localeChanged() {
    table.modelChanged();
  }

  public void modelChanged(Model oldModel, Model newModel) {
    if (oldModel != null) oldModel.removeModelListener(myListener);
    if (newModel != null) newModel.addModelListener(myListener);
    table.setModel(newModel == null ? null : this);
  }

  private class MyListener implements ModelListener {

    @Override
    public void testingChanged() {}

    @Override
    public void testResultsChanged(int numPass, int numFail) {
      table.dataChanged();
    }

    @Override
    public void vectorChanged() {
      table.modelChanged();
    }
  }
}
