/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import com.cburch.logisim.Main;
import com.cburch.logisim.fpga.gui.Reporter;

public class FileWriter {

  public static boolean CopyArchitecture(String source, String dest, String componentName) {
    try {
      if (HDL.isVerilog()) {
        Reporter.Report.AddFatalError("Empty VHDL box not supported in verilog.");
        return false;
      }
      File inFile = new File(source);
      if (!inFile.exists()) {
        Reporter.Report.AddFatalError("Source file \"" + source + "\" does not exist!");
        return false;
      }
      // copy file
      String destPath = dest + componentName + ARCHITECTURE_EXTENSION + ".vhd";
      File outFile = new File(destPath);
      InputStream in = new FileInputStream(inFile);
      OutputStream out = new FileOutputStream(outFile);
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
      Reporter.Report.AddInfo("\"" + source + "\" successfully copied to \"" + destPath + "\"");
      return true;
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Unable to copy file!");
      return false;
    }
  }

  public static ArrayList<String> getExtendedLibrary() {
    ArrayList<String> Lines = new ArrayList<>();
    Lines.add("");
    Lines.add("LIBRARY ieee;");
    Lines.add("USE ieee.std_logic_1164.all;");
    Lines.add("USE ieee.numeric_std.all;");
    Lines.add("");
    return Lines;
  }

  public static File GetFilePointer(
      String TargetDirectory,
      String ComponentName,
      boolean IsEntity) {
    try {
      File OutDir = new File(TargetDirectory);
      if (!OutDir.exists()) {
        if (!OutDir.mkdirs()) {
          return null;
        }
      }
      String FileName = TargetDirectory;
      if (!FileName.endsWith(File.separator)) {
        FileName += File.separator;
      }
      FileName += ComponentName;
      if (IsEntity) {
        if (HDL.isVHDL()) {
          FileName += ENTITY_EXTENSION;
        }
      } else {
        if (HDL.isVHDL()) {
          FileName += ARCHITECTURE_EXTENSION;
        }
      }
      if (HDL.isVHDL()) {
        FileName += ".vhd";
      } else {
        FileName += ".v";
      }
      File OutFile = new File(FileName);
      Reporter.Report.AddInfo("Creating HDL file : \"" + FileName + "\"");
      if (OutFile.exists()) {
        Reporter.Report.AddWarning("HDL file \"" + FileName + "\" already exists");
        return null;
      }
      return OutFile;
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Unable to create file!");
      return null;
    }
  }

  public static File GetFilePointer(String TargetDirectory, String Name) {
    try {
      File OutDir = new File(TargetDirectory);
      if (!OutDir.exists()) {
        if (!OutDir.mkdirs()) {
          return null;
        }
      }
      String FileName = TargetDirectory;
      if (!FileName.endsWith(File.separator)) {
        FileName += File.separator;
      }
      FileName += Name;
      File OutFile = new File(FileName);
      Reporter.Report.AddInfo("Creating file : \"" + FileName + "\"");
      if (OutFile.exists()) {
        Reporter.Report.AddWarning("File \"" + FileName + "\" already exists");
        return null;
      }
      return OutFile;
    } catch (Exception e) {
      Reporter.Report.AddFatalError("Unable to create file!");
      return null;
    }
  }

  public static ArrayList<String> getGenerateRemark(String compName, String projName) {
    ArrayList<String> Lines = new ArrayList<>();
    final int headWidth;
    final String headOpen;
    final String headClose;

    final var headText = " " + Main.APP_NAME + " goes FPGA automatic generated " + (HDL.isVHDL() ? "VHDL" : "Verilog") + " code";
    final var headUrl  = " " + Main.APP_URL;
    final var headProj = " Project   : " + projName;
    final var headComp = " Component : " + compName;

    if (HDL.isVHDL()) {
      headWidth = 74;
      headOpen = "--==";
      headClose = "==";

      Lines.add(headOpen + "=".repeat(headWidth) + headClose);
      Lines.add(headOpen + headText + " ".repeat(Math.max(0, headWidth - headText.length())) + headClose);
      Lines.add(headOpen + headUrl + " ".repeat(Math.max(0, headWidth - headUrl.length())) + headClose);
      Lines.add(headOpen + " ".repeat(headWidth) + headClose);
      Lines.add(headOpen + " ".repeat(headWidth) + headClose);
      Lines.add(headOpen + headProj + " ".repeat(Math.max(0, headWidth - headProj.length())) + headClose);
      Lines.add(headOpen + headComp + " ".repeat(Math.max(0, headWidth - headComp.length())) + headClose);
      Lines.add(headOpen + " ".repeat(headWidth) + headClose);
      Lines.add(headOpen + "=".repeat(headWidth) + headClose);
      Lines.add("");
    } else if (HDL.isVerilog()) {
      headWidth = 74;
      headOpen = " **";
      headClose = "**";

      Lines.add("/**" + "*".repeat(headWidth) + headClose);
      Lines.add(headOpen + headText + " ".repeat(Math.max(0, headWidth - headText.length())) + headClose);
      Lines.add(headOpen + headUrl + " ".repeat(Math.max(0, headWidth - headUrl.length())) + headClose);
      Lines.add(headOpen + " ".repeat(headWidth) + headClose);
      Lines.add(headOpen + headComp + " ".repeat(Math.max(0, headWidth - headComp.length())) + headClose);
      Lines.add(headOpen + " ".repeat(headWidth) + headClose);
      Lines.add(headOpen + "*".repeat(headWidth) + "*/");
      Lines.add("");
    }
    return Lines;
  }

  public static ArrayList<String> getStandardLibrary() {
    ArrayList<String> Lines = new ArrayList<>();
    Lines.add("");
    Lines.add("LIBRARY ieee;");
    Lines.add("USE ieee.std_logic_1164.all;");
    Lines.add("");
    return Lines;
  }

  public static boolean WriteContents(File outfile, ArrayList<String> Contents) {
    try {
      FileOutputStream output = new FileOutputStream(outfile);
      for (String ThisLine : Contents) {
        if (!ThisLine.isEmpty()) {
          output.write(ThisLine.getBytes());
        }
        output.write("\n".getBytes());
      }
      output.flush();
      output.close();
      return true;
    } catch (Exception e) {
      Reporter.Report.AddFatalError(
          "Could not write to file \"" + outfile.getAbsolutePath() + "\"");
      return false;
    }
  }

  public static final String REMARK_LINE =
      "--------------------------------------------------------------------------------";

  public static final String ENTITY_EXTENSION = "_entity";

  public static final String ARCHITECTURE_EXTENSION = "_behavior";
}
