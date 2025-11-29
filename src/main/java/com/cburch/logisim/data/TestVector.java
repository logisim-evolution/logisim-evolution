/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.filechooser.FileFilter;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class TestVector {

  private static class TestVectorFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      if (!f.isFile()) return true;

      String name = f.getName();
      int i = name.lastIndexOf('.');
      return (i > 0 && name.substring(i).equalsIgnoreCase(".txt"));
    }

    @Override
    public String getDescription() {
      return "Logisim-evolution Test Vector (*.txt)";
    }
  }

  private class TestVectorReader {
    private final BufferedReader in;
    private StringTokenizer curLine;
    private int setColumnIndex = -1;
    private int seqColumnIndex = -1;
    private int iterColumnIndex = -1;

    public TestVectorReader(BufferedReader in) throws IOException {
      this.in = in;
      curLine = findNonemptyLine();
    }

    private StringTokenizer findNonemptyLine() throws IOException {
      var line = in.readLine();

      while (line != null) {
        final var i = line.indexOf('#');
        if (i >= 0) line = line.substring(0, i);
        final var ret = new StringTokenizer(line);
        if (ret.hasMoreTokens()) return ret;
        line = in.readLine();
      }

      return null;
    }

    public void parse() throws IOException {
      if (curLine == null) throw new IOException("TestVector format error: empty file");

      parseHeader();
      TestVector.this.data = new ArrayList<>();
      final var localDontCareFlags = new ArrayList<boolean[]>();
      final var localFloatingFlags = new ArrayList<boolean[]>();
      final var localSetNumbers = new ArrayList<Integer>();
      final var localSeqNumbers = new ArrayList<Integer>();
      final var localIterNumbers = new ArrayList<Integer>();
      curLine = findNonemptyLine();

      while (curLine != null) {
        parseData(localDontCareFlags, localFloatingFlags, localSetNumbers, localSeqNumbers, localIterNumbers);
        curLine = findNonemptyLine();
      }
      
      // Convert lists to arrays
      TestVector.this.setNumbers = new int[localSetNumbers.size()];
      for (int i = 0; i < localSetNumbers.size(); i++) {
        TestVector.this.setNumbers[i] = localSetNumbers.get(i);
      }
      TestVector.this.seqNumbers = new int[localSeqNumbers.size()];
      for (int i = 0; i < localSeqNumbers.size(); i++) {
        TestVector.this.seqNumbers[i] = localSeqNumbers.get(i);
      }
      TestVector.this.iterNumbers = new int[localIterNumbers.size()];
      for (int i = 0; i < localIterNumbers.size(); i++) {
        TestVector.this.iterNumbers[i] = localIterNumbers.get(i);
      }
      TestVector.this.dontCareFlags = localDontCareFlags;
      TestVector.this.floatingFlags = localFloatingFlags;
      
      // Initialize to empty arrays if no data (backward compatibility)
      if (TestVector.this.setNumbers == null) {
        TestVector.this.setNumbers = new int[0];
      }
      if (TestVector.this.seqNumbers == null) {
        TestVector.this.seqNumbers = new int[0];
      }
      if (TestVector.this.iterNumbers == null) {
        TestVector.this.iterNumbers = new int[0];
      }
    }

    private void parseData(
        List<boolean[]> localDontCareFlags,
        List<boolean[]> localFloatingFlags,
        List<Integer> localSetNumbers,
        List<Integer> localSeqNumbers,
        List<Integer> localIterNumbers) throws IOException {
      final var vals = new Value[columnName.length];
      final var dcFlags = new boolean[columnName.length];
      final var floatFlags = new boolean[columnName.length];
      int setValue = 0;
      int seqValue = 0;
      int iterValue = 1; // Default to 1 iteration
      
      // Collect all tokens first
      final var tokens = new ArrayList<String>();
      while (curLine.hasMoreTokens()) {
        tokens.add(curLine.nextToken());
      }
      
      // Map tokens to columns based on original header positions
      int pinIndex = 0;
      for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
        final var t = tokens.get(tokenIndex);
        
        // Check if this position corresponds to set, seq, or iter column
        if (setColumnIndex >= 0 && tokenIndex == setColumnIndex) {
          try {
            setValue = Integer.parseInt(t);
          } catch (NumberFormatException e) {
            throw new IOException("Test Vector data format error: invalid set value: " + t);
          }
          continue;
        }
        if (seqColumnIndex >= 0 && tokenIndex == seqColumnIndex) {
          try {
            seqValue = Integer.parseInt(t);
          } catch (NumberFormatException e) {
            throw new IOException("Test Vector data format error: invalid seq value: " + t);
          }
          continue;
        }
        if (iterColumnIndex >= 0 && tokenIndex == iterColumnIndex) {
          try {
            iterValue = Integer.parseInt(t);
            if (iterValue < 1) {
              throw new IOException("Test Vector data format error: invalid iter value: " + t + " (must be >= 1)");
            }
          } catch (NumberFormatException e) {
            throw new IOException("Test Vector data format error: invalid iter value: " + t);
          }
          continue;
        }
        
        // This is a pin column
        if (pinIndex >= columnName.length) {
          throw new IOException("Test Vector data format error: too many values");
        }
        
        // Check for special values
        final var tUpper = t.toUpperCase();
        if ("<DC>".equals(tUpper)) {
          dcFlags[pinIndex] = true;
          vals[pinIndex] = Value.UNKNOWN; // Placeholder, won't be compared
        } else if ("<FLOAT>".equals(tUpper)) {
          floatFlags[pinIndex] = true;
          vals[pinIndex] = Value.UNKNOWN;
        } else {
          try {
            vals[pinIndex] = Value.fromLogString(columnWidth[pinIndex], t);
          } catch (Exception e) {
            String errorMsg = e.getMessage();
            // Enhance error message with column name if it's a "too many bits" error
            if (errorMsg != null && errorMsg.contains("Too many bits") && errorMsg.contains("did you mean [")) {
              // Extract the suggested bit width from the error message
              int startIdx = errorMsg.indexOf("did you mean [");
              int endIdx = errorMsg.indexOf("]", startIdx);
              if (startIdx >= 0 && endIdx > startIdx) {
                String bitWidthStr = errorMsg.substring(startIdx + "did you mean [".length(), endIdx);
                // Replace with column name and suggested bit width
                errorMsg = errorMsg.substring(0, startIdx) + "did you mean " + columnName[pinIndex] + "[" + bitWidthStr + "]?" + errorMsg.substring(endIdx + 1);
              }
            }
            throw new IOException("Test Vector data format error: " + errorMsg);
          }
        }
        
        if (TestVector.this.data.isEmpty()) {
          if (!dcFlags[pinIndex] && !floatFlags[pinIndex]) {
            columnRadix[pinIndex] = Value.radixOfLogString(columnWidth[pinIndex], t);
          }
        }
        pinIndex++;
      }
      
      if (pinIndex < columnName.length) {
        throw new IOException("Test Vector data format error: not enough values");
      }
      
      TestVector.this.data.add(vals);
      localDontCareFlags.add(dcFlags);
      localFloatingFlags.add(floatFlags);
      localSetNumbers.add(setValue);
      localSeqNumbers.add(seqValue);
      localIterNumbers.add(iterValue);
    }

    private void parseHeader() throws IOException {
      final var n = curLine.countTokens();
      final var tempColumnName = new ArrayList<String>();
      final var tempColumnWidth = new ArrayList<BitWidth>();
      final var tempColumnRadix = new ArrayList<Integer>();

      for (var i = 0; i < n; i++) {
        final var t = (String) curLine.nextElement();
        final var tUpper = t.toUpperCase();
        
        // Check for special columns
        if ("<SET>".equals(tUpper)) {
          setColumnIndex = i;
          continue;
        }
        if ("<SEQ>".equals(tUpper)) {
          seqColumnIndex = i;
          continue;
        }
        if ("<ITER>".equals(tUpper)) {
          iterColumnIndex = i;
          continue;
        }
        
        // Regular pin column
        int s = t.indexOf('[');
        if (s < 0) {
          tempColumnName.add(t);
          tempColumnWidth.add(BitWidth.ONE);
          tempColumnRadix.add(2);
        } else {
          final var e = t.indexOf(']');

          if (e != t.length() - 1 || s == 0 || e == s + 1)
            throw new IOException("Test Vector header format error: bad spec: " + t);

          tempColumnName.add(t.substring(0, s));
          var w = 0;
          try {
            w = Integer.parseInt(t.substring(s + 1, e));
          } catch (NumberFormatException ignored) {
          }

          if (w < 1 || w > 64)
            throw new IOException("Test Vector header format error: bad width: " + t);
          tempColumnWidth.add(BitWidth.create(w));
          tempColumnRadix.add(2);
        }
      }
      
      // Convert lists to arrays
      columnName = tempColumnName.toArray(new String[0]);
      columnWidth = tempColumnWidth.toArray(new BitWidth[0]);
      columnRadix = new int[tempColumnRadix.size()];
      for (int i = 0; i < tempColumnRadix.size(); i++) {
        columnRadix[i] = tempColumnRadix.get(i);
      }
    }
  }

  public static final FileFilter FILE_FILTER = new TestVectorFilter();
  public String[] columnName;
  public BitWidth[] columnWidth;
  public int[] columnRadix;

  public List<Value[]> data;
  public int[] setNumbers;
  public int[] seqNumbers;
  public int[] iterNumbers;
  private List<boolean[]> dontCareFlags;
  private List<boolean[]> floatingFlags;

  public TestVector(File src) throws IOException {
    try (final var in = new BufferedReader(new FileReader(src))) {
      final var r = new TestVectorReader(in);
      r.parse();
    }
  }

  public TestVector(String filename) throws IOException {
    this(new File(filename));
  }

  /**
   * Check if a value at the given row and column is marked as don't care.
   *
   * @param rowIndex The row index (0-based)
   * @param columnIndex The column index (0-based)
   * @return true if the value is don't care, false otherwise
   */
  public boolean isDontCare(int rowIndex, int columnIndex) {
    if (dontCareFlags == null || rowIndex < 0 || rowIndex >= dontCareFlags.size()) {
      return false;
    }
    final var flags = dontCareFlags.get(rowIndex);
    if (flags == null || columnIndex < 0 || columnIndex >= flags.length) {
      return false;
    }
    return flags[columnIndex];
  }

  /**
   * Check if a value at the given row and column is marked as floating.
   *
   * @param rowIndex The row index (0-based)
   * @param columnIndex The column index (0-based)
   * @return true if the value is floating, false otherwise
   */
  public boolean isFloating(int rowIndex, int columnIndex) {
    if (floatingFlags == null || rowIndex < 0 || rowIndex >= floatingFlags.size()) {
      return false;
    }
    final var flags = floatingFlags.get(rowIndex);
    if (flags == null || columnIndex < 0 || columnIndex >= flags.length) {
      return false;
    }
    return flags[columnIndex];
  }

  /**
   * Get the number of propagation iterations for a given row.
   * Defaults to 1 if not specified.
   *
   * @param rowIndex The row index (0-based)
   * @return The number of iterations (defaults to 1)
   */
  public int getIterations(int rowIndex) {
    if (iterNumbers == null || rowIndex < 0 || rowIndex >= iterNumbers.length) {
      return 1; // Default to 1 iteration
    }
    return iterNumbers[rowIndex];
  }
}
