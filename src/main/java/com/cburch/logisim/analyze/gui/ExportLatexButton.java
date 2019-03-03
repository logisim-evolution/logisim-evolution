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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.cburch.logisim.analyze.data.ExpressionLatex;
import com.cburch.logisim.analyze.gui.ExportTableButton.TableFilter;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.util.JFileChoosers;

public class ExportLatexButton extends JButton  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String SECTION_SEP = "%===============================================================================";
	private static String SUB_SECTION_SEP = "%-------------------------------------------------------------------------------";
	
	private JFrame parent;
	private AnalyzerModel model;
	public static int MAX_TRUTH_TABLE_ROWS = 64; 
	
	public static final FileFilter FILE_FILTER = new TableFilter(S.getter("tableLatexFilter"),".tex");

	ExportLatexButton(JFrame parent, AnalyzerModel model) {
		this.parent = parent;
		this.model = model;
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doSave();
			}
		});
	}

	void localeChanged() {
		setText(S.get("exportLatexButton"));
	}
	
	private File lastFile = null;
	private void doSave() {
		/* code taken from Kevin Walsh'e ExportTableButton and slightly modified*/
		if (lastFile == null) {
			Circuit c = model.getCurrentCircuit();
			if (c != null)
				lastFile = new File(c.getName() + ".tex");
			else
				lastFile = new File("logisim_evolution_analyze.tex");
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
	
	private int NrOfInCols() {
		int count = 0;
		VariableList inputs = model.getInputs();
		for (int i = 0 ; i < inputs.vars.size(); i++) {
			count += inputs.vars.get(i).width;
		}
		return count;
	}
	
	private int NrOfOutCols() {
		int count = 0;
		VariableList outputs = model.getOutputs();
		for (int i = 0 ; i < outputs.vars.size(); i++) {
			count += outputs.vars.get(i).width;
		}
		return count;
	}
	
	private String TruthTableHeader() {
		StringBuffer out = new StringBuffer();
		out.append("\\begin{center}\n");
		out.append("\\begin{tabular}{");
		int NrInCols = NrOfInCols();
		int NrOutCols = NrOfOutCols();
		for (int i = 0 ; i < NrInCols ; i++)
			out.append("c");
		out.append("|");
		for (int i = 0 ; i < NrOutCols ; i++)
			out.append("c");
		out.append("}\n");
		/* Make the header text */
		List<Var> inputVars = model.getInputs().vars;
		List<Var> outputVars = model.getOutputs().vars;
		for (int i = 0 ; i < inputVars.size() ; i++) {
			Var inp = inputVars.get(i); 
			if (inp.width == 1) {
				out.append("$\\textbf{"+inp.name+"}$&");
			} else {
				String format = i ==inputVars.size() - 1 ? "c|" : "c";
				out.append("\\multicolumn{"+inp.width+"}{"+format+"}{$\\textbf{"+inp.name+"["+(inp.width-1)+"..0]}$}&");
			}
		}
		for (int i = 0 ; i < outputVars.size(); i++) {
			Var outp = outputVars.get(i);
			if (outp.width == 1) {
				out.append("$\\textbf{"+outp.name+"}$");
			} else {
				out.append("\\multicolumn{"+outp.width+"}{c}{$\\textbf{"+outp.name+"["+(outp.width-1)+"..0]}$}");
			}
			out.append(i< outputVars.size()-1 ? "&" : "\\\\");
		}
		out.append("\n\\hline");
		return out.toString();
	}
	
	private String getCompactTruthTable(TruthTable tt) {
		StringBuffer content = new StringBuffer();
		for (int row = 0 ; row < tt.getVisibleRowCount() ; row++) {
			for (int col = 0 ; col < NrOfInCols() ; col++) {
				Entry val = tt.getVisibleInputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$&");
			}
			for (int col = 0 ; col < NrOfOutCols() ; col++) {
				Entry val = tt.getVisibleOutputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$");
				content.append(col == NrOfOutCols()-1 ? "\\\\\n" : "&");
			}
		}
		return content.toString();
	}
	
	private String getCompleteTruthTable(TruthTable tt) {
		StringBuffer content = new StringBuffer();
		for (int row = 0 ; row < tt.getRowCount() ; row++) {
			for (int col = 0 ; col < NrOfInCols() ; col++) {
				Entry val = tt.getInputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$&");
			}
			for (int col = 0 ; col < NrOfOutCols() ; col++) {
				Entry val = tt.getOutputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$");
				content.append(col == NrOfOutCols()-1 ? "\\\\\n" : "&");
			}
		}
		return content.toString();
	}
	
	private static final String K_INTRO = "\\begin{tikzpicture}[karnaugh,";
	private static final String K_INDEX = "enable indices,";
	private static final String K_SETUP = "x=1\\kmunitlength,y=1\\kmunitlength,kmbar left sep=1\\kmunitlength,grp/.style n args={4}{#1,fill=#1!30,minimum width= #2\\kmunitlength,minimum height=#3\\kmunitlength,rounded corners=0.2\\kmunitlength,fill opacity=0.6,rectangle,draw}]";
	
	private String getKarnaughInputs() {
		StringBuffer content = new StringBuffer();
		for (int i = 0 ; i < model.getInputs().vars.size() ; i++) {
			Var inp = model.getInputs().vars.get(i);
			if (inp.width==1) {
				content.append("{$\\text{"+inp.name+"}$}");
			} else {
				for (int j = inp.width-1 ; j >= 0 ; j--) {
					content.append("{$\\text{"+inp.name+"}_"+j+"$}");
				}
			}
		}
		return content.toString();
	}
	
	private String getKarnaughEmpty(String name, boolean index) {
		StringBuffer content = new StringBuffer();
		content.append("\\begin{center}\n");
		content.append(K_INTRO+(index ? K_INDEX: "")+K_SETUP+"\n");
		content.append("\\karnaughmap{"+NrOfInCols()+"}{"+name+"}{"+getKarnaughInputs()+"}{}{}\n");
		content.append("\\end{tikzpicture}\n");
		content.append("\\end{center}");
		return content.toString();
	}
	
	private String getKValues(int outcol) {
		StringBuffer content = new StringBuffer();
		for (int i = 0 ; i < model.getTruthTable().getRowCount() ; i++) {
			content.append(model.getTruthTable().getOutputEntry(i, outcol).getDescription());
		}
		return content.toString();
	}
	
	private String getKarnaugh(String name, int outcol) {
		StringBuffer content = new StringBuffer();
		content.append("\\begin{center}\n");
		content.append(K_INTRO+K_SETUP+"\n");
		content.append("\\karnaughmap{"+NrOfInCols()+"}{"+name+"}{"+getKarnaughInputs()+
				"}\n{"+getKValues(outcol)+"}{}\n");
		content.append("\\end{tikzpicture}\n");
		content.append("\\end{center}");
		return content.toString();
	}
	
	private void doSave(File file) throws IOException {
		PrintStream out = new PrintStream(file);
		try {
			/*
			 * We start to create the document header section with all required packages
			 */
			out.println("\\documentclass [15pt,a4paper,twoside]{article}");
			out.println("\\usepackage["+S.get("latexBabelLanguage")+",shorthands=off]{babel}        % shorhands=off is required for babel french in combination with tikz karnaugh....");
			out.println("\\usepackage[utf8]{inputenc}");
			out.println("\\usepackage[T1]{fontenc}");
			out.println("\\usepackage{amsmath}");
			out.println("\\usepackage{geometry}");
			out.println("\\geometry{verbose,a4paper, tmargin=3.5cm,bmargin=3.5cm,lmargin=2.5cm,rmargin=2.5cm,headsep=1cm,footskip=1.5cm}");
			out.println("\\usepackage{fancyhdr}");
			out.println("\\usepackage{colortbl}");
			out.println("\\usepackage[dvipsnames]{xcolor}");
			out.println("\\usepackage{tikz -timing}");
			out.println("\\usepackage{tikz}");
			out.println("\\usetikzlibrary{karnaugh}");
			out.println("\\pagestyle{fancy}");
			out.println();
			/*
			 * Here we create the headers and footers
			 */
			out.println("\\fancyhead{}");
			out.println("\\fancyhead[C] {"+S.fmt("latexHeader", new Date())+"}");
			out.println("\\fancyfoot[C] {\\thepage}");
			out.println("\\renewcommand{\\headrulewidth}{0.4pt}");
			out.println("\\renewcommand{\\footrulewidth}{0.4pt}");
			out.println();
			out.println("\\makeatother");
			out.println();
			/*
			 * Here the contents starts
			 */
			out.println("\\begin{document}");
			/*
			 * The introduction
			 */
			out.println("\\section{"+S.get("latexIntroduction")+"}");
			out.println(S.get("latexIntroductionText"));
			if (model.getInputs().vars.size() == 0 || model.getOutputs().vars.size() == 0) {
				out.println(SECTION_SEP);
				out.println("\\section{"+S.get("latexEmpty")+"}");
				out.println(S.get("latexEmptyText"));
			} else {
				/*
				 * Here we put the section on the truth table
				 */
				out.println(SECTION_SEP);
				out.println("\\section{"+S.get("latexTruthTable")+"}");
				out.println(S.get("latexTruthTableText"));
				final TruthTable tt = model.getTruthTable();
				if (tt.getRowCount() > MAX_TRUTH_TABLE_ROWS) {
					out.println(S.fmt("latexTruthTableToBig",MAX_TRUTH_TABLE_ROWS));
				} else {
					tt.compactVisibleRows();
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexTruthTableCompact")+"}");
					out.println(TruthTableHeader());
					out.println(getCompactTruthTable(tt));
					out.println("\\end{tabular}");
					out.println("\\end{center}");
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexTruthTableComlete")+"}");
					out.println(TruthTableHeader());
					out.println(getCompleteTruthTable(tt));
					out.println("\\end{tabular}");
					out.println("\\end{center}");
				}
				/*
				 * We continue with the Karnaugh diagrams
				 */
				out.println(SECTION_SEP);
				out.println("\\section{"+S.get("latexKarnaugh")+"}");
				if (tt.getRowCount() > MAX_TRUTH_TABLE_ROWS) {
					out.println(S.fmt("latexKarnaughToBig",(int)Math.ceil(Math.log(MAX_TRUTH_TABLE_ROWS)/Math.log(2))));
				} else {
					out.print(S.get("latexKarnaughText"));
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexKarnaughEmptyIndexed")+"}");
					for (int i = 0 ; i < model.getOutputs().vars.size() ; i++) {
						Var outp = model.getOutputs().vars.get(i);
						if (outp.width == 1) {
							String func = "$\\textbf{"+outp.name+"}$";
							out.println(getKarnaughEmpty(func,true));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaughEmpty(func,true));
							}
						}
					}
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexKarnaughEmpty")+"}");
					for (int i = 0 ; i < model.getOutputs().vars.size() ; i++) {
						Var outp = model.getOutputs().vars.get(i);
						if (outp.width == 1) {
							String func = "$\\textbf{"+outp.name+"}$";
							out.println(getKarnaughEmpty(func,false));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaughEmpty(func,false));
							}
						}
					}
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexKarnaughFilledIn")+"}");
					int outcol = 0;
					for (int i = 0 ; i < model.getOutputs().vars.size() ; i++) {
						Var outp = model.getOutputs().vars.get(i);
						if (outp.width == 1) {
							String func = "$\\textbf{"+outp.name+"}$";
							out.println(getKarnaugh(func,outcol++));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaugh(func,outcol++));
							}
						}
					}
					/*
					 * TODO: generated the karnaux maps with the colored groups
					 */
					/*
					 * Finally we print the minimal expressions 
					 */
					out.println(SECTION_SEP);
					out.println("\\section{"+S.get("latexMinimal")+"}");
					for (int o = 0 ; o < model.getTruthTable().getOutputVariables().size() ; o++) {
						Var outp = model.getTruthTable().getOutputVariable(o);
						if (outp.width == 1) {
							Expression exp = model.getOutputExpressions().getMinimalExpression(outp.name);
							out.println(new ExpressionLatex(exp,outp,1).get()+"\\\\");
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								Expression exp = model.getOutputExpressions().getMinimalExpression(outp.name+":"+idx);
								out.println(new ExpressionLatex(exp,outp,idx).get()+"\\\\");
							}
						}
					}
				}
			}
			/*
			 * That was all folks
			 */
			out.println("\\end{document}");
		} finally {
			out.close();
		}
	}



}
