/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.data;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.gui.VariableTab;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.util.SyntaxChecker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFrame;

public class CsvInterpretor {
  /*
   * Correct content of the Csv file (file format is conform RFC4180) must comply to:
   * 1) The first line must contain the names (Labels) of the inputs and the outputs
   *    there are 3 types of names:
   *    a) Just a string (e.g. "A", "MyNiceSignal"...) which indicates a 1 bit quantity
   *    b) A string followed by a : followed by a number (e.g. "A:3", "MyNiceSignal:5") which
   *       indicates the bit-number (indicated by the number) of the bit-vector indicated by
   *       the string. In this case you MUST adhere to the MSB...LSB rule (hence D:3,D:2,D:1,D:0
   *       is correct, but D:3,D:0,D:1,D:2 or D:0,D:1,D:2,D:3 are not!)
   *    c) A string followed by [<a>..0] or [<b>] (e.g. B[3..0], D[4]) indicate bit vectors
   *       where <a> indicates the MSB-index and <b> the number of bits in the vector. In this
   *       case the string indicates the MSB position and the following <a> resp. <b>-1 fields
   *       MUST be empty.
   *    To separate the inputs from the outputs the first line Must contain a separator field
   *    which contains a | character (extra characters are allowed).
   * 2) The following lines are the truth table entries. They must be formatted according to the
   *    following rules:
   *    a) Each line must have exactly the same number of entries as the first line.
   *    b) The separator filed may contain a | symbol, may be empty, or contain something else,
   *       it is ignored.
   *    c) Each field, other than the separator field must contain one of the following values;
   *       - 0 to indicate a logic 0
   *       - 1 to indicate a logic one
   *       - x,X, or - to indicate a don't care.
   *  3) empty lines are not allowed.
   *  4) Spaces are not allowed .
   *
   *    Example Csv file:
   *    "A","B[3..0]",,,,"|","D:3","D:2","D:1","D:0"
   *    0,0,0,-,0,"|",1,0,1,0
   *    0,0,0,0,1,"|",1,1,0,1
   *    0,0,0,1,1,"|",1,0,1,0
   *    0,0,1,0,0,"|",0,0,0,1
   *    0,0,1,0,1,"|",1,0,0,0
   *    0,0,1,1,0,"|",0,1,0,1
   *    0,0,1,1,1,"|",1,0,0,1
   *    0,1,0,0,0,"|",0,1,0,1
   *    0,1,0,0,1,"|",0,0,1,0
   *    0,1,0,1,0,"|",1,1,0,0
   *    0,1,0,1,1,"|",0,1,1,0
   *    0,1,1,0,0,"|",1,0,0,0
   *    0,1,1,0,1,"|",1,0,0,1
   *    0,1,1,1,0,"|",0,0,1,0
   *    0,1,1,1,1,"|",1,0,1,0
   *    1,-,-,-,-,"|",0,0,0,0
   */

  private ArrayList<List<String>> content;
  final JFrame parent;
  private final VariableList inputs;
  private final VariableList outputs;
  private final String fileName;

  public CsvInterpretor(File file, CsvParameter param, JFrame parent) {
    content = new ArrayList<>();
    inputs = new VariableList(AnalyzerModel.MAX_INPUTS);
    outputs = new VariableList(AnalyzerModel.MAX_OUTPUTS);
    fileName = file.getName();
    this.parent = parent;
    readFile(file, param);
    if (content.isEmpty()) {
      return;
    }
    if (!getInputsOutputs() || !checkEntries()) {
      content = new ArrayList<>();
    }
  }

  public void getTruthTable(AnalyzerModel model) throws IOException {
    if (content.size() <= 1) return;
    final var rows = new ArrayList<Entry[]>();
    var nrOfEntries = inputs.bits.size() + outputs.bits.size();
    for (int row = 1; row < content.size(); row++) {
      final var entryRow = new ArrayList<Entry>();
      final var line = content.get(row);
      var col = 0;
      while (col < line.size()) {
        if (col != inputs.bits.size()) {
          var entry = line.get(col);
          if ("-xX".indexOf(entry.charAt(0)) >= 0) {
            entryRow.add(Entry.DONT_CARE);
          } else if (entry.charAt(0) == '0') {
            entryRow.add(Entry.ZERO);
          } else if (entry.charAt(0) == '1') {
            entryRow.add(Entry.ONE);
          } else throw new IOException("Invalid entry value");
        }
        col++;
      }
      if (entryRow.size() != nrOfEntries) throw new IOException("Invalid nr of entries");
      rows.add(entryRow.toArray(new Entry[nrOfEntries]));
    }
    try {
      model.setVariables(inputs.vars, outputs.vars);
    } catch (IllegalArgumentException e) {
      throw new IOException(e.getMessage());
    }
    final var table = model.getTruthTable();
    try {
      table.setVisibleRows(rows, false);
    } catch (IllegalArgumentException e) {
      var confirm =
          OptionPane.showConfirmDialog(
              parent,
              new String[] {e.getMessage(), S.get("tableParseErrorMessage")},
              S.get("openButton"),
              OptionPane.YES_NO_OPTION);
      if (confirm != OptionPane.YES_OPTION) return;
      try {
        table.setVisibleRows(rows, true);
      } catch (IllegalArgumentException ex) {
        throw new IOException(ex.getMessage());
      }
    }
  }

  private boolean checkEntries() {
    if (content.size() == 1) {
      OptionPane.showMessageDialog(
          parent, S.get("CsvNoEntries", fileName), S.get("openButton"), OptionPane.ERROR_MESSAGE);
      return false;
    }
    for (int row = 1; row < content.size(); row++) {
      var col = 0;
      var line = content.get(row);
      while (col < line.size()) {
        /* we skip the seperator field */
        if (col != inputs.bits.size()) {
          var entry = line.get(col);
          if (entry == null || entry.length() != 1 || "01-xX".indexOf(entry.charAt(0)) < 0) {
            OptionPane.showMessageDialog(
                parent,
                S.get("CsvInvalidEntry", row + 1, fileName, entry, col + 1),
                S.get("openButton"),
                OptionPane.ERROR_MESSAGE);
            return false;
          }
        }
        col++;
      }
    }
    return true;
  }

  private boolean isDuplicate(String name) {
    for (Var v : inputs.vars) {
      if (v.name.equalsIgnoreCase(name)) {
        OptionPane.showMessageDialog(
            parent,
            S.get("CsvDuplicatedVar", 1, fileName, name),
            S.get("openButton"),
            OptionPane.ERROR_MESSAGE);
        return true;
      }
    }
    for (Var v : outputs.vars) {
      if (v.name.equalsIgnoreCase(name)) {
        OptionPane.showMessageDialog(
            parent,
            S.get("CsvDuplicatedVar", 1, fileName, name),
            S.get("openButton"),
            OptionPane.ERROR_MESSAGE);
        return true;
      }
    }
    return false;
  }

  private boolean isCorrectName(String name) {
    if (!SyntaxChecker.isVariableNameAcceptable(name, false)) {
      OptionPane.showMessageDialog(
          parent,
          S.get("CsvIncorrectVarName", 1, fileName, name),
          S.get("openButton"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;
  }

  private boolean getInputsOutputs() {
    /* first check: are all the lines the same size */
    final var header = content.get(0);
    final var nrOfEntries = header.size();
    for (int line = 1; line < content.size(); line++) {
      if (content.get(line).size() != nrOfEntries) {
        OptionPane.showMessageDialog(
            parent,
            S.get("CsvIncorrectLine", line + 1, fileName, content.get(line).size(), nrOfEntries),
            S.get("openButton"),
            OptionPane.ERROR_MESSAGE);
        return false;
      }
    }
    HashMap<String, ArrayList<Boolean>> bitspresent = new HashMap<>();
    var processingInputs = true;
    var inOuSepDetected = false;
    /* now read the cells */
    for (int idx = 0; idx < nrOfEntries; idx++) {
      final var field = header.get(idx);
      if (field == null) {
        OptionPane.showMessageDialog(
            parent,
            S.get("CsvIncorrectEmpty", 1, fileName, idx),
            S.get("openButton"),
            OptionPane.ERROR_MESSAGE);
        return false;
      }
      if (field.contains("|")) {
        processingInputs = false;
        inOuSepDetected = true;
        continue;
      }
      if (field.contains(":")) {
        /* Is B:<a> format */
        int pos = field.indexOf(":");
        final var name = field.substring(0, pos);
        if (!isCorrectName(name)) return false;
        final var index = field.substring(pos + 1);
        for (char kar : index.toCharArray()) {
          if ("0123456789".indexOf(kar) < 0) {
            OptionPane.showMessageDialog(
                parent,
                S.get("CsvIncorrectVarName", 1, fileName, field),
                S.get("openButton"),
                OptionPane.ERROR_MESSAGE);
            return false;
          }
        }
        final var bitIndex = Integer.parseInt(index);

        if (bitspresent.containsKey(name.toLowerCase())) {
          final var sels = bitspresent.get(name.toLowerCase());
          if (bitIndex >= sels.size() || !sels.get(bitIndex + 1)) {
            OptionPane.showMessageDialog(
                parent,
                S.get("CsvIncorrectBitOrder", 1, fileName, name),
                S.get("openButton"),
                OptionPane.ERROR_MESSAGE);
            return false;
          }
          if (sels.get(bitIndex)) {
            OptionPane.showMessageDialog(
                parent,
                S.get("CsvDuplicatedBit", 1, fileName, bitIndex, name),
                S.get("openButton"),
                OptionPane.ERROR_MESSAGE);
            return false;
          }
          sels.set(bitIndex, true);
        } else {
          if (isDuplicate(name)) return false;
          final var variable = new Var(name, bitIndex + 1);
          final var sels = new ArrayList<Boolean>();
          for (int a = 0; a < bitIndex; a++) {
            sels.add(false);
          }
          sels.add(true);
          bitspresent.put(name.toLowerCase(), sels);
          if (processingInputs) {
            inputs.add(variable);
          } else {
            outputs.add(variable);
          }
        }
      } else if (field.contains("[")) {
        /* check indexes and empty field */
        final var pos = field.indexOf('[');
        final var name = field.substring(0, pos);
        if (!isCorrectName(name)) return false;
        if (isDuplicate(name)) return false;
        final var nrOfBits = VariableTab.checkindex(field.substring(pos));
        if (nrOfBits <= 0) {
          OptionPane.showMessageDialog(
              parent,
              S.get("CsvIncorrectVarName", 1, fileName, field),
              S.get("openButton"),
              OptionPane.ERROR_MESSAGE);
          return false;
        }
        if (idx + nrOfBits > nrOfEntries) {
          OptionPane.showMessageDialog(
              parent,
              S.get("CsvNotEnoughEmpty", 1, fileName, field),
              S.get("openButton"),
              OptionPane.ERROR_MESSAGE);
          return false;
        }
        for (int x = 1; x < nrOfBits; x++) {
          if (header.get(idx + x) != null) {
            OptionPane.showMessageDialog(
                parent,
                S.get("CsvNotEnoughEmpty", 1, fileName, field),
                S.get("openButton"),
                OptionPane.ERROR_MESSAGE);
            return false;
          }
        }
        idx += nrOfBits - 1;
        final var variable = new Var(name, nrOfBits);
        if (processingInputs) {
          inputs.add(variable);
        } else {
          outputs.add(variable);
        }
      } else {
        if (!isCorrectName(field) || isDuplicate(field)) {
          return false;
        }
        final var variable = new Var(field, 1);
        if (processingInputs) {
          inputs.add(variable);
        } else {
          outputs.add(variable);
        }
      }
    }
    if (!inOuSepDetected) {
      OptionPane.showMessageDialog(
          parent,
          S.get("CsvNoSepFound", 1, fileName),
          S.get("openButton"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }
    if (inputs.bits.isEmpty()) {
      OptionPane.showMessageDialog(
          parent,
          S.get("CsvNoInputsFound", 1, fileName),
          S.get("openButton"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }
    for (String key : bitspresent.keySet()) {
      final var bit = bitspresent.get(key);
      for (int x = 0; x < bit.size(); x++) {
        if (!bit.get(x)) {
          OptionPane.showMessageDialog(
              parent,
              S.get("CsvBitNotSpecified", 1, fileName, x, key),
              S.get("openButton"),
              OptionPane.ERROR_MESSAGE);
          return false;
        }
      }
    }
    return true;
  }

  private void readFile(File file, CsvParameter param) {
    try {
      final var scanner = new Scanner(file);
      while (scanner.hasNext()) {
        content.add(parseCsvLine(scanner.next(), param.seperator(), param.quote()));
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      OptionPane.showMessageDialog(
          parent,
          S.get("cantReadMessage", file.getName()),
          S.get("openButton"),
          OptionPane.ERROR_MESSAGE);
    }
  }

  public static List<String> parseCsvLine(String line, char seperator, char quote) {
    var inQuote = false;
    var nrofcontquotes = 0;

    var working = new StringBuilder();
    List<String> result = new ArrayList<>();
    for (char kar : line.toCharArray()) {
      if (inQuote) {
        if (kar == quote) {
          nrofcontquotes++;
        } else {
          if (nrofcontquotes > 1) {
            var quotestoprint = nrofcontquotes >> 1;
            working.append(String.valueOf(quote).repeat(quotestoprint));
            nrofcontquotes -= quotestoprint << 1;
          }
          if (nrofcontquotes == 1) {
            inQuote = false;
            if (kar == seperator) {
              if (working.length() == 0) {
                result.add(null);
              } else {
                result.add(working.toString());
                working = new StringBuilder();
              }
            } else {
              working.append(kar);
            }
          } else {
            working.append(kar);
          }
          nrofcontquotes = 0;
        }
      } else {
        if (kar == seperator) {
          if (working.length() == 0) result.add(null);
          else {
            result.add(working.toString());
            working = new StringBuilder();
          }
        } else if (kar == quote) {
          inQuote = true;
        } else if (kar == '\r') {
          continue;
        } else if (kar == '\n') {
          break;
        } else working.append(kar);
      }
    }
    result.add(working.length() > 0 ? working.toString() : null);

    return result;
  }
}
