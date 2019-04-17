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

import com.cburch.logisim.analyze.data.CsvInterpretor;
import com.cburch.logisim.analyze.data.CsvParameter;
import com.cburch.logisim.analyze.gui.CsvReadParameterDialog;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

public class TruthtableCsvFile {

  public static final FileFilter FILE_FILTER =
      new TruthtableFileFilter(S.getter("tableCsvFileFilter"), ".csv");
  public static final char DEFAULT_SEPARATOR = ',';
  public static final char DEFAULT_QUOTE = '"';

  public static void doSave(File file, AnalyzerModel model) throws IOException {
    VariableList inputs = model.getInputs();
    VariableList outputs = model.getOutputs();
    if (inputs.vars.isEmpty() || outputs.vars.isEmpty()) return;
    PrintStream out = new PrintStream(file);
    try {
      TruthTable tt = model.getTruthTable();
      tt.compactVisibleRows();
      for (int i = 0; i < inputs.vars.size(); i++) {
        Var cur = inputs.vars.get(i);
        String Name = cur.width == 1 ? cur.name : cur.name + "[" + (cur.width - 1) + "..0]";
        out.print(DEFAULT_QUOTE + Name + DEFAULT_QUOTE + DEFAULT_SEPARATOR);
        for (int j = 1; j < cur.width; j++) out.print(DEFAULT_SEPARATOR);
      }
      out.print(DEFAULT_QUOTE + "|" + DEFAULT_QUOTE);
      for (int i = 0; i < outputs.vars.size(); i++) {
        out.print(DEFAULT_SEPARATOR);
        Var cur = outputs.vars.get(i);
        String Name = cur.width == 1 ? cur.name : cur.name + "[" + (cur.width - 1) + "..0]";
        out.print(DEFAULT_QUOTE + Name + DEFAULT_QUOTE);
        for (int j = 1; j < cur.width; j++) out.print(DEFAULT_SEPARATOR);
      }
      out.println();
      for (int row = 0; row < tt.getVisibleRowCount(); row++) {
        for (int i = 0; i < inputs.bits.size(); i++) {
          Entry e = tt.getVisibleInputEntry(row, i);
          out.print(e.getDescription() + DEFAULT_SEPARATOR);
        }
        out.print(DEFAULT_QUOTE + "|" + DEFAULT_QUOTE);
        for (int i = 0; i < outputs.bits.size(); i++) {
          out.print(DEFAULT_SEPARATOR);
          Entry e = tt.getVisibleOutputEntry(row, i);
          out.print(e.getDescription());
        }
        out.println();
      }
    } finally {
      out.close();
    }
  }

  public static void doLoad(File file, AnalyzerModel model, JFrame parrentFrame)
      throws IOException {
    CsvParameter param = new CsvParameter();
    new CsvReadParameterDialog(param, file, parrentFrame);
    if (!param.isValid()) return;
    CsvInterpretor cin = new CsvInterpretor(file, param, parrentFrame);
    cin.getTruthTable(model);
  }
}
