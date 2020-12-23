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

package com.cburch.logisim.analyze.file;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.data.CoverColor;
import com.cburch.logisim.analyze.data.KMapGroups;
import com.cburch.logisim.analyze.data.KMapGroups.CoverInfo;
import com.cburch.logisim.analyze.data.KMapGroups.KMapGroupInfo;
import com.cburch.logisim.analyze.gui.KarnaughMapPanel;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.Var.Bit;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

public class AnalyzerTexWriter {

  private static String SECTION_SEP =
      "%===============================================================================";
  private static String SUB_SECTION_SEP =
      "%-------------------------------------------------------------------------------";

  public static int MAX_TRUTH_TABLE_ROWS = 64;

  public static final FileFilter FILE_FILTER =
      new TruthtableFileFilter(S.getter("tableLatexFilter"), ".tex");

  private static int NrOfInCols(AnalyzerModel model) {
    int count = 0;
    VariableList inputs = model.getInputs();
    for (int i = 0; i < inputs.vars.size(); i++) {
      count += inputs.vars.get(i).width;
    }
    return count;
  }

  private static int NrOfOutCols(AnalyzerModel model) {
    int count = 0;
    VariableList outputs = model.getOutputs();
    for (int i = 0; i < outputs.vars.size(); i++) {
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
    for (int i = 0; i < NrInCols; i++) out.append("c");
    out.append("|");
    for (int i = 0; i < NrOutCols; i++) out.append("c");
    out.append("}\n");
    /* Make the header text */
    List<Var> inputVars = model.getInputs().vars;
    List<Var> outputVars = model.getOutputs().vars;
    for (int i = 0; i < inputVars.size(); i++) {
      Var inp = inputVars.get(i);
      if (inp.width == 1) {
        out.append("$" + inp.name + "" + "$&");
      } else {
        String format = i == inputVars.size() - 1 ? "c|" : "c";
        out.append(
            "\\multicolumn{"
                + inp.width
                + "}{"
                + format
                + "}{$"
                + inp.name
                + "["
                + (inp.width - 1)
                + "..0]$}&");
      }
    }
    for (int i = 0; i < outputVars.size(); i++) {
      Var outp = outputVars.get(i);
      if (outp.width == 1) {
        out.append("$" + outp.name + "$");
      } else {
        out.append(
            "\\multicolumn{"
                + outp.width
                + "}{c}{$"
                + outp.name
                + "["
                + (outp.width - 1)
                + "..0]$}");
      }
      out.append(i < outputVars.size() - 1 ? "&" : "\\\\");
    }
    out.append("\n\\hline");
    return out.toString();
  }

  private static String getCompactTruthTable(TruthTable tt, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    for (int row = 0; row < tt.getVisibleRowCount(); row++) {
      for (int col = 0; col < NrOfInCols(model); col++) {
        Entry val = tt.getVisibleInputEntry(row, col);
        content.append("$" + val.getDescription() + "$&");
      }
      for (int col = 0; col < NrOfOutCols(model); col++) {
        Entry val = tt.getVisibleOutputEntry(row, col);
        content.append("$" + val.getDescription() + "$");
        content.append(col == NrOfOutCols(model) - 1 ? "\\\\\n" : "&");
      }
    }
    return content.toString();
  }

  private static String getCompleteTruthTable(TruthTable tt, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    for (int row = 0; row < tt.getRowCount(); row++) {
      for (int col = 0; col < NrOfInCols(model); col++) {
        Entry val = tt.getInputEntry(row, col);
        content.append("$" + val.getDescription() + "$&");
      }
      for (int col = 0; col < NrOfOutCols(model); col++) {
        Entry val = tt.getOutputEntry(row, col);
        content.append("$" + val.getDescription() + "$");
        content.append(col == NrOfOutCols(model) - 1 ? "\\\\\n" : "&");
      }
    }
    return content.toString();
  }

  private static final String K_INTRO = "\\begin{tikzpicture}[karnaugh,";
  private static final String K_NUMBERED = "disable bars,";
  private static final String K_SETUP =
      "x=1\\kmunitlength,y=1\\kmunitlength,kmbar left sep=1\\kmunitlength,grp/.style n args={4}{#1,fill=#1!30,minimum width= #2\\kmunitlength,minimum height=#3\\kmunitlength,rounded corners=0.2\\kmunitlength,fill opacity=0.6,rectangle,draw}]";

  /*
   * The package takes another order of the input variables as logisim, therefore we have to reorder:
   *
   * 	kmapsize		logisim:		karnaugh_tikz:
   *      1			 A				 A
   *      2			 AB              AB
   *      3            ABC             BAC
   *      4            ABCD            ACBD
   *      5            ABCDE           CADBE
   *      6            ABCDEF			 ADBECF
   */

  private static int[] reordered(int NrOfInputs) {
    switch (NrOfInputs) {
      case 1:
        int[] ret1 = {0};
        return ret1;
      case 2:
        int[] ret2 = {0, 1};
        return ret2;
      case 3:
        int[] ret3 = {1, 0, 2};
        return ret3;
      case 4:
        int[] ret4 = {0, 2, 1, 3};
        return ret4;
      case 5:
        int[] ret5 = {2, 0, 3, 1, 4};
        return ret5;
      case 6:
        int[] ret6 = {0, 3, 1, 4, 2, 5};
        return ret6;
    }
    return null;
  }

  private static int reorderedIndex(int NrOfInputs, int row) {
    int result = 0;
    int[] reorder = reordered(NrOfInputs);
    int[] values = new int[NrOfInputs];
    for (int i = 0; i < NrOfInputs; i++) values[i] = 1 << (NrOfInputs - reorder[i] - 1);
    int mask = 1 << (NrOfInputs - 1);
    for (int i = 0; i < NrOfInputs; i++) {
      if ((row & mask) == mask) result |= values[i];
      mask >>= 1;
    }
    return result;
  }

  private static String getKarnaughInputs(AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    int[] reorder = reordered(model.getInputs().bits.size());
    for (int i = 0; i < model.getInputs().bits.size(); i++) {
      try {
	    Bit inp = Bit.parse(model.getInputs().bits.get(reorder[i]));
        content.append("{$"+inp.name);
        if (inp.b >= 0)
          content.append("_"+Integer.toString(inp.b));
        content.append("$}");
	  } catch (ParserException e) {
		// TODO Auto-generated catch block
	  }
    }
    return content.toString();
  }

  private static String getGrayCode(int nrVars) {
    switch (nrVars) {
      case 2:
        return "{0/00,1/01,2/11,3/10}";
      case 3:
        return "{0/000,1/001,2/011,3/010,4/110,5/111,6/101,7/100}";
      default:
        return "{0/0,1/1}";
    }
  }

  private static String getNumberedHeader(String name, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    TruthTable table = model.getTruthTable();
    DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    int kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    content.append("\n");
    StringBuffer leftVars = new StringBuffer();
    StringBuffer topVars = new StringBuffer();
    int nrLeftVars = KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    int count = 0;
    for (Var inp : table.getInputVariables()) {
      if (inp.width == 1) {
        if (count++ < nrLeftVars) {
          if (leftVars.length() != 0) leftVars.append(", ");
          leftVars.append("$" + inp.name + "$");
        } else {
          if (topVars.length() != 0) topVars.append(", ");
          topVars.append("$" + inp.name + "$");
        }
      } else {
        for (int idx = inp.width; idx >= 0; idx--) {
          if (count++ < nrLeftVars) {
            if (leftVars.length() != 0) leftVars.append(", ");
            leftVars.append("$" + inp.name + "_{" + idx + "}$");
          } else {
            if (topVars.length() != 0) topVars.append(", ");
            topVars.append("$" + inp.name + "_{" + idx + "}$");
          }
        }
      }
    }
    content.append(
        "\\draw[kmbox] (" + df.format(-0.5) + "," + df.format((double) kmapRows + 0.5) + ")\n");
    content.append("   node[below left]{" + leftVars.toString() + "}\n");
    content.append("   node[above right]{" + topVars.toString() + "} +(-0.2,0.2)\n");
    content.append("   node[above left]{" + name + "};");
    content.append(
        "\\draw (0," + kmapRows + ") -- (-0.7," + df.format((double) kmapRows + 0.7) + ");\n");
    content.append("\\foreach \\x/\\1 in %\n");
    content.append(getGrayCode(KarnaughMapPanel.COL_VARS[table.getInputColumnCount()]) + " {\n");
    content.append("   \\node at (\\x+0.5," + df.format(kmapRows + 0.2) + ") {\\1};\n}\n");
    content.append("\\foreach \\y/\\1 in %\n");
    content.append(getGrayCode(KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()]) + " {\n");
    content.append("   \\node at (-0.4,-0.5-\\y+" + df.format(kmapRows) + ") {\\1};\n}\n");
    return content.toString();
  }

  private static String getKarnaughEmpty(String name, boolean lined, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    content.append("\\begin{center}\n");
    content.append(K_INTRO + (lined ? "" : K_NUMBERED) + K_SETUP + "\n");
    content.append(
        "\\karnaughmap{"
            + NrOfInCols(model)
            + "}{"
            + name
            + "}{"
            + getKarnaughInputs(model)
            + "}{}{");
    if (!lined) content.append(getNumberedHeader(name, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  private static String getKValues(int outcol, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    for (int i = 0; i < model.getTruthTable().getRowCount(); i++) {
      int idx = reorderedIndex(model.getInputs().bits.size(), i);
      content.append(model.getTruthTable().getOutputEntry(idx, outcol).getDescription());
    }
    return content.toString();
  }

  private static String getKarnaugh(String name, boolean lined, int outcol, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    content.append("\\begin{center}\n");
    content.append(K_INTRO + (lined ? "" : K_NUMBERED) + K_SETUP + "\n");
    content.append(
        "\\karnaughmap{"
            + NrOfInCols(model)
            + "}{"
            + name
            + "}{"
            + getKarnaughInputs(model)
            + "}\n{"
            + getKValues(outcol, model)
            + "}{");
    if (!lined) content.append(getNumberedHeader(name, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  private static double OFFSET = 0.2;

  private static String getCovers(String name, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    TruthTable table = model.getTruthTable();
    if (table.getInputColumnCount() > KarnaughMapPanel.MAX_VARS) return content.toString();
    KMapGroups groups = new KMapGroups(model);
    groups.setOutput(name);
    DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    int idx = 0;
    int kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    for (KMapGroupInfo group : groups.getCovers()) {
      for (CoverInfo thiscover : group.getAreas()) {
        content.append("   \\node[grp={" + CoverColor.COVERCOLOR.getColorName(group.getColor()) + "}");
        double width = thiscover.getWidth() - OFFSET;
        double height = thiscover.getHeight() - OFFSET;
        content.append("{" + df.format(width) + "}{" + df.format(height) + "}]");
        content.append("(n" + (idx++) + ") at");
        double y = (double) kmapRows - ((double) thiscover.getHeight()) / 2.0 - thiscover.getRow();
        double x = ((double) thiscover.getWidth()) / 2.0 + thiscover.getCol();
        content.append("(" + df.format(x) + "," + df.format(y) + ") {};\n");
      }
    }
    return content.toString();
  }

  private static String getKarnaughGroups(
      String output, String name, boolean lined, int outcol, AnalyzerModel model) {
    StringBuffer content = new StringBuffer();
    content.append("\\begin{center}\n");
    content.append(K_INTRO + (lined ? "" : K_NUMBERED) + K_SETUP + "\n");
    content.append(
        "\\karnaughmap{"
            + NrOfInCols(model)
            + "}{"
            + name
            + "}{"
            + getKarnaughInputs(model)
            + "}\n{"
            + getKValues(outcol, model)
            + "}{");
    if (!lined) content.append(getNumberedHeader(name, model));
    else content.append("\n");
    content.append(getCovers(output, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  public static void doSave(File file, AnalyzerModel model) throws IOException {
    boolean linedStyle = AppPreferences.KMAP_LINED_STYLE.getBoolean();
    /* make sure the model is up to date */
    boolean modelIsUpdating = model.getOutputExpressions().UpdatesEnabled();
    model.getOutputExpressions().enableUpdates();
    PrintStream out = new PrintStream(file);
    try {
      /*
       * We start to create the document header section with all required packages
       */
      out.println("\\documentclass [15pt,a4paper,twoside]{article}");
      out.println(
          "\\usepackage["
              + S.get("latexBabelLanguage")
              + ",shorthands=off]{babel}        % shorhands=off is required for babel french in combination with tikz karnaugh....");
      out.println("\\usepackage[utf8x]{inputenc}");
      out.println("\\usepackage[T1]{fontenc}");
      out.println("\\usepackage{amsmath}");
      out.println("\\usepackage{geometry}");
      out.println(
          "\\geometry{verbose,a4paper, tmargin=3.5cm,bmargin=3.5cm,lmargin=2.5cm,rmargin=2.5cm,headsep=1cm,footskip=1.5cm}");
      out.println("\\usepackage{fancyhdr}");
      out.println("\\usepackage{colortbl}");
      out.println("\\usepackage[dvipsnames]{xcolor}");
      out.println("\\usepackage{tikz -timing}");
      out.println("\\usepackage{tikz}");
      out.println("\\usetikzlibrary{karnaugh}");
      out.println("\\pagestyle{fancy}");
      out.println();
      /*
       * Here we define our own colors
       */
      CoverColor cols = CoverColor.COVERCOLOR;
      for (int i = 0; i < cols.nrOfColors(); i++) {
        Color col = cols.getColor(i);
        out.println(
            "\\definecolor{"
                + cols.getColorName(col)
                + "}{RGB}{"
                + col.getRed()
                + ","
                + col.getGreen()
                + ","
                + col.getBlue()
                + "}");
      }
      out.println();
      /*
       * Here we create the headers and footers
       */
      out.println("\\fancyhead{}");
      out.println("\\fancyhead[C] {" + S.fmt("latexHeader", new Date()) + "}");
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
      out.println("\\section{" + S.get("latexIntroduction") + "}");
      out.println(S.get("latexIntroductionText"));
      if (model.getInputs().vars.size() == 0 || model.getOutputs().vars.size() == 0) {
        out.println(SECTION_SEP);
        out.println("\\section{" + S.get("latexEmpty") + "}");
        out.println(S.get("latexEmptyText"));
      } else {
        /*
         * Here we put the section on the truth table
         */
        out.println(SECTION_SEP);
        out.println("\\section{" + S.get("latexTruthTable") + "}");
        out.println(S.get("latexTruthTableText"));
        final TruthTable tt = model.getTruthTable();
        if (tt.getRowCount() > MAX_TRUTH_TABLE_ROWS) {
          out.println(S.fmt("latexTruthTableToBig", MAX_TRUTH_TABLE_ROWS));
        } else {
          tt.compactVisibleRows();
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexTruthTableCompact") + "}");
          out.println(TruthTableHeader(model));
          out.println(getCompactTruthTable(tt, model));
          out.println("\\end{tabular}");
          out.println("\\end{center}");
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexTruthTableComlete") + "}");
          out.println(TruthTableHeader(model));
          out.println(getCompleteTruthTable(tt, model));
          out.println("\\end{tabular}");
          out.println("\\end{center}");
        }
        /*
         * We continue with the Karnaugh diagrams
         */
        out.println(SECTION_SEP);
        out.println("\\section{" + S.get("latexKarnaugh") + "}");
        if (tt.getRowCount() > MAX_TRUTH_TABLE_ROWS) {
          out.println(S.fmt("latexKarnaughToBig",
                  (int) Math.ceil(Math.log(MAX_TRUTH_TABLE_ROWS) / Math.log(2))));
        } else {
          out.println(S.get("latexKarnaughText"));
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexKarnaughEmpty") + "}");
          for (int i = 0; i < model.getOutputs().vars.size(); i++) {
            Var outp = model.getOutputs().vars.get(i);
            if (outp.width == 1) {
              String func = "$" + outp.name + "$";
              out.println(getKarnaughEmpty(func, linedStyle, model));
            } else {
              for (int idx = outp.width - 1; idx >= 0; idx--) {
                String func = "$" + outp.name + "_{" + idx + "}$";
                out.println(getKarnaughEmpty(func, linedStyle, model));
              }
            }
          }
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexKarnaughFilledIn") + "}");
          int outcol = 0;
          for (int i = 0; i < model.getOutputs().vars.size(); i++) {
            Var outp = model.getOutputs().vars.get(i);
            if (outp.width == 1) {
              String func = "$" + outp.name + "$";
              out.println(getKarnaugh(func, linedStyle, outcol++, model));
            } else {
              for (int idx = outp.width - 1; idx >= 0; idx--) {
                String func = "$" + outp.name + "_{" + idx + "}$";
                out.println(getKarnaugh(func, linedStyle, outcol++, model));
              }
            }
          }
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexKarnaughFilledInGroups") + "}");
          outcol = 0;
          for (int i = 0; i < model.getOutputs().vars.size(); i++) {
            Var outp = model.getOutputs().vars.get(i);
            if (outp.width == 1) {
              String func = "$" + outp.name + "$";
              out.println(getKarnaughGroups(outp.name, func, linedStyle, outcol++, model));
            } else {
              for (int idx = outp.width - 1; idx >= 0; idx--) {
                String func = "$" + outp.name + "_{" + idx + "}$";
                out.println(
                    getKarnaughGroups(outp.name + "[" + idx + "]", func, linedStyle, outcol++, model));
              }
            }
          }
          /*
           * Finally we print the minimal expressions
           */
          out.println(SECTION_SEP);
          out.println("\\section{" + S.get("latexMinimal") + "}");
          for (int o = 0; o < model.getTruthTable().getOutputVariables().size(); o++) {
            Var outp = model.getTruthTable().getOutputVariable(o);
            if (outp.width == 1) {
              Expression exp = Expressions.eq(Expressions.variable(outp.name),
                  model.getOutputExpressions().getMinimalExpression(outp.name));
              out.println(exp.toString(Notation.LaTeX)+ "~\\\\");
            } else {
              for (int idx = outp.width - 1; idx >= 0; idx--) {
                String name = outp.bitName(idx);
                Expression exp = Expressions.eq(Expressions.variable(name),
                    model.getOutputExpressions().getMinimalExpression(name));
                out.println(exp.toString(Notation.LaTeX)+ "~\\\\");
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
    if (!modelIsUpdating)
    	model.getOutputExpressions().disableUpdates();
  }
}
