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

import java.util.ArrayList;
import java.util.HashMap;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocSupport;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfProgramHeader.ProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader.SectionHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader.SymbolTable;
import com.cburch.logisim.soc.rv32im.RV32im_state.ProcessorState;

public class RV32imDecoder {
  
  private static final int NR_OF_BYTES_PER_LINE = 16;
  
  private ArrayList<RV32imExecutionUnitInterface> exeUnits;
  
  public RV32imDecoder() {
    exeUnits = new ArrayList<RV32imExecutionUnitInterface>();
    /* Here we add the RV32I base integer instruction set */
    exeUnits.add(new RV32imIntegerRegisterImmediateInstructions());
    exeUnits.add(new RV32imIntegerRegisterRegisterOperations());
    exeUnits.add(new RV32imControlTransferInstructions());
    exeUnits.add(new RV32imLoadAndStoreInstructions());
    exeUnits.add(new Rv32imMemoryOrderingInstructions());
    exeUnits.add(new RV32imEnvironmentCallAndBreakpoints());
    /* Here we add the "M" standard extension for integer multiplication and Division */
    exeUnits.add(new RV32im_M_ExtensionInstructions());
  }
  
  public void decode(int instruction) {
    for (RV32imExecutionUnitInterface exe : exeUnits)
      exe.setBinInstruction(instruction);
  }
  
  public RV32imExecutionUnitInterface getExeUnit() {
    for (RV32imExecutionUnitInterface exe : exeUnits)
      if (exe.isValid())
        return exe;
    return null;
  }
  
  public ArrayList<String> getOpcodes() {
    ArrayList<String> opcodes = new ArrayList<String>();
    for (RV32imExecutionUnitInterface exe : exeUnits)
      opcodes.addAll(exe.getInstructions());
    return opcodes;
  }
  
  private static int addLabels(SectionHeader sh, HashMap<Integer,String> labels) {
    int maxSize = 0;
    for (SymbolTable st : sh.getSymbols()) {
      String stName = st.getName();
      if (stName != null && !stName.isEmpty()) {
      int addr = st.getValue(ElfSectionHeader.ST_VALUE);
      if (!labels.containsKey(addr)) {
          if (stName.length() > maxSize) maxSize = stName.length();
          labels.put(addr, stName);
      }
      }
    }
    return maxSize;
  }
  
  private static int getByte(Integer[] buffer, int byteIndex) {
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
  
  private static int getData(boolean lookForStrings, Integer[] contents, long startAddress,
          HashMap<Integer,String> labels, int maxLabelSize, StringBuffer lines, int lineNum) {
    int nrBytesWritten = 0;
    int size = contents.length<<2;
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
          str.append((char)getByte(contents,i));
          int j = i+1;
          while (j < size) {
            kar = getByte(contents,j);
            if (kar < 32 || kar >= 127) break;
            str.append((char)getByte(contents,j));
            j++;
          }
          if (str.length()>2) {
            stringFound = true;
            i = j;
            if (nrBytesWritten > 0) newLineNum = addLine(lines,"\n",newLineNum,true);
            if (nrBytesWritten >= 0) 
              for (int sp = 0 ; sp < maxLabelSize ; sp++) newLineNum = addLine(lines," ",newLineNum,false);
            newLineNum = addLine(lines," .string \""+str.toString()+"\"\n",newLineNum,true);
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
  
  private static int addLine(StringBuffer s, String val, int lineNum, boolean completedLine) {
    s.append(val);
    return completedLine ? lineNum+1 : lineNum;
  }
  
  public static String getProgram(CircuitState state, ProcessorState pstate, ElfProgramHeader header,
		  ElfSectionHeader sections, HashMap<Integer,Integer> ValidDebugLines) {
    StringBuffer lines = new StringBuffer();
    int lineNum = 1;
    if (sections != null && sections.isValid()) {
      /* The section header gives more information on the program, so we prefer this one over the
       * program header.
       */
      HashMap<Integer,String> labels = new HashMap<Integer,String>();
      int maxLabelSize = 0;
      ArrayList<SectionHeader> sortedList = new ArrayList<SectionHeader>();
      for (SectionHeader sh : sections.getHeaders()) {
        if (sh.isAllocated()) {
          if (sortedList.isEmpty()) {
            sortedList.add(sh);
            int size = addLabels(sh,labels);
            if (size > maxLabelSize) maxLabelSize = size;
          } else {
            boolean inserted = false;
            long shStart = SocSupport.convUnsignedInt((int)sh.getValue(ElfSectionHeader.SH_ADDR));
            for (int i = 0 ; i < sortedList.size() ; i++) {
              long start = SocSupport.convUnsignedInt((int)sortedList.get(i).getValue(ElfSectionHeader.SH_ADDR));
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
        long startAddress = SocSupport.convUnsignedInt((int)sh.getValue(ElfSectionHeader.SH_ADDR));
        long size = SocSupport.convUnsignedInt((int)sh.getValue(ElfSectionHeader.SH_SIZE));
        if (lines.length()!= 0) lineNum = addLine(lines,"\n",lineNum,true);
        lineNum = addLine(lines,".section "+sh.getName()+"\n",lineNum,true);
        lineNum = addLine(lines,".org "+String.format("0x%08X\n", startAddress),lineNum,true);
        int toBeRead = ((int)size%4)!=0 ? (int)(size>>2)+1 : (int)(size>>2);
        Integer[] contents = new Integer[toBeRead];
        for (int i = 0 ; i < toBeRead ; i++) {
          SocBusTransaction trans = new SocBusTransaction( SocBusTransaction.READTransaction,
            SocSupport.convUnsignedLong(startAddress+((long)i<<2)),0,SocBusTransaction.WordAccess,pstate.getMasterComponent());
          pstate.insertTransaction(trans, true, state);
          contents[i] = trans.getReadData();
        }
        if (sh.isExecutable()) {
          /* first pass, we are going to insert labels where we can find them */
          ArrayList<Integer> newLabels = new ArrayList<Integer>();
          for (int pc = 0 ; pc < (size>>2) ; pc++) {
            RV32im_state.DECODER.decode(contents[pc]);
            RV32imExecutionUnitInterface exe = RV32im_state.DECODER.getExeUnit();
            if (exe instanceof RV32imControlTransferInstructions) {
              RV32imControlTransferInstructions jump = (RV32imControlTransferInstructions) exe;
              if (jump.isPcRelative) {
                long addr = startAddress+((long)pc<<2);
                long target = addr+jump.getOffset();
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
          for (int i = 0 ; i < newLabels.size(); i++) {
            String label = "logisim_label_"+i;
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
            RV32im_state.DECODER.decode(contents[pc]);
            RV32imExecutionUnitInterface exe = RV32im_state.DECODER.getExeUnit();
            if (exe instanceof RV32imControlTransferInstructions) {
                RV32imControlTransferInstructions jump = (RV32imControlTransferInstructions) exe;
                if (jump.isPcRelative) {
                  long target = addr+jump.getOffset();
                  if (labels.containsKey(SocSupport.convUnsignedLong(target))) 
                    line.append(jump.getAsmInstruction(labels.get(SocSupport.convUnsignedLong(target))));
                  else
                    line.append(jump.getAsmInstruction());
                } else line.append(exe.getAsmInstruction());
            } else line.append(exe.getAsmInstruction());
            while (line.length() < remarkOffset) line.append(" ");
            line.append("# "+String.format("0x%08X", SocSupport.convUnsignedLong(startAddress+((long)pc<<2))));
            line.append(" "+String.format("0x%08X", contents[pc]));
            ValidDebugLines.put(lineNum, SocSupport.convUnsignedLong(startAddress+((long)pc<<2)));
            lineNum = addLine(lines,line.toString()+"\n",lineNum,true);
          }
        } else lineNum = getData(!sh.isWritable(),contents,startAddress,labels,maxLabelSize,lines,lineNum);
      }
    } else if (header != null && header.isValid()) {
      for (int i = 0 ; i < header.getNrOfHeaders() ; i++) {
        ProgramHeader p = header.getHeader(i);
        if (((int)p.getValue(ElfProgramHeader.P_FLAGS)&ElfProgramHeader.PF_X) != 0) {
          long start = SocSupport.convUnsignedInt((int)p.getValue(ElfProgramHeader.P_PADDR));
          long size = SocSupport.convUnsignedInt((int)p.getValue(ElfProgramHeader.P_MEMSZ));
          lineNum = addLine(lines,String.format(".org 0x%08X\n", start),lineNum,true);
          for (long pc = 0 ; pc < size ; pc += 4) {
          long addr = start+pc;
            SocBusTransaction trans = new SocBusTransaction(SocBusTransaction.READTransaction,
                    SocSupport.convUnsignedLong(addr),0,SocBusTransaction.WordAccess,pstate.getMasterComponent());
            pstate.insertTransaction(trans, true, state);
            if (!trans.hasError()) {
              int instr = trans.getReadData();
              lineNum = addLine(lines,String.format("0x%08X ", addr),lineNum,false);
              lineNum = addLine(lines,String.format("0x%08X ", instr),lineNum,false);
              if (addr == pstate.getEntryPoint())
               lineNum = addLine(lines,"_start: ",lineNum,false);
              else
                lineNum = addLine(lines,"       ",lineNum,false);
              RV32im_state.DECODER.decode(instr);
              if (RV32im_state.DECODER.getExeUnit()!= null) {
                lineNum = addLine(lines,RV32im_state.DECODER.getExeUnit().getAsmInstruction(),lineNum,false);
                ValidDebugLines.put(lineNum, SocSupport.convUnsignedLong(addr));
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
