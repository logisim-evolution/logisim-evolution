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
  
  private static final int SUCCESS = 0;
  private static final int SECTION_HEADER_NOT_FOUND_ERROR = 1;
  private static final int SECTION_HEADER_READ_ERROR = 2;
  private static final int SECTION_HEADER_SIZE_ERROR = 3;

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
    }
    
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

  public boolean isValid() {
    return status == SUCCESS;
  }

  public String getErrorString() {
    switch (status) {
      case SUCCESS : return S.get("ElfSectHeadSuccess");
      case SECTION_HEADER_NOT_FOUND_ERROR : return S.get("ElfSectHeadNotFound");
      case SECTION_HEADER_READ_ERROR : return S.get("ElfSectHeadReadError");
      case SECTION_HEADER_SIZE_ERROR : return S.get("ElfSectHeadSizeError");
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
}