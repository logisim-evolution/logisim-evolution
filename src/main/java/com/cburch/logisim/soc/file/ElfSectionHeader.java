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

public class ElfSectionHeader {

  public static final int SH_NAME = 0;
  public static final int SH_TYPE = 1;
  public static final int SH_FLAGS = 2;
  public static final int SH_ADDR = 3;
  public static final int SH_OFFSET = 4;
  public static final int SH_SIZE = 5;
  public static final int SH_LINK = 6;
  public static final int SH_INFO = 7;
  public static final int SH_ADDRALIGN = 8;
  public static final int SH_ENTSIZE = 9;
  
  public static final int ST_NAME = 0;
  public static final int ST_VALUE = 1;
  public static final int ST_SIZE = 2;
  public static final int ST_INFO = 3;
  public static final int ST_OTHER = 4;
  public static final int ST_SHNDX = 5;
  
  public static final int SHT_NULL = 0;
  public static final int SHT_PROGBITS = 1;
  public static final int SHT_SYMTAB = 2;
  public static final int SHT_STRTAB = 3;
  public static final int SHT_RELA = 4;
  public static final int SHT_HASH = 5;
  public static final int SHT_DYNAMIC = 6;
  public static final int SHT_NOTE = 7;
  public static final int SHT_NOBITS = 8;
  public static final int SHT_REL = 9;
  public static final int SHT_SHLIB = 10;
  public static final int SHT_DYNSYM = 11;
  public static final int SHT_LOPROC = 0x70000000;
  public static final int SHT_HIPROC = 0x7fffffff;
  public static final int SHT_LOUSER = 0x80000000;
  public static final int SHT_HIUSER = 0xffffffff;
  
  public static final int SHF_WRITE = 1;
  public static final int SHF_ALLOC = 2;
  public static final int SHF_EXECINSTR = 4;
  public static final int SHF_MASKPROC = 0xf0000000;
  
  public static final int STN_UNDEF = 0;
  
  public static final int STB_LOCAL = 0;
  public static final int STB_GLOBAL = 1;
  public static final int STB_WEAK = 2;
  public static final int STB_LOPROC = 13;
  public static final int STB_HIPROC = 15;
  
  public static final int STT_NOTYPE = 0;
  public static final int STT_OBJECT = 1;
  public static final int STT_FUNC = 2;
  public static final int STT_SECTION = 3;
  public static final int STT_FILE = 4;
  public static final int STT_LOPROC = 13;
  public static final int STT_HIPROC = 15;
  
  private static final int SUCCESS = 0;
  private static final int SECTION_HEADER_NOT_FOUND_ERROR = 1;
  private static final int SECTION_HEADER_READ_ERROR = 2;
  private static final int SECTION_HEADER_SIZE_ERROR = 3;
  private static final int SECTION_STRING_TABLE_INDEX_ERROR = 4;
  private static final int SECTION_STRING_TABLE_WRONG_TYPE = 5;
  private static final int SECTION_STRING_TABLE_NOT_FOUND_ERROR = 6;
  private static final int SECTION_STRING_TABLE_READ_ERROR = 7;
  private static final int SYMBOL_TABLE_MULTIPLE_TABLES_NOT_SUPPORT = 8;
  private static final int SYMBOL_TABLE_MULTIPLE_STRING_TABLES_NOT_SUPPORT = 9;
  private static final int SYMBOL_TABLE_NOT_FOUND_ERROR = 10;
  private static final int SYMBOL_TABLE_READ_ERROR = 11;
  
  private static final int SYMBOL_TABLE_SIZE = 16;

  public class SectionHeader {
    private Integer sh_name;
    private Integer sh_type;
    private Long sh_flags;
    private Long sh_addr;
    private Long sh_offset;
    private Long sh_size;
    private Integer sh_link;
    private Integer sh_info;
    private Long sh_addrAlign;
    private Long sh_entsize;
    private boolean is32Bit;
    private String name;
    private ArrayList<SymbolTable> symbols;
    
    public SectionHeader(byte[] buffer , boolean is32Bit, boolean isLittleEndian, int offset) {
      this.is32Bit = is32Bit;
      int index = offset;
      int increment = is32Bit ? 4 : 8;
      sh_name = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      sh_type = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index+=4;
      sh_flags = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      sh_addr = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      sh_offset = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      sh_size = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      sh_link = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      sh_info = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index+=4;
      sh_addrAlign = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      index += increment;
      sh_entsize = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
      name = "";
      symbols = new ArrayList<SymbolTable>();
    }
    
    public void setName(String val) { name = val; }
    public String getName() { return name; }
    public void addSymbol( SymbolTable info ) { symbols.add(info); }
    public ArrayList<SymbolTable> getSymbols() { return symbols; }
    public boolean isWritable() { return (sh_flags&(long)SHF_WRITE) != 0L; }
    public boolean isAllocated() { return (sh_flags&(long)SHF_ALLOC) != 0L; }
    public boolean isExecutable() { return (sh_flags&(long)SHF_EXECINSTR) != 0L; }
    
    public Object getValue(int identifier) {
      switch (identifier) {
        case SH_NAME : return sh_name;
        case SH_TYPE : return sh_type;
        case SH_FLAGS : return ElfHeader.returnCorrectValue(sh_flags, is32Bit);
        case SH_ADDR : return ElfHeader.returnCorrectValue(sh_addr, is32Bit);
        case SH_OFFSET : return ElfHeader.returnCorrectValue(sh_offset, is32Bit);
        case SH_SIZE : return ElfHeader.returnCorrectValue(sh_size, is32Bit);
        case SH_LINK : return sh_link;
        case SH_INFO : return sh_info;
        case SH_ADDRALIGN : return ElfHeader.returnCorrectValue(sh_addrAlign, is32Bit);
        case SH_ENTSIZE : return ElfHeader.returnCorrectValue(sh_entsize, is32Bit);
      }
      return null;
    }
  }
  
  public class SymbolTable {
    private Integer st_name;
    private Integer st_value;
    private Integer st_size;
    private Integer st_info;
    private Integer st_other;
    private Integer st_shndx;
    private String name;
    
    public SymbolTable(byte[] buffer , boolean isLittleEndian, int offset) {
      int index = offset;
      st_name = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      st_value = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      st_size = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
      index += 4;
      st_info = ((int) buffer[index++])&0xFF;
      st_other = ((int) buffer[index++])&0xFF;
      st_shndx = ElfHeader.getIntValue(buffer, index, 2, isLittleEndian);
      name = "";
    }
    
    public void setName(String val) { name = val; }
    public String getName() { return name; }
    public int getStType() { return st_info&0xF; }
    public int getStBind() { return (st_info>>4)&0xF; }
    
    public Integer getValue(int identifier ) {
      switch (identifier) {
        case ST_NAME  : return st_name;
        case ST_VALUE : return st_value;
        case ST_SIZE  : return st_size;
        case ST_INFO  : return st_info;
        case ST_OTHER : return st_other;
        case ST_SHNDX : return st_shndx;
      }
      return null;
    }
  }
    
  private int status;
  private ArrayList<SectionHeader> headers;
  
  public ElfSectionHeader( FileInputStream file , ElfHeader elfHeader) {
	/* Important: the FileInputStream should be located at the beginning of the file (so directly after open) */
	status = SUCCESS;
    try {
      file.skip(ElfHeader.getLongValue(elfHeader.getValue(ElfHeader.E_SHOFF)));
    } catch (IOException e) {
      status = SECTION_HEADER_NOT_FOUND_ERROR;
      return;
    }
    int nrOfHeaders = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_SHNUM));
    int HeaderSize = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_SHENTSIZE));
    long BlockSize = nrOfHeaders*HeaderSize;
    byte[] buffer = new byte[(int) BlockSize];
    int nrRead = 0;
    try {
      nrRead = file.read(buffer);
    } catch (IOException e) {
      status = SECTION_HEADER_READ_ERROR;
      return;
	}
    if (nrRead != BlockSize) {
      status = SECTION_HEADER_SIZE_ERROR;
      return;
    }
    int index = 0;
    headers = new ArrayList<SectionHeader>();
    for (int i = 0 ; i < nrOfHeaders; i++) {
      headers.add(new SectionHeader(buffer,elfHeader.is32Bit(),elfHeader.isLittleEndian(),index));
      index += HeaderSize;
    }
  }
  
  private String getString(byte[] buffer, int index) {
    StringBuffer s = new StringBuffer();
    int idx = index;
    while (idx < buffer.length && buffer[idx] != 0)
      s.append((char)buffer[idx++]);
    return s.toString();
  }
  
  public boolean readSectionNames(FileInputStream file , ElfHeader elfHeader ) {
    /* Important: the FileInputStream should be located at the beginning of the file (so directly after open) */
    int idx = (int)elfHeader.getValue(ElfHeader.E_SHSTRNDX); 
    if (idx == SHT_NULL)
      return true;
    if (idx < 0 || idx >= headers.size()) {
      status = SECTION_STRING_TABLE_INDEX_ERROR;
      return false;
    }
    SectionHeader h = headers.get(idx);
    if ((int)h.getValue(SH_TYPE) != SHT_STRTAB) {
      status = SECTION_STRING_TABLE_WRONG_TYPE;
    }
    int size = ElfHeader.getIntValue(h.getValue(SH_SIZE));
    try {
      file.skip(ElfHeader.getLongValue(h.getValue(SH_OFFSET)));
    } catch (IOException e) {
      status = SECTION_STRING_TABLE_NOT_FOUND_ERROR;
      return false;
    }
    byte[] buffer = new byte[size];
    int nrRead = 0;
    try {
      nrRead = file.read(buffer);
    } catch (IOException e) {
      status = SECTION_STRING_TABLE_READ_ERROR;
      return false;
	}
    if (nrRead != size) {
      status = SECTION_STRING_TABLE_READ_ERROR;
      return false;
    }
    for (SectionHeader head : headers) 
      head.setName(getString(buffer,(int)head.getValue(SH_NAME)));
    return true;
  }
  
  public boolean readSymbolTable(FileInputStream file, ElfHeader elfHeader) {
    /* Important: the FileInputStream should be located at the beginning of the file (so directly after open) */
    SectionHeader strtab = null;
    SectionHeader shstrtab = null;
    int symtabidx = (int)elfHeader.getValue(ElfHeader.E_SHSTRNDX);
    for (int i = 0 ; i < headers.size() ; i++) {
      if (i == symtabidx) continue;
      SectionHeader sh = headers.get(i);
      if ((int)sh.getValue(SH_TYPE) == SHT_SYMTAB) {
        if (shstrtab != null) {
          status = SYMBOL_TABLE_MULTIPLE_TABLES_NOT_SUPPORT;
          return false;
        }
        shstrtab = sh;
      }
      if ((int)sh.getValue(SH_TYPE) == SHT_STRTAB) {
        if (strtab != null) {
          status = SYMBOL_TABLE_MULTIPLE_STRING_TABLES_NOT_SUPPORT;
          return false;
        }
        strtab = sh;
      }
    }
    if (shstrtab == null)
      return true;
    int symTableOffset = (int)ElfHeader.getIntValue(shstrtab.getValue(SH_OFFSET));
    int symTableSize = (int)ElfHeader.getIntValue(shstrtab.getValue(SH_SIZE));
    int strTableOffset = strtab == null ? 1 : (int)ElfHeader.getIntValue(strtab.getValue(SH_OFFSET));
    int strTableSize = strtab == null ? 1 : (int)ElfHeader.getIntValue(strtab.getValue(SH_SIZE));
    byte[] symBuffer = new byte[symTableSize];
    byte[] strBuffer = new byte[strTableSize];
    try {
      file.skip(symTableOffset < strTableOffset ? symTableOffset : strTableOffset);
    } catch (IOException e) {
      status = SYMBOL_TABLE_NOT_FOUND_ERROR;
      return false;
    }
    int nrRead = 0;
    try {
      nrRead = file.read(symTableOffset < strTableOffset ? symBuffer : strBuffer);
    } catch (IOException e) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
  	}
    if (nrRead != (symTableOffset < strTableOffset ? symTableSize : strTableSize)) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
    }
    int fileOffset = (symTableOffset < strTableOffset ? symTableOffset : strTableOffset)+nrRead;
    int toskip = (symTableOffset > strTableOffset ? symTableOffset : strTableOffset)-fileOffset;
    try {
      file.skip(toskip);
    } catch (IOException e) {
      status = SYMBOL_TABLE_NOT_FOUND_ERROR;
      return false;
    }
    try {
      nrRead = file.read(symTableOffset > strTableOffset ? symBuffer : strBuffer);
    } catch (IOException e) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
  	}
    if (nrRead != (symTableOffset > strTableOffset ? symTableSize : strTableSize)) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
    }
    if ((symTableSize%SYMBOL_TABLE_SIZE) != 0) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
    }
    int index = 0;
    while (index < symTableSize) {
      SymbolTable st = new SymbolTable(symBuffer,elfHeader.isLittleEndian(),index);
      index += SYMBOL_TABLE_SIZE;
      if (strtab != null)
        st.setName(getString(strBuffer,st.getValue(ST_NAME)));
      int headerIndex = st.getValue(ST_SHNDX);
      if (headerIndex != SHT_NULL && headerIndex < headers.size())
        headers.get(headerIndex).addSymbol(st);
    }
    return true;
  }

  public boolean isValid() {
    return status == SUCCESS;
  }

  public String getErrorString() {
    switch (status) {
      case SUCCESS : return S.get("ElfSectHeadSuccess");
      case SECTION_HEADER_NOT_FOUND_ERROR : return S.get("ElfSectHeadNotFound");
      case SECTION_HEADER_READ_ERROR : return S.get("ElfSectHeadReadError");
      case SECTION_HEADER_SIZE_ERROR : return S.get("ElfSectHeadSizeError");
      case SECTION_STRING_TABLE_INDEX_ERROR : return S.get("ElfSectHeadStingIdxError");
      case SECTION_STRING_TABLE_WRONG_TYPE : return S.get("ElfSectHeadStingTypeError");
      case SECTION_STRING_TABLE_NOT_FOUND_ERROR : return S.get("ElfSectHeadStingNotFound");
      case SECTION_STRING_TABLE_READ_ERROR : return S.get("ElfSectHeadStingReadError");
      case SYMBOL_TABLE_MULTIPLE_TABLES_NOT_SUPPORT : return S.get("ElfSectHeadMultiSymtabError");
      case SYMBOL_TABLE_MULTIPLE_STRING_TABLES_NOT_SUPPORT : return S.get("ElfSectHeadMultiStringtabError");
      case SYMBOL_TABLE_NOT_FOUND_ERROR : return S.get("ElfSymTableNotFound");
      case SYMBOL_TABLE_READ_ERROR : return S.get("ElfSymTableReadError");
    }
    return "BUG: This should never happen in ElfSectionHeader";
  }
  
  public int getNrOfHeaders() {
    return headers.size();
  }
  
  public SectionHeader getHeader(int index) {
    if (index < 0 || index >= headers.size())
      return null;
    return headers.get(index);
  }
  
  public ArrayList<SectionHeader> getHeaders() { return headers; }
}