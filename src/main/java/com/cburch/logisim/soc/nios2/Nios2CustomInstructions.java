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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.soc.util.AssemblerAsmInstruction;
import com.cburch.logisim.soc.util.AssemblerExecutionInterface;
import com.cburch.logisim.soc.util.AssemblerToken;

public class Nios2CustomInstructions implements AssemblerExecutionInterface {

  private static final int CUSTOM = 0x32;

  private int instruction;
  private boolean valid = false;
  private int regA,regB,regC;
  private boolean readra,readrb,writerc;
  private int n;
  private boolean custActive = false;
  
  public boolean execute(Object processorState, CircuitState circuitState) {
    if (!valid) return false;
    Nios2State.ProcessorState state = (Nios2State.ProcessorState)processorState;
    int regAValue = state.getRegisterValue(regA);
    int regBValue = state.getRegisterValue(regB);
    Instance inst = state.getInstance();
    InstanceState istate = circuitState.getInstanceState(inst.getComponent());
    istate.setPort(Nios2.DATAA, Value.createKnown(32, regAValue), 5);
    istate.setPort(Nios2.DATAB, Value.createKnown(32, regBValue), 5);
    istate.setPort(Nios2.START, Value.createKnown(1, 1), 5);
    istate.setPort(Nios2.N, Value.createKnown(8, n), 5);
    istate.setPort(Nios2.A, Value.createKnown(5, regA), 5);
    istate.setPort(Nios2.READRA, Value.createKnown(1, readra ? 1 : 0), 5);
    istate.setPort(Nios2.B, Value.createKnown(5, regB), 5);
    istate.setPort(Nios2.READRB, Value.createKnown(1, readrb ? 1 : 0), 5);
    istate.setPort(Nios2.C, Value.createKnown(5, regC), 5);
    istate.setPort(Nios2.WRITERC, Value.createKnown(1, writerc ? 1 : 0), 5);
    custActive = true;
    return true;
  }
  
  public boolean waitingOnReady(Object processorState, CircuitState circuitState) {
	if (!custActive || !valid) return false;
    Nios2State.ProcessorState state = (Nios2State.ProcessorState)processorState;
    Instance inst = state.getInstance();
    InstanceState istate = circuitState.getInstanceState(inst.getComponent());
    Value done = istate.getPortValue(Nios2.DONE);
    istate.setPort(Nios2.START, Value.createKnown(1, 0), 0);
    if (!done.equals(Value.TRUE) && !done.equals(Value.FALSE)) {
      custActive = false;
      OptionPane.showMessageDialog(null, S.get("Nios2DonePinError"), "Nios2s", OptionPane.ERROR_MESSAGE);
      state.getSimState().errorInExecution();
      return true;
    }
    if (done.equals(Value.TRUE)) {
      custActive = false;
      if (!writerc) {
    	int result = 0;
    	Value rValue = istate.getPortValue(Nios2.RESULT);
    	if (rValue.isFullyDefined())
          result = Integer.parseUnsignedInt(rValue.toHexString(),16);
        state.writeRegister(regC, result);
      }
      return false;
    }
    return true;
  }

  public String getAsmInstruction() {
	if (!valid) return null;
	StringBuffer s = new StringBuffer();
	s.append("custom");
	while (s.length() < Nios2Support.ASM_FIELD_SIZE) s.append(" ");
	s.append(n+",");
	s.append((writerc ? "c" : "r")+regC+",");
	s.append((readra ? "c" : "r")+regA+",");
	s.append((readrb ? "c" : "r")+regB);
    return s.toString();
  }

  public int getBinInstruction() { return instruction; }

  public boolean setAsmInstruction(AssemblerAsmInstruction instr) {
	if (!instr.getOpcode().toLowerCase().equals("custom")) {
	  valid = false;
	  return false;
	}
	valid = true;
	if (instr.getNrOfParameters() != 4) {
	  valid = false;
	  instr.setError(instr.getInstruction(), S.getter("AssemblerExpectedFourArguments"));
	  return true;
	}
	AssemblerToken[] param1,param2,param3,param4;
	param1 = instr.getParameter(0);
	param2 = instr.getParameter(1);
	param3 = instr.getParameter(2);
	param4 = instr.getParameter(3);
	if (param1.length != 1 || !param1[0].isNumber()) {
	  valid = false;
	  instr.setError(param1[0], S.getter("AssemblerExpectedImmediateValue"));
	  return true;
	}
	n = param1[0].getNumberValue();
	if (n < 0 || n > 255) {
	  valid = false;
	  instr.setError(param1[0], S.getter("AssemblerImmediateOutOfRange"));
	  return true;
	}
	if (param2.length != 1 || !(param2[0].getType() == AssemblerToken.REGISTER || 
	    param2[0].getType() == Nios2Assembler.CUSTOM_REGISTER)) {
	  valid = false;
	  instr.setError(param2[0], S.getter("AssemblerExpectedRegister"));
	}
	writerc = param2[0].getType() == Nios2Assembler.CUSTOM_REGISTER;
	regC = Nios2State.getRegisterIndex(param2[0].getValue());
	if (regC < 0 || regC >31) {
	  valid = false;
	  instr.setError(param2[0], S.getter("AssemblerUnknownRegister"));
	}
	if (param3.length != 1 || !(param3[0].getType() == AssemblerToken.REGISTER || 
        param3[0].getType() == Nios2Assembler.CUSTOM_REGISTER)) {
      valid = false;
      instr.setError(param3[0], S.getter("AssemblerExpectedRegister"));
    }
    readra = param3[0].getType() == Nios2Assembler.CUSTOM_REGISTER;
    regA = Nios2State.getRegisterIndex(param3[0].getValue());
    if (regA < 0 || regA >31) {
      valid = false;
      instr.setError(param3[0], S.getter("AssemblerUnknownRegister"));
    }
	if (param4.length != 1 || !(param4[0].getType() == AssemblerToken.REGISTER || 
        param4[0].getType() == Nios2Assembler.CUSTOM_REGISTER)) {
      valid = false;
      instr.setError(param4[0], S.getter("AssemblerExpectedRegister"));
    }
    readrb = param4[0].getType() == Nios2Assembler.CUSTOM_REGISTER;
    regB = Nios2State.getRegisterIndex(param4[0].getValue());
    if (regB < 0 || regB >31) {
      valid = false;
      instr.setError(param4[0], S.getter("AssemblerUnknownRegister"));
    }
    if (valid) {
      int opx = n&0xFF;
      if (writerc) opx |= 1 << 8;
      if (readrb) opx |= 1 << 9;
      if (readra) opx |= 1 << 10;
      instruction = Nios2Support.getCustomInstructionCode(regA, regB, regC, opx, CUSTOM);
      instr.setInstructionByteCode(instruction, 4);
    }
    return true;
  }

  public boolean setBinInstruction(int instr) {
	instruction = instr;
	valid = false;
	if (Nios2Support.getOpcode(instr) == CUSTOM) {
	  valid = true;
	  regA = Nios2Support.getRegAIndex(instr, Nios2Support.R_TYPE);
	  regB = Nios2Support.getRegBIndex(instr, Nios2Support.R_TYPE);
	  regC = Nios2Support.getRegCIndex(instr, Nios2Support.R_TYPE);
	  int opx = Nios2Support.getOPX(instr, Nios2Support.R_TYPE);
	  n = opx&0xFF;
	  writerc = ((opx >> 8)&1) != 0;
	  readrb = ((opx >> 9)&1) != 0;
	  readra = ((opx >> 10)&1) != 0;
	}
    return valid;
  }

  public boolean performedJump() { return false; }

  public boolean isValid() { return valid; }

  public String getErrorMessage() { return null; }

  public ArrayList<String> getInstructions() { 
    ArrayList<String> opcodes = new ArrayList<String>();
    opcodes.add("custom");
    return opcodes; 
  }

  public int getInstructionSizeInBytes(String instruction) {
    if (instruction.toLowerCase().equals("custom"))
      return 4;
    return -1;
  }

}
