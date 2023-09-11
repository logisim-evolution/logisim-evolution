/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.file;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.data.CoverColor;
import com.cburch.logisim.analyze.data.KarnaughMapGroups;
import com.cburch.logisim.analyze.gui.KarnaughMapPanel;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.Var.Bit;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.filechooser.FileFilter;

public class AnalyzerTexWriter {

  private static final String SECTION_SEP = "%===============================================================================";
  private static final String SUB_SECTION_SEP = "%-------------------------------------------------------------------------------";

  public static final int MAX_TRUTH_TABLE_ROWS = 64;

  public static final FileFilter FILE_FILTER =
      new TruthtableFileFilter(S.getter("tableLatexFilter"), ".tex");

  private static int nrOfInCols(AnalyzerModel model) {
    var count = 0;
    var inputs = model.getInputs();
    for (int i = 0; i < inputs.vars.size(); i++) {
      count += inputs.vars.get(i).width;
    }
    return count;
  }

  private static int nrOfOutCols(AnalyzerModel model) {
    var count = 0;
    final var outputs = model.getOutputs();
    for (int i = 0; i < outputs.vars.size(); i++) {
      count += outputs.vars.get(i).width;
    }
    return count;
  }

  private static String truthTableHeader(AnalyzerModel model) {
    final var out = new StringBuilder();
    out.append("\\begin{center}\n");
    out.append("\\begin{tabular}{");
    final var nrInCols = nrOfInCols(model);
    final var nrOutCols = nrOfOutCols(model);
    out.append("c".repeat(nrInCols));
    out.append("|");
    out.append("c".repeat(nrOutCols));
    out.append("}\n");
    /* Make the header text */
    final var inputVars = model.getInputs().vars;
    final var outputVars = model.getOutputs().vars;
    for (int i = 0; i < inputVars.size(); i++) {
      final var inp = inputVars.get(i);
      if (inp.width == 1) {
        out.append("$").append(inp.name).append("$&");
      } else {
        final var format = i == inputVars.size() - 1 ? "c|" : "c";
        out.append("\\multicolumn{").append(inp.width).append("}{").append(format).append("}{$")
            .append(inp.name).append("[").append(inp.width - 1).append("..0]$}&");
      }
    }
    for (int i = 0; i < outputVars.size(); i++) {
      final var outp = outputVars.get(i);
      if (outp.width == 1) {
        out.append("$").append(outp.name).append("$");
      } else {
        out.append("\\multicolumn{").append(outp.width).append("}{c}{$").append(outp.name)
            .append("[").append(outp.width - 1).append("..0]$}");
      }
      out.append(i < outputVars.size() - 1 ? "&" : "\\\\");
    }
    out.append("\n\\hline");
    return out.toString();
  }

  private static String getCompactTruthTable(TruthTable tt, AnalyzerModel model) {
    final var content = new StringBuilder();
    for (int row = 0; row < tt.getVisibleRowCount(); row++) {
      for (int col = 0; col < nrOfInCols(model); col++) {
        final var val = tt.getVisibleInputEntry(row, col);
        content.append("$").append(val.getDescription()).append("$&");
      }
      for (int col = 0; col < nrOfOutCols(model); col++) {
        final var val = tt.getVisibleOutputEntry(row, col);
        content.append("$").append(val.getDescription()).append("$");
        content.append(col == nrOfOutCols(model) - 1 ? "\\\\\n" : "&");
      }
    }
    return content.toString();
  }

  private static String getCompleteTruthTable(TruthTable tt, AnalyzerModel model) {
    final var content = new StringBuilder();
    for (int row = 0; row < tt.getRowCount(); row++) {
      for (int col = 0; col < nrOfInCols(model); col++) {
        final var val = tt.getInputEntry(row, col);
        content.append("$").append(val.getDescription()).append("$&");
      }
      for (int col = 0; col < nrOfOutCols(model); col++) {
        final var val = tt.getOutputEntry(row, col);
        content.append("$").append(val.getDescription()).append("$");
        content.append(col == nrOfOutCols(model) - 1 ? "\\\\\n" : "&");
      }
    }
    return content.toString();
  }

  private static final String K_INTRO = "\\begin{tikzpicture}[karnaugh,";
  private static final String K_NUMBERED = "disable bars,";
  private static final String K_SETUP =
      "x=1\\kmunitlength,y=1\\kmunitlength,kmbar left sep=1\\kmunitlength,grp/.style n args={4}{#1,fill=#1!30,minimum width= #2\\kmunitlength,minimum height=#3\\kmunitlength,rounded corners=0.2\\kmunitlength,fill opacity=0.6,rectangle,draw}]";

  /*
   * The package takes another order of the input variables as logisim, therefore
   *  we have to reorder:
   *
   *   kmapsize      logisim:      karnaugh_tikz:
   *      1            A               A
   *      2            AB              AB
   *      3            ABC             BAC
   *      4            ABCD            ACBD
   *      5            ABCDE           CADBE
   *      6            ABCDEF          ADBECF
   */

  private static int[] reordered(int nrOfInputs) {
    return switch (nrOfInputs) {
      case 1 -> new int[]{0};
      case 2 -> new int[]{0, 1};
      case 3 -> new int[]{1, 0, 2};
      case 4 -> new int[]{0, 2, 1, 3};
      case 5 -> new int[]{2, 0, 3, 1, 4};
      case 6 -> new int[]{0, 3, 1, 4, 2, 5};
      default -> new int[0];
    };
  }

  private static int reorderedIndex(int nrOfInputs, int row) {
    var result = 0;
    final var reorder = reordered(nrOfInputs);
    final var values = new int[nrOfInputs];
    for (int i = 0; i < nrOfInputs; i++) values[i] = 1 << (nrOfInputs - reorder[i] - 1);
    int mask = 1 << (nrOfInputs - 1);
    for (int i = 0; i < nrOfInputs; i++) {
      if ((row & mask) == mask) result |= values[i];
      mask >>= 1;
    }
    return result;
  }

  private static String getKarnaughInputs(AnalyzerModel model) {
    final var content = new StringBuilder();
    final var reorder = reordered(model.getInputs().bits.size());
    for (var i = 0; i < model.getInputs().bits.size(); i++) {
      try {
        final var inp = Bit.parse(model.getInputs().bits.get(reorder[i]));
        content.append("{$").append(inp.name);
        if (inp.bitIndex >= 0) content.append("_").append(inp.bitIndex);
        content.append("$}");
      } catch (ParserException e) {
        // Do nothing.
      }
    }
    return content.toString();
  }

  private static String getGrayCode(int nrVars) {
    return switch (nrVars) {
      case 2 -> "{0/00,1/01,2/11,3/10}";
      case 3 -> "{0/000,1/001,2/011,3/010,4/110,5/111,6/101,7/100}";
      default -> "{0/0,1/1}";
    };
  }

  private static String getNumberedHeader(String name, AnalyzerModel model) {
    final var content = new StringBuilder();
    final var table = model.getTruthTable();
    final var df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    final var kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    content.append("\n");
    final var leftVars = new StringBuilder();
    final var topVars = new StringBuilder();
    final var nrLeftVars = KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    var count = 0;
    for (final var variable : table.getInputVariables()) {
      if (variable.width == 1) {
        if (count++ < nrLeftVars) {
          if (leftVars.length() != 0) leftVars.append(", ");
          leftVars.append("$").append(variable.name).append("$");
        } else {
          if (topVars.length() != 0) topVars.append(", ");
          topVars.append("$").append(variable.name).append("$");
        }
      } else {
        for (int idx = variable.width; idx >= 0; idx--) {
          if (count++ < nrLeftVars) {
            if (leftVars.length() != 0) leftVars.append(", ");
            leftVars.append("$").append(variable.name).append("_{").append(idx).append("}$");
          } else {
            if (topVars.length() != 0) topVars.append(", ");
            topVars.append("$").append(variable.name).append("_{").append(idx).append("}$");
          }
        }
      }
    }
    content.append("\\draw[kmbox] (").append(df.format(-0.5)).append(",")
        .append(df.format((double) kmapRows + 0.5)).append(")\n");
    content.append("   node[below left]{").append(leftVars).append("}\n");
    content.append("   node[above right]{").append(topVars).append("} +(-0.2,0.2)\n");
    content.append("   node[above left]{").append(name).append("};");
    content.append("\\draw (0,").append(kmapRows).append(") -- (-0.7,")
        .append(df.format((double) kmapRows + 0.7)).append(");\n");
    content.append("\\foreach \\x/\\1 in %\n");
    content.append(getGrayCode(KarnaughMapPanel.COL_VARS[table.getInputColumnCount()])).append(" {\n");
    content.append("   \\node at (\\x+0.5,").append(df.format(kmapRows + 0.2)).append(") {\\1};\n}\n");
    content.append("\\foreach \\y/\\1 in %\n");
    content.append(getGrayCode(KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()])).append(" {\n");
    content.append("   \\node at (-0.4,-0.5-\\y+").append(df.format(kmapRows)).append(") {\\1};\n}\n");

    return content.toString();
  }

  private static String getKarnaughEmpty(String name, boolean lined, AnalyzerModel model) {
    final var content = new StringBuilder();
    content.append("\\begin{center}\n");
    content.append(K_INTRO).append(lined ? "" : K_NUMBERED).append(K_SETUP).append("\n");
    content.append("\\karnaughmap{").append(nrOfInCols(model)).append("}{").append(name)
        .append("}{").append(getKarnaughInputs(model)).append("}{}{");
    if (!lined) content.append(getNumberedHeader(name, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  private static String getKValues(int outcol, AnalyzerModel model) {
    final var content = new StringBuilder();
    for (var i = 0; i < model.getTruthTable().getRowCount(); i++) {
      final var idx = reorderedIndex(model.getInputs().bits.size(), i);
      content.append(model.getTruthTable().getOutputEntry(idx, outcol).getDescription());
    }
    return content.toString();
  }

  private static String getKarnaugh(String name, boolean lined, int outcol, AnalyzerModel model) {
    final var content = new StringBuilder();
    content.append("\\begin{center}\n");
    content.append(K_INTRO).append(lined ? "" : K_NUMBERED).append(K_SETUP).append("\n");
    content.append("\\karnaughmap{").append(nrOfInCols(model)).append("}{").append(name)
        .append("}{").append(getKarnaughInputs(model)).append("}\n{")
        .append(getKValues(outcol, model)).append("}{");
    if (!lined) content.append(getNumberedHeader(name, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  private static final double OFFSET = 0.2;

  private static String getCovers(String name, AnalyzerModel model) {
    final var content = new StringBuilder();
    final var table = model.getTruthTable();
    if (table.getInputColumnCount() > KarnaughMapPanel.MAX_VARS) return content.toString();
    final var groups = new KarnaughMapGroups(model);
    groups.setOutput(name);
    final var df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    var idx = 0;
    final var kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
    for (final var group : groups.getCovers()) {
      for (final var thiscover : group.getAreas()) {
        content.append("   \\node[grp={").append(CoverColor.COVER_COLOR.getColorName(group.getColor())).append("}");
        double width = thiscover.getWidth() - OFFSET;
        double height = thiscover.getHeight() - OFFSET;
        content.append("{").append(df.format(width)).append("}{").append(df.format(height)).append("}]");
        content.append("(n").append(idx++).append(") at");
        double y = (double) kmapRows - ((double) thiscover.getHeight()) / 2.0 - thiscover.getRow();
        double x = ((double) thiscover.getWidth()) / 2.0 + thiscover.getCol();
        content.append("(").append(df.format(x)).append(",").append(df.format(y)).append(") {};\n");
      }
    }
    return content.toString();
  }

  private static String getKarnaughGroups(String output, String name, boolean lined, int outcol, AnalyzerModel model) {
    final var content = new StringBuilder();
    content.append("\\begin{center}\n");
    content.append(K_INTRO).append(lined ? "" : K_NUMBERED).append(K_SETUP).append("\n");
    content.append("\\karnaughmap{").append(nrOfInCols(model)).append("}{").append(name)
        .append("}{").append(getKarnaughInputs(model)).append("}\n{")
        .append(getKValues(outcol, model)).append("}{");
    if (!lined) {
      content.append(getNumberedHeader(name, model));
    } else {
      content.append("\n");
    }
    content.append(getCovers(output, model));
    content.append("}\n");
    content.append("\\end{tikzpicture}\n");
    content.append("\\end{center}");
    return content.toString();
  }

  public static void doSave(File file, AnalyzerModel model) throws IOException {
    final var linedStyle = AppPreferences.KMAP_LINED_STYLE.getBoolean();
    /* make sure the model is up to date */
    final var modelIsUpdating = model.getOutputExpressions().updatesEnabled();
    model.getOutputExpressions().enableUpdates();
    try (PrintStream out = new PrintStream(file)) {
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
      CoverColor cols = CoverColor.COVER_COLOR;
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
      out.println("\\fancyhead[C] {" + S.get("latexHeader", new Date()) + "}");
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
      if (model.getInputs().vars.isEmpty() || model.getOutputs().vars.isEmpty()) {
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
        final var tt = model.getTruthTable();
        if (tt.getRowCount() > MAX_TRUTH_TABLE_ROWS) {
          out.println(S.get("latexTruthTableToBig", MAX_TRUTH_TABLE_ROWS));
        } else {
          tt.compactVisibleRows();
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexTruthTableCompact") + "}");
          out.println(truthTableHeader(model));
          out.println(getCompactTruthTable(tt, model));
          out.println("\\end{tabular}");
          out.println("\\end{center}");
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexTruthTableComlete") + "}");
          out.println(truthTableHeader(model));
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
          out.println(S.get("latexKarnaughToBig",
              (int) Math.ceil(Math.log(MAX_TRUTH_TABLE_ROWS) / Math.log(2))));
        } else {
          out.println(S.get("latexKarnaughText"));
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexKarnaughEmpty") + "}");
          for (int i = 0; i < model.getOutputs().vars.size(); i++) {
            final var outp = model.getOutputs().vars.get(i);
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
          var outcol = 0;
          for (var i = 0; i < model.getOutputs().vars.size(); i++) {
            Var outp = model.getOutputs().vars.get(i);
            if (outp.width == 1) {
              final var func = "$" + outp.name + "$";
              out.println(getKarnaugh(func, linedStyle, outcol++, model));
            } else {
              for (var idx = outp.width - 1; idx >= 0; idx--) {
                final var func = "$" + outp.name + "_{" + idx + "}$";
                out.println(getKarnaugh(func, linedStyle, outcol++, model));
              }
            }
          }
          out.println(SUB_SECTION_SEP);
          out.println("\\subsection{" + S.get("latexKarnaughFilledInGroups") + "}");
          outcol = 0;
          for (var i = 0; i < model.getOutputs().vars.size(); i++) {
            final var outp = model.getOutputs().vars.get(i);
            if (outp.width == 1) {
              final var func = "$" + outp.name + "$";
              out.println(getKarnaughGroups(outp.name, func, linedStyle, outcol++, model));
            } else {
              for (var idx = outp.width - 1; idx >= 0; idx--) {
                final var func = "$" + outp.name + "_{" + idx + "}$";
                out.println(getKarnaughGroups(outp.name + "[" + idx + "]", func, linedStyle, outcol++, model));
              }
            }
          }
          /*
           * Finally we print the minimal expressions
           */
          out.println(SECTION_SEP);
          out.println("\\section{" + S.get("latexMinimal") + "}");
          for (var o = 0; o < model.getTruthTable().getOutputVariables().size(); o++) {
            final var outp = model.getTruthTable().getOutputVariable(o);
            if (outp.width == 1) {
              final var exp = Expressions.eq(Expressions.variable(outp.name),
                  model.getOutputExpressions().getMinimalExpression(outp.name));
              out.println(exp.toString(Notation.LATEX) + "~\\\\");
            } else {
              for (var idx = outp.width - 1; idx >= 0; idx--) {
                final var name = outp.bitName(idx);
                final var exp = Expressions.eq(Expressions.variable(name),
                    model.getOutputExpressions().getMinimalExpression(name));
                out.println(exp.toString(Notation.LATEX) + "~\\\\");
              }
            }
          }
        }
      }
      /*
       * That was all folks
       */
      out.println("\\end{document}");
    }
    if (!modelIsUpdating) model.getOutputExpressions().disableUpdates();
  }
}
