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

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

public final class Softwares {

  private static boolean createWorkLibrary(File tmpDir, String questaPath, StringBuffer result)
      throws IOException, InterruptedException {
    BufferedReader reader = null;

    if (new File(FileUtil.correctPath(tmpDir.getCanonicalPath()) + "work").exists()) return true;

    try {
      List<String> command = new ArrayList<String>();
      command.add(FileUtil.correctPath(questaPath) + QUESTA_BIN[VLIB]);
      command.add("work");

      ProcessBuilder vlibBuilder = new ProcessBuilder(command);
      vlibBuilder.directory(tmpDir);
      Process vlib = vlibBuilder.start();

      InputStream is = vlib.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      reader = new BufferedReader(isr);

      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
        result.append(System.getProperty("line.separator"));
      }

      return vlib.waitFor() == 0;
    } catch (IOException e) {
      throw e;
    } catch (InterruptedException e) {
      throw e;
    } finally {
      try {
        if (reader != null) reader.close();
      } catch (IOException ex) {
        Logger.getLogger(Softwares.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public static String getQuestaPath() {
    return getQuestaPath(null);
  }

  public static String getQuestaPath(Component parent) {
    String prefPath = AppPreferences.QUESTA_PATH.get();

    if (!validatePath(prefPath, QUESTA)) if ((prefPath = setQuestaPath()) == null) return null;

    return prefPath;
  }

  private static String[] loadQuesta() {
    String[] questaProgs = {"vcom", "vsim", "vmap", "vlib"};

    String osname = System.getProperty("os.name");
    if (osname == null) throw new IllegalArgumentException("no os.name");
    else if (osname.toLowerCase().contains("windows"))
      for (int i = 0; i < questaProgs.length; i++) questaProgs[i] += ".exe";

    return questaProgs;
  }

  public static String setQuestaPath() {
    return setQuestaPath(null);
  }

  public static String setQuestaPath(Component parent) {
    String path = null;

    JFileChooser chooser = JFileChoosers.create();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setDialogTitle(S.get("questaDialogTitle"));
    chooser.setApproveButtonText(S.get("questaDialogButton"));
    int action = chooser.showOpenDialog(parent);
    if (action == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();

      try {
        path = file.getCanonicalPath();
      } catch (IOException ex) {
        OptionPane.showMessageDialog(
            parent,
            S.get("questaIoErrorMessage"),
            S.get("questaErrorTitle"),
            OptionPane.ERROR_MESSAGE);
        return null;
      }

      if (validatePath(path, QUESTA)) {
        AppPreferences.QUESTA_PATH.set(path);
      } else {
        OptionPane.showMessageDialog(
            parent,
            S.get("questaErrorMessage"),
            S.get("questaErrorTitle"),
            OptionPane.ERROR_MESSAGE);
        return null;
      }
    }

    return path;
  }

  private static boolean validatePath(String path, String software) {
    String[] programs;

    if (software.equals(QUESTA)) programs = QUESTA_BIN;
    else return false;

    for (int i = 0; i < programs.length; i++) {
      File test = new File(FileUtil.correctPath(path) + programs[i]);
      if (!test.exists()) return false;
    }

    return true;
  }

  public static int validateVhdl(String vhdl, StringBuffer title, StringBuffer result) {
    if (!AppPreferences.QUESTA_VALIDATION.get()) return SUCCESS;

    String questaPath = getQuestaPath();
    BufferedReader reader = null;
    File tmp = null;

    if (questaPath == null) {
      result.append(S.get("questaValidationAbordedMessage"));
      title.append(S.get("questaValidationAbordedTitle"));
      return ABORD;
    }

    try {
      tmp = FileUtil.createTmpFile(vhdl, "tmp", ".vhd");
      File tmpDir = new File(tmp.getParentFile().getCanonicalPath());

      if (!createWorkLibrary(tmpDir, questaPath, result)) {
        title.insert(0, S.get("questaLibraryErrorTitle"));
        result.insert(0, System.getProperty("line.separator"));
        result.insert(0, S.get("questaLibraryErrorMessage"));
        return ERROR;
      }

      List<String> command = new ArrayList<String>();
      command.add(FileUtil.correctPath(questaPath) + QUESTA_BIN[VCOM]);
      command.add("-reportprogress");
      command.add("300");
      command.add("-93");
      command.add("-work");
      command.add("work");
      command.add(tmp.getName());

      ProcessBuilder questa = new ProcessBuilder(command);
      questa.directory(tmpDir);
      Process vcom = questa.start();

      InputStream is = vcom.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      reader = new BufferedReader(isr);

      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line);
        result.append(System.getProperty("line.separator"));
      }

      if (vcom.waitFor() != 0) {
        title.insert(0, S.get("questaValidationFailedTitle"));
        result.insert(0, System.getProperty("line.separator"));
        result.insert(0, S.get("questaValidationFailedMessage"));
        return ERROR;
      }
    } catch (IOException e) {
      title.insert(0, S.get("questaValidationFailedTitle"));
      result.replace(0, result.length(), e.getMessage());
      result.insert(0, System.getProperty("line.separator"));
      result.insert(0, S.get("questaValidationIoException"));
      return ERROR;
    } catch (InterruptedException e) {
      title.insert(0, S.get("questaValidationFailedTitle"));
      result.replace(0, result.length(), e.getMessage());
      result.insert(0, System.getProperty("line.separator"));
      result.insert(0, S.get("questaValidationInterrupted"));
      return ERROR;
    } finally {
      try {
        if (tmp != null) tmp.deleteOnExit();
        if (reader != null) reader.close();
      } catch (IOException ex) {
        Logger.getLogger(Softwares.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return SUCCESS;
  }

  public static final String QUESTA = "questaSim";

  public static final String[] QUESTA_BIN = loadQuesta();

  public static final int SUCCESS = 0;

  public static final int ERROR = 1;

  public static final int ABORD = 2;

  public static final int VCOM = 0;

  public static final int VSIM = 1;

  public static final int VMAP = 2;

  public static final int VLIB = 3;

  private Softwares() {}
}
