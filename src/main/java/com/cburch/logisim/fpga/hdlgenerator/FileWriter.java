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

package com.cburch.logisim.fpga.hdlgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.cburch.logisim.fpga.gui.FPGAReport;

public class FileWriter {

  public static boolean CopyArchitecture(
      String source, String dest, String componentName, FPGAReport reporter, String HDLType) {
    try {
      if (HDLType.equals(HDLGeneratorFactory.VERILOG)) {
        reporter.AddFatalError("Empty VHDL box not supported in verilog.");
        return false;
      }
      File inFile = new File(source);
      if (!inFile.exists()) {
        reporter.AddFatalError("Source file \"" + source + "\" does not exist!");
        return false;
      }
      // copy file
      String destPath = dest + componentName + ArchitectureExtension + ".vhd";
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
      reporter.AddInfo("\"" + source + "\" successfully copied to \"" + destPath + "\"");
      return true;
    } catch (Exception e) {
      reporter.AddFatalError("Unable to copy file!");
      return false;
    }
  }

  public static ArrayList<String> getExtendedLibrary() {
    ArrayList<String> Lines = new ArrayList<String>();
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
      boolean IsEntity,
      FPGAReport MyReporter,
      String HDLType) {
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
        if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
          FileName += EntityExtension;
        }
      } else {
        if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
          FileName += ArchitectureExtension;
        }
      }
      if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
        FileName += ".vhd";
      } else {
        FileName += ".v";
      }
      File OutFile = new File(FileName);
      MyReporter.AddInfo("Creating HDL file : \"" + FileName + "\"");
      if (OutFile.exists()) {
        MyReporter.AddWarning("HDL file \"" + FileName + "\" already exists");
        return null;
      }
      return OutFile;
    } catch (Exception e) {
      MyReporter.AddFatalError("Unable to create file!");
      return null;
    }
  }

  public static File GetFilePointer(String TargetDirectory, String Name, FPGAReport MyReporter) {
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
      MyReporter.AddInfo("Creating file : \"" + FileName + "\"");
      if (OutFile.exists()) {
        MyReporter.AddWarning("File \"" + FileName + "\" already exists");
        return null;
      }
      return OutFile;
    } catch (Exception e) {
      MyReporter.AddFatalError("Unable to create file!");
      return null;
    }
  }

  public static ArrayList<String> getGenerateRemark(
      String compName, String HDLIdentifier, String projName) {
    ArrayList<String> Lines = new ArrayList<String>();
    if (HDLIdentifier.equals(HDLGeneratorFactory.VHDL)) {
      Lines.add("--==============================================================================");
      Lines.add("--== Logisim goes FPGA automatic generated VHDL code                          ==");
      Lines.add("--==                                                                          ==");
      Lines.add("--==                                                                          ==");
      String ThisLine = "--== Project   : ";
      int nr_of_spaces = (80 - 2 - ThisLine.length() - projName.length());
      ThisLine += projName;
      for (int i = 0; i < nr_of_spaces; i++) {
        ThisLine += " ";
      }
      ThisLine += "==";
      Lines.add(ThisLine);
      ThisLine = "--== Component : ";
      nr_of_spaces = (80 - 2 - ThisLine.length() - compName.length());
      ThisLine += compName;
      for (int i = 0; i < nr_of_spaces; i++) {
        ThisLine += " ";
      }
      ThisLine += "==";
      Lines.add(ThisLine);
      Lines.add("--==                                                                          ==");
      Lines.add("--==============================================================================");
      Lines.add("");
    } else {
      if (HDLIdentifier.equals(HDLGeneratorFactory.VERILOG)) {
        Lines.add(
            "/******************************************************************************");
        Lines.add(
            " ** Logisim goes FPGA automatic generated Verilog code                       **");
        Lines.add(
            " **                                                                          **");
        String ThisLine = " ** Component : ";
        int nr_of_spaces = (79 - 2 - ThisLine.length() - compName.length());
        ThisLine += compName;
        for (int i = 0; i < nr_of_spaces; i++) {
          ThisLine += " ";
        }
        ThisLine += "**";
        Lines.add(ThisLine);
        Lines.add(
            " **                                                                          **");
        Lines.add(
            " ******************************************************************************/");
        Lines.add("");
      }
    }
    return Lines;
  }

  public static ArrayList<String> getStandardLibrary() {
    ArrayList<String> Lines = new ArrayList<String>();
    Lines.add("");
    Lines.add("LIBRARY ieee;");
    Lines.add("USE ieee.std_logic_1164.all;");
    Lines.add("");
    return Lines;
  }

  public static boolean WriteContents(
      File outfile, ArrayList<String> Contents, FPGAReport MyReporter) {
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
      MyReporter.AddFatalError("Could not write to file \"" + outfile.getAbsolutePath() + "\"");
      return false;
    }
  }

  public static final String RemarkLine =
      "--------------------------------------------------------------------------------";

  public static final String EntityExtension = "_entity";

  public static final String ArchitectureExtension = "_behavior";
}
