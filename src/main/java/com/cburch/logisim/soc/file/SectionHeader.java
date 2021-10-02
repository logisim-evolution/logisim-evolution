/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.file;

import java.util.ArrayList;
import java.util.List;

public class SectionHeader {
  public static final int SH_NAME = 0;
  public static final int SH_TYPE = 1;
  public static final int SH_FLAGS = 2;
  public static final int SH_ADDR = 3;
  public static final int SH_OFFSET = 4;
  public static final int SH_SIZE = 5;
  public static final int SH_LINK = 6;
  public static final int SH_INFO = 7;
  public static final int SH_ADDR_ALIGN = 8;
  public static final int SH_ENTSIZE = 9;

  public static final int SHF_WRITE = 1;
  public static final int SHF_ALLOC = 2;
  public static final int SHF_EXEC_INSTR = 4;
  public static final int SHF_MASK_PROC = 0xf0000000;

  public static final int SHT_NULL = 0;
  public static final int SHT_PROG_BITS = 1;
  public static final int SHT_SYMTAB = 2;
  public static final int SHT_STRTAB = 3;
  public static final int SHT_RELA = 4;
  public static final int SHT_HASH = 5;
  public static final int SHT_DYNAMIC = 6;
  public static final int SHT_NOTE = 7;
  public static final int SHT_NO_BITS = 8;
  public static final int SHT_REL = 9;
  public static final int SHT_SHLIB = 10;
  public static final int SHT_DYN_SYM = 11;
  public static final int SHT_LO_PROC = 0x70000000;
  public static final int SHT_HI_PROC = 0x7fffffff;
  public static final int SHT_LO_USER = 0x80000000;
  public static final int SHT_HI_USER = 0xffffffff;


  private final Integer shName;
  private final Integer shType;
  private Long shFlags;
  private Long shAddr;
  private final Long shOffset;
  private Long shSize;
  private final Integer shLink;
  private final Integer shInfo;
  private final Long shAddrAlign;
  private final Long shEntSize;
  private final boolean is32Bit;
  private String name;
  private final ArrayList<SymbolTable> symbols;

  public SectionHeader(byte[] buffer, boolean is32Bit, boolean isLittleEndian, int offset) {
    this.is32Bit = is32Bit;
    int index = offset;
    int increment = is32Bit ? 4 : 8;
    shName = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    shType = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    shFlags = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    index += increment;
    shAddr = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    index += increment;
    shOffset = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    index += increment;
    shSize = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    index += increment;
    shLink = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    shInfo = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    shAddrAlign = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    index += increment;
    shEntSize = ElfHeader.getLongValue(buffer, index, increment, isLittleEndian);
    name = "";
    symbols = new ArrayList<>();
  }

  public SectionHeader(String name) {
    symbols = new ArrayList<>();
    this.name = name;
    is32Bit = true;
    shName = -1;
    shType = SHT_PROG_BITS;
    shFlags = (long) SHF_ALLOC;
    if (!name.equals(".rodata")) shFlags |= (long) SHF_WRITE;
    shAddr = 0L;
    shOffset = -1L;
    shSize = -1L;
    shLink = 0;
    shInfo = 0;
    shAddrAlign = 0L;
    shEntSize = 0L;
  }

  public void setName(String val) {
    name = val;
  }

  public String getName() {
    return name;
  }

  public void addSymbol(SymbolTable info) {
    symbols.add(info);
  }

  public List<SymbolTable> getSymbols() {
    return symbols;
  }

  public boolean isWritable() {
    return (shFlags & SHF_WRITE) != 0L;
  }

  public boolean isAllocated() {
    return (shFlags & SHF_ALLOC) != 0L;
  }

  public boolean isExecutable() {
    return (shFlags & SHF_EXEC_INSTR) != 0L;
  }

  public void setStartAddress(long addr) {
    shAddr = addr;
  }

  public void addExecutableFlag() {
    shFlags |= (long) SHF_EXEC_INSTR;
  }

  public void setSize(long size) {
    shSize = size;
  }

  public Object getValue(int identifier) {
    return switch (identifier) {
      case SH_NAME -> shName;
      case SH_TYPE -> shType;
      case SH_FLAGS -> ElfHeader.returnCorrectValue(shFlags, is32Bit);
      case SH_ADDR -> ElfHeader.returnCorrectValue(shAddr, is32Bit);
      case SH_OFFSET -> ElfHeader.returnCorrectValue(shOffset, is32Bit);
      case SH_SIZE -> ElfHeader.returnCorrectValue(shSize, is32Bit);
      case SH_LINK -> shLink;
      case SH_INFO -> shInfo;
      case SH_ADDR_ALIGN -> ElfHeader.returnCorrectValue(shAddrAlign, is32Bit);
      case SH_ENTSIZE -> ElfHeader.returnCorrectValue(shEntSize, is32Bit);
      default -> null;
    };
  }

}
