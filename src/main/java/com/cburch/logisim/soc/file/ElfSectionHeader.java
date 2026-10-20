/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.file;

import static com.cburch.logisim.soc.Strings.S;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ElfSectionHeader {

  public static final int STN_UNDEF = 0;

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

  private int status;
  private ArrayList<SectionHeader> headers;

  public ElfSectionHeader() {
    status = SUCCESS;
    headers = new ArrayList<>();
  }

  public ElfSectionHeader(FileInputStream file, ElfHeader elfHeader) {
    // Important: the FileInputStream should be located at the beginning of the file (so directly
    // after open)
    status = SUCCESS;
    try {
      file.skip(ElfHeader.getLongValue(elfHeader.getValue(ElfHeader.E_SHOFF)));
    } catch (IOException e) {
      status = SECTION_HEADER_NOT_FOUND_ERROR;
      return;
    }
    int nrOfHeaders = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_SHNUM));
    int HeaderSize = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_SHENTSIZE));
    long BlockSize = nrOfHeaders * HeaderSize;
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
    headers = new ArrayList<>();
    for (int i = 0; i < nrOfHeaders; i++) {
      headers.add(
          new SectionHeader(buffer, elfHeader.is32Bit(), elfHeader.isLittleEndian(), index));
      index += HeaderSize;
    }
  }

  private String getString(byte[] buffer, int index) {
    StringBuilder s = new StringBuilder();
    int idx = index;
    while (idx < buffer.length && buffer[idx] != 0)
      s.append((char) buffer[idx++]);
    return s.toString();
  }

  public boolean readSectionNames(FileInputStream file, ElfHeader elfHeader) {
    // Important: the FileInputStream should be located at the beginning of the file (so directly
    // after open)
    int idx = (int) elfHeader.getValue(ElfHeader.E_SHSTRNDX);
    if (idx == SectionHeader.SHT_NULL)
      return true;
    if (idx < 0 || idx >= headers.size()) {
      status = SECTION_STRING_TABLE_INDEX_ERROR;
      return false;
    }
    SectionHeader h = headers.get(idx);
    if ((int) h.getValue(SectionHeader.SH_TYPE) != SectionHeader.SHT_STRTAB) {
      status = SECTION_STRING_TABLE_WRONG_TYPE;
    }
    int size = ElfHeader.getIntValue(h.getValue(SectionHeader.SH_SIZE));
    try {
      file.skip(ElfHeader.getLongValue(h.getValue(SectionHeader.SH_OFFSET)));
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
      head.setName(getString(buffer, (int) head.getValue(SectionHeader.SH_NAME)));
    return true;
  }

  public boolean readSymbolTable(FileInputStream file, ElfHeader elfHeader) {
    // Important: the FileInputStream should be located at the beginning of the file (so directly
    // after open)
    SectionHeader strtab = null;
    SectionHeader shstrtab = null;
    int symtabidx = (int) elfHeader.getValue(ElfHeader.E_SHSTRNDX);
    for (int i = 0; i < headers.size(); i++) {
      if (i == symtabidx) continue;
      SectionHeader sh = headers.get(i);
      if ((int) sh.getValue(SectionHeader.SH_TYPE) == SectionHeader.SHT_SYMTAB) {
        if (shstrtab != null) {
          status = SYMBOL_TABLE_MULTIPLE_TABLES_NOT_SUPPORT;
          return false;
        }
        shstrtab = sh;
      }
      if ((int) sh.getValue(SectionHeader.SH_TYPE) == SectionHeader.SHT_STRTAB) {
        if (strtab != null) {
          status = SYMBOL_TABLE_MULTIPLE_STRING_TABLES_NOT_SUPPORT;
          return false;
        }
        strtab = sh;
      }
    }
    if (shstrtab == null)
      return true;
    int symTableOffset = ElfHeader.getIntValue(shstrtab.getValue(SectionHeader.SH_OFFSET));
    int symTableSize = ElfHeader.getIntValue(shstrtab.getValue(SectionHeader.SH_SIZE));
    int strTableOffset =
        strtab == null ? 1 : ElfHeader.getIntValue(strtab.getValue(SectionHeader.SH_OFFSET));
    int strTableSize =
        strtab == null ? 1 : ElfHeader.getIntValue(strtab.getValue(SectionHeader.SH_SIZE));
    byte[] symBuffer = new byte[symTableSize];
    byte[] strBuffer = new byte[strTableSize];
    try {
      file.skip(Math.min(symTableOffset, strTableOffset));
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
    int fileOffset = (Math.min(symTableOffset, strTableOffset)) + nrRead;
    int toskip = (Math.max(symTableOffset, strTableOffset)) - fileOffset;
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
    if ((symTableSize % SymbolTable.SYMBOL_TABLE_SIZE) != 0) {
      status = SYMBOL_TABLE_READ_ERROR;
      return false;
    }
    int index = 0;
    while (index < symTableSize) {
      SymbolTable st = new SymbolTable(symBuffer, elfHeader.isLittleEndian(), index);
      index += SymbolTable.SYMBOL_TABLE_SIZE;
      if (strtab != null)
        st.setName(getString(strBuffer, st.getValue(SymbolTable.ST_NAME)));
      int headerIndex = st.getValue(SymbolTable.ST_SHNDX);
      if (headerIndex != SectionHeader.SHT_NULL && headerIndex < headers.size())
        headers.get(headerIndex).addSymbol(st);
    }
    return true;
  }

  public boolean isValid() {
    return status == SUCCESS;
  }

  public String getErrorString() {
    return switch (status) {
      case SUCCESS -> S.get("ElfSectHeadSuccess");
      case SECTION_HEADER_NOT_FOUND_ERROR -> S.get("ElfSectHeadNotFound");
      case SECTION_HEADER_READ_ERROR -> S.get("ElfSectHeadReadError");
      case SECTION_HEADER_SIZE_ERROR -> S.get("ElfSectHeadSizeError");
      case SECTION_STRING_TABLE_INDEX_ERROR -> S.get("ElfSectHeadStingIdxError");
      case SECTION_STRING_TABLE_WRONG_TYPE -> S.get("ElfSectHeadStingTypeError");
      case SECTION_STRING_TABLE_NOT_FOUND_ERROR -> S.get("ElfSectHeadStingNotFound");
      case SECTION_STRING_TABLE_READ_ERROR -> S.get("ElfSectHeadStingReadError");
      case SYMBOL_TABLE_MULTIPLE_TABLES_NOT_SUPPORT -> S.get("ElfSectHeadMultiSymtabError");
      case SYMBOL_TABLE_MULTIPLE_STRING_TABLES_NOT_SUPPORT ->
          S.get("ElfSectHeadMultiStringtabError");
      case SYMBOL_TABLE_NOT_FOUND_ERROR -> S.get("ElfSymTableNotFound");
      case SYMBOL_TABLE_READ_ERROR -> S.get("ElfSymTableReadError");
      default -> "BUG: This should never happen in ElfSectionHeader";
    };
  }

  public int getNrOfHeaders() {
    return headers.size();
  }

  public void addHeader(SectionHeader hdr) {
    headers.add(hdr);
  }

  public int indexOf(SectionHeader hdr) {
    return headers.indexOf(hdr);
  }

  public void clear() {
    headers.clear();
  }

  public SectionHeader getHeader(int index) {
    if (index < 0 || index >= headers.size())
      return null;
    return headers.get(index);
  }

  public ArrayList<SectionHeader> getHeaders() {
    return headers;
  }
}
