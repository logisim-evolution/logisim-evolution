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

package com.cburch.logisim.gui.hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.cburch.hex.HexModel;

public class HexFile {
	private static class HexReader {
		private BufferedReader in;
		private int[] data;
		private StringTokenizer curLine;
		private long leftCount;
		private long leftValue;

		public HexReader(BufferedReader in) throws IOException {
			this.in = in;
			data = new int[4096];
			curLine = findNonemptyLine();
		}

		private StringTokenizer findNonemptyLine() throws IOException {
			String line = in.readLine();
			while (line != null) {
				int index = line.indexOf(COMMENT_MARKER);
				if (index >= 0) {
					line = line.substring(0, index);
				}

				StringTokenizer ret = new StringTokenizer(line);
				if (ret.hasMoreTokens())
					return ret;
				line = this.in.readLine();
			}
			return null;
		}

		public boolean hasNext() throws IOException {
			if (leftCount > 0) {
				return true;
			} else if (curLine != null && curLine.hasMoreTokens()) {
				return true;
			} else {
				curLine = findNonemptyLine();
				return curLine != null;
			}
		}

		public int[] next() throws IOException {
			int pos = 0;
			if (leftCount > 0) {
				int n = (int) Math.min(data.length - pos, leftCount);
				if (n == 1) {
					data[pos] = (int) leftValue;
					pos++;
					leftCount--;
				} else {
					Arrays.fill(data, pos, pos + n, (int) leftValue);
					pos += n;
					leftCount -= n;
				}
			}
			if (pos >= data.length)
				return data;

			for (String tok = nextToken(); tok != null; tok = nextToken()) {
				try {
					int star = tok.indexOf("*");
					if (star < 0) {
						leftCount = 1;
						leftValue = Long.parseLong(tok, 16);
					} else {
						leftCount = Long.parseLong(tok.substring(0, star));
						leftValue = Long.parseLong(tok.substring(star + 1), 16);
					}
				} catch (NumberFormatException e) {
					throw new IOException(Strings.get("hexNumberFormatError"));
				}

				int n = (int) Math.min(data.length - pos, leftCount);
				if (n == 1) {
					data[pos] = (int) leftValue;
					pos++;
					leftCount--;
				} else {
					Arrays.fill(data, pos, pos + n, (int) leftValue);
					pos += n;
					leftCount -= n;
				}
				if (pos >= data.length)
					return data;
			}

			if (pos >= data.length) {
				return data;
			} else {
				int[] ret = new int[pos];
				System.arraycopy(data, 0, ret, 0, pos);
				return ret;
			}
		}

		private String nextToken() throws IOException {
			if (curLine != null && curLine.hasMoreTokens()) {
				return curLine.nextToken();
			} else {
				curLine = findNonemptyLine();
				return curLine == null ? null : curLine.nextToken();
			}
		}
	}

	public static void open(HexModel dst, File src) throws IOException {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(src));
		} catch (IOException e) {
			throw new IOException(Strings.get("hexFileOpenError"));
		}
		try {
			String header = in.readLine();
			if (!header.equals(RAW_IMAGE_HEADER)) {
				throw new IOException(Strings.get("hexHeaderFormatError"));
			}
			open(dst, in);
			try {
				BufferedReader oldIn = in;
				in = null;
				oldIn.close();
			} catch (IOException e) {
				throw new IOException(Strings.get("hexFileReadError"));
			}
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	public static void open(HexModel dst, Reader in) throws IOException {
		HexReader reader = new HexReader(new BufferedReader(in));
		long offs = dst.getFirstOffset();
		while (reader.hasNext()) {
			int[] values = reader.next();
			if (offs + values.length - 1 > dst.getLastOffset()) {
				throw new IOException(Strings.get("hexFileSizeError"));
			}
			dst.set(offs, values);
			offs += values.length;
		}
		dst.fill(offs, dst.getLastOffset() - offs + 1, 0);
	}

	public static int[] parse(Reader in) throws IOException {
		HexReader reader = new HexReader(new BufferedReader(in));
		int cur = 0;
		int[] data = new int[4096];
		while (reader.hasNext()) {
			int[] values = reader.next();
			if (cur + values.length > data.length) {
				int[] oldData = data;
				data = new int[Math.max(cur + values.length,
						3 * data.length / 2)];
				System.arraycopy(oldData, 0, data, 0, cur);
			}
			System.arraycopy(values, 0, data, cur, values.length);
			cur += values.length;
		}
		if (cur != data.length) {
			int[] oldData = data;
			data = new int[cur];
			System.arraycopy(oldData, 0, data, 0, cur);
		}
		return data;
	}

	public static void save(File dst, HexModel src) throws IOException {
		FileWriter out;
		try {
			out = new FileWriter(dst);
		} catch (IOException e) {
			throw new IOException(Strings.get("hexFileOpenError"));
		}
		try {
			try {
				out.write(RAW_IMAGE_HEADER + "\n");
			} catch (IOException e) {
				throw new IOException(Strings.get("hexFileWriteError"));
			}
			save(out, src);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				throw new IOException(Strings.get("hexFileWriteError"));
			}
		}
	}

	public static void save(Writer out, HexModel src) throws IOException {
		long first = src.getFirstOffset();
		long last = src.getLastOffset();
		while (last > first && src.get(last) == 0)
			last--;
		int tokens = 0;
		long cur = 0;
		while (cur <= last) {
			int val = src.get(cur);
			long start = cur;
			cur++;
			while (cur <= last && src.get(cur) == val)
				cur++;
			long len = cur - start;
			if (len < 4) {
				cur = start + 1;
				len = 1;
			}
			try {
				if (tokens > 0)
					out.write(tokens % 8 == 0 ? '\n' : ' ');
				if (cur != start + 1)
					out.write((cur - start) + "*");
				out.write(Integer.toHexString(val));
			} catch (IOException e) {
				throw new IOException(Strings.get("hexFileWriteError"));
			}
			tokens++;
		}
		if (tokens > 0)
			out.write('\n');
	}

	private static final String RAW_IMAGE_HEADER = "v2.0 raw";

	private static final String COMMENT_MARKER = "#";

	private HexFile() {
	}
}
