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
      curLine = findNonemptyLine();

      while (curLine != null) {
        parseData(localDontCareFlags, localFloatingFlags, localSetNumbers, localSeqNumbers);
        curLine = findNonemptyLine();
      }

      // Verify set and sequence order.
      int lastSet = 0;
      int lastSeq = 0;
      for (int i = 0; i < localSetNumbers.size(); i++) {
        final var thisSet = localSetNumbers.get(i);
        final var thisSeq = localSeqNumbers.get(i);
        if (thisSet < lastSet) {
          throw new IOException("<Set> numbers out of order: " + lastSet + " before " + thisSet);
        }
        if (thisSet == lastSet && (thisSet > 0 && thisSeq <= lastSeq)) {
          throw new IOException("<Seq> numbers out of order: " + lastSeq + " before " + thisSeq
              + " in set " + thisSet);
        }
        if (thisSet == 0 && thisSeq != 0) {
          throw new IOException("<Set> is 0 but <Seq> is " + thisSeq + ", not 0");
        }
        if (thisSet != 0 && thisSeq == 0) {
          throw new IOException("<Set> is " + thisSet + " which not 0 but <Seq> is 0");
        }
        lastSet = thisSet;
        lastSeq = thisSeq;
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
      TestVector.this.dontCareFlags = localDontCareFlags;
      TestVector.this.floatingFlags = localFloatingFlags;

    }

    private void parseData(
        List<boolean[]> localDontCareFlags,
        List<boolean[]> localFloatingFlags,
        List<Integer> localSetNumbers,
        List<Integer> localSeqNumbers) throws IOException {
      final var vals = new Value[columnName.length];
      final var dcFlags = new boolean[columnName.length];
      final var floatFlags = new boolean[columnName.length];
      int setValue = 0;
      int seqValue = 0;

      // Collect all tokens first
      final var tokens = new ArrayList<String>();
      while (curLine.hasMoreTokens()) {
        tokens.add(curLine.nextToken());
      }

      // Map tokens to columns based on original header positions
      int pinIndex = 0;
      for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
        final var t = tokens.get(tokenIndex);

        // Check if this position corresponds to set, seq column
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
          vals[pinIndex] = Value.createUnknown(columnWidth[pinIndex]);
        } else {
          try {
            vals[pinIndex] = fromLogString(columnWidth[pinIndex], t);
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
            columnRadix[pinIndex] = radixOfLogString(columnWidth[pinIndex], t);
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

  public String specialColumnEntry(int i) {
    if (floatingFlags != null) {
      for (int row = 0; row < floatingFlags.size(); row++) {
        if (isFloating(row, i)) {
          return "<FLOAT>";
        }
      }
    }
    if (dontCareFlags != null) {
      for (int row = 0; row < dontCareFlags.size(); row++) {
        if (isDontCare(row, i)) {
          return "<DC>";
        }
      }
    }
    return null;
  }

    /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static Value fromLogString(BitWidth width, String t) throws Exception {
    // Strip underscores from the string for readability (e.g., 0x0000_1111 -> 0x00001111)
    // This must be done before radix detection since radixOfLogString uses length
    final var sb = new StringBuilder(t.length());
    for (int i = 0; i < t.length(); i++) {
      final var c = t.charAt(i);
      if (c != '_') {
        sb.append(c);
      }
    }
    final var cleaned = sb.toString();

    final var radix = radixOfLogString(width, cleaned);
    int offset;

    if (radix == 16 && cleaned.startsWith("0x")) offset = 2;
    else if (radix == 8 && cleaned.startsWith("0o")) offset = 2;
    else if (radix == 2 && cleaned.startsWith("0b")) offset = 2;
    else if (radix == 10 && cleaned.startsWith("-")) offset = 1;
    else offset = 0;

    int n = cleaned.length();

    if (n <= offset) throw new Exception("expected digits");

    int w = width.getWidth();
    long value = 0;
    long unknown = 0;

    for (var i = offset; i < n; i++) {
      final var c = cleaned.charAt(i);
      int d;

      if (c == 'x' && radix != 10) d = -1;
      else if ('0' <= c && c <= '9') d = c - '0';
      else if ('a' <= c && c <= 'f') d = 0xa + (c - 'a');
      else if ('A' <= c && c <= 'F') d = 0xA + (c - 'A');
      else
        throw new Exception(
            "Unexpected character '" + cleaned.charAt(i) + "' in \"" + t + "\"");

      if (d >= radix)
        throw new Exception("Unexpected character '" + cleaned.charAt(i) + "' in \"" + t + "\"");

      value *= radix;
      unknown *= radix;

      if (radix != 10) {
        if (d == -1) unknown |= (radix - 1);
        else value |= d;
      } else {
        if (d == -1) unknown += (radix - 1);
        else value += d;
      }
    }
    if (radix == 10 && cleaned.charAt(0) == '-') {
      value = -value;
    }

    // Check bit width - for signed values, check the range instead of bit shift
    if (w == 64) {
      if (((value & 0x7FFFFFFFFFFFFFFFL) >> (w - 1)) != 0) {
        int actualBits = 64 - Long.numberOfLeadingZeros(value & 0x7FFFFFFFFFFFFFFFL);
        throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
            + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : ""));
      }
    } else {
      // For signed decimal, check if value fits in w-bit signed range
      if (radix == 10) {
        long maxPositive = (1L << (w - 1)) - 1;
        long minNegative = -(1L << (w - 1));
        if (value > maxPositive || value < minNegative) {
          // Calculate actual bits needed (for absolute value)
          long absValue = value < 0 ? -value : value;
          int actualBits = absValue == 0 ? 1 : 64 - Long.numberOfLeadingZeros(absValue) + 1; // +1 for sign bit
          throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
              + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : ""));
        }
        // Mask to width for signed values (two's complement representation)
        long mask = (1L << w) - 1;
        value &= mask;
      } else {
        // For unsigned (hex, octal, binary), use bit shift check
        if ((value >> w) != 0) {
          // Calculate actual bits needed
          int actualBits = value == 0 ? 1 : 64 - Long.numberOfLeadingZeros(value);
          String reminder = "";

          // For hex values, suggest based on number of hex digits * 4 (each hex digit = 4 bits)
          if (radix == 16 && cleaned.length() > 2) {
            int hexDigits = cleaned.length() - 2; // Subtract "0x" prefix
            // Use hex digits * 4 as the suggested bit width (each hex digit = 4 bits)
            actualBits = hexDigits * 4;
            reminder = " Remember that 0x means hex and each hex digit is 4 bits";
          } else if (radix == 2 && cleaned.length() > 0) {
            // For binary values, suggest based on number of binary digits (each binary digit = 1 bit)
            int binaryDigits = cleaned.length() - (cleaned.startsWith("0b") ? 2 : 0); // Subtract "0b" prefix if present
            // Use binary digits as the suggested bit width (each binary digit = 1 bit)
            actualBits = binaryDigits;
            reminder = " Remember that 0b means binary and each binary digit is 1 bit";
          } else if (radix == 8 && cleaned.length() > 2) {
            // For octal values, suggest based on number of octal digits * 3 (each octal digit = 3 bits)
            int octalDigits = cleaned.length() - 2; // Subtract "0o" prefix
            // Use octal digits * 3 as the suggested bit width (each octal digit = 3 bits)
            actualBits = octalDigits * 3;
            reminder = " Remember that 0o means octal and each octal digit is 3 bits";
          }

          throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
              + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : "") + reminder);
        }
      }
    }

    unknown &= ((1L << w) - 1);
    return Value.create_unsafe(w, 0, unknown, value);
  }

  private static int radixOfLogString(BitWidth width, String t) {
    if (t.startsWith("0x")) return 16;
    if (t.startsWith("0o")) return 8;
    if (t.startsWith("0b")) return 2;
    if (t.length() == width.getWidth()) return 2;
    return 10;
  }
}
