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

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2DataTransferInstructions implements AssemblerExecutionInterface {

  private static final int INSTR_LDW = 0;
  private static final int INSTR_LDH = 1;
  private static final int INSTR_LDHU = 2;
  private static final int INSTR_LDB = 3;
  private static final int INSTR_LDBU = 4;
  private static final int INSTR_LDWIO = 5;
  private static final int INSTR_LDHIO = 6;
  private static final int INSTR_LDHUIO = 7;
  private static final int INSTR_LDBIO = 8;
  private static final int INSTR_LDBUIO = 9;
  private static final int INSTR_STW = 10;
  private static final int INSTR_STH = 11;
  private static final int INSTR_STB = 12;
  private static final int INSTR_STWIO = 13;
  private static final int INSTR_STHIO = 14;
  private static final int INSTR_STBIO = 15;

  private final static String[] AsmOpcodes = {"LDW","LDH","LDHU","LDB","LDBU",
                                              "LDWIO","LDHIO","LDHUIO","LDBIO","LDBUIO",
                                              "STW","STH","STB",
                                              "STWIO","STHIO","STBIO" };
  private final static Integer[] AsmOpcs = {0x17,0x0F,0x0B,0x07,0x03,
                                            0x37,0x2F,0x2B,0x27,0x23,
                                            0x15,0x0d,0x05,
                                            0x35,0x2d,0x25 };

  private ArrayList<String> Opcodes;
  private ArrayList<Integer> OpcCodes; 
  
  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int immediate;
  private int base;
  private String errorMessage;
  
  public Nios2DataTransferInstructions() {
    Opcodes = new ArrayList<String>();
    OpcCodes = new ArrayList<Integer>();
    for (int i = 0 ; i < 16 ; i++) {
      Opcodes.add(AsmOpcodes[i].toLowerCase());
      OpcCodes.add(AsmOpcs[i]);
    }
  }

  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState cpuState = (Nios2State.ProcessorState) processorState;
    long address = SocSupport.convUnsignedInt(cpuState.getRegisterValue(base))+immediate;
    errorMessage = null;
    int toBeStored = cpuState.getRegisterValue(destination);
    int transType = -1;
    switch (operation) {
      case INSTR_STBIO  :
      case INSTR_STB    : toBeStored &= 0xFF;
                          transType = SocBusTransaction.ByteAccess;
      case INSTR_STHIO  :
      case INSTR_STH    : toBeStored &= 0xFFFF;
                          if (transType < 0 ) transType = SocBusTransaction.HalfWordAccess;
      case INSTR_STWIO  :
      case INSTR_STW    : if (transType < 0) transType = SocBusTransaction.WordAccess;
                          SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.WRITETransaction,
                              SocSupport.convUnsignedLong(address),toBeStored,transType,
                              cpuState.getMasterComponent());
                          cpuState.insertTransaction(trans, false, circuitState);
                          return !transactionHasError(trans);
      case INSTR_LDB    : 
      case INSTR_LDBIO  :
      case INSTR_LDBU   :
      case INSTR_LDBUIO : transType = SocBusTransaction.ByteAccess;
      case INSTR_LDH    :
      case INSTR_LDHIO  :
      case INSTR_LDHU   :
      case INSTR_LDHUIO : if (transType < 0) transType = SocBusTransaction.HalfWordAccess;
      case INSTR_LDW    :
      case INSTR_LDWIO  : if (transType < 0) transType = SocBusTransaction.WordAccess;
                          trans = new SocBusTransaction(SocBusTransaction.READTransaction,
                                  SocSupport.convUnsignedLong(address),0,transType,cpuState.getMasterComponent());
                                  cpuState.insertTransaction(trans, false, circuitState);
                          if (transactionHasError(trans)) return false;
                          int toBeLoaded = trans.getReadData();
                          switch (operation) {
                            case INSTR_LDBU    :
                            case INSTR_LDBUIO  : toBeLoaded &= 0xFF;
                                                 break;
                            case INSTR_LDB     :
                            case INSTR_LDBIO   : toBeLoaded <<= 24;
                                                 toBeLoaded >>= 24;
                                                 break;
                            case INSTR_LDHU    :
                            case INSTR_LDHUIO  : toBeLoaded &= 0xFFFF;
                                                 break;
                            case INSTR_LDH     :
                            case INSTR_LDHIO   : toBeLoaded <<= 16;
                                                 toBeLoaded >>= 16;
                                                 break;
                          }
                          cpuState.writeRegister(destination, toBeLoaded);
                          return true;
    }
    return false;
  }

  private boolean transactionHasError(SocBusTransaction trans) {
    if (trans.hasError()) {
      StringBuffer s = new StringBuffer();
      if (trans.isReadTransaction())
        s.append(S.get("LoadStoreErrorInReadTransaction")+"\n");
      else
        s.append(S.get("LoadStoreErrorInWriteTransaction")+"\n");
      s.append(trans.getErrorMessage());
      errorMessage = s.toString();
    }
    return trans.hasError();
  }

  public String getAsmInstruction() {
	if (!valid) return null;
	StringBuffer s = new StringBuffer();
	s.append(Opcodes.get(operation));
	while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
	s.append(Nios2State.registerABINames[destination]+","+immediate+"(");
	s.append(Nios2State.registerABINames[base]+")");
    return s.toString();
  }

  public int getBinInstruction() { return instruction; }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
    valid = false;
    if (!Opcodes.contains(instr.getOpcode().toLowerCase())) return false;
    operation = Opcodes.indexOf(instr.getOpcode().toLowerCase());
    valid = true;
    if (instr.getNrOfParameters() != 2) {
      valid = false;
      instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedTwoArguments"));
      return true;
    }
    AssemblerToken[] param2 = instr.getParameter(1);
    valid &= Nios2Support.isCorrectRegister(instr, 0);
    destination = Nios2Support.getRegisterIndex(instr,0);
    if (param2.length != 2) {
      valid = false;
      instr.setError(param2[0], S.getter("Nios2AssemblerExpectedImmediateIndexedRegister"));
    }
    if (!valid) return true;
    if (!param2[0].isNumber()) {
      valid = false;
      instr.setError(param2[0], S.getter("AssemblerExpectedImmediateValue"));
    }
    if (param2[1].getType() != AssemblerToken.BRACKETED_REGISTER) {
      valid = false;
      instr.setError(param2[1], S.getter("Nios2AssemblerExpectedBracketedRegister"));
    }
    if (!valid) return true;
    if (Nios2State.isCustomRegister(param2[1].getValue())) {
      valid = false;
      instr.setError(param2[1], S.getter("Nios2CannotUseCustomRegister"));
    }
    if (Nios2State.isControlRegister(param2[1].getValue())) {
        valid = false;
        instr.setError(param2[1], S.getter("Nios2CannotUseControlRegister"));
      }
    base = Nios2State.getRegisterIndex(param2[1].getValue());
    if (base < 0 || base > 31) {
      valid = false;
      instr.setError(param2[1], S.getter("AssemblerUnknownRegister"));
    }
    immediate = param2[0].getNumberValue();
    if (immediate >= (1<<15) || immediate < -(1<<15)) {
      valid = false;
      instr.setError(param2[0], S.getter("AssemblerImmediateOutOfRange"));
    }
    if (!valid) return true;
    instruction = Nios2Support.getITypeInstructionCode(base, destination, immediate, OpcCodes.get(operation));
    instr.setInstructionByteCode(instruction, 4);
    return true;
  }

  public boolean setBinInstruction(int instr) {
    valid = false;
    int opc = Nios2Support.getOpcode(instr);
    if (!OpcCodes.contains(opc)) return false;
    valid = true;
    instruction = instr;
    operation = OpcCodes.indexOf(opc);
    immediate = Nios2Support.getImmediate(instr, Nios2Support.I_TYPE);
    if (((immediate >>15)&1)!= 0) immediate |= 0xFFFF0000;
    base = Nios2Support.getRegAIndex(instr, Nios2Support.I_TYPE);
    destination = Nios2Support.getRegBIndex(instr, Nios2Support.I_TYPE);
    return valid;
  }

  public boolean performedJump() { return false; }
  public boolean isValid() { return valid; }
  public String getErrorMessage() { return errorMessage; }
  public ArrayList<String> getInstructions() { return Opcodes; }

  public int getInstructionSizeInBytes(String instruction) {
    if (Opcodes.contains(instruction.toLowerCase())) return 4;
    return -1;
  }

}
