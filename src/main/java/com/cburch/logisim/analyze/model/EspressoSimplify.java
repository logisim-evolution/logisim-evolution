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

import java.util.ArrayList;

import com.cburch.logisim.analyze.data.EspressoCubeStruct;
import com.cburch.logisim.analyze.data.EspressoPset;

public class EspressoSimplify {

  public static final int TRUE = 1;
  public static final int FALSE = 0;
  public static final int MAYBE = 2;

  private final EspressoCubeStruct cubeInfo;

  public EspressoSimplify(EspressoCubeStruct cube) {
    cubeInfo = cube;
  }

  public ArrayList<ArrayList<Integer>> simplify(EspressoPset coverSet) {
    return new ArrayList<>();
  }

  public int simplifySpecialCases(EspressoPset cover1, ArrayList<ArrayList<Integer>> cover2) {
    cover2.clear();
    final var cof = cover1.getTempSet();
    // Check for no cubes in the cover
    if (cover1.coverIsEmpty()) {
      return TRUE;
    }
    // Check for only a single cube in the cover
    if (cover1.singleElementCover()) {
      final var newCover = cubeInfo.newCube();
      cubeInfo.setOr(newCover, cof, cover1.getCover().get(0));
      cover2.add(newCover);
      return TRUE;
    }
    // Check for a row of all 1's (implies function is a tautology)
    for (final var cube : cover1.getCover() ) {
      if (fullRow(cube, cof)) {
        cover2.add(cubeInfo.getFullSet());
        return TRUE;
      }
    }
    // Check for a column of all 0's which can be factored out
    final var ceil = cubeInfo.setCopy(cof);
    for (final var cube : cover1.getCover()) {
      cubeInfo.setOr(ceil, ceil, cube);
    }
    if (!cubeInfo.cubesAreEqual(ceil, cubeInfo.getFullSet())) {
      final var tempCube = cubeInfo.newCube();
      cubeInfo.setDiff(tempCube, cubeInfo.getFullSet(), ceil);
      cubeInfo.setOr(cof, cof, tempCube);
      tempCube.clear();
      cover2 = simplify(cover1);
      for (final var cube : cover2) {
        cubeInfo.setAnd(cube, cube, ceil);
      }
      ceil.clear();
      return TRUE;
    }
    ceil.clear();
    // Collect column counts, determine unate variables, etc.
    cubeInfo.getCData().massiveCount(cover1);
    // TODO: continue
    return FALSE;
  }

  public EspressoPset cube1List(ArrayList<ArrayList<Integer>> cover) {
    return new EspressoPset(cover, cubeInfo);
  }

  public boolean fullRow(ArrayList<Integer> cube, ArrayList<Integer> cof) {
    for (var wordId = 0; wordId < cube.size(); wordId++) {
      if ((cube.get(wordId)|cof.get(wordId)) != cubeInfo.getFullSet().get(wordId)) {
        return false;
      }
    }
    return true;
  }
}
