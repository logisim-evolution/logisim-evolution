/*
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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public interface AssemblerInterface {
  void decode(int instruction);
  boolean assemble(AssemblerAsmInstruction instruction);
  AssemblerExecutionInterface getExeUnit();
  ArrayList<String> getOpcodes();
  int getInstructionSize(String opcode);
  boolean usesRoundedBrackets();
  String getProgram(CircuitState circuitState, SocProcessorInterface processorInterface,
      ElfProgramHeader elfHeader, ElfSectionHeader elfSections,
      HashMap<Integer, Integer> validDebugLines);
  String getHighlightStringIdentifier();
  void performUpSpecificOperationsOnTokens(LinkedList<AssemblerToken> tokens);
  HashSet<Integer> getAcceptedParameterTypes();
}
