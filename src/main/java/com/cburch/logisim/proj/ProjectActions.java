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

package com.cburch.logisim.proj;

import static com.cburch.logisim.proj.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.StringUtil;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ProjectActions {
  private static String FILE_NAME_FORMAT_ERROR = "FileNameError";
  private static String FILE_NAME_KEYWORD_ERROR = "ExistingToolName";

  private static class CreateFrame implements Runnable {
    private Loader loader;
    private Project proj;
    private boolean isStartupScreen;

    public CreateFrame(Loader loader, Project proj, boolean isStartup) {
      this.loader = loader;
      this.proj = proj;
      this.isStartupScreen = isStartup;
    }

    @Override
    public void run() {
      try {
        Frame frame = createFrame(null, proj);
        frame.setVisible(true);
        frame.toFront();
        frame.getCanvas().requestFocus();
        loader.setParent(frame);
        if (isStartupScreen) {
          proj.setStartupScreen(true);
        }
      } catch (Exception e) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        OptionPane.showMessageDialog(null, result.toString());
        System.exit(-1);
      }
    }
  }

  /**
   * Returns true if the filename contains valid characters only, that is, alphanumeric characters
   * and underscores.
   */
  private static boolean checkValidFilename(
      String filename, Project proj, HashMap<String, String> Errors) {
    boolean IsOk = true;
    HashMap<String, Library> TempSet = new HashMap<String, Library>();
    HashSet<String> ForbiddenNames = new HashSet<String>();
    LibraryTools.BuildLibraryList(proj.getLogisimFile(), TempSet);
    LibraryTools.BuildToolList(proj.getLogisimFile(), ForbiddenNames);
    ForbiddenNames.addAll(TempSet.keySet());
    Pattern p = Pattern.compile("[^a-z0-9_.]", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(filename);
    if (m.find()) {
      IsOk = false;
      Errors.put(FILE_NAME_FORMAT_ERROR, S.get("InvalidFileFormatError"));
    }
    if (ForbiddenNames.contains(filename.toUpperCase())) {
      IsOk = false;
      Errors.put(FILE_NAME_KEYWORD_ERROR, S.get("UsedLibraryToolnameError"));
    }
    return IsOk;
  }

  private static Project completeProject(
      SplashScreen monitor, Loader loader, LogisimFile file, boolean isStartup) {
    if (monitor != null) monitor.setProgress(SplashScreen.PROJECT_CREATE);
    Project ret = new Project(file);
    if (monitor != null) monitor.setProgress(SplashScreen.FRAME_CREATE);
    SwingUtilities.invokeLater(new CreateFrame(loader, ret, isStartup));
    updatecircs(file, ret);
    return ret;
  }

  private static LogisimFile createEmptyFile(Loader loader, Project proj) {
    InputStream templReader = AppPreferences.getEmptyTemplate().createStream();
    LogisimFile file;
    try {
      file = loader.openLogisimFile(templReader);
    } catch (Exception t) {
      file = LogisimFile.createNew(loader, proj);
      file.addCircuit(new Circuit("main", file, proj));
    } finally {
      try {
        templReader.close();
      } catch (IOException e) {
      }
    }
    return file;
  }

  private static Frame createFrame(Project sourceProject, Project newProject) {
    if (sourceProject != null) {
      Frame frame = sourceProject.getFrame();
      if (frame != null) {
        frame.savePreferences();
      }
    }
    Frame newFrame = new Frame(newProject);
    newProject.setFrame(newFrame);
    return newFrame;
  }

  public static LogisimFile createNewFile(Project baseProject) {
    Loader loader = new Loader(baseProject == null ? null : baseProject.getFrame());
    InputStream templReader = AppPreferences.getTemplate().createStream();
    LogisimFile file;
    try {
      file = loader.openLogisimFile(templReader);
    } catch (IOException ex) {
      displayException(baseProject.getFrame(), ex);
      file = createEmptyFile(loader, baseProject);
    } catch (LoadFailedException ex) {
      if (!ex.isShown()) {
        displayException(baseProject.getFrame(), ex);
      }
      file = createEmptyFile(loader, baseProject);
    } finally {
      try {
        templReader.close();
      } catch (IOException e) {
      }
    }
    return file;
  }

  private static void displayException(Component parent, Exception ex) {
    String msg = StringUtil.format(S.get("templateOpenError"), ex.toString());
    String ttl = S.get("templateOpenErrorTitle");
    OptionPane.showMessageDialog(parent, msg, ttl, OptionPane.ERROR_MESSAGE);
  }

  public static Project doNew(Project baseProject) {
    LogisimFile file = createNewFile(baseProject);
    Project newProj = new Project(file);
    Frame frame = createFrame(baseProject, newProj);
    frame.setVisible(true);
    frame.getCanvas().requestFocus();
    newProj.getLogisimFile().getLoader().setParent(frame);
    updatecircs(file, newProj);
    return newProj;
  }

  public static Project doNew(SplashScreen monitor) {
    return doNew(monitor, false);
  }

  public static Project doNew(SplashScreen monitor, boolean isStartupScreen) {
    if (monitor != null) monitor.setProgress(SplashScreen.FILE_CREATE);
    Loader loader = new Loader(monitor);
    InputStream templReader = AppPreferences.getTemplate().createStream();
    LogisimFile file = null;
    try {
      file = loader.openLogisimFile(templReader);
    } catch (IOException ex) {
      displayException(monitor, ex);
    } catch (LoadFailedException ex) {
      displayException(monitor, ex);
    } finally {
      try {
        templReader.close();
      } catch (IOException e) {
      }
    }
    if (file == null) file = createEmptyFile(loader, null);
    return completeProject(monitor, loader, file, isStartupScreen);
  }

  public static void doMerge(Component parent, Project baseProject) {
    JFileChooser chooser;
    LogisimFile mergelib;
    Loader loader = null;
    if (baseProject != null) {
      Loader oldLoader = baseProject.getLogisimFile().getLoader();
      chooser = oldLoader.createChooser();
      if (oldLoader.getMainFile() != null) {
        chooser.setSelectedFile(oldLoader.getMainFile());
      }
    } else {
      chooser = JFileChoosers.create();
    }
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    chooser.setDialogTitle(S.get("FileMergeItem"));

    int returnVal = chooser.showOpenDialog(parent);
    if (returnVal != JFileChooser.APPROVE_OPTION) return;
    File selected = chooser.getSelectedFile();
    loader = new Loader(baseProject == null ? parent : baseProject.getFrame());
    try {
      mergelib = loader.openLogisimFile(selected);
      if (mergelib == null) return;
    } catch (LoadFailedException ex) {
      if (!ex.isShown()) {
        OptionPane.showMessageDialog(
            parent,
            StringUtil.format(S.get("fileMergeError"), ex.toString()),
            S.get("FileMergeErrorItem"),
            OptionPane.ERROR_MESSAGE);
      }
      return;
    }
    baseProject.doAction(LogisimFileActions.MergeFile(mergelib, baseProject.getLogisimFile()));
  }

  private static void updatecircs(LogisimFile lib, Project proj) {
    for (Circuit circ : lib.getCircuits()) {
      circ.SetProject(proj);
    }
    for (Library libs : lib.getLibraries()) {
      if (libs instanceof LoadedLibrary) {
        LoadedLibrary test = (LoadedLibrary) libs;
        if (test.getBase() instanceof LogisimFile) {
          updatecircs((LogisimFile) test.getBase(), proj);
        }
      }
    }
  }

  public static boolean doOpen(Component parent, Project baseProject) {
    JFileChooser chooser;
    if (baseProject != null) {
      Loader oldLoader = baseProject.getLogisimFile().getLoader();
      chooser = oldLoader.createChooser();
      if (oldLoader.getMainFile() != null) {
        chooser.setSelectedFile(oldLoader.getMainFile());
      }
    } else {
      chooser = JFileChoosers.create();
    }
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    chooser.setDialogTitle(S.get("FileOpenItem"));

    int returnVal = chooser.showOpenDialog(parent);
    if (returnVal != JFileChooser.APPROVE_OPTION) return false;
    File selected = chooser.getSelectedFile();
    if (selected != null) {
      doOpen(parent, baseProject, selected);
    }

    return true;
  }

  public static Project doOpen(Component parent, Project baseProject, File f) {
    Project proj = Projects.findProjectFor(f);
    Loader loader = null;
    if (proj != null) {
      proj.getFrame().toFront();
      loader = proj.getLogisimFile().getLoader();
      if (proj.isFileDirty()) {
        String message =
            StringUtil.format(S.get("openAlreadyMessage"), proj.getLogisimFile().getName());
        String[] options = {
          S.get("openAlreadyLoseChangesOption"),
          S.get("openAlreadyNewWindowOption"),
          S.get("openAlreadyCancelOption"),
        };
        int result =
            OptionPane.showOptionDialog(
                proj.getFrame(),
                message,
                S.get("openAlreadyTitle"),
                0,
                OptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        if (result == 0) {; // keep proj as is, so that load happens into the window
        } else if (result == 1) {
          proj = null; // we'll create a new project
        } else {
          return proj;
        }
      }
    }

    if (proj == null && baseProject != null && baseProject.isStartupScreen()) {
      proj = baseProject;
      proj.setStartupScreen(false);
      loader = baseProject.getLogisimFile().getLoader();
    } else {
      loader = new Loader(baseProject == null ? parent : baseProject.getFrame());
    }

    try {
      LogisimFile lib = loader.openLogisimFile(f);
      AppPreferences.updateRecentFile(f);
      if (lib == null) return null;
      LibraryTools.RemovePresentLibraries(lib, new HashMap<String, Library>(), true);
      if (proj == null) {
        proj = new Project(lib);
        updatecircs(lib, proj);
      } else {
        updatecircs(lib, proj);
        proj.setLogisimFile(lib);
      }
    } catch (LoadFailedException ex) {
      if (!ex.isShown()) {
        OptionPane.showMessageDialog(
            parent,
            StringUtil.format(S.get("fileOpenError"), ex.toString()),
            S.get("fileOpenErrorTitle"),
            OptionPane.ERROR_MESSAGE);
      }
      return null;
    }

    Frame frame = proj.getFrame();
    if (frame == null) {
      frame = createFrame(baseProject, proj);
    }
    frame.setVisible(true);
    frame.toFront();
    frame.getCanvas().requestFocus();
    proj.getLogisimFile().getLoader().setParent(frame);
    return proj;
  }

  public static Project doOpen(SplashScreen monitor, File source, Map<File, File> substitutions)
      throws LoadFailedException {
    if (monitor != null) monitor.setProgress(SplashScreen.FILE_LOAD);
    Loader loader = new Loader(monitor);
    LogisimFile file = loader.openLogisimFile(source, substitutions);
    AppPreferences.updateRecentFile(source);

    return completeProject(monitor, loader, file, false);
  }

  public static Project doOpenNoWindow(SplashScreen monitor, File source)
      throws LoadFailedException {
    Loader loader = new Loader(monitor);
    LogisimFile file = loader.openLogisimFile(source);
    Project ret = new Project(file);
    updatecircs(file, ret);
    return ret;
  }

  public static void doQuit() {
    Frame top = Projects.getTopFrame();
    top.savePreferences();

    for (Project proj : new ArrayList<Project>(Projects.getOpenProjects())) {
      if (!proj.confirmClose(S.get("confirmQuitTitle"))) return;
    }
    System.exit(0);
  }

  public static boolean doSave(Project proj) {
    Loader loader = proj.getLogisimFile().getLoader();
    File f = loader.getMainFile();
    if (f == null) return doSaveAs(proj);
    else return doSave(proj, f);
  }

  public static boolean doSave(Project proj, File f) {
    Loader loader = proj.getLogisimFile().getLoader();
    Tool oldTool = proj.getTool();
    proj.setTool(null);
    boolean ret = loader.save(proj.getLogisimFile(), f);
    if (ret) {
      AppPreferences.updateRecentFile(f);
      proj.setFileAsClean();
    }
    proj.setTool(oldTool);
    return ret;
  }

  /**
   * Saves a Logisim project in a .circ file.
   *
   * <p>It is the action listener for the File->Save as... menu option.
   *
   * @param proj project to be saved
   * @return true if success, false otherwise
   */
  public static boolean doSaveAs(Project proj) {
    Loader loader = proj.getLogisimFile().getLoader();
    JFileChooser chooser = loader.createChooser();
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    if (loader.getMainFile() != null) {
      chooser.setSelectedFile(loader.getMainFile());
    }

    int returnVal;
    boolean validFilename = false;
    HashMap<String, String> Error = new HashMap<String, String>();
    do {
      Error.clear();
      returnVal = chooser.showSaveDialog(proj.getFrame());
      if (returnVal != JFileChooser.APPROVE_OPTION) {
        return false;
      }
      validFilename = checkValidFilename(chooser.getSelectedFile().getName(), proj, Error);
      if (!validFilename) {
        String Message = "\"" + chooser.getSelectedFile() + "\":\n";
        for (String key : Error.keySet()) {
          String error = S.get(Error.get(key));
          Message = Message.concat("=> " + error + "\n");
        }
        OptionPane.showMessageDialog(
            chooser, Message, S.get("FileSaveAsItem"), OptionPane.ERROR_MESSAGE);
      }
    } while (!validFilename);

    File f = chooser.getSelectedFile();
    String circExt = Loader.LOGISIM_EXTENSION;
    if (!f.getName().endsWith(circExt)) {
      String old = f.getName();
      int ext0 = old.lastIndexOf('.');
      if (ext0 < 0 || !Pattern.matches("\\.\\p{L}{2,}[0-9]?", old.substring(ext0))) {
        f = new File(f.getParentFile(), old + circExt);
      } else {
        String ext = old.substring(ext0);
        String ttl = S.get("replaceExtensionTitle");
        String msg = S.fmt("replaceExtensionMessage", ext);
        Object[] options = {
          S.fmt("replaceExtensionReplaceOpt", ext),
          S.fmt("replaceExtensionAddOpt", circExt),
          S.get("replaceExtensionKeepOpt")
        };
        JOptionPane dlog = new JOptionPane(msg);
        dlog.setMessageType(OptionPane.QUESTION_MESSAGE);
        dlog.setOptions(options);
        dlog.createDialog(proj.getFrame(), ttl).setVisible(true);

        Object result = dlog.getValue();
        if (result == options[0]) {
          String name = old.substring(0, ext0) + circExt;
          f = new File(f.getParentFile(), name);
        } else if (result == options[1]) {
          f = new File(f.getParentFile(), old + circExt);
        }
      }
    }

    if (f.exists()) {
      int confirm =
          OptionPane.showConfirmDialog(
              proj.getFrame(),
              S.get("confirmOverwriteMessage"),
              S.get("confirmOverwriteTitle"),
              OptionPane.YES_NO_OPTION);
      if (confirm != OptionPane.YES_OPTION) return false;
    }
    return doSave(proj, f);
  }

  private ProjectActions() {}
}
