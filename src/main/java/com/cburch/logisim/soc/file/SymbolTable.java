/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.file;

public class SymbolTable {

  public static final int ST_NAME = 0;
  public static final int ST_VALUE = 1;
  public static final int ST_SIZE = 2;
  public static final int ST_INFO = 3;
  public static final int ST_OTHER = 4;
  public static final int ST_SHNDX = 5;

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

  public static final int SYMBOL_TABLE_SIZE = 16;

  private final Integer st_name;
  private final Integer st_value;
  private final Integer st_size;
  private final Integer st_info;
  private final Integer st_other;
  private final Integer st_shndx;
  private String name;

  public SymbolTable(byte[] buffer, boolean isLittleEndian, int offset) {
    int index = offset;
    st_name = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    st_value = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    st_size = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    st_info = ((int) buffer[index++]) & 0xFF;
    st_other = ((int) buffer[index++]) & 0xFF;
    st_shndx = ElfHeader.getIntValue(buffer, index, 2, isLittleEndian);
    name = "";
  }

  public SymbolTable(String name, int addr) {
    this.name = name;
    st_name = 0;
    st_value = addr;
    st_size = name.length() + 1;
    st_info = STT_FUNC;
    st_other = 0;
    st_shndx = 0;
  }

  public void setName(String val) {
    name = val;
  }

  public String getName() {
    return name;
  }

  public int getStType() {
    return st_info & 0xF;
  }

  public int getStBind() {
    return (st_info >> 4) & 0xF;
  }

  public Integer getValue(int identifier) {
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
