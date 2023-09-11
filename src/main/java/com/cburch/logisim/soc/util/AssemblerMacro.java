/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class AssemblerMacro {

  private final String name;
  private final int nrOfParameters;
  private final LinkedList<AssemblerToken> tokens;
  private final HashMap<String, Long> localLabels;
  private final ArrayList<AssemblerToken[]> parameters;
  private boolean sizeDeterminationActive;
  private long macroSize;

  public AssemblerMacro(String name, int nrOfParameters) {
    this.name = name;
    this.nrOfParameters = nrOfParameters;
    tokens = new LinkedList<>();
    localLabels = new HashMap<>();
    parameters = new ArrayList<>();
    sizeDeterminationActive = false;
    macroSize = -1L;
  }

  public String getName() {
    return name;
  }

  public int getNrOfParameters() {
    return nrOfParameters;
  }

  public void addToken(AssemblerToken token) {
    tokens.add(token);
  }

  public void addLabel(String label) {
    localLabels.put(label, -1L);
  }

  public void clearParameters() {
    parameters.clear();
  }

  public void addParameter(AssemblerToken[] param) {
    parameters.add(param);
  }

  public boolean hasCorrectNumberOfParameters() {
    return nrOfParameters == parameters.size();
  }

  public boolean checkParameters(HashMap<AssemblerToken, StringGetter> errors) {
    boolean hasErrors = false;
    for (AssemblerToken token : tokens)
      if (token.getType() == AssemblerToken.MACRO_PARAMETER)
        if (token.getNumberValue() < 1 || token.getNumberValue() > nrOfParameters) {
          hasErrors = true;
          errors.put(token, S.getter("AssemblerMacroParameterNotDefined"));
        }
    return !hasErrors;
  }

  public LinkedList<AssemblerToken> getMacroTokens() {
    LinkedList<AssemblerToken> makroTokens = new LinkedList<>();
    for (AssemblerToken token : tokens) {
      if (token.getType() == AssemblerToken.MACRO_PARAMETER) {
        int index = token.getNumberValue() - 1;
        AssemblerToken[] param = parameters.get(index);
        makroTokens.addAll(Arrays.asList(param));
      } else {
        AssemblerToken copy =
            new AssemblerToken(token.getType(), token.getValue(), token.getoffset());
        makroTokens.add(copy);
      }
    }
    return makroTokens;
  }

  public boolean checkForMacros(HashMap<AssemblerToken, StringGetter> errors, Set<String> names) {
    boolean hasErrors = false;
    for (AssemblerToken token : tokens) {
      if (token.getType() == AssemblerToken.MAYBE_LABEL) {
        if (token.getValue().equals(name)) {
          errors.put(token, S.getter("AssemblerMacroCannotUseRecurency"));
          hasErrors = true;
        }
        if (names.contains(token.getValue())) token.setType(AssemblerToken.MACRO);
      }
    }
    return hasErrors;
  }

  public long getMacroSize(
      HashMap<AssemblerToken, StringGetter> errors,
      AssemblerInterface assembler,
      HashMap<String, AssemblerMacro> macros,
      ArrayList<AssemblerToken> hierarchy) {
    if (macroSize >= 0) return macroSize;
    if (sizeDeterminationActive) {
      /* fatal error, we have macros that call each other; deadlock */
      for (AssemblerToken asm : hierarchy) {
        errors.put(asm, S.getter("AssemblerMacroCallingEachotherDeadlock"));
      }
      return -1;
    }
    sizeDeterminationActive = true;
    long pc = 0;
    Iterator<AssemblerToken> iter = tokens.iterator();
    while (iter.hasNext()) {
      AssemblerToken asm = iter.next();
      if (asm.getType() == AssemblerToken.INSTRUCTION)
        pc += assembler.getInstructionSize(asm.getValue());
      else if (asm.getType() == AssemblerToken.MACRO) {
        hierarchy.add(asm);
        long msize = macros.get(asm.getValue()).getMacroSize(errors, assembler, macros, hierarchy);
        if (msize < 0) return -1;
        pc += msize;
      } else if (asm.getType() == AssemblerToken.LABEL)
        if (localLabels.containsKey(asm.getValue())) {
          localLabels.put(asm.getValue(), pc);
          iter.remove();
        }
    }
    sizeDeterminationActive = false;
    macroSize = pc;
    return macroSize;
  }

  public boolean replaceLabels(
      HashMap<String, Long> globalLabels,
      HashMap<AssemblerToken, StringGetter> errors,
      AssemblerInterface assembler,
      HashMap<String, AssemblerMacro> macros) {
    /* first pass: determine size of the macro and mark local labels */
    ArrayList<AssemblerToken> hierarchy = new ArrayList<>();
    long msize = getMacroSize(errors, assembler, macros, hierarchy);
    if (msize < 0) return false;
    /* second pass: replace local labels by pc-relative addresses */
    boolean hasErrors = false;
    long pc = 0;
    long nextpc = 0;
    hierarchy.clear();
    for (int i = 0; i < tokens.size(); i++) {
      AssemblerToken asm = tokens.get(i);
      if (asm.getType() == AssemblerToken.INSTRUCTION) {
        pc = nextpc;
        nextpc += assembler.getInstructionSize(asm.getValue());
      } else if (asm.getType() == AssemblerToken.MACRO) {
        pc = nextpc;
        nextpc += macros.get(asm.getValue()).getMacroSize(errors, assembler, macros, hierarchy);
      }
      if (asm.getType() == AssemblerToken.PARAMETER_LABEL) {
        if (globalLabels.containsKey(asm.getValue())) continue;
        if (localLabels.containsKey(asm.getValue())) {
          /* replace */
          long target = localLabels.get(asm.getValue());
          long offset = target - pc;
          boolean negative = offset < 0;
          if (negative) offset = -offset;
          asm.setType(AssemblerToken.HEX_NUMBER);
          asm.setValue(String.format("0x%X", offset));
          AssemblerToken operator =
              new AssemblerToken(
                  negative ? AssemblerToken.MATH_SUBTRACT : AssemblerToken.MATH_ADD,
                  null,
                  asm.getoffset());
          AssemblerToken pcid =
              new AssemblerToken(AssemblerToken.PROGRAM_COUNTER, "pc", asm.getoffset());
          tokens.add(i, operator);
          tokens.add(i, pcid);
        } else {
          hasErrors = true;
          errors.put(asm, S.getter("AssemblerUnknownLabel"));
        }
      }
    }
    return !hasErrors;
  }
}
