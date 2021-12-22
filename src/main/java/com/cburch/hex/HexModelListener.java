/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hex;

public interface HexModelListener {
  void bytesChanged(HexModel source, long start, long numBytes, long[] oldValues);

  void metainfoChanged(HexModel source);
}
