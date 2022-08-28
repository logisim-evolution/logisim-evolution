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

package com.cburch.logisim.analyze.data;

import java.util.ArrayList;

public class EspressoPset {

  private ArrayList<Integer> tempSet;
  private final ArrayList<ArrayList<Integer>> cover;

  public EspressoPset(ArrayList<ArrayList<Integer>> cover, EspressoCubeStruct cube) {
    this.cover = cover;
    tempSet = cube.newCube();
  }

  public ArrayList<ArrayList<Integer>> getCover() {
    return cover;
  }

  public ArrayList<Integer> getTempSet() {
    return tempSet;
  }

  public void setTempSet(ArrayList<Integer> value) {
    tempSet = value;
  }

  public boolean coverIsEmpty() {
    return cover.size() == 0;
  }

  public boolean singleElementCover() {
    return cover.size() == 1;
  }
}
