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

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.generic.OptionPane;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

public class TruthtableTextFile {

  /*
   * File format:
   *
   * # Hints and Notes on Formatting:
   * # * You can edit this file then import it back into Logisim!
   * # * Anything after a '#' is a comment and will be ignored.
   * # * Blank lines and separator lines (e.g., ~~~~~~) are ignored.
   * # * Keep column names simple (no spaces, punctuation, etc.)
   * # * 'Name[N..0]' indicates an N+1 bit variable, whereas
   * #   'Name' by itself indicates a 1-bit variable.
   * # * You can use 'x' or '-' to indicate "don't care" for both
   * #   input and output bits.
   * # * You can use binary (e.g., '10100011xxxx') notation or
   * #   or hex (e.g., 'C3x'). Logisim will figure out which is which.
   *
   * A B[3..0] | D[3..0]
   * ~~~~~~~~~~~~~~~~~~~
   * 0  0000   |  1010
   * 0  0001   |  1101
   * 0  0010   |  1010
   * 0  0011   |  1010
   * 0  0100   |  0001
   * 0  0101   |  1000
   * 0  0110   |  0101
   * 0  0111   |  1001
   * 0  1000   |  0101
   * 0  1001   |  0010
   * 0  1010   |  1100
   * 0  1011   |  0110
   * 0  1100   |  1000
   * 0  1101   |  1001
   * 0  1110   |  0010
   * 0  1111   |  1010
   * 1  ----   |  0000
   */

  private static void center(PrintStream out, String s, int n) {
    int pad = n - s.length();
    if (pad <= 0) {
      out.printf("%s", s);
    } else {
      String left = (pad / 2 > 0) ? "%" + (pad / 2) + "s" : "%s";
      String right = (pad - pad / 2) > 0 ? "%" + (pad - pad / 2) + "s" : "%s";
      out.printf(left + "%s" + right, "", s, "");
    }
  }

  public static void doSave(File file, AnalyzerModel model) throws IOException {
    PrintStream out = new PrintStream(file);
    try {
      out.println(S.get("tableRemark1"));
      Circuit c = model.getCurrentCircuit();
      if (c != null) out.println(S.fmt("tableRemark2", c.getName()));
      out.println(S.fmt("tableRemark3", new Date()));
      out.println();
      out.println(S.get("tableRemark4"));
      out.println();
      VariableList inputs = model.getInputs();
      VariableList outputs = model.getOutputs();
      int colwidth[] = new int[inputs.vars.size() + outputs.vars.size()];
      int i;
      i = 0;
      for (Var var : inputs.vars) colwidth[i++] = Math.max(var.toString().length(), var.width);
      for (Var var : outputs.vars) colwidth[i++] = Math.max(var.toString().length(), var.width);
      i = 0;
      for (Var var : inputs.vars) {
        center(out, var.toString(), colwidth[i++]);
        out.print(" ");
      }
      out.print("|");
      for (Var var : outputs.vars) {
        out.print(" ");
        center(out, var.toString(), colwidth[i++]);
      }
      out.println();
      for (i = 0; i < colwidth.length; i++) {
        for (int j = 0; j < colwidth[i] + 1; j++) out.print("~");
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

  static final Pattern NAME_FORMAT =
      Pattern.compile("([a-zA-Z][a-zA-Z_0-9]*)\\[(-?[0-9]+)\\.\\.(-?[0-9]+)\\]");

  static void validateHeader(String line, VariableList inputs, VariableList outputs, int lineno)
      throws IOException {
    String s[] = line.split("\\s+");
    VariableList cur = inputs;
    for (int i = 0; i < s.length; i++) {
      if (s[i].equals("|")) {
        if (cur == inputs) cur = outputs;
        else
          throw new IOException(
              String.format("Line %d: Separator '|' must appear only once.", lineno));
        continue;
      }
      String name = s[i];
      if (name.matches("[a-zA-Z][a-zA-Z_0-9]*")) {
        cur.add(new Var(name, 1));
      } else {
        Matcher m = NAME_FORMAT.matcher(name);
        if (!m.matches())
          throw new IOException(
              String.format("Line %d: Invalid variable name '%s'.", lineno, name));
        String n = m.group(1);
        int a, b;
        try {
          a = Integer.parseInt(m.group(2));
          b = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
          throw new IOException(String.format("Line %d: Invalid bit range in '%s'.", lineno, name));
        }
        if (a < 1 || b != 0)
          throw new IOException(String.format("Line %d: Invalid bit range in '%s'.", lineno, name));
        try {
          cur.add(new Var(n, a - b + 1));
        } catch (IllegalArgumentException e) {
          throw new IOException(
              String.format(
                  "Line %d: Too many bits in %s for truth table (max = %d bits).",
                  lineno,
                  (cur == inputs ? "input" : "output"),
                  (cur == inputs ? AnalyzerModel.MAX_INPUTS : AnalyzerModel.MAX_OUTPUTS)));
        }
      }
    }
    if (inputs.vars.size() == 0)
      throw new IOException(String.format("Line %d: Truth table has no inputs.", lineno));
    if (outputs.vars.size() == 0)
      throw new IOException(String.format("Line %d: Truth table has no outputs.", lineno));
  }

  static Entry parseBit(char c, String sval, Var var, int lineno) throws IOException {
    if (c == 'x' || c == 'X' || c == '-') return Entry.DONT_CARE;
    else if (c == '0') return Entry.ZERO;
    else if (c == '1') return Entry.ONE;
    else
      throw new IOException(
          String.format(
              "Line %d: Bit value '%c' in \"%s\" must be one of '0', '1', 'x', or '-'.",
              lineno, c, sval));
  }

  static Entry parseHex(char c, int bit, int nbits, String sval, Var var, int lineno)
      throws IOException {
    if (c == 'x' || c == 'X' || c == '-') return Entry.DONT_CARE;
    int d = 0;
    if ('0' <= c && c <= '9') d = c - '0';
    else if ('a' <= c && c <= 'f') d = 0xa + (c - 'a');
    else if ('A' <= c && c <= 'F') d = 0xA + (c - 'A');
    else
      throw new IOException(
          String.format(
              "Line %d: Hex digit '%c' in \"%s\" must be one of '0'-'9', 'a'-'f' or 'x'.",
              lineno, c, sval));
    if (nbits < 4 && (d >= (1 << nbits)))
      throw new IOException(
          String.format(
              "Line %d: Hex value \"%s\" contains too many bits for %s.", lineno, sval, var.name));
    return (((d & (1 << bit)) == 0) ? Entry.ZERO : Entry.ONE);
  }

  static int parseVal(Entry[] row, int col, String sval, Var var, int lineno) throws IOException {
    if (sval.length() == var.width) {
      // must be binary
      for (int i = 0; i < var.width; i++) row[col++] = parseBit(sval.charAt(i), sval, var, lineno);
    } else if (sval.length() == (var.width + 3) / 4) {
      // try hex
      for (int i = 0; i < var.width; i++) {
        row[col++] =
            parseHex(
                sval.charAt((i + ((4 - (var.width % 4)) % 4)) / 4),
                (var.width - i - 1) % 4,
                var.width - ((var.width - i - 1) / 4) * 4,
                sval,
                var,
                lineno);
      }
    } else {
      throw new IOException(
          String.format(
              "Line %d: Expected %d bits (or %d hex digits) in column %s, but found \"%s\".",
              lineno, var.width, (var.width + 3) / 4, var.name, sval));
    }
    return col;
  }

  static void validateRow(
      String line, VariableList inputs, VariableList outputs, ArrayList<Entry[]> rows, int lineno)
      throws IOException {
    Entry[] row = new Entry[inputs.bits.size() + outputs.bits.size()];
    int col = 0;
    String s[] = line.split("\\s+");
    int ix = 0;
    for (Var var : inputs.vars) {
      if (ix >= s.length || s[ix].equals("|"))
        throw new IOException(String.format("Line %d: Not enough input columns.", lineno));
      col = parseVal(row, col, s[ix++], var, lineno);
    }
    if (ix >= s.length)
      throw new IOException(String.format("Line %d: Missing '|' column separator.", lineno));
    else if (!s[ix].equals("|"))
      throw new IOException(String.format("Line %d: Too many input columns.", lineno));
    ix++;
    for (Var var : outputs.vars) {
      if (ix >= s.length)
        throw new IOException(String.format("Line %d: Not enough output columns.", lineno));
      else if (s[ix].equals("|"))
        throw new IOException(
            String.format("Line %d: Column separator '|' must appear only once.", lineno));
      col = parseVal(row, col, s[ix++], var, lineno);
    }
    if (ix != s.length)
      throw new IOException(String.format("Line %d: Too many output columns.", lineno));
    rows.add(row);
  }

  public static void doLoad(File file, AnalyzerModel model, JFrame parent) throws IOException {
    int lineno = 0;
    Scanner sc = new Scanner(file);
    VariableList inputs = new VariableList(AnalyzerModel.MAX_INPUTS);
    VariableList outputs = new VariableList(AnalyzerModel.MAX_OUTPUTS);
    ArrayList<Entry[]> rows = new ArrayList<>();
    try {
      while (sc.hasNextLine()) {
        lineno++;
        String line = sc.nextLine();
        int ix = line.indexOf('#');
        if (ix >= 0) line = line.substring(0, ix);
        line = line.trim();
        if (line.equals("")) continue;
        else if (line.matches("\\s*[~_=-][ ~_=-|]*")) continue;
        else if (inputs.vars.size() == 0) validateHeader(line, inputs, outputs, lineno);
        else validateRow(line, inputs, outputs, rows, lineno);
      }
      if (rows.size() == 0) throw new IOException("End of file: Truth table has no rows.");
      try {
        model.setVariables(inputs.vars, outputs.vars);
      } catch (IllegalArgumentException e) {
        throw new IOException(e.getMessage());
      }
      TruthTable table = model.getTruthTable();
      try {
        table.setVisibleRows(rows, false);
      } catch (IllegalArgumentException e) {
        int confirm =
            OptionPane.showConfirmDialog(
                parent,
                new String[] {e.getMessage(), S.get("tableParseErrorMessage")},
                S.get("tableParseErrorTitle"),
                OptionPane.YES_NO_OPTION);
        if (confirm != OptionPane.YES_OPTION) return;
        try {
          table.setVisibleRows(rows, true);
        } catch (IllegalArgumentException ex) {
          throw new IOException(ex.getMessage());
        }
      }
    } finally {
      sc.close();
    }
  }

  public static final FileFilter FILE_FILTER =
      new TruthtableFileFilter(S.getter("tableTxtFileFilter"), ".txt");
}
