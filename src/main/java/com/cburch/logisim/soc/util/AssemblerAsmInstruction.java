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

package com.cburch.logisim.soc.util;

import java.util.ArrayList;
import java.util.HashMap;

import com.cburch.logisim.util.StringGetter;

public class AssemblerAsmInstruction {
  private AssemblerToken instruction;
  private ArrayList<AssemblerToken[]> parameters;
  private int size;
  private HashMap<AssemblerToken,StringGetter> errors;
  private Byte[] bytes;
  private long programCounter;
   
  public AssemblerAsmInstruction(AssemblerToken instruction, int size) {
    this.instruction = instruction;
    parameters = new ArrayList<AssemblerToken[]>();
    errors = new HashMap<AssemblerToken,StringGetter>();
    this.size = size;
    bytes = null;
    programCounter = -1;
  }
  
  public String getOpcode() { return instruction.getValue(); }
  public AssemblerToken getInstruction() { return instruction; }
  public int getNrOfParameters() { return parameters.size(); }
  public void addParameter(AssemblerToken[] param) { parameters.add(param); }
  public int getSizeInBytes() { return size; }
  public boolean hasErrors() { return !errors.isEmpty(); }
  public void setError(AssemblerToken token, StringGetter error) { errors.put(token, error); }
  public HashMap<AssemblerToken,StringGetter> getErrors() { return errors; }
  public Byte[] getBytes() { return bytes; }
  public void setProgramCounter(long value) { programCounter = value; }
  public long getProgramCounter() { return programCounter; }
  
  public void setInstructionByteCode(int instruction, int nrOfBytes) {
    if (bytes == null) bytes = new Byte[size];
    for (int i = 0 ; i < nrOfBytes && i < size ; i++) {
      bytes[i] = (byte)((instruction >> (i*8))&0xFF);
    }
  }
  
  public AssemblerToken[] getParameter(int index) {
    if (index < 0 || index >= parameters.size()) return null;
    return parameters.get(index);
  }
  
  public boolean replaceLabels(HashMap<String,Long> labels) {
	for (AssemblerToken[] parameter : parameters) {
	  for (int i = 0 ; i < parameter.length ; i++) {
	    if (parameter[i].getType() == AssemblerToken.PARAMETER_LABEL) {
	      String Name = parameter[i].getValue();
	      if (!labels.containsKey(Name))
	        return false;
          parameter[i].setType(AssemblerToken.HEX_NUMBER);
          parameter[i].setValue(String.format("0x%08X", labels.get(Name)));
	    }
	  }
	}
    return true;
  }
}
