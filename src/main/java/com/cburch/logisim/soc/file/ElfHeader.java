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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElfHeader {

  public static final int ELF_HEADER_CORRECT = 0;

  public static final int EI_MAG0 = 0x0;
  public static final byte EI_MAG0_VALUE = 0x7F;
  public static final int EI_MAG1 = 0x1;
  public static final byte EI_MAG1_VALUE = 0x45;
  public static final int EI_MAG2 = 0x2;
  public static final byte EI_MAG2_VALUE = 0x4C;
  public static final int EI_MAG3 = 0x3;
  public static final byte EI_MAG3_VALUE = 0x46;
  public static final int EI_CLASS = 0x4;
  public static final byte EI_CLASS_32 = 0x1;
  public static final byte EI_CLASS_64 = 0x2;
  public static final int EI_DATA = 0x5;
  public static final byte EI_DATA_LITTLE_ENDIAN = 0x1;
  public static final byte EI_DATA_BIG_ENDIAN = 0x2;
  public static final int EI_VERSION = 0x6;
  public static final int EI_OSABI = 0x7;
  public static final int EI_ABIVERSION = 0x8;
  public static final int EI_PAD = 0x9;
  public static final int E_IDENT_SIZE = 0x10;
  
  private static final int EI_ERROR_READING_FILE = 0x1;
  private static final int EI_SIZE_ERROR = 0x2;
  private static final int EI_MAGIC_ERROR = 0x4;
  private static final int EI_CLASS_ERROR = 0x8;
  private static final int EI_DATA_ERROR = 0x10;
  
  public static final int ELF_HEADER_SIZE_32 = 0x34;
  public static final int ELF_HEADER_SIZE_64 = 0x40;
  
  public static final int E_TYPE = 0x11;
  public static final int E_MACHINE = 0x12;
  public static final int E_VERSION = 0x13;
  public static final int E_ENTRY = 0x14;
  public static final int E_PHOFF = 0x15;
  public static final int E_SHOFF = 0x16;
  public static final int E_FLAGS = 0x17;
  public static final int E_EHSIZE = 0x18;
  public static final int E_PHENTSIZE = 0x19;
  public static final int E_PHNUM = 0x1A;
  public static final int E_SHENTSIZE = 0x1B;
  public static final int E_SHNUM = 0x1C;
  public static final int E_SHSTRNDX = 0x1D;
  
  private static final int E_SIZE_ERROR = 0x20;
  
  public static final int ET_NONE = 0x00;
  public static final int ET_REL = 0x01;
  public static final int ET_EXEC = 0x02;
  public static final int ET_DYN = 0x03;
  public static final int ET_CORE = 0x04;
  public static final int ET_LOOS = 0xfe00;
  public static final int ET_HIOS = 0xfeff;
  public static final int ET_LOPROC = 0xff00;
  public static final int ET_HIPROC = 0xffff;
  
  /* source for the below constants is : https://docs.rs/goblin/0.0.13/i686-unknown-linux-gnu/goblin/elf/header/ */
  public static final int EM_OPENRISC = 92;
  public static final int EM_INTEL_NIOS2 = 113;
  public static final int EM_RISCV = 243;
  private static final Map<Integer, String> ARCHITECTURES;
  static {
    Map<Integer,String> aMap = new HashMap<Integer,String>();
    aMap.put(EM_OPENRISC, "Open Risc");
    aMap.put(EM_INTEL_NIOS2, "Nios II");
    aMap.put(EM_RISCV, "Risc V");
    ARCHITECTURES = Collections.unmodifiableMap(aMap);
  }
  
  
  public final static long LONGMASK = Long.parseUnsignedLong("00FFFFFFFFFFFFFF", 16);
  public final static long LONGINTMASK = Long.parseUnsignedLong("00000000FFFFFFFF", 16);
  public final static int INTMASK = Integer.parseUnsignedInt("00FFFFFF", 16);

  private class EInfo {
    private Integer e_type;
    private Integer e_machine;
    private Integer e_version;
    private Long e_entry;
    private Long e_phoff;
    private Long e_shoff;
    private Integer e_flags;
    private Integer e_ehsize;
    private Integer e_phentsize;
    private Integer e_phnum;
    private Integer e_shentsize;
    private Integer e_shnum;
    private Integer e_shstrndx;
    private boolean is32Bit;
    
    
    public EInfo (byte[] buffer , boolean is32Bit, boolean isLittleEndian) {
      this.is32Bit = is32Bit;
      int index = 0;
      int fieldSize = is32Bit ? 4 : 8;
      e_type = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_machine = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_version = getIntValue(buffer,index,4,isLittleEndian);
      index +=4;
      e_entry = getLongValue(buffer,index,fieldSize,isLittleEndian);
      index += fieldSize;
      e_phoff = getLongValue(buffer,index,fieldSize,isLittleEndian);
      index += fieldSize;
      e_shoff = getLongValue(buffer,index,fieldSize,isLittleEndian);
      index += fieldSize;
      e_flags = getIntValue(buffer,index,4,isLittleEndian);
      index +=4;
      e_ehsize = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_phentsize = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_phnum = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_shentsize = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_shnum = getIntValue(buffer,index,2,isLittleEndian);
      index +=2;
      e_shstrndx = getIntValue(buffer,index,2,isLittleEndian);
    }
    
	public Object getValue(int identifier) {
      switch (identifier) {
        case E_TYPE : return e_type;
        case E_MACHINE : return e_machine;
        case E_VERSION : return e_version;
        case E_ENTRY : return returnCorrectValue(e_entry,is32Bit); 
        case E_PHOFF : return returnCorrectValue(e_phoff,is32Bit); 
        case E_SHOFF : return returnCorrectValue(e_shoff,is32Bit); 
        case E_FLAGS : return e_flags;
        case E_EHSIZE : return e_ehsize;
        case E_PHENTSIZE : return e_phentsize;
        case E_PHNUM : return e_phnum;
        case E_SHENTSIZE : return e_shentsize;
        case E_SHNUM : return e_shnum;
        case E_SHSTRNDX : return e_shstrndx;
      }
      return null;
    }
    
  }
  
  private int status = ELF_HEADER_CORRECT;
  private EInfo eInfo;
  private byte[] e_ident = new byte[E_IDENT_SIZE];
  
  public ElfHeader(FileInputStream file) {
    int nrRead = -1;
    try {
      nrRead = file.read(e_ident);
    } catch (IOException e) {
      status |= EI_ERROR_READING_FILE;
      return;
    }
    if (nrRead != E_IDENT_SIZE) {
      status |= EI_SIZE_ERROR;
      return;
    }
    if (!isElfFile()) {
      status |= EI_MAGIC_ERROR;
      return;
    }
    if (!isCorrectClass()) {
      status |= EI_CLASS_ERROR;
      return;
    }
    if (!isCorrectEncoding()) {
      status |= EI_DATA_ERROR;
      return;
    }
    int hsize = is32Bit() ? ELF_HEADER_SIZE_32 : ELF_HEADER_SIZE_64;
    hsize -= E_IDENT_SIZE;
    byte[] buffer = new byte[hsize];
    try {
      nrRead = file.read(buffer);
    } catch (IOException e) {
      status |= EI_ERROR_READING_FILE;
      return;
    }
    if (nrRead != hsize) {
      status |= E_SIZE_ERROR;
      return;
    }
    eInfo = new EInfo(buffer,is32Bit(),isLittleEndian());
  }
  
  public Object getValue(int field) {
    if (!isValid())
      return null;
    if (field >= E_TYPE && field <= E_SHSTRNDX)
      return eInfo.getValue(field);
    if (field < EI_PAD)
      return e_ident[field];
    if (field == EI_PAD) {
      Byte[] pad = new Byte[7];
      for (int i = 0 ; i < 7 ; i++)
        pad[i] = e_ident[EI_PAD+i];
      return pad;
    }
    return null;
  }
  
  public boolean isValid() {
    return status == ELF_HEADER_CORRECT;
  }
  
  public boolean is32Bit() {
    return e_ident[EI_CLASS] == EI_CLASS_32;
  }
  
  public boolean isLittleEndian() {
    return e_ident[EI_DATA] == EI_DATA_LITTLE_ENDIAN;
  }
  
  public boolean isElfFile() {
    return (e_ident[EI_MAG0] == EI_MAG0_VALUE) &&
           (e_ident[EI_MAG1] == EI_MAG1_VALUE) &&
           (e_ident[EI_MAG2] == EI_MAG2_VALUE) &&
           (e_ident[EI_MAG3] == EI_MAG3_VALUE);
  }
  
  public int getElfHeaderSize() {
    return getIntValue(eInfo.getValue(E_EHSIZE));
  }
  
  public String getArchitectureString(int arch) {
    if (ARCHITECTURES.containsKey(arch))
      return ARCHITECTURES.get(arch);
    return S.get("ElfHeaderUnknownArchitecture");
  }
  
  public String getErrorString() {
    if (status == ELF_HEADER_CORRECT)
      return S.get("ElfHeaderNoErrors");
    StringBuffer s = new StringBuffer();
    boolean insertNl = false;
    if (status == EI_ERROR_READING_FILE) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderReadingFileError"));
      insertNl = true;
    }
    if (status == EI_SIZE_ERROR) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderIncorrectEISize"));
      insertNl = true;
    }
    if (status == EI_MAGIC_ERROR) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderIncorrectMagic"));
      insertNl = true;
    }
    if (status == EI_CLASS_ERROR) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderEIClassError"));
      insertNl = true;
    }
    if (status == EI_DATA_ERROR) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderEIDataError"));
      insertNl = true;
    }
    if (status == E_SIZE_ERROR) {
      s.append((insertNl?"\n":"")+S.get("ElfHeaderIncorrectESize"));
      insertNl = true;
    }
    return s.toString();
  }
  
  public long getSize() {
	return getLongValue(getValue(ElfHeader.E_PHOFF));
  }
  
  public static long getLongValue(byte[] buffer, int startIndex, int NrOfBytes, boolean isLittleEndian) {
    long result = 0;
    for (int i = 0 ; i < NrOfBytes ; i++) {
      int index = startIndex+i;
      long value = (index < buffer.length) ? ((long)buffer[index])&0xFF : 0;
      if (isLittleEndian) {
        result >>= 8;
        result &= LONGMASK; /* prevent sign extensions */
        result |= value << (NrOfBytes-1)*8;
      } else {
        result <<= 8;
        result |= value;
      }
    }
    return result;
  }

  public static int getIntValue(byte[] buffer, int startIndex, int NrOfBytes, boolean isLittleEndian) {
    int result = 0;
    for (int i = 0 ; i < NrOfBytes ; i++) {
      int index = startIndex+i;
      int value = (index < buffer.length) ? ((int)buffer[startIndex+i])&0xFF : 0;
      if (isLittleEndian) {
        result >>= 8;
        result &= INTMASK; /* prevent sign extensions */
        result |= value << (NrOfBytes-1)*8;
      } else {
        result <<= 8;
        result |= value;
      }
    }
    return result;
  }
  
  public static Object returnCorrectValue(Long value, boolean is32Bit) {
	Integer intVal = Integer.parseUnsignedInt(String.format("%08X", value&LONGINTMASK), 16);
	if (is32Bit)
	  return intVal;
    return value; 
  }
  
  public static int getIntValue(Object v) {
    if (v instanceof Integer)
      return (int)v;
    if (v instanceof Long)
      return Integer.parseUnsignedInt(String.format("%08X", ((Long)v)&LONGINTMASK), 16);
    return 0;
  }
  
  public static long getLongValue(Object v) {
    if (v instanceof Integer)
      return Long.parseUnsignedLong(String.format("%08X", (Integer)v),16);
    if (v instanceof Long)
      return (long)v;
    return 0;
  }
  
  private boolean isCorrectClass() {
    return (e_ident[EI_CLASS] == EI_CLASS_32) ||
           (e_ident[EI_CLASS] == EI_CLASS_64);
  }
  
  private boolean isCorrectEncoding() {
    return (e_ident[EI_DATA] == EI_DATA_LITTLE_ENDIAN) ||
           (e_ident[EI_DATA] == EI_DATA_BIG_ENDIAN);
  }
}
