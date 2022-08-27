/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 * 
 * This part of logisim implements (parts of) espresso:
 * 
 * Copyright (c) 1988, 1989, Regents of the University of California.
 * All rights reserved.
 *
 * Use and copying of this software and preparation of derivative works
 * based upon this software are permitted.  However, any distribution of
 * this software or derivative works must include the above copyright
 * notice.
 *
 * This software is made available AS IS, and neither the Electronics
 * Research Laboratory or the University of California make any
 * warranty about the software, its performance or its conformity to
 * any specification.
*/

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.analyze.data.EspressoCubeStruct;

public class EspressoOptimize {
  
  private final int format;
  private final AnalyzerModel model;
  private final EspressoCubeStruct cube;

  public EspressoOptimize(int format, AnalyzerModel model) {
    this.format = format;
    this.model = model;
    cube = new EspressoCubeStruct(format, model);
  }
  
}
