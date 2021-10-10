/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import static com.cburch.logisim.proj.Strings.S;

import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;

public class ProjectBundleInfoFile {

  public static final String VERSION_PREAMBLE = "Created with: ";
  public static final String MAIN_FILE_PREAMBLE = "Main file: ";
  
  public static class infofileInformation {
    private final String logisimVersion;
    private final String mainCircuitFile;

    public infofileInformation(String logisimVersion, String mainCircuitFile) {
      this.logisimVersion = logisimVersion;
      this.mainCircuitFile = mainCircuitFile;
    }

    public String getBundleLogisimVersion() {
      return logisimVersion;
    }

    public String getMainLogisimFilename() {
      return mainCircuitFile;
    }
  }
  
  public static infofileInformation getInfoContainer(String logisimVersion, String mainCircuitFile) {
    return new infofileInformation(logisimVersion, mainCircuitFile);
  }

  public static void writeBundleInfoFile(ZipOutputStream zipFile, infofileInformation info) throws IOException {
    zipFile.putNextEntry(new ZipEntry(Loader.LOGISIM_PROJECT_BUNDLE_INFO_FILE));
    zipFile.write(String.format("%s%s\n", VERSION_PREAMBLE, info.logisimVersion).getBytes());
    zipFile.write(String.format("%s%s\n", MAIN_FILE_PREAMBLE, info.mainCircuitFile).getBytes());
    
  }
  
  public static infofileInformation getBundleInfoFileContents(ZipFile zipFile, Frame frame) throws IOException {
    final var projInfoEntry = zipFile.getEntry(Loader.LOGISIM_PROJECT_BUNDLE_INFO_FILE);
    if (projInfoEntry == null) {
      OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleNoInfo")));
      return null;
    }
    final var projInfoStream = zipFile.getInputStream(projInfoEntry);
    final var projInputLines = new ArrayList<String>();
    final var oneLine = new StringBuilder();
    int kar;
    do {
      kar = projInfoStream.read();
      if (kar == '\n') {
        projInputLines.add(oneLine.toString());
        oneLine.setLength(0);
      } else if (kar >= 0) {
        oneLine.append((char) kar);
      }
    } while (kar >= 0);
    if (oneLine.length() > 0) projInputLines.add(oneLine.toString());
    projInfoStream.close();
    String infoCreatedVersion = null;
    String infoMainCircuit = null;
    final var infoEntryIterator = projInputLines.iterator();
    while (infoEntryIterator.hasNext()) {
      final var infoLine = infoEntryIterator.next();
      if (infoLine.startsWith(VERSION_PREAMBLE)) {
        if (infoCreatedVersion != null) {
          OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
          return null;
        }
        infoCreatedVersion = infoLine.replace(VERSION_PREAMBLE, "");
        infoEntryIterator.remove();
      } else if (infoLine.startsWith(MAIN_FILE_PREAMBLE)) {
        if (infoMainCircuit != null) {
          OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
          return null;
        }
        infoMainCircuit = infoLine.replace(MAIN_FILE_PREAMBLE, "");
        infoEntryIterator.remove(); 
      }
    }
    if ((projInputLines.size() != 0) || (infoCreatedVersion == null) || (infoMainCircuit == null)) {
      OptionPane.showMessageDialog(frame, S.fmt("projBundleReadError", S.get("projBundleMisformatted")));
      return null;
    }
    return new infofileInformation(infoCreatedVersion, infoMainCircuit);
  }
}
