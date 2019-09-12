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

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.file.ElfHeader;

public class RV32imLoadAndStoreInstructions implements RV32imExecutionUnitInterface {

  private static final int LOAD = 0x3;
  private static final int STORE = 0x23;
  private static final int LB = 0;
  private static final int LH = 1;
  private static final int LW = 2;
  private static final int LBU = 4;
  private static final int LHU = 5;
  
  private static final int INSTR_LB = 0;
  private static final int INSTR_LH = 1;
  private static final int INSTR_LW = 2;
  private static final int INSTR_LBU = 3;
  private static final int INSTR_LHU = 4;
  private static final int INSTR_SB = 5;
  private static final int INSTR_SH = 6;
  private static final int INSTR_SW = 7;
  private static final int INSTR_SBZ = 8;
  private static final int INSTR_SHZ = 9;
  private static final int INSTR_SWZ = 10;
  
  private static final String[] AsmOpcodes = {"LB","LH","LW","LBU","LHU","SB","SH","SW","SBZ","SHZ","SWZ"};

  private int instruction = 0;
  private boolean valid = false;
  private int operation;
  private int destination;
  private int immediate;
  private int base;
  private String errorMessage;
  
  public boolean execute(RV32im_state state) {
    if (!valid)
      return false;
    errorMessage = null;
    int toBeStored = state.getRegisterValue(destination);
    long address = ElfHeader.getLongValue((Integer)state.getRegisterValue(base))+immediate;
    int transType = -1;
    switch (operation) {
      case INSTR_SBZ:
      case INSTR_SB : toBeStored &= 0xFF;
                      transType = SocBusTransaction.ByteAccess;
      case INSTR_SHZ:
      case INSTR_SH : toBeStored &= 0xFFFF;
                      if (transType < 0 ) transType = SocBusTransaction.HalfWordAccess;
      case INSTR_SWZ:
      case INSTR_SW : if (transType < 0) transType = SocBusTransaction.WordAccess;
                      SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.WRITETransaction,
                          ElfHeader.getIntValue((Long)address),toBeStored,transType,
                          state.getMasterName());
                      SocBusTransaction ret = state.insertTransaction(trans, false);
                      return !transactionHasError(ret);
      case INSTR_LB :
      case INSTR_LBU: transType = SocBusTransaction.ByteAccess;
      case INSTR_LH :
      case INSTR_LHU: if (transType < 0 ) transType = SocBusTransaction.HalfWordAccess;
      case INSTR_LW : if (transType < 0) transType = SocBusTransaction.WordAccess;
                      trans = new SocBusTransaction(SocBusTransaction.READTransaction,
                          ElfHeader.getIntValue((Long)address),0,transType,state.getMasterName());
                      ret = state.insertTransaction(trans, false);
                      if (transactionHasError(ret)) return false;
                      int toBeLoaded = ret.getData();
                      switch (operation) {
                        case INSTR_LBU : toBeLoaded &= 0xFF;
                                         break;
                        case INSTR_LB  : toBeLoaded <<= 24;
                                         toBeLoaded >>= 24;
                                         break;
                        case INSTR_LHU : toBeLoaded &= 0xFFFF;
                                         break;
                        case INSTR_LH  : toBeLoaded <<= 16;
                                         toBeLoaded >>= 16;
                                         break;
                      }
                      state.writeRegister(destination, toBeLoaded);
                      return true;
    }
    return false;
  }
  
  private boolean transactionHasError(SocBusTransaction trans) {
    if (trans.hasError()) {
      StringBuffer s = new StringBuffer();
      if (trans.isReadTransaction())
        s.append(S.get("RV32imLoadStoreErrorInReadTransaction")+"\n");
      else
        s.append(S.get("RV32imLoadStoreErrorInWriteTransaction")+"\n");
      s.append(trans.getErrorMessage());
      errorMessage = s.toString();
    }
    return trans.hasError();
  }

  public String getAsmInstruction() {
    if (!valid)
      return null;
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    switch (operation) {
      case INSTR_SBZ:
      case INSTR_SHZ:
      case INSTR_SWZ: s.append(immediate);
                      s.append("("+RV32im_state.registerABINames[base]+")");
                      break;
      default       : s.append(RV32im_state.registerABINames[destination]+",");
                      s.append(immediate);
                      s.append("("+RV32im_state.registerABINames[base]+")");
                      break;
    }
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setAsmInstruction(String instr) {
    valid = false;
    return valid;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  public boolean performedJump() { return false; }

  public boolean isValid() { return valid; }
  
  private boolean decodeBin() {
    int opcode = RV32imSupport.getOpcode(instruction);
    if (opcode == LOAD) {
      destination = RV32imSupport.getDestinationRegisterIndex(instruction);
      immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.I_TYPE);
      base = RV32imSupport.getSourceRegister1Index(instruction);
      int funct3 = RV32imSupport.getFunct3(instruction); 
      switch (funct3) {
        case LB  :
        case LH  :
        case LW  : operation = funct3;
                   return true;
        case LBU :
        case LHU : operation = funct3-1;
                   return true;
        default  : return false;
      }
    }
    if (opcode == STORE) {
      int funct3 = RV32imSupport.getFunct3(instruction);
      if (funct3 > 2)
        return false;
      operation = funct3+5;
      immediate = RV32imSupport.getImmediateValue(instruction, RV32imSupport.S_TYPE);
      base = RV32imSupport.getSourceRegister1Index(instruction);
      destination = RV32imSupport.getSourceRegister2Index(instruction);
      if (destination == 0)
        operation += 3;
      return true;
    }
    return false;
  }

  public String getErrorMessage() { return errorMessage; }

}
