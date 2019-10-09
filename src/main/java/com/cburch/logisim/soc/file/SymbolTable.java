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
  
  public SymbolTable(String name, int addr) {
    this.name = name;
    st_name = 0;
    st_value = addr;
    st_size = name.length()+1;
    st_info = STT_FUNC;
    st_other = 0;
    st_shndx = 0;
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
