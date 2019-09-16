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

package com.cburch.logisim.soc.rv32im;

public class RV32imSupport {
  public static final int ASM_FIELD_SIZE = 10;
  
  public static final int R_TYPE = 0;
  public static final int I_TYPE = 1;
  public static final int S_TYPE = 2;
  public static final int B_TYPE = 3;
  public static final int U_TYPE = 4;
  public static final int J_TYPE = 5;
  
  public static int getImmediateValue(int instruction, int type) {
	int[] shifts = {0,20,20,19,0,11};
    if (type == R_TYPE)
      return 0;
    int result = (instruction < 0) ? Integer.MIN_VALUE : 0;
    result >>= shifts[type];
    int bits30_25 = (instruction >>25)&0x3F;
    int bits24_21 = (instruction >>21)&0xF;
    int bit20 = (instruction >> 20)&0x1;
    int bits19_12 = (instruction >>12)&0xFF;
    int bits11_8 = (instruction >> 8)&0xF;
    int bit7 = (instruction >> 7)&0x1;
    switch (type) {
      case I_TYPE : result |= (bits30_25 << 5) | (bits24_21 << 1) | bit20;
                    break;
      case S_TYPE : result |= (bits30_25 << 5) | (bits11_8 << 1) | bit7;
                    break;
      case B_TYPE : result |= (bit7<<11) | (bits30_25 << 5) | (bits11_8 << 1);
                    break;
      case U_TYPE : result |= (bits30_25<<25) | (bits24_21<<21) | (bit20<<20) | (bits19_12<<12);
                    break;
      case J_TYPE : result |= (bits19_12<<12) | (bit20 << 11) | (bits30_25 << 5) | (bits24_21<< 1);
                    break;
    }
    return result;
  }
  
  public static int getOpcode(int instruction) {
    return instruction&0x7F;
  }
  
  public static int getFunct3(int instruction) {
    return (instruction >> 12)&0x7;
  }
  
  public static int getDestinationRegisterIndex(int instruction) {
    return (instruction >> 7)&0x1F;
  }
  
  public static int getSourceRegister1Index(int instruction) {
    return (instruction >>15)&0x1F;
  }

  public static int getSourceRegister2Index(int instruction) {
    return (instruction >>20)&0x1F;
  }
  
  public static int getFunct7(int instruction) {
    return (instruction >>25)&0x7F;
  }
}
