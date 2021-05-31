/*
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

package com.cburch.hex;

public interface HexModel {
  /** Registers a listener for changes to the values. */
  void addHexModelListener(HexModelListener l);

  /** Fills a series of values with the same value. */
  void fill(long start, long length, long value);

  /** Returns the value at the given address. */
  long get(long address);

  /** Returns the offset of the initial value to be displayed. */
  long getFirstOffset();

  /** Returns the number of values to be displayed. */
  long getLastOffset();

  /** Returns number of bits in each value. */
  int getValueWidth();

  /** Unregisters a listener for changes to the values. */
  void removeHexModelListener(HexModelListener l);

  /** Changes the value at the given address. */
  void set(long address, long value);

  /** Changes a series of values at the given addresses. */
  void set(long start, long[] values);
}
