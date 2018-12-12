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

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.EventSourceWeakSupport;

class MemContents implements Cloneable, HexModel {
	static MemContents create(int addrBits, int width, boolean IsRom) {
		return new MemContents(addrBits, width,IsRom);
	}

	private static final int PAGE_SIZE_BITS = 12;
	private static final int PAGE_SIZE = 1 << PAGE_SIZE_BITS;

	private static final int PAGE_MASK = PAGE_SIZE - 1;

	private EventSourceWeakSupport<HexModelListener> listeners = null;
	private int width;
	private int addrBits;
	private int mask;
	private MemContentsSub.ContentsInterface[] pages;

	private MemContents(int addrBits, int width, boolean IsRom) {
		listeners = null;
		setDimensions(addrBits, width, IsRom);
	}

	//
	// HexModel methods
	//
	public void addHexModelListener(HexModelListener l) {
		if (listeners == null)
			listeners = new EventSourceWeakSupport<HexModelListener>();
		listeners.add(l);
	}

	public void clear() {
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] != null) {
				if (pages[i] != null)
					clearPage(i);
			}
		}
	}

	private void clearPage(int index) {
		MemContentsSub.ContentsInterface page = pages[index];
		int[] oldValues = new int[page.getLength()];
		boolean changed = false;
		for (int j = 0; j < oldValues.length; j++) {
			int val = page.get(j) & mask;
			oldValues[j] = val;
			if (val != 0)
				changed = true;
		}
		if (changed) {
			pages[index] = null;
			fireBytesChanged(index << PAGE_SIZE_BITS, oldValues.length,
					oldValues);
		}
	}

	//
	// other methods
	//
	@Override
	public MemContents clone() {
		try {
			MemContents ret = (MemContents) super.clone();
			ret.listeners = null;
			ret.pages = new MemContentsSub.ContentsInterface[this.pages.length];
			for (int i = 0; i < ret.pages.length; i++) {
				if (this.pages[i] != null) {
					ret.pages[i] = this.pages[i].clone();
				}
			}
			return ret;
		} catch (CloneNotSupportedException ex) {
			return this;
		}
	}

	private void ensurePage(int index) {
		if (pages[index] == null) {
			pages[index] = MemContentsSub.createContents(PAGE_SIZE, width);
		}
	}

	public void fill(long start, long len, int value) {
		if (len == 0)
			return;

		int pageStart = (int) (start >>> PAGE_SIZE_BITS);
		int startOffs = (int) (start & PAGE_MASK);
		int pageEnd = (int) ((start + len - 1) >>> PAGE_SIZE_BITS);
		int endOffs = (int) ((start + len - 1) & PAGE_MASK);
		value &= mask;

		if (pageStart == pageEnd) {
			ensurePage(pageStart);
			int[] vals = new int[(int) len];
			Arrays.fill(vals, value);
			MemContentsSub.ContentsInterface page = pages[pageStart];
			if (!page.matches(vals, startOffs, mask)) {
				int[] oldValues = page.get(startOffs, (int) len);
				page.load(startOffs, vals, mask);
				if (value == 0 && page.isClear())
					pages[pageStart] = null;
				fireBytesChanged(start, len, oldValues);
			}
		} else {
			if (startOffs == 0) {
				pageStart--;
			} else {
				if (value == 0 && pages[pageStart] == null) {
					// nothing to do
				} else {
					ensurePage(pageStart);
					int[] vals = new int[PAGE_SIZE - startOffs];
					Arrays.fill(vals, value);
					MemContentsSub.ContentsInterface page = pages[pageStart];
					if (!page.matches(vals, startOffs, mask)) {
						int[] oldValues = page.get(startOffs, vals.length);
						page.load(startOffs, vals, mask);
						if (value == 0 && page.isClear())
							pages[pageStart] = null;
						fireBytesChanged(start, PAGE_SIZE - pageStart,
								oldValues);
					}
				}
			}
			if (value == 0) {
				for (int i = pageStart + 1; i < pageEnd; i++) {
					if (pages[i] != null)
						clearPage(i);
				}
			} else {
				int[] vals = new int[PAGE_SIZE];
				Arrays.fill(vals, value);
				for (int i = pageStart + 1; i < pageEnd; i++) {
					ensurePage(i);
					MemContentsSub.ContentsInterface page = pages[i];
					if (!page.matches(vals, 0, mask)) {
						int[] oldValues = page.get(0, PAGE_SIZE);
						page.load(0, vals, mask);
						fireBytesChanged((long) i << PAGE_SIZE_BITS, PAGE_SIZE,
								oldValues);
					}
				}
			}
			if (endOffs >= 0) {
				MemContentsSub.ContentsInterface page = pages[pageEnd];
				if (value == 0 && page == null) {
					// nothing to do
				} else {
					ensurePage(pageEnd);
					int[] vals = new int[endOffs + 1];
					Arrays.fill(vals, value);
					if (!page.matches(vals, 0, mask)) {
						int[] oldValues = page.get(0, endOffs + 1);
						page.load(0, vals, mask);
						if (value == 0 && page.isClear())
							pages[pageEnd] = null;
						fireBytesChanged((long) pageEnd << PAGE_SIZE_BITS,
								endOffs + 1, oldValues);
					}
				}
			}
		}
	}

	private void fireBytesChanged(long start, long numBytes, int[] oldValues) {
		if (listeners == null)
			return;
		boolean found = false;
		for (HexModelListener l : listeners) {
			found = true;
			l.bytesChanged(this, start, numBytes, oldValues);
		}
		if (!found)
			listeners = null;
	}

	private void fireMetainfoChanged() {
		if (listeners == null)
			return;
		boolean found = false;
		for (HexModelListener l : listeners) {
			found = true;
			l.metainfoChanged(this);
		}
		if (!found)
			listeners = null;
	}

	public int get(long addr) {
		int page = (int) (addr >>> PAGE_SIZE_BITS);
		int offs = (int) (addr & PAGE_MASK);
		if (page < 0 || page >= pages.length || pages[page] == null)
			return 0;
		return pages[page].get(offs) & mask;
	}

	public long getFirstOffset() {
		return 0;
	}

	public long getLastOffset() {
		return (1L << addrBits) - 1;
	}

	public int getLogLength() {
		return addrBits;
	}

	public int getValueWidth() {
		return width;
	}

	public int getWidth() {
		return width;
	}

	public boolean isClear() {
		for (int i = 0; i < pages.length; i++) {
			MemContentsSub.ContentsInterface page = pages[i];
			if (page != null) {
				for (int j = page.getLength() - 1; j >= 0; j--) {
					if (page.get(j) != 0)
						return false;
				}
			}
		}
		return true;
	}

	public void removeHexModelListener(HexModelListener l) {
		if (listeners == null)
			return;
		listeners.add(l);
		if (listeners.isEmpty())
			listeners = null;
	}

	public void set(long addr, int value) {
		int page = (int) (addr >>> PAGE_SIZE_BITS);
		int offs = (int) (addr & PAGE_MASK);
		int old = pages[page] == null ? 0 : pages[page].get(offs) & mask;
		int val = value & mask;
		if (old != val) {
			if (pages[page] == null) {
				pages[page] = MemContentsSub.createContents(PAGE_SIZE, width);
			}
			pages[page].set(offs, val);
			fireBytesChanged(addr, 1, new int[] { old });
		}
	}

	public void set(long start, int[] values) {
		if (values.length == 0)
			return;

		int pageStart = (int) (start >>> PAGE_SIZE_BITS);
		int startOffs = (int) (start & PAGE_MASK);
		int pageEnd = (int) ((start + values.length - 1) >>> PAGE_SIZE_BITS);
		int endOffs = (int) ((start + values.length - 1) & PAGE_MASK);

		if (pageStart == pageEnd) {
			ensurePage(pageStart);
			MemContentsSub.ContentsInterface page = pages[pageStart];
			if (!page.matches(values, startOffs, mask)) {
				int[] oldValues = page.get(startOffs, values.length);
				page.load(startOffs, values, mask);
				if (page.isClear())
					pages[pageStart] = null;
				fireBytesChanged(start, values.length, oldValues);
			}
		} else {
			int nextOffs;
			if (startOffs == 0) {
				pageStart--;
				nextOffs = 0;
			} else {
				ensurePage(pageStart);
				int[] vals = new int[PAGE_SIZE - startOffs];
				System.arraycopy(values, 0, vals, 0, vals.length);
				MemContentsSub.ContentsInterface page = pages[pageStart];
				if (!page.matches(vals, startOffs, mask)) {
					int[] oldValues = page.get(startOffs, vals.length);
					page.load(startOffs, vals, mask);
					if (page.isClear())
						pages[pageStart] = null;
					fireBytesChanged(start, PAGE_SIZE - pageStart, oldValues);
				}
				nextOffs = vals.length;
			}
			int[] vals = new int[PAGE_SIZE];
			int offs = nextOffs;
			for (int i = pageStart + 1; i < pageEnd; i++, offs += PAGE_SIZE) {
				MemContentsSub.ContentsInterface page = pages[i];
				if (page == null) {
					boolean allZeroes = true;
					for (int j = 0; j < PAGE_SIZE; j++) {
						if ((values[offs + j] & mask) != 0) {
							allZeroes = false;
							break;
						}
					}
					if (!allZeroes) {
						page = MemContentsSub.createContents(PAGE_SIZE, width);
						pages[i] = page;
					}
				}
				if (page != null) {
					System.arraycopy(values, offs, vals, 0, PAGE_SIZE);
					if (!page.matches(vals, startOffs, mask)) {
						int[] oldValues = page.get(0, PAGE_SIZE);
						page.load(0, vals, mask);
						if (page.isClear())
							pages[i] = null;
						fireBytesChanged((long) i << PAGE_SIZE_BITS, PAGE_SIZE,
								oldValues);
					}
				}
			}
			if (endOffs >= 0) {
				ensurePage(pageEnd);
				vals = new int[endOffs + 1];
				System.arraycopy(values, offs, vals, 0, endOffs + 1);
				MemContentsSub.ContentsInterface page = pages[pageEnd];
				if (!page.matches(vals, startOffs, mask)) {
					int[] oldValues = page.get(0, endOffs + 1);
					page.load(0, vals, mask);
					if (page.isClear())
						pages[pageEnd] = null;
					fireBytesChanged((long) pageEnd << PAGE_SIZE_BITS,
							endOffs + 1, oldValues);
				}
			}
		}
	}

	public void setDimensions(int addrBits, int width, boolean IsRom) {
		if (addrBits == this.addrBits && width == this.width)
			return;
		this.addrBits = addrBits;
		this.width = width;
		this.mask = width == 32 ? 0xffffffff : ((1 << width) - 1);

		MemContentsSub.ContentsInterface[] oldPages = pages;
		int pageCount;
		int pageLength;
		if (addrBits < PAGE_SIZE_BITS) {
			pageCount = 1;
			pageLength = 1 << addrBits;
		} else {
			pageCount = 1 << (addrBits - PAGE_SIZE_BITS);
			pageLength = PAGE_SIZE;
		}
		pages = new MemContentsSub.ContentsInterface[pageCount];
		if (oldPages != null) {
			int n = Math.min(oldPages.length, pages.length);
			for (int i = 0; i < n; i++) {
				if (oldPages[i] != null) {
					pages[i] = MemContentsSub.createContents(pageLength, width);
					int m = Math.min(oldPages[i].getLength(), pageLength);
					for (int j = 0; j < m; j++) {
						pages[i].set(j, oldPages[i].get(j));
					}
				}
			}
			if (AppPreferences.Memory_Startup_Unknown.get()&&!IsRom) {
				for (int i = oldPages.length ; i < pages.length ; i++)
					pages[i] = MemContentsSub.createContents(pageLength, width);
			}
		} else if (AppPreferences.Memory_Startup_Unknown.get()&&!IsRom){
			for (int i = 0 ; i < pageCount ; i++)
				pages[i] = MemContentsSub.createContents(pageLength, width);
		}
		if (pageCount == 0 && pages[0] == null) {
			pages[0] = MemContentsSub.createContents(pageLength, width);
		}
		
		fireMetainfoChanged();
	}
}
