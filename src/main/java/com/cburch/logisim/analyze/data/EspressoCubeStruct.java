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
  
  private int size;
  private int numberOfInputs;
  private int numberOfOutputs;
  private ArrayList<Integer> firstPart;
  private ArrayList<Integer> lastPart;
  private ArrayList<Integer> partSize;
  private ArrayList<Integer> firstWord;
  private ArrayList<Integer> lastWord;
  private ArrayList<Integer> binaryMask;
  private ArrayList<Integer> mvMask;
  private ArrayList<ArrayList<Integer>> varMask;
  private ArrayList<Integer> fullSet;
  private ArrayList<Integer> emptySet;
  private int inMask;
  private int inWord;
  private ArrayList<Integer> sparce;
  private EspressoCdataStruct cData;

  public EspressoCubeStruct(int format, AnalyzerModel model) {
    final var truthTable = model.getTruthTable();
    numberOfInputs = truthTable.getInputColumnCount();
    numberOfOutputs = truthTable.getOutputColumnCount();
    setup();
  }

  public EspressoCdataStruct getCData() {
    return cData;
  }

  public int getNumberOfVariables() {
    return numberOfInputs+1;
  }

  public int getFirtPart(int index) {
    if ((index < 0) || (index >= firstPart.size())) return -1;
    return firstPart.get(index);
  }

  public int getLastPart(int index) {
    if ((index < 0) || (index >= lastPart.size())) return -1;
    return lastPart.get(index);
  }

  public int getOutputOffset() {
    return firstPart.get(numberOfInputs);
  }

  public int whichWord(int element) {
    // as we can expect for certain situations that we need more than 32 bits for information storage, this function
    // returns in which of the 32-bits word the information is present.
    // in-lining this would be more efficient but less readable
    // Info: Logisim only supports 20 inputs and 256 outputs, hence element will never "over/underflow"/ be negative. Hence this check is suppressed.
    //       the problem would occure if logisim supports > Integer.MAX_VALUE in-/outputs which is not likely to happen
    return (element >> LOG2_BPI);
  }

  public int whichBit(int element) {
    // only take the lower bits indexing the word
    // in-lining this would be more efficient but less readable
    // Info: Logisim only supports 20 inputs and 256 outputs, hence element will never "over/underflow"/ be negative. Hence this check is suppressed.
    //       the problem would occure if logisim supports > Integer.MAX_VALUE in-/outputs which is not likely to happen
    return element & (BPI - 1);
  }

  public ArrayList<Integer> newCube() {
    final var ret = allocInt(getNumberOfWords());
    setClear(ret, size);
    return ret;
  }

  public void setInsert(ArrayList<Integer> set, int entry) {
    final var wordToTake = whichWord(entry);
    var wordValue = set.get(wordToTake);
    wordValue |= 1 << whichBit(entry);
  }

  public int getNumberOfWords() {
    return whichWord(size - 1) + 1;
  }

  public ArrayList<Integer> setClear(ArrayList<Integer> data, int size) {
    final var nrOfWords = getNumberOfWords();
    for (var word = 0; word < nrOfWords; word++) {
      data.set(word, 0); // input 0 indicates the nr. of words. therefore + 1
    }
    return data;
  }

  public ArrayList<Integer> setFill(ArrayList<Integer> data, int size) {
    final var nrOfWords = getNumberOfWords();
    final var lastWordRest = size % BPI;
    for (var word = 0; word < nrOfWords - 1; word++) data.set(word, 0xffffffff);
    final var finalMask = lastWordRest == 31 ? 0xffffffff : (0x7fffffff >> (30 - lastWordRest)); // we have to take care of int versus unsigned int
    data.set(nrOfWords - 1, finalMask);
    return data;
  }

  public void setOr(ArrayList<Integer> result, ArrayList<Integer> array1, ArrayList<Integer> array2) {
    final var nrOfWords = loop(array1);
    for (var infoIndex = 0; infoIndex < nrOfWords; infoIndex++) {
      result.set(infoIndex, array1.get(infoIndex) | array2.get(infoIndex));
    }
  }

  public void setAnd(ArrayList<Integer> result, ArrayList<Integer> array1, ArrayList<Integer> array2) {
    final var nrOfWords = loop(array1);
    for (var infoIndex = 0; infoIndex < nrOfWords; infoIndex++) {
      result.set(infoIndex, array1.get(infoIndex) & array2.get(infoIndex));
    }
  }

  public ArrayList<Integer> setCopy(ArrayList<Integer> res, ArrayList<Integer> toCopy) {
    res.clear();
    res.addAll(toCopy);
    return res;
  }

  public ArrayList<Integer> setCopy(ArrayList<Integer> toCopy) {
    final var res = new ArrayList<Integer>();
    return setCopy(res, toCopy);
  }

  public ArrayList<Integer> setDiff(ArrayList<Integer> result, ArrayList<Integer> set1, ArrayList<Integer> set2) {
    final var nrOfWords = loop(set1);
    for (var infoIndex = 0; infoIndex < nrOfWords; infoIndex++) {
      result.set(infoIndex, set1.get(infoIndex) & ~set2.get(infoIndex));
    }
    return result;
  }

  public boolean cubesAreEqual(ArrayList<Integer> cube1, ArrayList<Integer> cube2) {
    if (cube1.size() != cube2.size()) return false;
    for (var element = 0 ; element < cube1.size(); element++) {
      if (cube1.get(element) != cube2.get(element)) return false;
    }
    return true;
  }

  public ArrayList<Integer> getFullSet() {
    return fullSet;
  }

  private int loop(ArrayList<Integer> set) {
    return numberOfInputs + 1;
  }

  public ArrayList<Integer> allocInt(int size) {
    final var res = new ArrayList<Integer>();
    while (res.size() < size) res.add(0);
    return res;
  }

  public ArrayList<Boolean> allocBool(int size) {
    final var res = new ArrayList<Boolean>();
    while (res.size() < size) res.add(false);
    return res;
  }

  private void setup() {
    size = 0;
    final var nrOfLocations = numberOfInputs+1;
    final var outputLocation = numberOfInputs;
    partSize = allocInt(nrOfLocations);
    firstPart = allocInt(nrOfLocations);
    lastPart = allocInt(nrOfLocations);
    firstWord = allocInt(nrOfLocations);
    lastWord = allocInt(nrOfLocations);
    for (var input = 0; input < nrOfLocations; input++) {
      partSize.set(input, input == outputLocation ? numberOfOutputs : 2); 
      // this is a simplification for logisim, we do not take into account input busses 
      // as the rest of logisim will take care of that
      firstPart.set(input, size);
      firstWord.set(input, whichWord(size));
      size += partSize.get(input);
      lastPart.set(input, size - 1);
      lastWord.set(input, whichWord(size - 1));
    }
    varMask = new ArrayList<>();
    sparce = allocInt(nrOfLocations);
    binaryMask = newCube();
    mvMask = newCube();
    for (var input = 0; input < nrOfLocations; input++) {
      final var mask = newCube();
      varMask.add(mask);
      for (var inputInfoMask = firstPart.get(input); inputInfoMask < lastPart.get(input); inputInfoMask++) {
        setInsert(mask, inputInfoMask);
      }
      if (input == outputLocation) {
        sparce.set(input, 1);
        setOr(mvMask, mvMask, mask);
      } else {
        sparce.set(input, 0);
        setOr(binaryMask, binaryMask, mask);
      }
    }
    inWord = lastWord.get(numberOfInputs - 1);
    inMask = binaryMask.get(inWord) & DISJOINT;
    fullSet = setFill(newCube(), size);
    emptySet = newCube();
    cData = new EspressoCdataStruct(this);
  }
}
