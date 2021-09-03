/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

public interface LibraryEventSource {
  void addLibraryListener(LibraryListener listener);

  void removeLibraryListener(LibraryListener listener);
}
