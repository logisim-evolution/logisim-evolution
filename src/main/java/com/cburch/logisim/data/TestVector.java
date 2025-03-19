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
 * Represents the data contents of a Logisim Evolution test vector file,
 * Also provides functionality for parsing test vector files from disk.
 * <p>
 * The contents of the test vector are made available through the fields <code>columnName</code>,
 * <code>columnWidth</code>, <code>columnRadix</code> and <code>data</code>.
 * Each of these is an * array containing column information, in order.
 * They all have the same length.
 * Code taken from <a href="http://www.cs.cornell.edu/courses/cs3410/2015sp/">Cornell's version of Logisim</a>
 */
public class TestVector {

  private static class TestVectorFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      if (!f.isFile()) return true;

      final var name = f.getName();
      final var i = name.lastIndexOf('.');
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
      data = new ArrayList<>();
      curLine = findNonemptyLine();

      while (curLine != null) {
        parseData();
        curLine = findNonemptyLine();
      }
    }

    private void parseData() throws IOException {
      final var vals = new Value[columnName.length];
      for (var i = 0; i < columnName.length; i++) {
        final var t = curLine.nextToken();

        try {
          vals[i] = Value.fromLogString(columnWidth[i], t);
        } catch (Exception e) {
          throw new IOException("Test Vector data format error: " + e.getMessage());
        }
        if (data.isEmpty()) columnRadix[i] = Value.radixOfLogString(columnWidth[i], t);
      }
      if (curLine.hasMoreTokens())
        throw new IOException("Test Vector data format error: " + curLine.nextToken());
      data.add(vals);
    }

    private void parseHeader() throws IOException {
      final var n = curLine.countTokens();
      columnName = new String[n];
      columnWidth = new BitWidth[n];
      columnRadix = new int[n];

      for (var i = 0; i < n; i++) {
        columnRadix[i] = 2;
        final var t = (String) curLine.nextElement();
        int s = t.indexOf('[');

        if (s < 0) {
          columnName[i] = t;
          columnWidth[i] = BitWidth.ONE;
        } else {
          final var e = t.indexOf(']');

          if (e != t.length() - 1 || s == 0 || e == s + 1)
            throw new IOException("Test Vector header format error: bad spec: " + t);

          columnName[i] = t.substring(0, s);
          var w = 0;
          try {
            w = Integer.parseInt(t.substring(s + 1, e));
          } catch (NumberFormatException ignored) {
          }

          if (w < 1 || w > 64)
            throw new IOException("Test Vector header format error: bad width: " + t);
          columnWidth[i] = BitWidth.create(w);
        }
      }
    }
  }

  /**
   * A java FileFilter that accepts Logisim Evoltion test vector files.
   */
  public static final FileFilter FILE_FILTER = new TestVectorFilter();
  /**
   * An array containing the names of the test vector columns.
   */
  public String[] columnName;
  /**
   * An array containing the bit widths of the test vector columns.
   */
  public BitWidth[] columnWidth;
  /**
   * An array containing the numerical radix of the string representation
   * of the test vector columns.
   */
  public int[] columnRadix;
  /**
   * An array containing data values represented by the test vector columns.
   */
  public List<Value[]> data;

  /**
   * Constructs a TestVector object by parsing the contents of the given File.
   *
   * @param src the File object to read from.
   */
  public TestVector(File src) throws IOException {
    try (final var in = new BufferedReader(new FileReader(src))) {
      final var r = new TestVectorReader(in);
      r.parse();
    }
  }

  /**
   * Constructs a TestVector object by parsing the contents of the file with the given filename.
   *
   * @param filename the path of the file to read from.
   */
  public TestVector(String filename) throws IOException {
    this(new File(filename));
  }
}
