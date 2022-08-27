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

  // TODO: This code has not been optimized for performance, it just tries to implement espresso for logisim in java.
  //       For sure a lot can be done here.
  // Important: The original code is based on "unsigned int" for storage, as java does not know the unsigned version I
  //            tried to adapt to int's, however I might have left some "bugs"
  // Note: The (variable/function) names have been kept as close as possible to the original onces keeping in mind logisim
  //       programming conventions.
  public static final int BPI = 32; // number of bits in an Integer
  public static final int LOG2_BPI = 5; // log2(BPI)
  public static final int MaxInputMask = 0x03ff;
  public static final int DISJOINT = 0x55555555;
  
  public class EspressoCdataStuct {
    List<Integer> partZeros;
    List<Integer> varZeros;
    List<Integer> partActive;
    List<Boolean> isUnate;
    int varsActive;
    int varsUnate;
    int best;
  }

  private int size;
  private int numberOfInputs;
  private int numberOfOutputs;
  private List<Integer> firstPart;
  private List<Integer> lastPart;
  private List<Integer> partSize;
  private List<Integer> firstWord;
  private List<Integer> lastWord;
  private List<Integer> binaryMask;
  private List<List<Integer>> varMask;
  private List<Integer> fullSet;
  private List<Integer> emptySet;
  private int inMask;
  private int inWord;
  private List<Integer> sparce;
  private EspressoCdataStuct cData;

  public EspressoCubeStruct(int format, AnalyzerModel model) {
    final var truthTable = model.getTruthTable();
    numberOfInputs = truthTable.getInputColumnCount();
    numberOfOutputs = truthTable.getOutputColumnCount();
    setup();
  }

  private void setup() {
    size = 0;
    partSize = new ArrayList<>(numberOfInputs);
    firstPart = new ArrayList<>(numberOfInputs);
    lastPart = new ArrayList<>(numberOfInputs);
    firstWord = new ArrayList<>(numberOfInputs);
    lastWord = new ArrayList<>(numberOfInputs);
    for (var input = 0; input < numberOfInputs; input++) {
      partSize.set(input, 2); // this is a simplification for logisim, we do not take into account input busses 
                                       // as the rest of logisim will take care of that
      firstPart.set(input, size);
      firstWord.set(input, whichWord(size));
      size += partSize.get(input);
      lastPart.set(input, size - 1 );
      lastWord.set(input, whichWord(size - 1));
    }
    varMask = new ArrayList<>(numberOfInputs);
    sparce = new ArrayList<>(numberOfInputs);
    binaryMask = newCube();
    // mv-mask has been suppressed for logisim
    for (var input = 0; input < numberOfInputs; input++) {
      final var mask = newCube();
      varMask.set(input, mask);
      for (var inputInfoMask = firstPart.get(input); inputInfoMask < lastPart.get(input); inputInfoMask++) {
        setInsert(mask, inputInfoMask);
      }
      sparce.set(input, 0);
      setOr(binaryMask, binaryMask, mask);
    }
    inWord = lastWord.get(numberOfInputs - 1);
    inMask = binaryMask.get(inWord) & DISJOINT;
    fullSet = setFill(newCube(), size);
    emptySet = newCube();
    cData = new EspressoCdataStuct();
    cData.partZeros = new ArrayList<>(size);
    cData.varZeros = new ArrayList<>(numberOfInputs);
    cData.partActive = new ArrayList<>(numberOfInputs);
    cData.isUnate = new ArrayList<>(numberOfInputs);
  }

  public static int whichWord(int element) {
    // as we can expect for certain situations that we need more than 32 bits for information storage, this function
    // returns in which of the 32-bits word the information is present.
    // in-lining this would be more efficient but less readable
    // Info: Logisim only supports 20 inputs and 256 outputs, hence element will never "over/underflow"/ be negative. Hence this check is suppressed.
    //       the problem would occure if logisim supports > Integer.MAX_VALUE in-/outputs which is not likely to happen
    return ((element >> LOG2_BPI) + 1);
  }

  public static int whichBit(int element) {
    // only take the lower bits indexing the word
    // in-lining this would be more efficient but less readable
    // Info: Logisim only supports 20 inputs and 256 outputs, hence element will never "over/underflow"/ be negative. Hence this check is suppressed.
    //       the problem would occure if logisim supports > Integer.MAX_VALUE in-/outputs which is not likely to happen
    return element & (BPI-1);
  }

  public static void setInsert(List<Integer> set, int entry) {
    final var wordToTake = whichWord(entry);
    var wordValue = set.get(wordToTake);
    wordValue |= 1 << whichBit(entry);
  }

  public List<Integer> newCube() {
    final var ret = new ArrayList<Integer>(setSize(size));
    setClear(ret, size);
    return ret;
  }

  public static List<Integer> setClear(List<Integer> data, int size) {
    final var nrOfWords = loopInit(size);
    data.set(0,nrOfWords); // indicates how many words hold the information
    for (var word = 0 ; word < nrOfWords; word++) {
      data.set(word + 1,0); // input 0 indicates the nr. of words. therefore + 1
    }
    return data;
  }

  public static List<Integer> setFill(List<Integer> data, int size) {
    final var nrOfWords = loopInit(size);
    data.set(0, nrOfWords);
    final var lastWordRest = size % BPI;
    for (var word = 0; word < nrOfWords - 1; word++) data.set(word + 1, 0xffffffff);
    final var finalMask = lastWordRest == 31 ? 0xffffffff : (0x7fffffff >> (30 - lastWordRest)); // we have to take care of int versus unsigned int
    data.set(nrOfWords, finalMask);
    return data;
  }

  public static void setOr(List<Integer> result, List<Integer> array1, List<Integer> array2) {
    final var nrOfWords = loop(array1);
    putLoop(result, nrOfWords);
    for (var infoIndex = 0; infoIndex < nrOfWords; infoIndex++) {
      final var infoWord = infoIndex + 1;
      result.set(infoWord, array1.get(infoWord) | array2.get(infoWord));
    }
  }

  private static int loop(List<Integer> set) {
    return set.get(0) & MaxInputMask;
  }

  private static void putLoop(List<Integer> set, int value) {
    var oldvalue = set.get(0);
    oldvalue &= ~MaxInputMask;
    oldvalue |= value;
    set.set(0, oldvalue);
  }

  private static int setSize(int size) {
    return (size <= BPI) ? 2 : whichWord(size - 1) + 1;
  }

  private static int loopInit(int size) {
    return ((size <= BPI) ? 1 : whichWord(size));
  }
}
