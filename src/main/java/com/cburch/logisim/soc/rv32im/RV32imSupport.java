/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    int[] shifts = {0, 20, 20, 19, 0, 11};
    if (type == R_TYPE) return 0;
    int result = (instruction < 0) ? Integer.MIN_VALUE : 0;
    result >>= shifts[type];
    int bits30_25 = (instruction >> 25) & 0x3F;
    int bits24_21 = (instruction >> 21) & 0xF;
    int bit20 = (instruction >> 20) & 0x1;
    int bits19_12 = (instruction >> 12) & 0xFF;
    int bits11_8 = (instruction >> 8) & 0xF;
    int bit7 = (instruction >> 7) & 0x1;
    switch (type) {
      case I_TYPE:
        result |= (bits30_25 << 5) | (bits24_21 << 1) | bit20;
        break;
      case S_TYPE:
        result |= (bits30_25 << 5) | (bits11_8 << 1) | bit7;
        break;
      case B_TYPE:
        result |= (bit7 << 11) | (bits30_25 << 5) | (bits11_8 << 1);
        break;
      case U_TYPE:
        result |= (bits30_25 << 25) | (bits24_21 << 21) | (bit20 << 20) | (bits19_12 << 12);
        break;
      case J_TYPE:
        result |= (bits19_12 << 12) | (bit20 << 11) | (bits30_25 << 5) | (bits24_21 << 1);
        break;
    }
    return result;
  }

  public static int getITypeInstruction(int opcode, int rd, int func3, int rs, int imm) {
    int instruction = opcode & 0x7F;
    instruction |= (rd & 0x1F) << 7;
    instruction |= (func3 & 0x7) << 12;
    instruction |= (rs & 0x1F) << 15;
    instruction |= (imm & 0xFFF) << 20;
    return instruction;
  }

  public static int getRTypeInstruction(
      int opcode, int rd, int func3, int rs1, int rs2, int funct7) {
    int instruction = opcode & 0x7F;
    instruction |= (rd & 0x1F) << 7;
    instruction |= (func3 & 0x7) << 12;
    instruction |= (rs1 & 0x1F) << 15;
    instruction |= (rs2 & 0x1F) << 20;
    instruction |= (funct7 & 0x7F) << 25;
    return instruction;
  }

  public static int getSTypeInstruction(int opcode, int rs1, int rs2, int funct3, int imm) {
    int instruction = opcode & 0x7F;
    instruction |= (funct3 & 0x7) << 12;
    instruction |= (rs1 & 0x1F) << 15;
    instruction |= (rs2 & 0x1F) << 20;
    instruction |= (imm & 0x1F) << 7;
    instruction |= ((imm >> 5) & 0x7F) << 25;
    return instruction;
  }

  public static int getJTypeInstruction(int opcode, int rd, int imm) {
    int instruction = opcode & 0x7F;
    instruction |= (rd & 0x1F) << 7;
    instruction |= ((imm >> 12) & 0xFF) << 12;
    instruction |= ((imm >> 11) & 1) << 20;
    instruction |= ((imm >> 1) & 0x3FF) << 21;
    instruction |= ((imm >> 20) & 1) << 31;
    return instruction;
  }

  public static int getBTypeInstruction(int opcode, int funct3, int rs1, int rs2, int imm) {
    int instruction = opcode & 0x7F;
    instruction |= (funct3 & 0x7) << 12;
    instruction |= (rs1 & 0x1F) << 15;
    instruction |= (rs2 & 0x1F) << 20;
    instruction |= ((imm >> 11) & 1) << 7;
    instruction |= ((imm >> 1) & 0xF) << 8;
    instruction |= ((imm >> 5) & 0x3F) << 25;
    instruction |= ((imm >> 12) & 1) << 31;
    return instruction;
  }

  public static int getUTypeInstruction(int opcode, int rd, int imm) {
    int instruction = opcode & 0x7F;
    instruction |= (rd & 0x1F) << 7;
    instruction |= (imm & 0xFFFFF) << 12;
    return instruction;
  }

  public static int getOpcode(int instruction) {
    return instruction & 0x7F;
  }

  public static int getFunct3(int instruction) {
    return (instruction >> 12) & 0x7;
  }

  public static int getDestinationRegisterIndex(int instruction) {
    return (instruction >> 7) & 0x1F;
  }

  public static int getSourceRegister1Index(int instruction) {
    return (instruction >> 15) & 0x1F;
  }

  public static int getSourceRegister2Index(int instruction) {
    return (instruction >> 20) & 0x1F;
  }

  public static int getFunct7(int instruction) {
    return (instruction >> 25) & 0x7F;
  }
}
