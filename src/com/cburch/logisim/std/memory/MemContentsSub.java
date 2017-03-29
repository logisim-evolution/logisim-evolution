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

package com.cburch.logisim.std.memory;

import java.util.Arrays;

import com.cburch.logisim.prefs.AppPreferences;

class MemContentsSub {
	private static class ByteContents extends ContentsInterface {
		private byte[] data;

		public ByteContents(int size, int mask) {
			data = new byte[size];
			if (AppPreferences.Memory_Startup_Unknown.get()) {
				java.util.Random generator = new java.util.Random();
				for (int i = 0 ; i < size ; i++) {
					data[i]= (byte)(generator.nextInt(256)&mask);
				}
			}
		}

		@Override
		void clear() {
			Arrays.fill(data, (byte) 0);
		}

		@Override
		public ByteContents clone() {
			ByteContents ret = (ByteContents) super.clone();
			ret.data = new byte[this.data.length];
			System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
			return ret;
		}

		@Override
		int get(int addr) {
			return addr >= 0 && addr < data.length ? data[addr] : 0;
		}

		//
		// methods for accessing data within memory
		//
		@Override
		int getLength() {
			return data.length;
		}

		@Override
		void load(int start, int[] values, int mask) {
			int n = Math.min(values.length, data.length - start);
			for (int i = 0; i < n; i++) {
				data[start + i] = (byte) (values[i] & mask);
			}
		}

		@Override
		void set(int addr, int value) {
			if (addr >= 0 && addr < data.length) {
				byte oldValue = data[addr];
				if (value != oldValue) {
					data[addr] = (byte) value;
				}
			}
		}
	}

	static abstract class ContentsInterface implements Cloneable {
		abstract void clear();

		@Override
		public ContentsInterface clone() {
			try {
				return (ContentsInterface) super.clone();
			} catch (CloneNotSupportedException e) {
				return this;
			}
		}

		abstract int get(int addr);

		int[] get(int start, int len) {
			int[] ret = new int[len];
			for (int i = 0; i < ret.length; i++)
				ret[i] = get(start + i);
			return ret;
		}

		abstract int getLength();

		boolean isClear() {
			for (int i = 0, n = getLength(); i < n; i++) {
				if (get(i) != 0)
					return false;
			}
			return true;
		}

		abstract void load(int start, int[] values, int mask);

		boolean matches(int[] values, int start, int mask) {
			for (int i = 0; i < values.length; i++) {
				if (get(start + i) != (values[i] & mask))
					return false;
			}
			return true;
		}

		abstract void set(int addr, int value);
	}

	private static class IntContents extends ContentsInterface {
		private int[] data;

		public IntContents(int size,int mask) {
			data = new int[size];
			if (AppPreferences.Memory_Startup_Unknown.get()) {
				java.util.Random generator = new java.util.Random();
				for (int i = 0 ; i < size ; i++)
					data[i]= (int)generator.nextInt()&mask;
			}
		}

		@Override
		void clear() {
			Arrays.fill(data, 0);
		}

		@Override
		public IntContents clone() {
			IntContents ret = (IntContents) super.clone();
			ret.data = new int[this.data.length];
			System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
			return ret;
		}

		@Override
		int get(int addr) {
			return addr >= 0 && addr < data.length ? data[addr] : 0;
		}

		//
		// methods for accessing data within memory
		//
		@Override
		int getLength() {
			return data.length;
		}

		@Override
		void load(int start, int[] values, int mask) {
			int n = Math.min(values.length, data.length - start);
			for (int i = 0; i < n; i++) {
				data[i] = values[i] & mask;
			}
		}

		@Override
		void set(int addr, int value) {
			if (addr >= 0 && addr < data.length) {
				int oldValue = data[addr];
				if (value != oldValue) {
					data[addr] = value;
				}
			}
		}
	}

	private static class ShortContents extends ContentsInterface {
		private short[] data;

		public ShortContents(int size, int mask) {
			data = new short[size];
			if (AppPreferences.Memory_Startup_Unknown.get()) {
				java.util.Random generator = new java.util.Random();
				for (int i = 0 ; i < size ; i++)
					data[i]= (short)(generator.nextInt(1<<16)&mask);
			}
		}

		@Override
		void clear() {
			Arrays.fill(data, (short) 0);
		}

		@Override
		public ShortContents clone() {
			ShortContents ret = (ShortContents) super.clone();
			ret.data = new short[this.data.length];
			System.arraycopy(this.data, 0, ret.data, 0, this.data.length);
			return ret;
		}

		@Override
		int get(int addr) {
			return addr >= 0 && addr < data.length ? data[addr] : 0;
		}

		//
		// methods for accessing data within memory
		//
		@Override
		int getLength() {
			return data.length;
		}

		@Override
		void load(int start, int[] values, int mask) {
			int n = Math.min(values.length, data.length - start);
			/* 
			 * Bugfix in memory writing (by Roy77)
			 * https://github.com/roy77
			 */
			for (int i = start; i < n; i++) {
				data[start + i] = (short) (values[i] & mask);
			}
		}

		@Override
		void set(int addr, int value) {
			if (addr >= 0 && addr < data.length) {
				short oldValue = data[addr];
				if (value != oldValue) {
					data[addr] = (short) value;
				}
			}
		}
	}

	static ContentsInterface createContents(int size, int bits) {
		int mask = (bits==32) ? 0xffffffff : (1<<bits)-1;
		if (bits <= 8)
			return new ByteContents(size,mask);
		else if (bits <= 16)
			return new ShortContents(size,mask);
		else
			return new IntContents(size,mask);
	}

	private MemContentsSub() {
	}
}
