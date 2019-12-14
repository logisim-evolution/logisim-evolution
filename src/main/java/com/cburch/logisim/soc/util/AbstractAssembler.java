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

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.file.ElfProgramHeader.ProgramHeader;
import com.cburch.logisim.soc.file.SectionHeader;
import com.cburch.logisim.soc.file.SymbolTable;

public abstract class AbstractAssembler implements AssemblerInterface {

  private static final int NR_OF_BYTES_PER_LINE = 16;
  private ArrayList<AssemblerExecutionInterface> exeUnits = new ArrayList<AssemblerExecutionInterface>();
  private HashSet<Integer> acceptedParameterTypes;
  
  public AbstractAssembler() {
    acceptedParameterTypes = new HashSet<Integer>();
    acceptedParameterTypes.add(AssemblerToken.BRACKETED_REGISTER);
    acceptedParameterTypes.add(AssemblerToken.DEC_NUMBER);
    acceptedParameterTypes.add(AssemblerToken.HEX_NUMBER);
    acceptedParameterTypes.add(AssemblerToken.PARAMETER_LABEL);
    acceptedParameterTypes.add(AssemblerToken.REGISTER);
    acceptedParameterTypes.add(AssemblerToken.SEPERATOR);
    acceptedParameterTypes.add(AssemblerToken.MAYBE_LABEL);
    acceptedParameterTypes.add(AssemblerToken.PROGRAM_COUNTER);
    acceptedParameterTypes.addAll(AssemblerToken.MATH_OPERATORS);
  }
  
  public void AddAcceptedParameterType(int type) { acceptedParameterTypes.add(type); }
  public HashSet<Integer> getAcceptedParameterTypes() { return acceptedParameterTypes; }
  
  public void addAssemblerExecutionUnit( AssemblerExecutionInterface exe ) { exeUnits.add(exe); }

  public void decode(int instruction) {
    for (AssemblerExecutionInterface exe : exeUnits)
      exe.setBinInstruction(instruction);
  }
  
  public AssemblerExecutionInterface getExeUnit() {
    for (AssemblerExecutionInterface exe : exeUnits)
      if (exe.isValid())
        return exe;
    return null;
  }
  
  public ArrayList<String> getOpcodes() {
    ArrayList<String> opcodes = new ArrayList<String>();
    for (AssemblerExecutionInterface exe : exeUnits)
      opcodes.addAll(exe.getInstructions());
    return opcodes;
  }

  public int getInstructionSize(String opcode) {
    for (AssemblerExecutionInterface exe : exeUnits) {
      int size = exe.getInstructionSizeInBytes(opcode);
      if (size > 0) return size;
    }
    return 1; /* to make sure that instructions are not overwritten */
  }

  public boolean assemble(AssemblerAsmInstruction instruction) {
    boolean found = false;
    for (AssemblerExecutionInterface exe : exeUnits) {
      found |= exe.setAsmInstruction(instruction);
    }
    if (!found)
      instruction.setError(instruction.getInstruction(), S.getter("AssemblerUnknownOpcode"));
    return !instruction.hasErrors();
  }

  private int addLabels(SectionHeader sh, HashMap<Integer,String> labels) {
    int maxSize = 0;
    for (SymbolTable st : sh.getSymbols()) {
      String stName = st.getName();
      if (stName != null && !stName.isEmpty()) {
      int addr = st.getValue(SymbolTable.ST_VALUE);
      if (!labels.containsKey(addr)) {
          if (stName.length() > maxSize) maxSize = stName.length();
          labels.put(addr, stName);
      }
      }
    }
    return maxSize;
  }
  
  private int getByte(Integer[] buffer, int byteIndex) {
    int index = byteIndex>>2;
    int byteSelect = byteIndex&3;
    int data = buffer[index];
    int info;
    switch (byteSelect) {
      case 0  : info = data&0xFF; break;
      case 1  : info = (data>>8)&0xFF; break;
      case 2  : info = (data>>16)&0xFF; break;
      default : info = (data>>24)&0xFF; break;
    }
    return info;
  }
  
  private int getData(boolean lookForStrings, Integer[] contents, 
          long sizeInBytes, long startAddress, HashMap<Integer,String> labels, 
          int maxLabelSize, StringBuffer lines, int lineNum) {
    int nrBytesWritten = 0;
    int size = (int)sizeInBytes;
    int i = 0;
    int newLineNum = lineNum;
    while (i < size) {
      boolean stringFound = false;
      boolean zerosFound = false;
      if (labels.containsKey(SocSupport.convUnsignedLong(startAddress+i))) {
        StringBuffer label = new StringBuffer();
        label.append(labels.get(SocSupport.convUnsignedLong(startAddress+i))+":");
        while (label.length() < maxLabelSize) label.append(" ");
        if (nrBytesWritten != 0)  newLineNum = addLine(lines,"\n",newLineNum,true);
        newLineNum = addLine(lines,label.toString(),newLineNum,false);
        nrBytesWritten = -1;
      } 
      int kar = getByte(contents,i); 
      if (lookForStrings) {
        if (kar >= 32 && kar <= 127) {
          StringBuffer str = new StringBuffer();
          if (kar == 34 || kar == 92) str.append('\\');
          str.append((char)kar);
          int j = i+1;
          while (j < size) {
            kar = getByte(contents,j);
            switch (kar) {
              case  8 : str.append("\\");
                        kar = 'b';
                        break;
              case  9 : str.append("\\");
                        kar = 't';
                        break;
              case 10 : str.append("\\");
                        kar = 'n';
                        break;
              case 12 : str.append("\\");
                        kar = 'f';
                        break;
              case 13 : str.append("\\");
                        kar = 'r';
                        break;
              case 34 :
              case 92 : str.append("\\");
                        break;
            }
            if (kar < 32 || kar >= 127) break;
            str.append((char)kar);
            j++;
          }
          if (str.length()>2) {
            stringFound = true;
            i = j;
            String type = ".ascii";
            if (i < size && getByte(contents,i) == 0) {
              i++;
              type = ".string";
            }
            if (nrBytesWritten > 0) newLineNum = addLine(lines,"\n",newLineNum,true);
            if (nrBytesWritten >= 0) 
              for (int sp = 0 ; sp < maxLabelSize ; sp++) newLineNum = addLine(lines," ",newLineNum,false);
            newLineNum = addLine(lines," "+type+" \""+str.toString()+"\"\n",newLineNum,true);
            nrBytesWritten = 0;
          }
        }
      }
      if (kar == 0) {
        /* we are going to reduce the number of zeros inserted */
        int j = i+1;
        while (j < size) {
          kar = getByte(contents,j);
          if (kar != 0) break;
          j++;
        }
        if ((j-i) > 1) {
          zerosFound = true;
          if (nrBytesWritten > 0) newLineNum = addLine(lines,"\n",newLineNum,true);
          if (nrBytesWritten >= 0) 
            for (int sp = 0 ; sp < maxLabelSize ; sp++) newLineNum = addLine(lines," ",newLineNum,false);
          newLineNum = addLine(lines," .zero "+(j-i)+"\n",newLineNum,true);
          i = j;
          nrBytesWritten = 0;
        }
      }
      if (!stringFound && !zerosFound) {
        if (nrBytesWritten <= 0 || nrBytesWritten >= NR_OF_BYTES_PER_LINE) {
          StringBuffer label = new StringBuffer();
          while (label.length() < maxLabelSize) label.append(" ");
          if (nrBytesWritten >= NR_OF_BYTES_PER_LINE) newLineNum = addLine(lines,"\n",newLineNum,true);
          if (nrBytesWritten == 0 || nrBytesWritten >= NR_OF_BYTES_PER_LINE) newLineNum = addLine(lines,label.toString(),newLineNum,false);
          if (nrBytesWritten <= 0 || nrBytesWritten >= NR_OF_BYTES_PER_LINE) newLineNum = addLine(lines," .byte ",newLineNum,false);
          nrBytesWritten = 0;
        }
        if (nrBytesWritten > 0) newLineNum = addLine(lines,", ",newLineNum,false);
        newLineNum = addLine(lines,String.format("0x%02X", getByte(contents,i)),newLineNum,false);
        nrBytesWritten++;
        i++;
      }
    }
    if (nrBytesWritten != 0) newLineNum = addLine(lines,"\n",newLineNum,true);
    return newLineNum;
  }
  
  private int addLine(StringBuffer s, String val, int lineNum, boolean completedLine) {
    s.append(val);
    return completedLine ? lineNum+1 : lineNum;
  }
  
  public String getProgram(CircuitState circuitState, SocProcessorInterface processorInterface, 
          ElfProgramHeader elfHeader, ElfSectionHeader elfSections, 
          HashMap<Integer,Integer> validDebugLines) {

    StringBuffer lines = new StringBuffer();
    int lineNum = 1;
    if (elfSections != null && elfSections.isValid()) {
      /* The section header gives more information on the program, so we prefer this one over the
       * program header.
       */
      HashMap<Integer,String> labels = new HashMap<Integer,String>();
      int maxLabelSize = 0;
      ArrayList<SectionHeader> sortedList = new ArrayList<SectionHeader>();
      for (SectionHeader sh : elfSections.getHeaders()) {
        if (sh.isAllocated()) {
          if (sortedList.isEmpty()) {
            sortedList.add(sh);
            int size = addLabels(sh,labels);
            if (size > maxLabelSize) maxLabelSize = size;
          } else {
            boolean inserted = false;
            long shStart = SocSupport.convUnsignedInt((int)sh.getValue(SectionHeader.SH_ADDR));
            for (int i = 0 ; i < sortedList.size() ; i++) {
              long start = SocSupport.convUnsignedInt((int)sortedList.get(i).getValue(SectionHeader.SH_ADDR));
              if (shStart < start) {
                inserted = true;
                sortedList.add(i, sh);
                int size = addLabels(sh,labels);
                if (size > maxLabelSize) maxLabelSize = size;
                break;
              }
            }
            if (!inserted) {
              sortedList.add(sh);
              int size = addLabels(sh,labels);
              if (size > maxLabelSize) maxLabelSize = size;
            }
          }
        }
      }
      maxLabelSize++; // Account for the ":"
      for (SectionHeader sh : sortedList) {
        long startAddress = SocSupport.convUnsignedInt((int)sh.getValue(SectionHeader.SH_ADDR));
        long size = SocSupport.convUnsignedInt((int)sh.getValue(SectionHeader.SH_SIZE));
        if (lines.length()!= 0) lineNum = addLine(lines,"\n",lineNum,true);
        lineNum = addLine(lines,".section "+sh.getName()+"\n",lineNum,true);
        lineNum = addLine(lines,".org "+String.format("0x%08X\n", startAddress),lineNum,true);
        int toBeRead = ((int)size%4)!=0 ? (int)(size>>2)+1 : (int)(size>>2);
        Integer[] contents = new Integer[toBeRead];
        for (int i = 0 ; i < toBeRead ; i++) {
          SocBusTransaction trans = new SocBusTransaction( SocBusTransaction.READTransaction,
            SocSupport.convUnsignedLong(startAddress+((long)i<<2)),0,SocBusTransaction.WordAccess,"assembler");
          processorInterface.insertTransaction(trans, true, circuitState);
          contents[i] = trans.getReadData();
        }
        if (sh.isExecutable()) {
          /* first pass, we are going to insert labels where we can find them */
          ArrayList<Integer> newLabels = new ArrayList<Integer>();
          for (int pc = 0 ; pc < (size>>2) ; pc++) {
            decode(contents[pc]);
            AssemblerExecutionInterface exe = getExeUnit();
            if (exe instanceof AbstractExecutionUnitWithLabelSupport) {
              AbstractExecutionUnitWithLabelSupport jump = (AbstractExecutionUnitWithLabelSupport) exe;
              if (jump.isLabelSupported()) {
                long addr = startAddress+((long)pc<<2);
                long target = jump.getLabelAddress(addr);
                Integer labelLoc = SocSupport.convUnsignedLong(target);
                if (!labels.containsKey(labelLoc) && !newLabels.contains(labelLoc)) {
                  if (newLabels.isEmpty()) newLabels.add(labelLoc);
                  else {
                    boolean inserted = false;
                    for (int j = 0 ; j < newLabels.size() ; j++) {
                      if (newLabels.get(j)>labelLoc) {
                        newLabels.add(j, labelLoc);
                        inserted = true;
                        break;
                      }
                    }
                    if (!inserted) newLabels.add(labelLoc);
                  }
                }
              }
            }
          }
          /* when we already have labels with the given name, lets determine the offset */
          int offset = 0;
          for (Integer addr : labels.keySet()) {
            String label = labels.get(addr);
            if (label != null && label.startsWith("logisim_label_")) {
              int index = 0;
              try { index = Integer.parseUnsignedInt(label.substring(14)); }
              catch (NumberFormatException e) {};
              if (index >= offset) offset = index+1;
            }
          }

          for (int i = 0 ; i < newLabels.size(); i++) {
            String label = "logisim_label_"+(i+offset);
            labels.put(newLabels.get(i), label);
            if (label.length() > maxLabelSize) maxLabelSize = label.length();
          }
          /* second pass, we are going to insert the code into the buffer */
          StringBuffer remark = new StringBuffer();
          int remarkOffset = (2*maxLabelSize)+23 < 60 ? 60 : (2*maxLabelSize)+23;
          for (int i = 0 ; i <  remarkOffset ; i++) remark.append(" ");
          remark.append("#    pc:       opcode:\n");
          lineNum = addLine(lines,remark.toString(),lineNum,true);
          for (int pc = 0 ; pc < (size>>2) ; pc++) {
          StringBuffer line = new StringBuffer();
          long addr = startAddress+(((long)pc)<<2);
            StringBuffer label = new StringBuffer();
            if (labels.containsKey(SocSupport.convUnsignedLong(addr))) label.append(labels.get(SocSupport.convUnsignedLong(addr))+":");
            while (label.length() <= maxLabelSize) label.append(" ");
            line.append(label.toString()+" ");
            decode(contents[pc]);
            AssemblerExecutionInterface exe = getExeUnit();
            if (exe != null && exe instanceof AbstractExecutionUnitWithLabelSupport) {
              AbstractExecutionUnitWithLabelSupport jump = (AbstractExecutionUnitWithLabelSupport) exe;
              if (jump.isLabelSupported()) {
                long target = jump.getLabelAddress(addr);
                if (labels.containsKey(SocSupport.convUnsignedLong(target))) 
                  line.append(jump.getAsmInstruction(labels.get(SocSupport.convUnsignedLong(target))));
                else
                  line.append(jump.getAsmInstruction());
              } else line.append(exe.getAsmInstruction());
            } else if (exe != null) line.append(exe.getAsmInstruction());
            else line.append(S.get("UnknownInstruction"));
            while (line.length() < remarkOffset) line.append(" ");
            line.append("# "+String.format("0x%08X", SocSupport.convUnsignedLong(startAddress+((long)pc<<2))));
            line.append(" "+String.format("0x%08X", contents[pc]));
            validDebugLines.put(lineNum, SocSupport.convUnsignedLong(startAddress+((long)pc<<2)));
            lineNum = addLine(lines,line.toString()+"\n",lineNum,true);
          }
        } else lineNum = getData(!sh.isWritable(),contents,size,startAddress,labels,maxLabelSize,lines,lineNum);
      }
    } else if (elfHeader != null && elfHeader.isValid()) {
      for (int i = 0 ; i < elfHeader.getNrOfHeaders() ; i++) {
        ProgramHeader p = elfHeader.getHeader(i);
        if (((int)p.getValue(ElfProgramHeader.P_FLAGS)&ElfProgramHeader.PF_X) != 0) {
          long start = SocSupport.convUnsignedInt((int)p.getValue(ElfProgramHeader.P_PADDR));
          long size = SocSupport.convUnsignedInt((int)p.getValue(ElfProgramHeader.P_MEMSZ));
          lineNum = addLine(lines,String.format(".org 0x%08X\n", start),lineNum,true);
          for (long pc = 0 ; pc < size ; pc += 4) {
          long addr = start+pc;
            SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.READTransaction,
                    SocSupport.convUnsignedLong(addr),0,SocBusTransaction.WordAccess,"assembler");
            processorInterface.insertTransaction(trans, true, circuitState);
            if (!trans.hasError()) {
              int instr = trans.getReadData();
              lineNum = addLine(lines,String.format("0x%08X ", addr),lineNum,false);
              lineNum = addLine(lines,String.format("0x%08X ", instr),lineNum,false);
              if (addr == processorInterface.getEntryPoint(circuitState))
               lineNum = addLine(lines,"_start: ",lineNum,false);
              else
                lineNum = addLine(lines,"       ",lineNum,false);
              decode(instr);
              if (getExeUnit()!= null) {
                lineNum = addLine(lines,getExeUnit().getAsmInstruction(),lineNum,false);
                validDebugLines.put(lineNum, SocSupport.convUnsignedLong(addr));
              } else lineNum = addLine(lines,"????",lineNum,false);
              lineNum = addLine(lines,"\n",lineNum,true);
            }
          }
        }
      }
    }
    return lines.toString();
  }
}
