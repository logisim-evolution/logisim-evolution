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

import java.util.ArrayList;

import com.cburch.logisim.soc.file.SymbolTable;

public class SectionHeader {
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

  public static final int SHF_WRITE = 1;
  public static final int SHF_ALLOC = 2;
  public static final int SHF_EXECINSTR = 4;
  public static final int SHF_MASKPROC = 0xf0000000;
  
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
  
  public SectionHeader(String name) {
    symbols = new ArrayList<SymbolTable>();
    this.name = name;
    is32Bit = true;
    sh_name = -1;
    sh_type = SHT_PROGBITS;
    sh_flags = (long)SHF_ALLOC;
    if (!name.equals(".rodata")) sh_flags |= (long)SHF_WRITE;
    sh_addr = 0L;
    sh_offset = -1L;
    sh_size = -1L;
    sh_link = 0;
    sh_info = 0;
    sh_addrAlign = 0L;
    sh_entsize = 0L; 
  }
  
  public void setName(String val) { name = val; }
  public String getName() { return name; }
  public void addSymbol( SymbolTable info ) { symbols.add(info); }
  public ArrayList<SymbolTable> getSymbols() { return symbols; }
  public boolean isWritable() { return (sh_flags&(long)SHF_WRITE) != 0L; }
  public boolean isAllocated() { return (sh_flags&(long)SHF_ALLOC) != 0L; }
  public boolean isExecutable() { return (sh_flags&(long)SHF_EXECINSTR) != 0L; }
  public void setStartAddress(long addr) {sh_addr = addr;}
  public void addExecutableFlag() {sh_flags |= (long) SHF_EXECINSTR; }
  public void setSize(long size) {sh_size = size;}
  
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
