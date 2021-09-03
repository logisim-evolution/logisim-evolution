/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.FileStatistics;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.TableSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import lombok.val;

public class StatisticsDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;

  private StatisticsDialog(JFrame parent, String circuitName, StatisticsTableModel model) {
    super(parent, true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setTitle(S.get("statsDialogTitle", circuitName));

    val table = new StatisticsTable();
    val mySorter = new TableSorter(model, table.getTableHeader());
    Comparator<String> comp = new CompareString("", S.get("statsTotalWithout"), S.get("statsTotalWith"));
    mySorter.setColumnComparator(String.class, comp);
    table.setModel(mySorter);
    val tablePane = new JScrollPane(table);

    val button = new JButton(S.get("statsCloseButton"));
    button.addActionListener(this);
    val buttonPanel = new JPanel();
    buttonPanel.add(button);

    val contents = this.getContentPane();
    contents.setLayout(new BorderLayout());
    contents.add(tablePane, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.PAGE_END);
    this.pack();

    val pref = contents.getPreferredSize();
    if (pref.width > 750 || pref.height > 550) {
      if (pref.width > 750) pref.width = 750;
      if (pref.height > 550) pref.height = 550;
      this.setSize(pref);
    }
  }

  public static void show(JFrame parent, LogisimFile file, Circuit circuit) {
    val stats = FileStatistics.compute(file, circuit);
    val dlog = new StatisticsDialog(parent, circuit.getName(), new StatisticsTableModel(stats));
    dlog.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.dispose();
  }

  private static class CompareString implements Comparator<String> {
    private final String[] fixedAtBottom;

    public CompareString(String... fixedAtBottom) {
      this.fixedAtBottom = fixedAtBottom;
    }

    @Override
    public int compare(String a, String b) {
      for (var i = fixedAtBottom.length - 1; i >= 0; i--) {
        val s = fixedAtBottom[i];
        if (a.equals(s)) return b.equals(s) ? 0 : 1;
        if (b.equals(s)) return -1;
      }
      return a.compareToIgnoreCase(b);
    }
  }

  private static class StatisticsTable extends JTable {
    private static final long serialVersionUID = 1L;

    @Override
    public void setBounds(int x, int y, int width, int height) {
      super.setBounds(x, y, width, height);
      setPreferredColumnWidths(new double[] {0.45, 0.25, 0.1, 0.1, 0.1});
    }

    protected void setPreferredColumnWidths(double[] percentages) {
      Dimension tableDim = getPreferredSize();

      double total = 0;
      for (var i = 0; i < getColumnModel().getColumnCount(); i++) {
        total += percentages[i];
      }

      for (var i = 0; i < getColumnModel().getColumnCount(); i++) {
        val column = getColumnModel().getColumn(i);
        var width = tableDim.width * (percentages[i] / total);
        column.setPreferredWidth((int) width);
      }
    }
  }

  private static class StatisticsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final FileStatistics stats;

    StatisticsTableModel(FileStatistics stats) {
      this.stats = stats;
    }

    @Override
    public Class<?> getColumnClass(int column) {
      return column < 2 ? String.class : Integer.class;
    }

    @Override
    public int getColumnCount() {
      return 5;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0:
          return S.get("statsComponentColumn");
        case 1:
          return S.get("statsLibraryColumn");
        case 2:
          return S.get("statsSimpleCountColumn");
        case 3:
          return S.get("statsUniqueCountColumn");
        case 4:
          return S.get("statsRecursiveCountColumn");
        default:
          return "??"; // should never happen
      }
    }

    @Override
    public int getRowCount() {
      return stats.getCounts().size() + 2;
    }

    @Override
    public Object getValueAt(int row, int column) {
      val counts = stats.getCounts();
      val countsLen = counts.size();
      if (row < 0 || row >= countsLen + 2) return "";
      FileStatistics.Count count;

      if (row < countsLen) count = counts.get(row);
      else if (row == countsLen) count = stats.getTotalWithoutSubcircuits();
      else count = stats.getTotalWithSubcircuits();

      switch (column) {
        case 0:
          if (row < countsLen) {
            return count.getFactory().getDisplayName();
          } else if (row == countsLen) {
            return S.get("statsTotalWithout");
          } else {
            return S.get("statsTotalWith");
          }
        case 1:
          if (row < countsLen) {
            Library lib = count.getLibrary();
            return lib == null ? "-" : lib.getDisplayName();
          } else {
            return "";
          }
        case 2:
          return count.getSimpleCount();
        case 3:
          return count.getUniqueCount();
        case 4:
          return count.getRecursiveCount();
        default:
          return ""; // should never happen
      }
    }
  }
}
