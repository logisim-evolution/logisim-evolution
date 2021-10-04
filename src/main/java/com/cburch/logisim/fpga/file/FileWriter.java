/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.file;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.generated.BuildInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileWriter {

  public static final String ENTITY_EXTENSION = "_entity";
  public static final String ARCHITECTURE_EXTENSION = "_behavior";

  public static File getFilePointer(
      String targetDirectory,
      String componentName,
      boolean isEntity) {
    final var fileName = new StringBuilder();
    try {
      final var outDir = new File(targetDirectory);
      if (!outDir.exists()) {
        if (!outDir.mkdirs()) {
          return null;
        }
      }
      fileName.append(targetDirectory);
      if (!targetDirectory.endsWith(File.separator)) fileName.append(File.separator);
      fileName.append(componentName);
      if (isEntity && Hdl.isVhdl()) fileName.append(ENTITY_EXTENSION);
      if (!isEntity && Hdl.isVhdl()) fileName.append(ARCHITECTURE_EXTENSION);
      fileName.append(Hdl.isVhdl() ? ".vhd" : ".v");
      final var outFile = new File(fileName.toString());
      Reporter.report.addInfo(S.fmt("fileCreateHDLFile", fileName.toString()));
      if (outFile.exists()) {
        Reporter.report.addWarning(S.fmt("fileHDLFileExists", fileName.toString()));
        return null;
      }
      return outFile;
    } catch (Exception e) {
      Reporter.report.addFatalError(S.fmt("fileUnableToCreate", fileName.toString()));
      return null;
    }
  }

  public static File getFilePointer(String targetDirectory, String name) {
    final var fileName = new StringBuilder();
    try {
      final var outDir = new File(targetDirectory);
      if (!outDir.exists()) {
        if (!outDir.mkdirs()) {
          return null;
        }
      }
      fileName.append(targetDirectory);
      if (!targetDirectory.endsWith(File.separator)) fileName.append(File.separator);
      fileName.append(name);
      final var outFile = new File(fileName.toString());
      Reporter.report.addInfo(S.fmt("fileCreateScriptFile", fileName.toString()));
      if (outFile.exists()) {
        Reporter.report.addWarning(S.fmt("fileScriptFileExists", fileName.toString()));
        return null;
      }
      return outFile;
    } catch (Exception e) {
      Reporter.report.addFatalError(S.fmt("fileUnableToCreate", fileName.toString()));
      return null;
    }
  }

  public static List<String> getGenerateRemark(String compName, String projName) {
    ArrayList<String> lines = new ArrayList<>();
    final int headWidth;
    final String headOpen;
    final String headClose;

    final var headText = " " + BuildInfo.name + " goes FPGA automatic generated " + (Hdl.isVhdl() ? "VHDL" : "Verilog") + " code";
    final var headUrl  = " " + BuildInfo.url;
    final var headProj = " Project   : " + projName;
    final var headComp = " Component : " + compName;

    if (Hdl.isVhdl()) {
      headWidth = 74;
      headOpen = "--==";
      headClose = "==";

      lines.add(headOpen + "=".repeat(headWidth) + headClose);
      lines.add(headOpen + headText + " ".repeat(Math.max(0, headWidth - headText.length())) + headClose);
      lines.add(headOpen + headUrl + " ".repeat(Math.max(0, headWidth - headUrl.length())) + headClose);
      lines.add(headOpen + " ".repeat(headWidth) + headClose);
      lines.add(headOpen + " ".repeat(headWidth) + headClose);
      lines.add(headOpen + headProj + " ".repeat(Math.max(0, headWidth - headProj.length())) + headClose);
      lines.add(headOpen + headComp + " ".repeat(Math.max(0, headWidth - headComp.length())) + headClose);
      lines.add(headOpen + " ".repeat(headWidth) + headClose);
      lines.add(headOpen + "=".repeat(headWidth) + headClose);
      lines.add("");
    } else if (Hdl.isVerilog()) {
      headWidth = 74;
      headOpen = " **";
      headClose = "**";

      lines.add("/**" + "*".repeat(headWidth) + headClose);
      lines.add(headOpen + headText + " ".repeat(Math.max(0, headWidth - headText.length())) + headClose);
      lines.add(headOpen + headUrl + " ".repeat(Math.max(0, headWidth - headUrl.length())) + headClose);
      lines.add(headOpen + " ".repeat(headWidth) + headClose);
      lines.add(headOpen + headComp + " ".repeat(Math.max(0, headWidth - headComp.length())) + headClose);
      lines.add(headOpen + " ".repeat(headWidth) + headClose);
      lines.add(headOpen + "*".repeat(headWidth) + "*/");
      lines.add("");
    }
    return lines;
  }

  public static boolean writeContents(File outfile, List<String> contents) {
    try {
      final var output = new FileOutputStream(outfile);
      for (var thisLine : contents) {
        if (!thisLine.isEmpty()) {
          output.write(thisLine.getBytes());
        }
        output.write("\n".getBytes());
      }
      output.flush();
      output.close();
      return true;
    } catch (Exception e) {
      Reporter.report.addFatalError(S.fmt("fileUnableToWrite", outfile.getAbsolutePath()));
      return false;
    }
  }
}
