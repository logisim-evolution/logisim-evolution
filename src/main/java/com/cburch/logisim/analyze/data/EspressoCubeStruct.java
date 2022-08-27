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
import java.util.List;

import com.cburch.logisim.analyze.model.AnalyzerModel;

public class EspressoCubeStruct {
  
  public class EspressoCdataStruct {
    List<Long> partZeros;
    List<Long> varZeros;
    List<Long> partsActive;
    List<Boolean> isUnate;
    int varsActive;
    int varsUnate;
    int best;
  }
      
  private int size;
  private int numberOfInputs;
  private int numberOfOutputs;
  private List<Long> firstPart;
  private List<Long> lastPart;
  private List<Long> partSize;
  private List<Long> firstWord;
  private List<Long> lastWord;
  private List<Long> binaryMask;
  private List<List<Long>> varMask;
  private List<List<Long>> temp;
  private List<Long> fullSet;
  private List<Long> emptySet;
  private int inMask;
  private int inWord;
  private List<Long> sparce;
  private int outputIndex;
  private EspressoCdataStruct cdata;

  private final static int DISJOINT = 0x55555555;
  private final static int CUBE_TEMP = 10;
  private final static int BPI = 32;

  public EspressoCubeStruct(int format, AnalyzerModel model) {
    final var truthTable = model.getTruthTable();
    numberOfInputs = truthTable.getInputColumnCount();
    numberOfOutputs = truthTable.getOutputColumnCount();
    setup();
  }

  public List<Long> getTemp(int index) {
    if ((index < 0) || (index >= temp.size())) return null;
    return temp.get(index);
  }

  public int getSize() {
    return size;
  }

  private void setup() {
    final var nrOfVars = numberOfInputs + 1;
    outputIndex = numberOfInputs;
    size = 0;
    firstPart = new ArrayList<>();
    lastPart = new ArrayList<>();
    partSize = new ArrayList<>();
    firstWord = new ArrayList<>();
    lastWord = new ArrayList<>();
    for (var varIndex = 0; varIndex < nrOfVars; varIndex++) {
      if (varIndex < numberOfInputs) partSize.add(2L);
      firstPart.add((long) size);
      firstWord.add(whichWord(size));
      size += Math.abs(partSize.get(varIndex));
      lastPart.add((long) size - 1L);
      lastWord.add(whichWord(size - 1));
    }
    partSize.add((long) numberOfOutputs);
    varMask = new ArrayList<>();
    sparce = new ArrayList<>();
    binaryMask = newCube();
    for (var varIndex = 0; varIndex < nrOfVars; varIndex++) {
      final var mask = newCube();
      varMask.add(mask);
      for (var loop = firstPart.get(varIndex); loop < lastPart.get(varIndex); loop++) {
        final var index = (int) whichWord(loop);
        var value = mask.get(index);
        value |= 1L << whichBit(loop);
        mask.set(index, value);
        if (varIndex < numberOfInputs) {
          sparce.add(0L);
          doSetOr(binaryMask, binaryMask, mask);
        } else {
          sparce.add(1L);
        }
      }      
    }
    inWord = lastWord.get(numberOfInputs - 1).intValue();
    inMask = binaryMask.get(inWord).intValue() & DISJOINT;

    temp = new ArrayList<>();
    for (var index = 0; index < CUBE_TEMP; index++) temp.add(newCube());
    emptySet = newCube();
    fullSet = setFill(newCube(), size);
    cdata = new EspressoCdataStruct();
    cdata.partZeros = allocLong(size);
    cdata.varZeros = allocLong(nrOfVars);
    cdata.partsActive = allocLong(nrOfVars);
    cdata.isUnate = allocBool(nrOfVars);
  }

  public static long whichWord(long value) {
    return (value >> 5L) + 1;
  }

  public static long whichBit(long value) {
    return value & 31L;
  }

  private static List<Long> allocLong(int size) {
    final var res = new ArrayList<Long>();
    for (var index = 0; index < size; index++) res.add(0L);
    return res;
  }

  private static List<Boolean> allocBool(int size) {
    final var res = new ArrayList<Boolean>();
    for (var index = 0; index < size; index++) res.add(false);
    return res;
  }

  public static void doSetOr(List<Long> result, List<Long> set1, List<Long> set2) {
    final var loopEnd = set1.get(0) & 0x03ffL;
    var resultValue = result.get(0);
    resultValue &= ~0x03ffL;
    resultValue |= loopEnd;
    result.set(0, resultValue);
    for (var index = (int) loopEnd; index > 0; index--) {
      final var val1 = set1.get(index);
      final var val2 = set2.get(index);
      result.set(index, val1 | val2);
    }
  }

  public static int loopInit(int size) {
    return (size <= 32) ? 1 : (int) whichWord(size - 1);
  }

  public static List<Long> setFill(List<Long> res, int size) {
    final var loopInit = loopInit(size);
    res.set(0, (long) loopInit);
    var value = 0xffffffffL;
    value >>= (loopInit * BPI) - size;
    res.set(loopInit, value);
    for (var index = loopInit - 1; index > 0; index --) {
      res.set(index, 0xffffffffL);
    }
    return res;
  }

  public List<Long> setClear(List<Long> res, int size) {
    res.clear();
    final var loopInit = loopInit(size);
    res.add((long) loopInit);
    for (var i = loopInit; i > 0 ; i--) res.add(0L);
    return res;
  }

  public void setInsert(List<Long> res, int loc) {
    final var index = (int) whichWord(loc);
    var value = res.get(index);
    value |= 1 << whichBit(loc);
    res.set(index, value);
  }

  public void setCopy(List<Long> result, List<Long> source) {
    result.clear();
    result.addAll(source);
  }

  public List<Long> newCube() {
    return setClear(new ArrayList<Long>(), size);
  }
}
