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

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class ProjectLibraryActions {
  private static class BuiltinOption {
    Library lib;

    BuiltinOption(Library lib) {
      this.lib = lib;
    }

    @Override
    public String toString() {
      return lib.getDisplayName();
    }
  }

  @SuppressWarnings("rawtypes")
  private static class LibraryJList extends JList {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    LibraryJList(List<Library> libraries) {
      ArrayList<BuiltinOption> options = new ArrayList<BuiltinOption>();
      for (Library lib : libraries) {
        options.add(new BuiltinOption(lib));
      }
      setListData(options.toArray());
    }

    Library[] getSelectedLibraries() {
      Object[] selected = getSelectedValuesList().toArray();
      if (selected != null && selected.length > 0) {
        Library[] libs = new Library[selected.length];
        for (int i = 0; i < selected.length; i++) {
          libs[i] = ((BuiltinOption) selected[i]).lib;
        }
        return libs;
      } else {
        return null;
      }
    }
  }

  public static void doLoadBuiltinLibrary(Project proj) {
    LogisimFile file = proj.getLogisimFile();
    List<Library> baseBuilt = file.getLoader().getBuiltin().getLibraries();
    ArrayList<Library> builtins = new ArrayList<Library>(baseBuilt);
    builtins.removeAll(file.getLibraries());
    if (builtins.isEmpty()) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("loadBuiltinNoneError"),
          S.get("loadBuiltinErrorTitle"),
          OptionPane.INFORMATION_MESSAGE);
      return;
    }
    LibraryJList list = new LibraryJList(builtins);
    JScrollPane listPane = new JScrollPane(list);
    int action =
        OptionPane.showConfirmDialog(
            proj.getFrame(),
            listPane,
            S.get("loadBuiltinDialogTitle"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
    if (action == OptionPane.OK_OPTION) {
      Library[] libs = list.getSelectedLibraries();
      if (libs != null)
        proj.doAction(LogisimFileActions.loadLibraries(libs, proj.getLogisimFile()));
    }
  }

  public static void doLoadJarLibrary(Project proj) {
    Loader loader = proj.getLogisimFile().getLoader();
    JFileChooser chooser = loader.createChooser();
    chooser.setDialogTitle(S.get("loadJarDialogTitle"));
    chooser.setFileFilter(Loader.JAR_FILTER);
    int check = chooser.showOpenDialog(proj.getFrame());
    if (check == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      String className = null;

      // try to retrieve the class name from the "Library-Class"
      // attribute in the manifest. This section of code was contributed
      // by Christophe Jacquet (Request Tracker #2024431).
      JarFile jarFile = null;
      try {
        jarFile = new JarFile(f);
        Manifest manifest = jarFile.getManifest();
        className = manifest.getMainAttributes().getValue("Library-Class");
      } catch (IOException e) {
        // if opening the JAR file failed, do nothing
      } finally {
        if (jarFile != null) {
          try {
            jarFile.close();
          } catch (IOException e) {
          }
        }
      }

      // if the class name was not found, go back to the good old dialog
      if (className == null) {
        className =
            OptionPane.showInputDialog(
                proj.getFrame(),
                S.get("jarClassNamePrompt"),
                S.get("jarClassNameTitle"),
                OptionPane.QUESTION_MESSAGE);
        // if user canceled selection, abort
        if (className == null) return;
      }

      Library lib = loader.loadJarLibrary(f, className);
      if (lib != null) {
        proj.doAction(LogisimFileActions.loadLibrary(lib, proj.getLogisimFile()));
      }
    }
  }

  public static void doLoadLogisimLibrary(Project proj) {
    Loader loader = proj.getLogisimFile().getLoader();
    JFileChooser chooser = loader.createChooser();
    chooser.setDialogTitle(S.get("loadLogisimDialogTitle"));
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    int check = chooser.showOpenDialog(proj.getFrame());
    if (check == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      Library lib = loader.loadLogisimLibrary(f);
      if (lib != null) {
        proj.doAction(LogisimFileActions.loadLibrary(lib, proj.getLogisimFile()));
      }
    }
  }

  public static void doUnloadLibraries(Project proj) {
    LogisimFile file = proj.getLogisimFile();
    ArrayList<Library> canUnload = new ArrayList<Library>();
    for (Library lib : file.getLibraries()) {
      String message = file.getUnloadLibraryMessage(lib);
      if (message == null) canUnload.add(lib);
    }
    if (canUnload.isEmpty()) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("unloadNoneError"),
          S.get("unloadErrorTitle"),
          OptionPane.INFORMATION_MESSAGE);
      return;
    }
    LibraryJList list = new LibraryJList(canUnload);
    JScrollPane listPane = new JScrollPane(list);
    int action =
        OptionPane.showConfirmDialog(
            proj.getFrame(),
            listPane,
            S.get("unloadLibrariesDialogTitle"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
    if (action == OptionPane.OK_OPTION) {
      Library[] libs = list.getSelectedLibraries();
      if (libs != null) proj.doAction(LogisimFileActions.unloadLibraries(libs));
    }
  }

  public static void doUnloadLibrary(Project proj, Library lib) {
    String message = proj.getLogisimFile().getUnloadLibraryMessage(lib);
    if (message != null) {
      OptionPane.showMessageDialog(
          proj.getFrame(), message, S.get("unloadErrorTitle"), OptionPane.ERROR_MESSAGE);
    } else {
      proj.doAction(LogisimFileActions.unloadLibrary(lib));
    }
  }

  private ProjectLibraryActions() {}
}
