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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.soc.data.AssemblerHighlighter;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.file.SectionHeader;
import com.cburch.logisim.soc.file.SymbolTable;
import com.cburch.logisim.util.StringGetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblerInfo {

  private class AssemblerSectionInfo extends SectionHeader {
    /* In his class, all locations between sectionStart and sectionEnd that have no value associated
     * are filled in with 0.
     */
    private long sectionStart;
    private long sectionEnd;
    private Map<Long, Byte> data;
    private Map<Long, AssemblerAsmInstruction> instructions;
    private final AssemblerToken identifier;

    public AssemblerSectionInfo() {
      super("NoName");
      identifier = null;
      init(0);
    }

    public AssemblerSectionInfo(String name, AssemblerToken identifier) {
      super(name);
      this.identifier = identifier;
      init(0);
    }

    public AssemblerSectionInfo(long start, String name, AssemblerToken identifier) {
      super(name);
      this.identifier = identifier;
      init(start);
    }

    public String getSectionName() {
      return super.getName();
    }

    public long getSectionEnd() {
      return sectionEnd;
    }

    public boolean hasInstructions() {
      return !instructions.isEmpty();
    }

    public AssemblerToken getIdentifier() {
      return identifier;
    }

    public long getEntryPoint() {
      if (instructions.isEmpty()) return -1;
      var result = -1L;
      for (final var x : instructions.keySet()) {
        if (result < 0 || x < result) result = x;
      }
      return result;
    }

    public void setOrgInfo(long address) {
      if (sectionStart == sectionEnd) {
        sectionStart = sectionEnd = address;
        return;
      }
      sectionEnd = address;
    }

    public void addZeroBytes(int nr) {
      sectionEnd += nr;
    }

    public void addString(String str) {
      for (var i = 0; i < str.length(); i++) {
        if (str.charAt(i) == '\\') {
          i++;
          if (i < str.length()) {
            char kar = str.charAt(i);
            byte val = switch (kar) {
              case 'b' -> 8;
              case 't' -> 9;
              case 'n' -> 10;
              case 'f' -> 12;
              case 'r' -> 13;
              default -> (byte) kar;
            };
            data.put(sectionEnd, val);
            sectionEnd++;
          }
        } else if (i < str.length()) {
          data.put(sectionEnd, str.getBytes()[i]);
          sectionEnd++;
        }
      }
    }

    public void addByte(Byte b) {
      if (b != 0)
        data.put(sectionEnd, b);
      sectionEnd++;
    }

    public void addInstruction(AssemblerAsmInstruction instr) {
      /* in the case we have a pc related parameter we can now replace it by a value */
      instr.replacePcAndDoCalc(sectionEnd, errors);
      instructions.put(sectionEnd, instr);
      sectionEnd += instr.getSizeInBytes();
    }

    public boolean replaceLabels(Map<String, Long> labels, Map<AssemblerToken, StringGetter> errors) {
      var hasError = false;
      for (final var addr : instructions.keySet())
        hasError |= !instructions.get(addr).replaceLabels(labels, errors);
      for (String label : labels.keySet()) {
        final var addr = labels.get(label);
        if (addr >= sectionStart && addr < sectionEnd) {
          final var st = new SymbolTable(label, SocSupport.convUnsignedLong(addr));
          super.addSymbol(st);
        }
      }
      return !hasError;
    }

    public boolean replaceDefines(Map<String, Integer> defines, Map<AssemblerToken, StringGetter> errors) {
      var errorsFound = false;
      for (final var addr : instructions.keySet()) {
        errorsFound |= !instructions.get(addr).replaceDefines(defines, errors);
      }
      return !errorsFound;
    }

    public Map<AssemblerToken, StringGetter> replaceInstructions(AssemblerInterface assembler) {
      final var errors = new HashMap<AssemblerToken, StringGetter>();
      for (long addr : instructions.keySet()) {
        final var asm = instructions.get(addr);
        asm.setProgramCounter(addr);
        if (!assembler.assemble(asm)) {
          errors.putAll(asm.getErrors());
        } else {
          Byte[] bytes = asm.getBytes();
          for (int i = 0; i < asm.getSizeInBytes(); i++)
            data.put(addr + i, bytes[i]);
        }
      }
      return errors;
    }

    public boolean download(SocProcessorInterface cpu, CircuitState state) {
      for (var i = sectionStart; i < sectionEnd; i++) {
        final var datab = data.containsKey(i) ? data.get(i) : 0;
        final var trans =
            new SocBusTransaction(
                SocBusTransaction.WRITE_TRANSACTION,
                SocSupport.convUnsignedLong(i),
                datab,
                SocBusTransaction.BYTE_ACCESS,
                "Assembler");
        cpu.insertTransaction(trans, true, state);
        if (hasInstructions()) super.addExecutableFlag();
        super.setSize(sectionEnd - sectionStart);
        super.setStartAddress(sectionStart);
        if (trans.hasError()) return false;
      }
      return true;
    }

    private void init(long start) {
      sectionStart = start;
      sectionEnd = start;
      data = new HashMap<>();
      instructions = new HashMap<>();
    }
  }

  public class SectionHeaders extends ElfSectionHeader {
    public AssemblerSectionInfo get(int index) {
      return (AssemblerSectionInfo) super.getHeader(index);
    }

    public void add(AssemblerSectionInfo obj) {
      super.addHeader(obj);
    }

    public ArrayList<AssemblerSectionInfo> getAll() {
      final var ret = new ArrayList<AssemblerSectionInfo>();
      for (final var hdr : super.getHeaders()) {
        if (hdr instanceof AssemblerSectionInfo sectionInfo)
          ret.add(sectionInfo);
      }
      return ret;
    }

    public int size() {
      return super.getHeaders().size();
    }
  }

  private final SectionHeaders sections;
  private final HashMap<AssemblerToken, StringGetter> errors;
  private int currentSection;
  private final AssemblerInterface assembler;

  public AssemblerInfo(AssemblerInterface assembler) {
    sections = new SectionHeaders();
    errors = new HashMap<>();
    currentSection = -1;
    this.assembler = assembler;
  }

  public Map<AssemblerToken, StringGetter> getErrors() {
    return errors;
  }

  public void assemble(List<AssemblerToken> tokens, Map<String, Long> labels, Map<String, AssemblerMacro> macros) {
    errors.clear();
    sections.clear();
    final var defines = new HashMap<String, Integer>();
    currentSection = -1;
    /* first pass: go through all tokens and mark the labels */
    for (var i = 0; i < tokens.size(); i++) {
      final var asm = tokens.get(i);
      switch (asm.getType()) {
        case AssemblerToken.ASM_INSTRUCTION -> i += handleAsmInstructions(tokens, i, asm, defines);
        case AssemblerToken.LABEL -> handleLabels(labels, asm);
        case AssemblerToken.INSTRUCTION -> i += handleInstruction(tokens, i, asm);
        case AssemblerToken.MACRO -> i += handleMacros(tokens, i, asm, macros);
        default -> errors.put(asm, S.getter("AssemblerUnknownIdentifier"));
      }
    }
    if (!errors.isEmpty()) return;
    /* second pass: replace all parameter labels by a value */
    for (final var label : labels.keySet()) {
      if (labels.get(label) < 0) {
        /* this should never happen */
        OptionPane.showMessageDialog(null, "Severe bug in AssemblerInfo.java");
        return;
      }
    }
    boolean errorsFound = false;
    for (final var section : sections.getAll()) {
      errorsFound |= !section.replaceLabels(labels, errors);
    }
    if (errorsFound) return;
    errorsFound = false;
    for (final var section : sections.getAll()) {
      errorsFound |= !section.replaceDefines(defines, errors);
    }
    if (errorsFound) return;
    /* third pass: Check for overlapping sections and mark them */
    for (int i = 0; i < sections.size(); i++) {
      final var section = sections.get(i);
      for (int j = i + 1; j < sections.size(); j++) {
        final var check = sections.get(j);
        if ((section.sectionStart > check.sectionStart && section.sectionStart < check.sectionEnd)
            || (section.sectionEnd > check.sectionStart && section.sectionEnd < check.sectionEnd)) {
          errorsFound = true;
          if (section.getIdentifier() != null) errors.put(section.getIdentifier(), S.getter("AssemblerOverlappingSections"));
          if (check.getIdentifier() != null) errors.put(section.getIdentifier(), S.getter("AssemblerOverlappingSections"));
        }
      }
    }
    if (errorsFound) return;
    /* last pass: transform instructions to bytes */
    for (final var section : sections.getAll()) {
      errors.putAll(section.replaceInstructions(assembler));
    }
  }

  public boolean download(SocProcessorInterface cpu, CircuitState state) {
    for (final var section : sections.getAll())
      if (!section.download(cpu, state)) return false;
    return true;
  }

  public long getEntryPoint() {
    long entry = -1;
    for (final var section : sections.getAll()) {
      if (section.hasInstructions()) {
        final var sentry = section.getEntryPoint();
        if (entry < 0 || sentry < entry) entry = sentry;
      }
    }
    return entry;
  }

  public ElfSectionHeader getSectionHeader() {
    return sections;
  }

  private int handleMacros(List<AssemblerToken> tokens, int index, AssemblerToken current, Map<String, AssemblerMacro> macros) {
    if (!macros.containsKey(current.getValue())) return 0;
    final var macro = macros.get(current.getValue());
    macro.clearParameters();
    final var acceptedParameters = assembler.getAcceptedParameterTypes();
    int skip = 0;
    if (index + 1 < tokens.size()) {
      final var params = new ArrayList<AssemblerToken>();
      AssemblerToken next;
      do {
        next = tokens.get(index + skip + 1);
        if (acceptedParameters.contains(next.getType())) {
          skip++;
          if (next.getType() == AssemblerToken.SEPERATOR) {
            if (params.isEmpty()) {
              errors.put(next, S.getter("AssemblerExpectedParameter"));
              return tokens.size();
            }
            final var set = new AssemblerToken[params.size()];
            for (int i = 0; i < params.size(); i++) set[i] = params.get(i);
            params.clear();
            macro.addParameter(set);
          } else {
            params.add(next);
          }
        }
      } while (index + skip + 1 < tokens.size() && acceptedParameters.contains(next.getType()));
      if (!params.isEmpty()) {
        final var set = new AssemblerToken[params.size()];
        for (int i = 0; i < params.size(); i++) {
          set[i] = params.get(i);
        }
        params.clear();
        macro.addParameter(set);
      }
    }
    if (!macro.hasCorrectNumberOfParameters()) {
      errors.put(current, S.getter("AssemblerMacroIncorrectNumberOfParameters"));
      return tokens.size();
    }
    final var macroTokens = macro.getMacroTokens();
    for (int i = 0; i < macroTokens.size(); i++) {
      final var asm = macroTokens.get(i);
      switch (asm.getType()) {
        case AssemblerToken.INSTRUCTION -> i += handleInstruction(macroTokens, i, asm);
        case AssemblerToken.MACRO -> i += handleMacros(macroTokens, i, asm, macros);
        default -> errors.put(asm, S.getter("AssemblerUnknownIdentifier"));
      }
    }
    return skip;
  }

  private int handleInstruction(List<AssemblerToken> tokens, int index, AssemblerToken current) {
    final var instruction = new AssemblerAsmInstruction(current, assembler.getInstructionSize(current.getValue()));
    final var acceptedParameters = assembler.getAcceptedParameterTypes();
    int skip = 0;
    if (index + 1 < tokens.size()) {
      final var params = new ArrayList<AssemblerToken>();
      AssemblerToken next;
      do {
        next = tokens.get(index + skip + 1);
        if (acceptedParameters.contains(next.getType())) {
          skip++;
          if (next.getType() == AssemblerToken.SEPERATOR) {
            if (params.isEmpty()) {
              errors.put(next, S.getter("AssemblerExpectedParameter"));
              return tokens.size();
            }
            final var set = new AssemblerToken[params.size()];
            for (int i = 0; i < params.size(); i++) set[i] = params.get(i);
            params.clear();
            instruction.addParameter(set);
          } else {
            params.add(next);
          }
        }
      } while (index + skip + 1 < tokens.size() && acceptedParameters.contains(next.getType()));
      if (!params.isEmpty()) {
        final var set = new AssemblerToken[params.size()];
        for (int i = 0; i < params.size(); i++) set[i] = params.get(i);
        params.clear();
        instruction.addParameter(set);
      }
    }
    checkIfActiveSection();
    sections.get(currentSection).addInstruction(instruction);
    return skip;
  }

  private void handleLabels(Map<String, Long> labels, AssemblerToken current) {
    if (!labels.containsKey(current.getValue())) {
      /* this should never happen as the assembler.java should have taken care of this */
      errors.put(current, S.getter("AssemblerUnknownLabel"));
      return;
    }
    if (labels.get(current.getValue()) >= 0) {
      errors.put(current, S.getter("AssemblerDuplicatedLabelNotSupported"));
      return;
    }
    checkIfActiveSection();
    labels.put(current.getValue(), sections.get(currentSection).getSectionEnd());
  }

  private int handleAsmInstructions(List<AssemblerToken> tokens, int index, AssemblerToken current, Map<String, Integer> defines) {
    if (current.getValue().equals(".section")) {
      /* we start a new section, hence the next parameter represents the section name */
      if (index + 1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingSectionName"));
        return 0;
      }
      AssemblerToken next = tokens.get(index + 1);
      if (next.getType() != AssemblerToken.MAYBE_LABEL
          && next.getType() != AssemblerToken.LABEL_IDENTIFIER
          && next.getType() != AssemblerToken.ASM_INSTRUCTION) {
        errors.put(current, S.getter("AssemblerExpectingSectionName"));
        return 0;
      }
      if (!addSection(next.getValue(), next)) {
        errors.put(next, S.getter("AssemblerDuplicatedSectionError"));
        return tokens.size();
      }
      return 1;
    }
    if (current.getValue().equals(".text")
        || current.getValue().equals(".data")
        || current.getValue().equals(".rodata")
        || current.getValue().equals(".bss")) {
      if (!addSection(current.getValue(), current)) {
        errors.put(current, S.getter("AssemblerDuplicatedSectionError"));
        return tokens.size();
      }
      return 0;
    }
    if (current.getValue().equals(".org")) {
      /* we expect a number after the org */
      if (index + 1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      AssemblerToken next = tokens.get(index + 1);
      if (!next.isNumber()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      long addr = SocSupport.convUnsignedInt(next.getNumberValue());
      checkIfActiveSection();
      sections.get(currentSection).setOrgInfo(addr);
      return 1;
    }
    if (current.getValue().equals(".zero")) {
      /* we expect a number after the .zero */
      if (index + 1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      AssemblerToken next = tokens.get(index + 1);
      if (!next.isNumber()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      if (next.getNumberValue() < 0) {
        errors.put(next, S.getter("AssemblerExpectingPositiveNumber"));
        return 1;
      }
      if (next.getNumberValue() > 0) {
        checkIfActiveSection();
        sections.get(currentSection).addZeroBytes(next.getNumberValue());
      }
      return 1;
    }
    if (AssemblerHighlighter.STRINGS.contains(current.getValue())) {
      /* we expect a String after .string */
      if (index + 1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingString"));
        return 0;
      }
      final var next = tokens.get(index + 1);
      if (next.getType() != AssemblerToken.STRING) {
        errors.put(current, S.getter("AssemblerExpectingString"));
        return 0;
      }
      checkIfActiveSection();
      sections.get(currentSection).addString(next.getValue());
      if (!current.getValue().equals(".ascii"))
        sections.get(currentSection).addByte((byte) 0);
      return 1;
    }
    if (AssemblerHighlighter.BYTES.contains(current.getValue())
        || AssemblerHighlighter.SHORTS.contains(current.getValue())
        || AssemblerHighlighter.INTS.contains(current.getValue())
        || AssemblerHighlighter.LONGS.contains(current.getValue())) {
      /* we expect at least one value */
      if (index + 1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      final var maxRange = AssemblerHighlighter.BYTES.contains(current.getValue()) ? 8 :
                     AssemblerHighlighter.SHORTS.contains(current.getValue()) ? 16 :
                     AssemblerHighlighter.INTS.contains(current.getValue()) ? 32 : -1;
      int skip = 0;
      AssemblerToken next;
      do {
        skip++;
        next = tokens.get(index + skip);
        if (!next.isNumber()) {
          errors.put(next, S.getter("AssemblerExpectingNumber"));
          return skip;
        }
        var value = next.getLongValue();
        if (maxRange > 0 &&  value >= (1L << maxRange)) {
          errors.put(next, S.getter("AssemblerValueOutOfRange"));
          return skip;
        }
        checkIfActiveSection();
        final var nrOfBytes = (maxRange < 0) ? 8 : maxRange >> 3;
        for (int i = 0; i < nrOfBytes; i++) {
          sections.get(currentSection).addByte((byte) (value & 0xFF));
          value >>= 8;
        }
        if (index + skip + 1 < tokens.size()) {
          next = tokens.get(index + skip + 1);
          if (next.getType() == AssemblerToken.SEPERATOR) skip++;
        }
      } while (index + skip < tokens.size() && next.getType() == AssemblerToken.SEPERATOR);
      return skip;
    }
    if (current.getValue().equals(".equ")) {
      if (index + 1 > tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectedLabel"));
        return 0;
      }
      int type = tokens.get(index + 1).getType();
      if (type != AssemblerToken.MAYBE_LABEL && type != AssemblerToken.PARAMETER_LABEL) {
        errors.put(tokens.get(index + 1), S.getter("AssemblerExpectedLabel"));
        return 1;
      }
      if (index + 2 > tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectedLabelAndNumber"));
        return 1;
      }
      if (!tokens.get(index + 2).isNumber()) {
        errors.put(tokens.get(index + 2), S.getter("AssemblerExpectedImmediateValue"));
        return 2;
      }
      final var label = tokens.get(index + 1).getValue();
      final var value = tokens.get(index + 2).getNumberValue();
      if (type == AssemblerToken.PARAMETER_LABEL) {
        errors.put(tokens.get(index + 1), S.getter("AssemblerDuplicatedName"));
        return 2;
      }
      defines.put(label, value);
      return 2;
    }
    errors.put(current, S.getter("AssemblerUnsupportedAssemblerInstruction"));
    return 0;
  }

  private void checkIfActiveSection() {
    if (currentSection < 0) {
      sections.add(new AssemblerSectionInfo());
      currentSection = 0;
    }
  }

  private boolean addSection(String name, AssemblerToken identifier) {
    /* first check if a section with this name exists */
    for (final var section : sections.getAll()) {
      if (section.getSectionName().equals(name)) return false;
    }
    if (currentSection < 0) {
      sections.add(new AssemblerSectionInfo(name, identifier));
      currentSection = 0;
    } else {
      long start = sections.get(currentSection).getSectionEnd();
      final var si = new AssemblerSectionInfo(start, name, identifier);
      sections.add(si);
      currentSection = sections.indexOf(si);
    }
    return true;
  }
}
