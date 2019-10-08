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

import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.util.StringGetter;

public class AssemblerInfo {

  private class AssemblerSectionInfo {
    /* In his class, all locations between sectionStart and sectionEnd that have no value associated
     * are filled in with 0.
     */
    private long sectionStart;
    private long sectionEnd;
    private String sectionName;
    private HashMap<Long,Byte> data;
    private HashMap<Long,AssemblerAsmInstruction> instructions;
    
    public AssemblerSectionInfo() {
      init(0,"NoName");
    }
    
    public AssemblerSectionInfo(String name) {
      init(0,name); 
    }
    
    public AssemblerSectionInfo(long start, String name) {
      init(start,name); 
    }
      
    public String getSectionName() { return sectionName; }
    public long getSectionEnd() { return sectionEnd; }
    
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
    
    private void init(long start ,String name) {
      sectionStart = start;
      sectionEnd = start;
      sectionName = name;
      data = new HashMap<Long,Byte>();
      instructions = new HashMap<Long,AssemblerAsmInstruction>();
    }
  }
  
  private ArrayList<AssemblerSectionInfo> sections;
  private HashMap<AssemblerToken,StringGetter> errors;
  private int currentSection;
  private AssemblerInterface assembler;

  public AssemblerInfo(AssemblerInterface assembler) {
    sections = new ArrayList<AssemblerSectionInfo>();
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
    for (AssemblerSectionInfo section : sections) labelErrors.addAll(section.replaceLabels(labels));
    if (!labelErrors.isEmpty()) {
      for (AssemblerToken error : labelErrors) {
        errors.put(error, S.getter("AssemblerCouldNotFindAddressForLabel"));
      }
      return;
    }
    /* last pass: transform instructions to bytes */
    for (AssemblerSectionInfo section : sections) errors.putAll(section.replaceInstructions(assembler));
  }
  
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
    for (AssemblerSectionInfo section : sections) {
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
