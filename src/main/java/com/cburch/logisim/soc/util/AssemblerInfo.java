/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.util;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.file.SectionHeader;
import com.cburch.logisim.soc.file.SymbolTable;
import com.cburch.logisim.util.StringGetter;

public class AssemblerInfo {

  private class AssemblerSectionInfo extends SectionHeader {
    /* In his class, all locations between sectionStart and sectionEnd that have no value associated
     * are filled in with 0.
     */
    private long sectionStart;
    private long sectionEnd;
    private HashMap<Long,Byte> data;
    private HashMap<Long,AssemblerAsmInstruction> instructions;
    
    public AssemblerSectionInfo() {
      super("NoName");
      init(0);
    }
    
    public AssemblerSectionInfo(String name) {
      super(name);
      init(0); 
    }
    
    public AssemblerSectionInfo(long start, String name) {
      super(name);
      init(start); 
    }
      
    public String getSectionName() { return super.getName(); }
    public long getSectionEnd() { return sectionEnd; }
    public boolean hasInstructions() { return !instructions.isEmpty(); }
    
    public long getEntryPoint() {
      if (instructions.isEmpty()) return -1;
      long result = -1;
      for (Long x : instructions.keySet()) {
        if (result < 0 || x < result) result = x;
      }
      return result;
    }
    
    public void setOrgInfo( long address ) {
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
      for (int i = 0 ; i < str.length() ; i++) {
        if (str.charAt(i) == '\\') i++;
        if (i < str.length()) {
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
      instructions.put(sectionEnd, instr);
      sectionEnd += instr.getSizeInBytes();
    }
    
    public ArrayList<AssemblerToken> replaceLabels(HashMap<String,Long> labels) {
      ArrayList<AssemblerToken> errors = new ArrayList<AssemblerToken>();
      for (long addr : instructions.keySet()) {
        if (!instructions.get(addr).replaceLabels(labels))
          errors.add(instructions.get(addr).getInstruction());
      }
      for (String label : labels.keySet()) {
        long addr = labels.get(label);
        if (addr >= sectionStart && addr < sectionEnd) {
          SymbolTable st = new SymbolTable(label,SocSupport.convUnsignedLong(addr));
          super.addSymbol(st);
        }
      }
      return errors;
    }
    
    public HashMap<AssemblerToken,StringGetter> replaceInstructions(AssemblerInterface assembler) {
      HashMap<AssemblerToken,StringGetter> errors = new HashMap<AssemblerToken,StringGetter>();
      for (long addr : instructions.keySet()) {
        AssemblerAsmInstruction asm = instructions.get(addr);
        asm.setProgramCounter(addr);
        if (!assembler.assemble(asm)) {
          errors.putAll(asm.getErrors());
        } else {
          Byte[] bytes = asm.getBytes(); 
          for (int i = 0 ; i < asm.getSizeInBytes() ; i++)
            data.put(addr+i,bytes[i]); 
        }
      }
      return errors;
    }
    
    public boolean Download(SocProcessorInterface cpu, CircuitState state) {
      for (long i = sectionStart ; i < sectionEnd ; i++) {
    	byte datab = data.containsKey(i) ? data.get(i) : 0; 
        SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.WRITETransaction,
        		SocSupport.convUnsignedLong(i),datab,SocBusTransaction.ByteAccess,"Assembler");
        cpu.insertTransaction(trans, true, state);
        if (hasInstructions()) super.addExecutableFlag();
        super.setSize(sectionEnd-sectionStart);
        super.setStartAddress(sectionStart);
        if (trans.hasError()) return false;
      }
      return true;
    }
    
    private void init(long start) {
      sectionStart = start;
      sectionEnd = start;
      data = new HashMap<Long,Byte>();
      instructions = new HashMap<Long,AssemblerAsmInstruction>();
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
      ArrayList<AssemblerSectionInfo> ret = new ArrayList<AssemblerSectionInfo>();
      for (SectionHeader hdr : super.getHeaders()) 
        if (hdr instanceof AssemblerSectionInfo) 
          ret.add((AssemblerSectionInfo)hdr);
      return ret;
    }
  }
  
  private SectionHeaders sections;
  private HashMap<AssemblerToken,StringGetter> errors;
  private int currentSection;
  private AssemblerInterface assembler;

  public AssemblerInfo(AssemblerInterface assembler) {
    sections = new SectionHeaders();
    errors = new HashMap<AssemblerToken,StringGetter>();
    currentSection = -1;
    this.assembler = assembler;
  }
  
  public HashMap<AssemblerToken,StringGetter> getErrors() { return errors; }
  
  public void assemble(LinkedList<AssemblerToken> tokens, HashMap<String,Long> labels) {
    errors.clear();
    sections.clear();
    currentSection = -1;
    /* first pass: go through all tokens and mark the labels */
    for (int i = 0 ; i < tokens.size() ; i++) {
      AssemblerToken asm = tokens.get(i);
      switch (asm.getType()) {
        case AssemblerToken.ASM_INSTRUCTION : i += handleAsmInstructions(tokens,i,asm);
                                              continue;
        case AssemblerToken.LABEL           : handleLabels(labels,asm);
                                              continue;
        case AssemblerToken.INSTRUCTION     : i += handleInstruction(tokens,i,asm);
                                              continue;
        default                             : errors.put(asm, S.getter("AssemblerUnknownIdentifier"));
                                              continue;
      }
    }
    if (!errors.isEmpty()) return;
    /* second pass: replace all parameter labels by a value */
    for (String label : labels.keySet()) {
      if (labels.get(label) < 0) {
        /* this should never happen */
    	JOptionPane.showMessageDialog(null, "Severe bug in AssemblerInfo.java");
    	return;
      }
    }
    ArrayList<AssemblerToken> labelErrors = new ArrayList<AssemblerToken>(); 
    for (AssemblerSectionInfo section : sections.getAll()) labelErrors.addAll(section.replaceLabels(labels));
    if (!labelErrors.isEmpty()) {
      for (AssemblerToken error : labelErrors) {
        errors.put(error, S.getter("AssemblerCouldNotFindAddressForLabel"));
      }
      return;
    }
    /* last pass: transform instructions to bytes */
    for (AssemblerSectionInfo section : sections.getAll()) errors.putAll(section.replaceInstructions(assembler));
  }
  
  public boolean download(SocProcessorInterface cpu, CircuitState state) {
    for (AssemblerSectionInfo section : sections.getAll())
      if (!section.Download(cpu, state)) return false;
    return true;
  }
  
  public long getEntryPoint() {
	long entry = -1;
    for (AssemblerSectionInfo section : sections.getAll()) {
      if (section.hasInstructions()) {
        long sentry = section.getEntryPoint();
        if (entry < 0 || sentry < entry) entry = sentry;
      }
    }
    return entry;
  }
  
  public ElfSectionHeader getSectionHeader() { return sections; }
  
  private int handleInstruction(LinkedList<AssemblerToken> tokens, int index , AssemblerToken current) {
    AssemblerAsmInstruction instruction = new AssemblerAsmInstruction(current, assembler.getInstructionSize(current.getValue()));
    HashSet<Integer> acceptedParameters = new HashSet<Integer>();
    acceptedParameters.add(AssemblerToken.BRACKETED_REGISTER);
    acceptedParameters.add(AssemblerToken.DEC_NUMBER);
    acceptedParameters.add(AssemblerToken.HEX_NUMBER);
    acceptedParameters.add(AssemblerToken.PARAMETER_LABEL);
    acceptedParameters.add(AssemblerToken.REGISTER);
    acceptedParameters.add(AssemblerToken.SEPERATOR);
    int skip = 0;
    if (index+1 < tokens.size()) {
      ArrayList<AssemblerToken> params = new ArrayList<AssemblerToken>();
      AssemblerToken next;
      do {
    	next = tokens.get(index+skip+1);
    	if (acceptedParameters.contains(next.getType())) {
    	  skip++;
    	  if (next.getType() == AssemblerToken.SEPERATOR) {
    	    if (params.isEmpty()) {
    	      errors.put(next, S.getter("AssemblerExpectedParameter"));
    	      return tokens.size();
    	    }
    	    AssemblerToken[] set = new AssemblerToken[params.size()];
    	    for (int i = 0 ; i < params.size() ; i++) set[i] = params.get(i);
    	    params.clear();
    	    instruction.addParameter(set);
    	  } else {
    	    params.add(next);
    	  }
    	}
      } while (index+skip+1 < tokens.size() && acceptedParameters.contains(next.getType()));
      if (!params.isEmpty()) {
  	    AssemblerToken[] set = new AssemblerToken[params.size()];
  	    for (int i = 0 ; i < params.size() ; i++) set[i] = params.get(i);
  	    params.clear();
  	    instruction.addParameter(set);
      }
    }
    checkIfActiveSection();
    sections.get(currentSection).addInstruction(instruction);
    return skip;
  }
  
  private void handleLabels(HashMap<String,Long> labels, AssemblerToken current) {
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
  
  private int handleAsmInstructions(LinkedList<AssemblerToken> tokens, int index , AssemblerToken current) {
    if (current.getValue().equals(".section")) {
      /* we start a new section, hence the next parameter represents the section name */
      if (index+1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingSectionName"));
        return 0;
      }
      AssemblerToken next = tokens.get(index+1);
      if (next.getType() != AssemblerToken.MAYBE_LABEL && next.getType() != AssemblerToken.LABEL_IDENTIFIER &&
          next.getType() != AssemblerToken.ASM_INSTRUCTION) {
        errors.put(current, S.getter("AssemblerExpectingSectionName"));
        return 0;
      }
      if (!addSection(next.getValue())) {
        errors.put(next, S.getter("AssemblerDuplicatedSectionError"));
        return tokens.size();
      }
      return 1;
    }
    if (current.getValue().equals(".text") || current.getValue().equals(".data") ||
        current.getValue().equals(".rodata") || current.getValue().equals(".bss")) {
      if (!addSection(current.getValue())) {
        errors.put(current, S.getter("AssemblerDuplicatedSectionError"));
        return tokens.size();
      }
      return 0;
    }
    if (current.getValue().equals(".org")) {
      /* we expect a number after the org */
      if (index+1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      AssemblerToken next = tokens.get(index+1);
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
      if (index+1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      AssemblerToken next = tokens.get(index+1);
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
    if (current.getValue().equals(".string")) {
      /* we expect a String after .string */
      if (index+1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingString"));
        return 0;
      }
      AssemblerToken next = tokens.get(index+1);
      if (next.getType() != AssemblerToken.STRING) {
        errors.put(current, S.getter("AssemblerExpectingString"));
        return 0;
      }
      checkIfActiveSection();
      sections.get(currentSection).addString(next.getValue());
      return 1;
    }
    if (current.getValue().equals(".byte")) {
      /* we expect at least one value after .byte */
      if (index+1 >= tokens.size()) {
        errors.put(current, S.getter("AssemblerExpectingNumber"));
        return 0;
      }
      int skip = 0;
      AssemblerToken next;
      do {
    	skip++;
        next = tokens.get(index+skip);
        if (!next.isNumber()) {
          errors.put(current, S.getter("AssemblerExpectingNumber"));
          return skip-1;
        }
        if (next.getNumberValue() < 0 || next.getNumberValue() > 255) {
          errors.put(current, S.getter("AssemblerExpectingByteValue"));
          return skip-1;
        }
        checkIfActiveSection();
        sections.get(currentSection).addByte((byte)(next.getNumberValue()&0xFF));
        if (index+skip+1 < tokens.size()) {
          next = tokens.get(index+skip+1);
          if (next.getType() == AssemblerToken.SEPERATOR) skip++;
        }
      } while (index+skip < tokens.size() && next.getType() == AssemblerToken.SEPERATOR);
      return skip;
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
  
  private boolean addSection(String name) {
    /* first check if a section with this name exists */
    for (AssemblerSectionInfo section : sections.getAll()) {
      if (section.getSectionName().equals(name)) return false;
    }
    if (currentSection < 0) {
      sections.add(new AssemblerSectionInfo(name));
      currentSection = 0;
    } else {
      long start = sections.get(currentSection).getSectionEnd();
      AssemblerSectionInfo si = new AssemblerSectionInfo(start,name);
      sections.add(si);
      currentSection = sections.indexOf(si);
    }
    return true;
  }
}
