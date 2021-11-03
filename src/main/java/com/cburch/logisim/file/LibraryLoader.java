/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import com.cburch.logisim.tools.Library;

interface LibraryLoader {
  String getDescriptor(Library lib);

  Library loadLibrary(String desc);

  void showError(String description);
}
