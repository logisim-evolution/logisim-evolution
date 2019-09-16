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

package com.cburch.logisim.soc.file;

import static com.cburch.logisim.soc.Strings.S;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElfProgramHeader {

  public final static int P_TYPE = 0;
  public final static int P_FLAGS = 1;
  public final static int P_OFFSET = 2;
  public final static int P_VADDR = 3;
  public final static int P_PADDR = 4;
  public final static int P_FILESZ = 5;
  public final static int P_MEMSZ = 6;
  public final static int P_ALIGN = 7;
  
  public final static int PT_NULL = 0;
  public final static int PT_LOAD = 1;
  public final static int PT_DYNAMIC = 2;
  public final static int PT_INTERP = 3;
  public final static int PT_NOTE = 4;
  public final static int PT_SHLIB = 5;
  public final static int PT_PHDR = 6;
  public final static int PT_LOPROC = 0x70000000;
  public final static int PT_HIPROC = 0x7FFFFFFF;
  private static final Map<Integer, String> PT_TYPES;
  static {
    Map<Integer,String> aMap = new HashMap<Integer,String>();
    aMap.put(PT_NULL, "PT_NULL");
    aMap.put(PT_LOAD, "PT_LOAD");
    aMap.put(PT_DYNAMIC, "PT_DYNAMIC");
    aMap.put(PT_INTERP, "PT_INTERP");
    aMap.put(PT_NOTE, "PT_NOTE");
    aMap.put(PT_SHLIB, "PT_SHLIB");
    aMap.put(PT_PHDR, "PT_PHDR");
    aMap.put(PT_LOPROC, "PT_LOPROC");
    aMap.put(PT_HIPROC, "PT_HIPROC");
    PT_TYPES = Collections.unmodifiableMap(aMap);
  }
  
  public final static int PF_X = 1;
  public final static int PF_W = 2;
  public final static int PF_R = 4;
  private static final Map<Integer, String> PF_FLAGS;
  static {
    Map<Integer,String> aMap = new HashMap<Integer,String>();
    aMap.put(PF_X, "PF_X");
    aMap.put(PF_W, "PF_W");
    aMap.put(PF_R, "PF_R");
    PF_FLAGS = Collections.unmodifiableMap(aMap);
  }
  
  
  private final static int SUCCESS = 0;
  private final static int PROGRAM_HEADER_NOT_FOUND_ERROR = 1;
  private final static int PROGRAM_HEADER_READ_ERROR = 2;
  private static final int PROGRAM_HEADER_SIZE_ERROR = 3;

  public class ProgramHeader {
    private Integer p_type;
    private Integer p_flags;
    private Long p_offset;
    private Long p_vaddr;
    private Long p_paddr;
    private Long p_filesz;
    private Long p_memsz;
    private Long p_align;
    private boolean is32Bit;
    
    public ProgramHeader(byte[] buffer , boolean is32Bit, boolean isLittleEndian, int offset) {
      int index = offset;
      this.is32Bit = is32Bit;
      p_type = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      int increment = (is32Bit) ? 4 : 8;
      if (!is32Bit) {
        p_flags = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
        index += 4;
      }
      p_offset = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      p_vaddr = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      p_paddr = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      p_filesz = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      p_memsz = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      if (is32Bit) {
        p_flags = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
        index += 4;
      }
      p_align = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    }
    
    public Object getValue(int identifier) {
      switch (identifier) {
        case P_TYPE : return p_type;
        case P_FLAGS : return p_flags;
        case P_OFFSET : return ElfHeader.returnCorrectValue(p_offset, is32Bit);
        case P_VADDR : return ElfHeader.returnCorrectValue(p_vaddr, is32Bit);
        case P_PADDR : return ElfHeader.returnCorrectValue(p_paddr, is32Bit);
        case P_FILESZ : return ElfHeader.returnCorrectValue(p_filesz, is32Bit);
        case P_MEMSZ : return ElfHeader.returnCorrectValue(p_memsz, is32Bit);
        case P_ALIGN : return ElfHeader.returnCorrectValue(p_align, is32Bit);
      }
      return null;
    }
    
    public String toString() {
      StringBuffer s = new StringBuffer();
      s.append("Program Header Info:\np_type   : ");
      if (PT_TYPES.containsKey(p_type))
        s.append(PT_TYPES.get(p_type));
      else
        s.append("unknown");
      s.append("\np_flags  : ");
      boolean first = true;
      for (int i : PF_FLAGS.keySet()) {
        if ((p_flags&i) != 0) {
          if (!first)
            s.append(", ");
          first = false;
          s.append(PF_FLAGS.get(i));
        }
      }
      s.append("\np_offset : "+String.format("0x%X", p_offset)+"\n");
      s.append("p_vaddr  : "+String.format("0x%X", p_vaddr)+"\n");
      s.append("p_paddr  : "+String.format("0x%X", p_paddr)+"\n");
      s.append("p_filesz : "+String.format("0x%X", p_filesz)+"\n");
      s.append("p_memsz  : "+String.format("0x%X", p_memsz)+"\n");
      s.append("p_align  : "+String.format("0x%X", p_align)+"\n\n");
      return s.toString();
    }
    
  }
  
  private int status;
  private ArrayList<ProgramHeader> headers;
  private long programHeaderSize;

  public ElfProgramHeader(FileInputStream file , ElfHeader elfHeader) {
	/* Important: the FileInputStream should be located at the beginning of the file (so directly after open) */
    status = SUCCESS;
    try {
      file.skip(elfHeader.getSize());
    } catch (IOException e) {
      status = PROGRAM_HEADER_NOT_FOUND_ERROR;
      return;
    }
    int nrOfProgramHeaders = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_PHNUM));
    int progHeaderEntrySize = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_PHENTSIZE));
    programHeaderSize = nrOfProgramHeaders*progHeaderEntrySize;
    byte[] buffer = new byte[(int) programHeaderSize];
    int nrRead = 0;
    try {
      nrRead = file.read(buffer);
    } catch (IOException e) {
      status = PROGRAM_HEADER_READ_ERROR;
      return;
    }
    if (nrRead != programHeaderSize) {
      status = PROGRAM_HEADER_SIZE_ERROR;
      return;
    }
    int index = 0;
    headers = new ArrayList<ProgramHeader>();
    for (int i = 0 ; i < nrOfProgramHeaders ; i++) {
      headers.add(new ProgramHeader(buffer,elfHeader.is32Bit(),elfHeader.isLittleEndian(),index));
      index += progHeaderEntrySize;
    }
  }
  
  public boolean isValid() {
    return status == SUCCESS;
  }
  
  public long getSize() {
    return programHeaderSize;
  }
  
  public String getErrorString() {
    switch (status) {
      case SUCCESS : return S.get("ProgHeaderSuccess");
      case PROGRAM_HEADER_NOT_FOUND_ERROR : return S.get("ProgHeaderNotFound");
      case PROGRAM_HEADER_READ_ERROR : return S.get("ProgHeaderReadError");
      case PROGRAM_HEADER_SIZE_ERROR : return S.get("ProgHeaderSizeError");
    }
    return "BUG: this should never happen in ElfProgramHeader";
  }
  
  public int getNrOfHeaders() {
    return headers.size();
  }
  
  public ProgramHeader getHeader(int index) {
    if (index < 0 || index >= headers.size())
      return null;
    return headers.get(index);
  }

}
