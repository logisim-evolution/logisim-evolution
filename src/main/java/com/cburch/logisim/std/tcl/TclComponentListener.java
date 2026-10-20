/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.instance.Instance;
import java.io.File;

public class TclComponentListener {
  final Instance instance;

  TclComponentListener(Instance instance) {
    this.instance = instance;
  }

  public void changed(File source) {
    instance.fireInvalidated();
    instance.recomputeBounds();
  }
}
