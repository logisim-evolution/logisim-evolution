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

package com.cburch.logisim.analyze.file;

import static com.cburch.logisim.analyze.Strings.S;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

import javax.swing.filechooser.FileFilter;

import com.cburch.logisim.analyze.data.ExpressionLatex;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;

public class AnalyzerTexWriter {

	private static String SECTION_SEP = "%===============================================================================";
	private static String SUB_SECTION_SEP = "%-------------------------------------------------------------------------------";

	public static int MAX_TRUTH_TABLE_ROWS = 64; 
	
	public static final FileFilter FILE_FILTER = new TruthtableFileFilter(S.getter("tableLatexFilter"),".tex");

	private static int NrOfInCols(AnalyzerModel model) {
		int count = 0;
		VariableList inputs = model.getInputs();
		for (int i = 0 ; i < inputs.vars.size(); i++) {
			count += inputs.vars.get(i).width;
		}
		return count;
	}
	
	private static int NrOfOutCols(AnalyzerModel model) {
		int count = 0;
		VariableList outputs = model.getOutputs();
		for (int i = 0 ; i < outputs.vars.size(); i++) {
			count += outputs.vars.get(i).width;
		}
		return count;
	}
	
	private static String TruthTableHeader(AnalyzerModel model) {
		StringBuffer out = new StringBuffer();
		out.append("\\begin{center}\n");
		out.append("\\begin{tabular}{");
		int NrInCols = NrOfInCols(model);
		int NrOutCols = NrOfOutCols(model);
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
	
	private static String getCompactTruthTable(TruthTable tt, AnalyzerModel model) {
		StringBuffer content = new StringBuffer();
		for (int row = 0 ; row < tt.getVisibleRowCount() ; row++) {
			for (int col = 0 ; col < NrOfInCols(model) ; col++) {
				Entry val = tt.getVisibleInputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$&");
			}
			for (int col = 0 ; col < NrOfOutCols(model) ; col++) {
				Entry val = tt.getVisibleOutputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$");
				content.append(col == NrOfOutCols(model)-1 ? "\\\\\n" : "&");
			}
		}
		return content.toString();
	}
	
	private static String getCompleteTruthTable(TruthTable tt, AnalyzerModel model) {
		StringBuffer content = new StringBuffer();
		for (int row = 0 ; row < tt.getRowCount() ; row++) {
			for (int col = 0 ; col < NrOfInCols(model) ; col++) {
				Entry val = tt.getInputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$&");
			}
			for (int col = 0 ; col < NrOfOutCols(model) ; col++) {
				Entry val = tt.getOutputEntry(row, col);
				content.append("$\\textbf{"+val.getDescription()+"}$");
				content.append(col == NrOfOutCols(model)-1 ? "\\\\\n" : "&");
			}
		}
		return content.toString();
	}
	
	private static final String K_INTRO = "\\begin{tikzpicture}[karnaugh,";
	private static final String K_INDEX = "enable indices,";
	private static final String K_SETUP = "x=1\\kmunitlength,y=1\\kmunitlength,kmbar left sep=1\\kmunitlength,grp/.style n args={4}{#1,fill=#1!30,minimum width= #2\\kmunitlength,minimum height=#3\\kmunitlength,rounded corners=0.2\\kmunitlength,fill opacity=0.6,rectangle,draw}]";
	
	private static String getKarnaughInputs(AnalyzerModel model) {
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
	
	private static String getKarnaughEmpty(String name, boolean index, AnalyzerModel model) {
		StringBuffer content = new StringBuffer();
		content.append("\\begin{center}\n");
		content.append(K_INTRO+(index ? K_INDEX: "")+K_SETUP+"\n");
		content.append("\\karnaughmap{"+NrOfInCols(model)+"}{"+name+"}{"+getKarnaughInputs(model)+"}{}{}\n");
		content.append("\\end{tikzpicture}\n");
		content.append("\\end{center}");
		return content.toString();
	}
	
	private static String getKValues(int outcol,AnalyzerModel model) {
		StringBuffer content = new StringBuffer();
		for (int i = 0 ; i < model.getTruthTable().getRowCount() ; i++) {
			content.append(model.getTruthTable().getOutputEntry(i, outcol).getDescription());
		}
		return content.toString();
	}
	
	private static String getKarnaugh(String name, int outcol,AnalyzerModel model) {
		StringBuffer content = new StringBuffer();
		content.append("\\begin{center}\n");
		content.append(K_INTRO+K_SETUP+"\n");
		content.append("\\karnaughmap{"+NrOfInCols(model)+"}{"+name+"}{"+getKarnaughInputs(model)+
				"}\n{"+getKValues(outcol,model)+"}{}\n");
		content.append("\\end{tikzpicture}\n");
		content.append("\\end{center}");
		return content.toString();
	}
	
	public static void doSave(File file, AnalyzerModel model) throws IOException {
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
					out.println(TruthTableHeader(model));
					out.println(getCompactTruthTable(tt,model));
					out.println("\\end{tabular}");
					out.println("\\end{center}");
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexTruthTableComlete")+"}");
					out.println(TruthTableHeader(model));
					out.println(getCompleteTruthTable(tt,model));
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
					out.println(S.get("latexKarnaughText"));
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexKarnaughEmptyIndexed")+"}");
					for (int i = 0 ; i < model.getOutputs().vars.size() ; i++) {
						Var outp = model.getOutputs().vars.get(i);
						if (outp.width == 1) {
							String func = "$\\textbf{"+outp.name+"}$";
							out.println(getKarnaughEmpty(func,true,model));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaughEmpty(func,true,model));
							}
						}
					}
					out.println(SUB_SECTION_SEP);
					out.println("\\subsection{"+S.get("latexKarnaughEmpty")+"}");
					for (int i = 0 ; i < model.getOutputs().vars.size() ; i++) {
						Var outp = model.getOutputs().vars.get(i);
						if (outp.width == 1) {
							String func = "$\\textbf{"+outp.name+"}$";
							out.println(getKarnaughEmpty(func,false,model));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaughEmpty(func,false,model));
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
							out.println(getKarnaugh(func,outcol++,model));
						} else {
							for (int idx = outp.width-1 ; idx >= 0 ; idx--) {
								String func = "$\\textbf{"+outp.name+"}_{\\textbf{"+idx+"}}$";
								out.println(getKarnaugh(func,outcol++,model));
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
							out.println(new ExpressionLatex(exp,outp,0).get()+"\\\\");
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
