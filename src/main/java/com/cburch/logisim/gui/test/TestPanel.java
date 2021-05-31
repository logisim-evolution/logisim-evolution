/*
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

package com.cburch.logisim.gui.test;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.FailException;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.ValueTable;
import com.cburch.logisim.util.StringUtil;
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

  public void changeColumnValueRadix(int i) {
    if (i == 0) return;
    TestVector vec = getModel().getVector();
    switch (vec.columnRadix[i - 1]) {
      case 2:
        vec.columnRadix[i - 1] = 10;
        break;
      case 10:
        vec.columnRadix[i - 1] = 16;
        break;
      default:
        vec.columnRadix[i - 1] = 2;
        break;
    }
    table.modelChanged();
  }

  public int getColumnCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.columnName.length + 1;
  }

  public String getColumnName(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? S.get("statusHeader") : vec.columnName[i - 1];
  }

  public int getColumnValueRadix(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? 0 : vec.columnRadix[i - 1];
  }

  // ValueTable.Model implementation

  public BitWidth getColumnValueWidth(int i) {
    TestVector vec = getModel().getVector();
    return i == 0 ? null : vec.columnWidth[i - 1];
  }

  Model getModel() {
    return testFrame.getModel();
  }

  public int getRowCount() {
    TestVector vec = getModel().getVector();
    return vec == null ? 0 : vec.data.size();
  }

  public void getRowData(int firstRow, int numRows, ValueTable.Cell[][] rowData) {
    Model model = getModel();
    TestException[] results = model.getResults();
    int numPass = model.getPass();
    int numFail = model.getFail();
    TestVector vec = model.getVector();
    int columns = vec.columnName.length;
    String[] msg = new String[columns];
    Value[] altdata = new Value[columns];
    String passMsg = S.get("passStatus");
    String failMsg = S.get("failStatus");

    for (int i = firstRow; i < firstRow + numRows; i++) {
      int row = model.sortedIndex(i);
      Value[] data = vec.data.get(row);
      String rowmsg = null;
      String status = null;
      boolean failed = false;
      if (row < numPass + numFail) {
        TestException err = results[row];
        if (err instanceof FailException) {
          failed = true;
          for (FailException e : ((FailException) err).getAll()) {
            int col = e.getColumn();
            msg[col] =
                StringUtil.format(
                    S.get("expectedValueMessage"),
                    e.getExpected().toDisplayString(getColumnValueRadix(col + 1)));
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

      for (int col = 0; col < columns; col++) {
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

    public void testingChanged() {}

    public void testResultsChanged(int numPass, int numFail) {
      table.dataChanged();
    }

    public void vectorChanged() {
      table.modelChanged();
    }
  }
}
