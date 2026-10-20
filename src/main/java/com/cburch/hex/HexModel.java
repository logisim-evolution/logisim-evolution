/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
