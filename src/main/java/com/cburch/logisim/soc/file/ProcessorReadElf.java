/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.file;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.soc.data.SocBusTransaction;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfProgramHeader.ProgramHeader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ProcessorReadElf {

  private static final int SUCCESS = 0;
  private static final int FILE_OPEN_ERROR = 1;
  private static final int ELF_HEADER_ERROR = 2;
  private static final int ARCHITECTURE_ERROR = 3;
  private static final int NO_EXECUTABLE_ERROR = 4;
  private static final int ENDIAN_MISMATCH_ERROR = 5;
  private static final int PROGRAM_HEADER_INVALID = 6;
  private static final int SECTION_HEADER_INVALID = 7;
  private static final int LOADABLE_SECTION_NOT_FOUND = 8;
  private static final int LOADABLE_SECTION_TOO_BIG = 9;
  private static final int LOADABLE_SECTION_READ_ERROR = 10;
  private static final int LOADABLE_SECTION_SIZE_ERROR = 11;
  private static final int NOT_SUPPORTED_YET_ERROR = 12;
  private static final int MEM_LOAD_ERROR = 13;

  private final SocProcessorInterface cpu;
  private final int architecture;
  private final File elfFile;
  private FileInputStream elfFileStream;
  private int status;
  private ElfHeader elfHeader;
  private ElfProgramHeader programHeader;
  private ElfSectionHeader sectionHeader;
  private long start, end;

  public ProcessorReadElf(File elfFile, Instance instance, int architecture, boolean littleEndian) {
    cpu =
        ((SocInstanceFactory) instance.getFactory())
            .getProcessorInterface(instance.getAttributeSet());
    this.architecture = architecture;
    this.elfFile = elfFile;
    status = SUCCESS;
    if (!open()) return;
    elfHeader = new ElfHeader(elfFileStream);
    close();
    if (!elfHeader.isValid()) {
      status = ELF_HEADER_ERROR;
      return;
    }
    int arch = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_MACHINE));
    if (arch != architecture) {
      status = ARCHITECTURE_ERROR;
      return;
    }
    int type = ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_TYPE));
    if (type != ElfHeader.ET_EXEC) {
      status = NO_EXECUTABLE_ERROR;
      return;
    }
    if (elfHeader.isLittleEndian() != littleEndian) {
      status = ENDIAN_MISMATCH_ERROR;
      return;
    }
    if (!open()) return;
    programHeader = new ElfProgramHeader(elfFileStream, elfHeader);
    close();
    if (!programHeader.isValid()) {
      status = PROGRAM_HEADER_INVALID;
      return;
    }
    if (!open()) return;
    sectionHeader = new ElfSectionHeader(elfFileStream, elfHeader);
    close();
    if (!sectionHeader.isValid()) {
      status = SECTION_HEADER_INVALID;
      return;
    }
    if (!elfHeader.is32Bit()) {
      status = NOT_SUPPORTED_YET_ERROR;
      return;
    }
    if (!open()) return;
    sectionHeader.readSectionNames(elfFileStream, elfHeader);
    close();
    if (!sectionHeader.isValid()) {
      status = SECTION_HEADER_INVALID;
      return;
    }
    if (!open()) return;
    sectionHeader.readSymbolTable(elfFileStream, elfHeader);
    close();
    if (!sectionHeader.isValid()) {
      status = SECTION_HEADER_INVALID;
      return;
    }
  }

  public boolean canExecute() {
    return status == SUCCESS;
  }

  public String getErrorMessage() {
    switch (status) {
      case SUCCESS : return S.get("ProcReadElfSuccess");
      case FILE_OPEN_ERROR : return S.get("ProcReadElfErrorOpeningFile");
      case ELF_HEADER_ERROR : return elfHeader.getErrorString();
      case ARCHITECTURE_ERROR:
        return S.get(
            "ProcReadElfArchError",
            elfHeader.getArchitectureString(
                ElfHeader.getIntValue(elfHeader.getValue(ElfHeader.E_MACHINE))),
            elfHeader.getArchitectureString(architecture));
      case NO_EXECUTABLE_ERROR : return S.get("ProcReadElfNotExecutable");
      case ENDIAN_MISMATCH_ERROR:
        return S.get(
            "ProcReadElfEndianMismatch",
            elfHeader.isLittleEndian() ? "little endian" : "big endian",
            elfHeader.isLittleEndian() ? "big endian" : "little endian");
      case PROGRAM_HEADER_INVALID : return programHeader.getErrorString();
      case SECTION_HEADER_INVALID : return sectionHeader.getErrorString();
      case LOADABLE_SECTION_NOT_FOUND : return S.get("ProcReadElfLoadableSectionNotFound");
      case LOADABLE_SECTION_TOO_BIG : return S.get("ProcReadElfLoadableSectionTooBig");
      case LOADABLE_SECTION_READ_ERROR : return S.get("ProcReadElfLoadableSectionReadError");
      case LOADABLE_SECTION_SIZE_ERROR : return S.get("ProcReadElfLoadableSectionSizeError");
      case NOT_SUPPORTED_YET_ERROR : return S.get("ProcReadElf64BitNotSupportedYet");
      case MEM_LOAD_ERROR:
        return S.get(
            "ProcReadElfMemoryError", String.format("0x%08X", start), String.format("0x%08X", end));
    }
    return "BUG: Should not happen";
  }

  public boolean execute(CircuitState cState) {
    for (int i = 0; i < programHeader.getNrOfHeaders(); i++) {
      ProgramHeader h = programHeader.getHeader(i);
      if (ElfHeader.getIntValue(h.getValue(ElfProgramHeader.P_TYPE)) != ElfProgramHeader.PT_LOAD)
        continue;
      if (!open()) return false;
      try {
        elfFileStream.skip(ElfHeader.getLongValue(h.getValue(ElfProgramHeader.P_OFFSET)));
      } catch (IOException e) {
        status = LOADABLE_SECTION_NOT_FOUND;
        return false;
      }
      long sectionSize = ElfHeader.getLongValue(h.getValue(ElfProgramHeader.P_FILESZ));
      long memSize = ElfHeader.getLongValue(h.getValue(ElfProgramHeader.P_MEMSZ));
      if ((sectionSize > (long) Integer.MAX_VALUE) || (memSize > (long) Integer.MAX_VALUE)) {
        status = LOADABLE_SECTION_TOO_BIG;
        return false;
      }
      byte[] buffer = new byte[(int) sectionSize];
      int nrRead = 0;
      try {
        nrRead = elfFileStream.read(buffer);
      } catch (IOException e) {
        status = LOADABLE_SECTION_READ_ERROR;
        return false;
      }
      if (sectionSize != nrRead) {
        status = LOADABLE_SECTION_SIZE_ERROR;
        return false;
      }
      long startAddr = ElfHeader.getLongValue(h.getValue(ElfProgramHeader.P_PADDR));
      for (int j = 0; j < memSize; j++) {
        int data = (j < buffer.length) ? buffer[j] : 0;
        int addr = ElfHeader.getIntValue(ElfHeader.returnCorrectValue(startAddr + (long) j, true));
        SocBusTransaction trans =
            new SocBusTransaction(
                SocBusTransaction.WRITE_TRANSACTION,
                addr,
                data,
                SocBusTransaction.BYTE_ACCESS,
                "elf");
        cpu.insertTransaction(trans, true, cState);
        if (trans.hasError()) {
          start = startAddr;
          end = startAddr + memSize - 1;
          status = MEM_LOAD_ERROR;
          return false;
        }
      }
    }
    cpu.setEntryPointandReset(
        cState,
        ElfHeader.getLongValue(elfHeader.getValue(ElfHeader.E_ENTRY)),
        programHeader,
        sectionHeader);
    return true;
  }

  private boolean open() {
    try {
      elfFileStream = new FileInputStream(elfFile);
    } catch (FileNotFoundException e) {
      status = FILE_OPEN_ERROR;
      return false;
    }
    return true;
  }

  private void close() {
    try {
      elfFileStream.close();
    } catch (IOException ignored) {
    }
  }
}
