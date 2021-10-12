/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
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

  private final Integer stName;
  private final Integer stValue;
  private final Integer stSize;
  private final Integer stInfo;
  private final Integer stOther;
  private final Integer stShndx;
  private String name;

  public SymbolTable(byte[] buffer, boolean isLittleEndian, int offset) {
    var index = offset;
    stName = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    stValue = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    stSize = ElfHeader.getIntValue(buffer, index, 4, isLittleEndian);
    index += 4;
    stInfo = ((int) buffer[index++]) & 0xFF;
    stOther = ((int) buffer[index++]) & 0xFF;
    stShndx = ElfHeader.getIntValue(buffer, index, 2, isLittleEndian);
    name = "";
  }

  public SymbolTable(String name, int addr) {
    this.name = name;
    stName = 0;
    stValue = addr;
    stSize = name.length() + 1;
    stInfo = STT_FUNC;
    stOther = 0;
    stShndx = 0;
  }

  public void setName(String val) {
    name = val;
  }

  public String getName() {
    return name;
  }

  public int getStType() {
    return stInfo & 0xF;
  }

  public int getStBind() {
    return (stInfo >> 4) & 0xF;
  }

  public Integer getValue(int identifier) {
    return switch (identifier) {
      case ST_NAME ->  stName;
      case ST_VALUE -> stValue;
      case ST_SIZE -> stSize;
      case ST_INFO -> stInfo;
      case ST_OTHER -> stOther;
      case ST_SHNDX -> stShndx;
      default -> null;
    };
  }
}
