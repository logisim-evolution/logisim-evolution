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

package com.cburch.logisim.data;

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.filechooser.FileFilter;

public class TestVector {

	private static class TestVectorFilter extends FileFilter {

		public boolean accept(File f) {
			if (!f.isFile())
				return true;

			String name = f.getName();
			int i = name.lastIndexOf('.');
			return (i > 0 && name.substring(i).toLowerCase().equals(".txt"));
		}

		public String getDescription() {
			return "Logisim-evolution Test Vector (*.txt)";
		}

	}

	private class TestVectorReader {

		private BufferedReader in;
		private StringTokenizer curLine;

		public TestVectorReader(BufferedReader in) throws IOException {
			this.in = in;
			curLine = findNonemptyLine();
		}

		private StringTokenizer findNonemptyLine() throws IOException {
			String line = in.readLine();

			while (line != null) {
				int i = line.indexOf('#');
				if (i >= 0)
					line = line.substring(0, i);
				StringTokenizer ret = new StringTokenizer(line);
				if (ret.hasMoreTokens())
					return ret;
				line = in.readLine();
			}

			return null;
		}

		public void parse() throws IOException {
			if (curLine == null)
				throw new IOException("TestVector format error: empty file");

			parseHeader();
			data = new ArrayList<Value[]>();
			curLine = findNonemptyLine();

			while (curLine != null) {
				parseData();
				curLine = findNonemptyLine();
			}
		}

		private void parseData() throws IOException {
			Value vals[] = new Value[columnName.length];
			for (int i = 0; i < columnName.length; i++) {
				String t = curLine.nextToken();

				try {
					vals[i] = Value.fromLogString(columnWidth[i], t);
				} catch (Exception e) {
					throw new IOException("Test Vector data format error: "
							+ e.getMessage());
				}
				if (data.isEmpty())
					columnRadix[i] = Value.radixOfLogString(columnWidth[i], t);
			}
			if (curLine.hasMoreTokens())
				throw new IOException("Test Vector data format error: "
						+ curLine.nextToken());
			data.add(vals);
		}

		private void parseHeader() throws IOException {
			int n = curLine.countTokens();
			columnName = new String[n];
			columnWidth = new BitWidth[n];
			columnRadix = new int[n];

			for (int i = 0; i < n; i++) {
				columnRadix[i] = 2;
				String t = (String) curLine.nextElement();
				int s = t.indexOf('[');

				if (s < 0) {
					columnName[i] = t;
					columnWidth[i] = BitWidth.ONE;
				} else {
					int e = t.indexOf(']');

					if (e != t.length() - 1 || s == 0 || e == s + 1)
						throw new IOException(
								"Test Vector header format error: bad spec: "
										+ t);

					columnName[i] = t.substring(0, s);
					int w = new Integer(t.substring(s + 1, e)).intValue();

					if (w < 1 || w > 32)
						throw new IOException(
								"Test Vector header format error: bad width: "
										+ t);
					columnWidth[i] = BitWidth.create(w);
				}
			}
		}

	}
	public static final FileFilter FILE_FILTER = new TestVectorFilter();
	public String[] columnName;
	public BitWidth[] columnWidth;
	public int[] columnRadix;

	public ArrayList<Value[]> data;

	public TestVector(File src) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(src));

		try {
			TestVectorReader r = new TestVectorReader(in);
			r.parse();
		} finally {
			in.close();
		}
	}

	public TestVector(String filename) throws IOException {
		this(new File(filename));
	}

}
