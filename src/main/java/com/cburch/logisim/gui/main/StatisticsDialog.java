/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.FileStatistics;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.TableSorter;

public class StatisticsDialog extends JDialog implements ActionListener {
	private static class CompareString implements Comparator<String> {
		private String[] fixedAtBottom;

		public CompareString(String... fixedAtBottom) {
			this.fixedAtBottom = fixedAtBottom;
		}

		public int compare(String a, String b) {
			for (int i = fixedAtBottom.length - 1; i >= 0; i--) {
				String s = fixedAtBottom[i];
				if (a.equals(s))
					return b.equals(s) ? 0 : 1;
				if (b.equals(s))
					return -1;
			}
			return a.compareToIgnoreCase(b);
		}
	}

	private static class StatisticsTable extends JTable {
		private static final long serialVersionUID = 1L;

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds(x, y, width, height);
			setPreferredColumnWidths(new double[] { 0.45, 0.25, 0.1, 0.1, 0.1 });
		}

		protected void setPreferredColumnWidths(double[] percentages) {
			Dimension tableDim = getPreferredSize();

			double total = 0;
			for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
				total += percentages[i];
			}

			for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
				TableColumn column = getColumnModel().getColumn(i);
				double width = tableDim.width * (percentages[i] / total);
				column.setPreferredWidth((int) width);
			}
		}
	}

	private static class StatisticsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private FileStatistics stats;

		StatisticsTableModel(FileStatistics stats) {
			this.stats = stats;
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return column < 2 ? String.class : Integer.class;
		}

		public int getColumnCount() {
			return 5;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return Strings.get("statsComponentColumn");
			case 1:
				return Strings.get("statsLibraryColumn");
			case 2:
				return Strings.get("statsSimpleCountColumn");
			case 3:
				return Strings.get("statsUniqueCountColumn");
			case 4:
				return Strings.get("statsRecursiveCountColumn");
			default:
				return "??"; // should never happen
			}
		}

		public int getRowCount() {
			return stats.getCounts().size() + 2;
		}

		public Object getValueAt(int row, int column) {
			List<FileStatistics.Count> counts = stats.getCounts();
			int countsLen = counts.size();
			if (row < 0 || row >= countsLen + 2)
				return "";
			FileStatistics.Count count;
			if (row < countsLen)
				count = counts.get(row);
			else if (row == countsLen)
				count = stats.getTotalWithoutSubcircuits();
			else
				count = stats.getTotalWithSubcircuits();
			switch (column) {
			case 0:
				if (row < countsLen) {
					return count.getFactory().getDisplayName();
				} else if (row == countsLen) {
					return Strings.get("statsTotalWithout");
				} else {
					return Strings.get("statsTotalWith");
				}
			case 1:
				if (row < countsLen) {
					Library lib = count.getLibrary();
					return lib == null ? "-" : lib.getDisplayName();
				} else {
					return "";
				}
			case 2:
				return Integer.valueOf(count.getSimpleCount());
			case 3:
				return Integer.valueOf(count.getUniqueCount());
			case 4:
				return Integer.valueOf(count.getRecursiveCount());
			default:
				return ""; // should never happen
			}
		}
	}

	public static void show(JFrame parent, LogisimFile file, Circuit circuit) {
		FileStatistics stats = FileStatistics.compute(file, circuit);
		StatisticsDialog dlog = new StatisticsDialog(parent, circuit.getName(),
				new StatisticsTableModel(stats));
		dlog.setVisible(true);
	}

	private static final long serialVersionUID = 1L;

	private StatisticsDialog(JFrame parent, String circuitName,
			StatisticsTableModel model) {
		super(parent, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Strings.get("statsDialogTitle", circuitName));

		JTable table = new StatisticsTable();
		TableSorter mySorter = new TableSorter(model, table.getTableHeader());
		Comparator<String> comp = new CompareString("",
				Strings.get("statsTotalWithout"), Strings.get("statsTotalWith"));
		mySorter.setColumnComparator(String.class, comp);
		table.setModel(mySorter);
		JScrollPane tablePane = new JScrollPane(table);

		JButton button = new JButton(Strings.get("statsCloseButton"));
		button.addActionListener(this);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(button);

		Container contents = this.getContentPane();
		contents.setLayout(new BorderLayout());
		contents.add(tablePane, BorderLayout.CENTER);
		contents.add(buttonPanel, BorderLayout.PAGE_END);
		this.pack();

		Dimension pref = contents.getPreferredSize();
		if (pref.width > 750 || pref.height > 550) {
			if (pref.width > 750)
				pref.width = 750;
			if (pref.height > 550)
				pref.height = 550;
			this.setSize(pref);
		}
	}

	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
}
