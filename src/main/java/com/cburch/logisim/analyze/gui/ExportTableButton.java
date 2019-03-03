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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.StringGetter;

public class ExportTableButton extends JButton {

	private static final long serialVersionUID = 1L;

	private JFrame parent;
	private AnalyzerModel model;

	ExportTableButton(JFrame parent, AnalyzerModel model) {
		this.parent = parent;
		this.model = model;
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doSave();
			}
		});
	}

	void localeChanged() {
		setText(S.get("exportTableButton"));
	}

	private static void center(PrintStream out, String s, int n) {
		int pad = n - s.length();
		if (pad <= 0) {
			out.printf("%s", s);
		} else {
			String left = (pad/2 > 0) ? "%" + (pad/2) + "s" : "%s";
			String right = (pad-pad/2) > 0 ? "%" + (pad-pad/2) + "s" : "%s";
			out.printf(left + "%s" + right, "", s, "");
		}
	}

	void doSave(File file) throws IOException {
		PrintStream out = new PrintStream(file);
		try {
			out.println(S.get("tableRemark1"));
			Circuit c = model.getCurrentCircuit();
			if (c != null)
				out.println(S.fmt("tableRemark2", c.getName()));
			out.println(S.fmt("tableRemark3", new Date()));
			out.println();
			out.println(S.get("tableRemark4"));
			out.println();
			VariableList inputs = model.getInputs();
			VariableList outputs = model.getOutputs();
			int colwidth[] = new int[inputs.vars.size() + outputs.vars.size()];
			int i;
			i = 0;
			for(Var var : inputs.vars)
				colwidth[i++] = Math.max(var.toString().length(), var.width);
			for(Var var : outputs.vars)
				colwidth[i++] = Math.max(var.toString().length(), var.width);
			i = 0;
			for(Var var : inputs.vars) {
				center(out, var.toString(), colwidth[i++]);
				out.print(" ");
			}
			out.print("|");
			for(Var var : outputs.vars) {
				out.print(" ");
				center(out, var.toString(), colwidth[i++]);
			}
			out.println();
			for(i = 0; i < colwidth.length; i++) {
				for (int j = 0; j < colwidth[i]+1; j++)
					out.print("~");
			}
			out.println("~");
			TruthTable table = model.getTruthTable();
			int rows = table.getVisibleRowCount();
			for (int row = 0; row < rows; row++) {
				i = 0;
				int col;
				col = 0;
				for (Var var : inputs.vars) {
					String s = "";
					for (int b = var.width - 1; b >= 0; b--) {
						Entry val = table.getVisibleInputEntry(row, col++);
						s += val.toBitString();
					}
					center(out, s, colwidth[i++]);
					out.print(" ");
				}
				out.print("|");
				col = 0;
				for (Var var : outputs.vars) {
					String s = "";
					for (int b = var.width - 1; b >= 0; b--) {
						Entry val = table.getVisibleOutputEntry(row, col++);
						s += val.toBitString();
					}
					out.print(" ");
					center(out, s, colwidth[i++]);
				}
				out.println();
			}
		} finally {
			out.close();
		}
	}

	private File lastFile = null;
	void doSave() {
		if (lastFile == null) {
			Circuit c = model.getCurrentCircuit();
			if (c != null)
				lastFile = new File(c.getName() + ".txt");
			else
				lastFile = new File("truthtable.txt");
		}
		JFileChooser chooser = JFileChoosers.createSelected(lastFile);
		chooser.setDialogTitle(S.get("saveButton"));
		chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
		chooser.addChoosableFileFilter(FILE_FILTER);
		chooser.setFileFilter(FILE_FILTER);
		int choice = chooser.showSaveDialog(parent);
		if (choice == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			if (file.isDirectory()) {
				JOptionPane.showMessageDialog(parent,
						S.fmt("notFileMessage", file.getName()),
						S.get("saveErrorTitle"), JOptionPane.OK_OPTION);
				return;
			}
			if (file.exists() && !file.canWrite()) {
				JOptionPane.showMessageDialog(parent,
						S.fmt("cantWriteMessage", file.getName()),
						S.get("saveErrorTitle"), JOptionPane.OK_OPTION);
				return;
			}
			if (file.exists()) {
				int confirm = JOptionPane.showConfirmDialog(parent,
						S.fmt("confirmOverwriteMessage", file.getName()),
						S.get("confirmOverwriteTitle"),
						JOptionPane.YES_NO_OPTION);
				if (confirm != JOptionPane.YES_OPTION)
					return;
			}
			try {
				doSave(file);
				lastFile = file;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(parent,
						e.getMessage(),
						S.get("saveErrorTitle"),
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public static final FileFilter FILE_FILTER = new TableFilter(S.getter("tableFileFilter"),".txt");
	public static class TableFilter extends FileFilter {
		StringGetter description;
		String extention;
		
		public TableFilter(StringGetter descr, String ext) {
			description = descr;
			extention = ext;
		}
		
		public boolean accept(File f) {
			if (!f.isFile())
				return true;
			String name = f.getName();
			int i = name.lastIndexOf('.');
			return (i > 0 && name.substring(i).toLowerCase().equals(extention));
		}

		public String getDescription() {
			return description.toString();
		}
	}}
