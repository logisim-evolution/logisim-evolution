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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class ProjectActions {
  private static final String FILE_NAME_FORMAT_ERROR = "FileNameError";
  private static final String FILE_NAME_KEYWORD_ERROR = "ExistingToolName";

  private ProjectActions() {}

  private static class CreateFrame implements Runnable {
    private final Loader loader;
    private final Project proj;
    private final boolean isStartupScreen;

    public CreateFrame(Loader loader, Project proj, boolean isStartup) {
      this.loader = loader;
      this.proj = proj;
      this.isStartupScreen = isStartup;
    }

    @Override
    public void run() {
      try {
        final var frame = createFrame(null, proj);
        frame.setVisible(true);
        frame.toFront();
        frame.getCanvas().requestFocus();
        loader.setParent(frame);
        if (isStartupScreen) {
          proj.setStartupScreen(true);
        }
      } catch (Exception e) {
        final var result = new StringWriter();
        final var printWriter = new PrintWriter(result);
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
      String filename, Project proj, HashMap<String, String> errors) {
    var isOk = true;
    var tempSet = new HashMap<String, Library>();
    var forbiddenNames = new HashSet<String>();
    LibraryTools.buildLibraryList(proj.getLogisimFile(), tempSet);
    LibraryTools.buildToolList(proj.getLogisimFile(), forbiddenNames);
    forbiddenNames.addAll(tempSet.keySet());
    var pattern = Pattern.compile("[^a-z\\d_.]", Pattern.CASE_INSENSITIVE);
    var matcher = pattern.matcher(filename);
    if (matcher.find()) {
      isOk = false;
      errors.put(FILE_NAME_FORMAT_ERROR, S.get("InvalidFileFormatError"));
    }
    if (forbiddenNames.contains(filename.toUpperCase())) {
      isOk = false;
      errors.put(FILE_NAME_KEYWORD_ERROR, S.get("UsedLibraryToolnameError"));
    }
    return isOk;
  }

  private static Project completeProject(
      SplashScreen monitor, Loader loader, LogisimFile file, boolean isStartup) {
    if (monitor != null) monitor.setProgress(SplashScreen.PROJECT_CREATE);
    final var ret = new Project(file);
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
      } catch (IOException ignored) {
      }
    }
    return file;
  }

  private static Frame createFrame(Project sourceProject, Project newProject) {
    if (sourceProject != null) {
      final var frame = sourceProject.getFrame();
      if (frame != null) {
        frame.savePreferences();
      }
    }
    final var newFrame = new Frame(newProject);
    newProject.setFrame(newFrame);
    return newFrame;
  }

  public static LogisimFile createNewFile(Project baseProject) {
    final var parent = (baseProject == null) ? null : baseProject.getFrame();
    final var loader = new Loader(parent);
    final var templReader = AppPreferences.getTemplate().createStream();
    LogisimFile file;
    try {
      file = loader.openLogisimFile(templReader);
    } catch (IOException ex) {
      displayException(baseProject.getFrame(), ex);
      file = createEmptyFile(loader, baseProject);
    } finally {
      try {
        templReader.close();
      } catch (IOException ignored) {
        // Do nothing.
      }
    }
    return file;
  }

  private static void displayException(Component parent, Exception ex) {
    String msg = S.get("templateOpenError", ex.toString());
    String ttl = S.get("templateOpenErrorTitle");
    OptionPane.showMessageDialog(parent, msg, ttl, OptionPane.ERROR_MESSAGE);
  }

  public static Project doNew(Project baseProject) {
    final var file = createNewFile(baseProject);
    final var newProj = new Project(file);
    final var frame = createFrame(baseProject, newProj);
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
    final var loader = new Loader(monitor);
    final var templReader = AppPreferences.getTemplate().createStream();
    LogisimFile file = null;
    try {
      file = loader.openLogisimFile(templReader);
    } catch (IOException ex) {
      displayException(monitor, ex);
    } finally {
      try {
        templReader.close();
      } catch (IOException ignored) {
      }
    }
    if (file == null) file = createEmptyFile(loader, null);
    return completeProject(monitor, loader, file, isStartupScreen);
  }

  public static void doMerge(Component parent, Project baseProject) {
    JFileChooser chooser;
    if (baseProject != null) {
      final var oldLoader = baseProject.getLogisimFile().getLoader();
      chooser = oldLoader.createChooser();
      if (oldLoader.getMainFile() != null) {
        chooser.setSelectedFile(oldLoader.getMainFile());
      }
    } else {
      chooser = JFileChoosers.create();
    }
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    chooser.setDialogTitle(S.get("FileMergeItem"));

    LogisimFile mergelib;
    int returnVal = chooser.showOpenDialog(parent);
    if (returnVal != JFileChooser.APPROVE_OPTION) return;
    final var selected = chooser.getSelectedFile();
    final var loader = new Loader(baseProject == null ? parent : baseProject.getFrame());
    try {
      mergelib = loader.openLogisimFile(selected);
      if (mergelib == null) return;
    } catch (LoadFailedException ex) {
      if (!ex.isShown()) {
        OptionPane.showMessageDialog(
            parent,
            S.get("fileMergeError", ex.toString()),
            S.get("FileMergeErrorItem"),
            OptionPane.ERROR_MESSAGE);
      }
      return;
    }
    baseProject.doAction(LogisimFileActions.mergeFile(mergelib, baseProject.getLogisimFile()));
  }

  private static void updatecircs(LogisimFile lib, Project proj) {
    for (final var circ : lib.getCircuits()) {
      circ.setProject(proj);
    }
    for (final var libs : lib.getLibraries()) {
      if (libs instanceof LoadedLibrary test) {
        if (test.getBase() instanceof LogisimFile lsFile) {
          updatecircs(lsFile, proj);
        }
      }
    }
  }

  public static Project doOpen(Component parent, Project baseProject) {
    JFileChooser chooser;
    if (baseProject != null) {
      final var oldLoader = baseProject.getLogisimFile().getLoader();
      chooser = oldLoader.createChooser();
      if (oldLoader.getMainFile() != null) {
        chooser.setSelectedFile(oldLoader.getMainFile());
      }
    } else {
      chooser = JFileChoosers.create();
    }
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    chooser.setDialogTitle(S.get("FileOpenItem"));

    final var returnVal = chooser.showOpenDialog(parent);
    if (returnVal != JFileChooser.APPROVE_OPTION) return null;
    final var selected = chooser.getSelectedFile();
    if (selected == null) return null;
    return doOpen(parent, baseProject, selected);
  }

  public static Project doOpen(Component parent, Project baseProject, File f) {
    var proj = Projects.findProjectFor(f);
    Loader loader = null;
    if (proj != null) {
      proj.getFrame().toFront();
      loader = proj.getLogisimFile().getLoader();
      if (proj.isFileDirty()) {
        String message = S.get("openAlreadyMessage", proj.getLogisimFile().getName());
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
        if (result == 0) { // keep proj as is, so that load happens into the window
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
      final var lib = loader.openLogisimFile(f);
      AppPreferences.updateRecentFile(f);
      if (lib == null) return null;
      LibraryTools.removePresentLibraries(lib, new HashMap<>(), true);
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
            S.get("fileOpenError", ex.toString()),
            S.get("fileOpenErrorTitle"),
            OptionPane.ERROR_MESSAGE);
      }
      return null;
    }

    var frame = proj.getFrame();
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
    final var loader = new Loader(monitor);
    final var file = loader.openLogisimFile(source, substitutions);
    AppPreferences.updateRecentFile(source);

    return completeProject(monitor, loader, file, false);
  }

  public static Project doOpenNoWindow(SplashScreen monitor, File source)
      throws LoadFailedException {
    final var loader = new Loader(monitor);
    final var file = loader.openLogisimFile(source);
    final var ret = new Project(file);
    updatecircs(file, ret);
    return ret;
  }

  public static void doQuit() {
    final var top = Projects.getTopFrame();
    top.savePreferences();

    for (Project proj : new ArrayList<>(Projects.getOpenProjects())) {
      if (!proj.confirmClose(S.get("confirmQuitTitle"))) return;
    }
    System.exit(0);
  }

  public static boolean doSave(Project proj) {
    final var loader = proj.getLogisimFile().getLoader();
    final var f = loader.getMainFile();
    if (f == null) return doSaveAs(proj);
    else return doSave(proj, f);
  }

  public static boolean doSave(Project proj, File f) {
    final var loader = proj.getLogisimFile().getLoader();
    final var oldTool = proj.getTool();
    proj.setTool(null);
    final var ret = loader.save(proj.getLogisimFile(), f);
    if (ret) {
      AppPreferences.updateRecentFile(f);
      proj.setFileAsClean();
    }
    proj.setTool(oldTool);
    return ret;
  }

  /**
   * Imports a Logisim project in a zip file
   *
   * <p>It is the action listener for the File->Import project bundle... menu option.
   *
   * @param proj the current project to perform the file->open action afterwards 
   * @return true if success, false otherwise 
   */
  public static boolean doExtractAndRunProject(Project proj) {
    var ret = true;
    final var loader = proj.getLogisimFile().getLoader();
    final var chooser = loader.createChooser();
    var isCorrectFile = true;
    do {
      chooser.setFileFilter(Loader.LOGISIM_BUNDLE_FILTER);
      chooser.setAcceptAllFileFilterUsed(false);
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.setDialogTitle(S.get("projImportBundle"));
      ret &= chooser.showOpenDialog(proj.getFrame()) == JFileChooser.APPROVE_OPTION;
      if (!ret) return ret;
      final var zipFileName = chooser.getSelectedFile().getAbsolutePath();
      isCorrectFile = Files.exists(Paths.get(zipFileName));
      if (isCorrectFile) {
        try {
          final var zipFile = new ZipFile(zipFileName);
          final var bundleInfo = ProjectBundleManifest.getManifestInfo(zipFile, proj.getFrame());
          if (bundleInfo == null) return false;
          final var mainFileEntry = zipFile.getEntry(bundleInfo.getMainLogisimFilename());
          if (mainFileEntry == null) {
            OptionPane.showMessageDialog(proj.getFrame(), S.fmt("projBundleReadError", S.get("projBundleMainNotFound")));
            return false;
          }
          final var readmeFileEntry = zipFile.getEntry(ProjectBundleReadme.README_FILE_NAME);
          if (readmeFileEntry != null) {
            final var readmeInStream = zipFile.getInputStream(readmeFileEntry);
            final var dialog = new ProjectBundleReadme(proj, "");
            dialog.showReadme(readmeInStream);
            readmeInStream.close();
          }
          chooser.setFileFilter(Loader.LOGISIM_DIRECTORY);
          chooser.setAcceptAllFileFilterUsed(false);
          chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          chooser.setDialogTitle(S.get("projBundleDirectory"));
          var isCorrectDirectory = true;
          do {
            ret &= chooser.showOpenDialog(proj.getFrame()) == JFileChooser.APPROVE_OPTION;
            if (!ret) return ret;
            final var exportDirectory = chooser.getSelectedFile().getAbsolutePath();
            final var mainProjectFileName = String.format("%s%s%s", exportDirectory, File.separator, bundleInfo.getMainLogisimFilename()); 
            var filename =  mainProjectFileName;
            if (Files.exists(Paths.get(filename)) 
                || Files.exists(Paths.get(String.format("%s%s%s", exportDirectory, File.separator, Loader.LOGISIM_LIBRARY_DIR)))) {
              isCorrectDirectory = false;
              OptionPane.showMessageDialog(proj.getFrame(), S.fmt("projContainsFileDir", bundleInfo.getMainLogisimFilename(), Loader.LOGISIM_LIBRARY_DIR));
            } else {
              isCorrectDirectory = true;
              // extract the main file
              var zipInput = zipFile.getInputStream(mainFileEntry);
              var fileOutput = new FileOutputStream(filename);
              var data = zipInput.read();
              while (data > 0) {
                fileOutput.write(data);
                data = zipInput.read();
              }
              zipInput.close();
              fileOutput.close();
              final var zipFileEntries = zipFile.entries();
              final var libDir = String.format("%s%s%s", exportDirectory, File.separator, Loader.LOGISIM_LIBRARY_DIR);
              while (zipFileEntries.hasMoreElements()) {
                final var entry = zipFileEntries.nextElement();
                if (entry.isDirectory()) {
                  final var dirName = entry.getName();
                  if (!dirName.equals(Loader.LOGISIM_LIBRARY_DIR)) continue;
                  new File(String.format("%s%s%s", exportDirectory, File.separator, dirName)).mkdirs();
                } else {
                  final var entryName = entry.getName();
                  if (!entryName.startsWith(String.format("%s%s", Loader.LOGISIM_LIBRARY_DIR, File.separator))) continue;
                  if (entryName.lastIndexOf(File.separator) != entryName.indexOf(File.separator)) continue;
                  if (!entryName.endsWith(Loader.LOGISIM_EXTENSION) 
                      && !entryName.toLowerCase().endsWith(".jar")) {
                    continue;
                  }
                  if (entryName.startsWith("..")) continue;
                  // make sure the library dir exists
                  if (!Files.exists(Paths.get(libDir))) new File(libDir).mkdirs();
                  filename = String.format("%s%s%s", exportDirectory, File.separator, entryName); 
                  zipInput = zipFile.getInputStream(entry);
                  fileOutput = new FileOutputStream(filename);
                  final var bytes = new byte[1024];
                  var length = 0;
                  while (((length = zipInput.read(bytes)) >= 0)) {
                    fileOutput.write(bytes, 0, length);
                  }
                  fileOutput.close();
                  zipInput.close();
                }
              }
              ProjectActions.doOpen(proj.getFrame().getCanvas(), proj, new File(mainProjectFileName));
            }
          } while (!isCorrectDirectory);
          zipFile.close();
        } catch (IOException e) {
          isCorrectFile = false;
          OptionPane.showMessageDialog(proj.getFrame(), S.fmt("fileOpenError", 
              String.format("%s\n%s", zipFileName, e.getMessage())));
        }
      } else {
        OptionPane.showMessageDialog(proj.getFrame(), S.fmt("fileOpenError", zipFileName));
      }
    } while (!isCorrectFile);
    return ret;
  }

  /**
   * Exports a Logisim project in a zip file
   *
   * <p>It is the action listener for the File->Export project bundle... menu option.
   *
   * @param proj Project to be exported
   * @return true if success, false otherwise 
   */
  public static boolean doExportProject(Project proj) {
    var ret = true;
    final var loader = proj.getLogisimFile().getLoader();
    final var oldTool = proj.getTool();
    proj.setTool(null);
    var mainFileName = loader.getMainFile() == null ? "Untitled.circ" : loader.getMainFile().getName();
    var zipFile = mainFileName.replace(Loader.LOGISIM_EXTENSION, Loader.LOGISIM_PROJECT_BUNDLE_EXTENSION);
    final var chooser = loader.createChooser();
    chooser.setFileFilter(Loader.LOGISIM_BUNDLE_FILTER);
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setSelectedFile(new File(zipFile));
    chooser.setDialogTitle(S.get("projExportBundle"));
    var isCorrectFile = true;
    do {
      ret &= chooser.showSaveDialog(proj.getFrame()) == JFileChooser.APPROVE_OPTION;
      if (!ret) {
        proj.setTool(oldTool);
        return false;
      }
      try {
        zipFile = chooser.getSelectedFile().getAbsolutePath();
        if (!zipFile.endsWith(Loader.LOGISIM_PROJECT_BUNDLE_EXTENSION)) {
          zipFile = zipFile.concat(Loader.LOGISIM_PROJECT_BUNDLE_EXTENSION);
        }
        final var path = Paths.get(zipFile);
        if (Files.exists(path)) {
          isCorrectFile = OptionPane.showConfirmDialog(proj.getFrame(), S.fmt("projExistsOverwrite", 
              new File(zipFile).getName()), S.get("projExportBundle"), OptionPane.YES_NO_OPTION) == OptionPane.YES_OPTION;
        } else {
          isCorrectFile = true;
        }
        if (isCorrectFile) {
          final var dialog = new ProjectBundleReadme(proj, mainFileName.replace(Loader.LOGISIM_EXTENSION, ""));
          final var readmeInfo = dialog.getReadmeInfo();
          if (readmeInfo == null) return false;
          final var projectFile = new FileOutputStream(zipFile);
          final var projectZipFile = new ZipOutputStream(projectFile);
          ProjectBundleReadme.writeReadmeFile(projectZipFile, readmeInfo);
          projectZipFile.putNextEntry(new ZipEntry(String.format("%s%s", Loader.LOGISIM_LIBRARY_DIR, File.separator)));
          mainFileName = chooser.getSelectedFile().getName().replace(Loader.LOGISIM_PROJECT_BUNDLE_EXTENSION, "").concat(Loader.LOGISIM_EXTENSION);
          ret &= loader.export(proj.getLogisimFile(), projectZipFile, mainFileName);
          final var info = ProjectBundleManifest.getInfoContainer(BuildInfo.displayName, mainFileName);
          ProjectBundleManifest.writeManifest(projectZipFile, info);
          projectZipFile.close();
          projectFile.close();
        }
      } catch (IOException e) {
        OptionPane.showMessageDialog(proj.getFrame(), S.get("ProjUnableToCreate", e.getMessage()));
        proj.setTool(oldTool);
        return false;
      }
    } while (!isCorrectFile);
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
    var loader = proj.getLogisimFile().getLoader();
    var chooser = loader.createChooser();
    chooser.setFileFilter(Loader.LOGISIM_FILTER);
    if (loader.getMainFile() != null) {
      chooser.setSelectedFile(loader.getMainFile());
    }

    int returnVal;
    var validFilename = false;
    var errors = new HashMap<String, String>();
    do {
      errors.clear();
      returnVal = chooser.showSaveDialog(proj.getFrame());
      if (returnVal != JFileChooser.APPROVE_OPTION) {
        return false;
      }
      validFilename = checkValidFilename(chooser.getSelectedFile().getName(), proj, errors);
      if (!validFilename) {
        var message = "\"" + chooser.getSelectedFile() + "\":\n";
        for (String key : errors.keySet()) {
          message = message.concat("=> " + S.get(errors.get(key)) + "\n");
        }
        OptionPane.showMessageDialog(
            chooser, message, S.get("FileSaveAsItem"), OptionPane.ERROR_MESSAGE);
      }
    } while (!validFilename);

    var selectedFile = chooser.getSelectedFile();
    if (!selectedFile.getName().endsWith(Loader.LOGISIM_EXTENSION)) {
      var old = selectedFile.getName();
      int ext0 = old.lastIndexOf('.');
      if (ext0 < 0 || !Pattern.matches("\\.\\p{L}{2,}\\d?", old.substring(ext0))) {
        selectedFile = new File(selectedFile.getParentFile(), old + Loader.LOGISIM_EXTENSION);
      } else {
        var ext = old.substring(ext0);
        var ttl = S.get("replaceExtensionTitle");
        var msg = S.get("replaceExtensionMessage", ext);
        Object[] options = {
          S.get("replaceExtensionReplaceOpt", ext),
          S.get("replaceExtensionAddOpt", Loader.LOGISIM_EXTENSION),
          S.get("replaceExtensionKeepOpt")
        };
        var dlog = new JOptionPane(msg);
        dlog.setMessageType(OptionPane.QUESTION_MESSAGE);
        dlog.setOptions(options);
        dlog.createDialog(proj.getFrame(), ttl).setVisible(true);

        Object result = dlog.getValue();
        if (result == options[0]) {
          var name = old.substring(0, ext0) + Loader.LOGISIM_EXTENSION;
          selectedFile = new File(selectedFile.getParentFile(), name);
        } else if (result == options[1]) {
          selectedFile = new File(selectedFile.getParentFile(), old + Loader.LOGISIM_EXTENSION);
        }
      }
    }

    if (selectedFile.exists()) {
      var confirm =
          OptionPane.showConfirmDialog(
              proj.getFrame(),
              S.get("confirmOverwriteMessage"),
              S.get("confirmOverwriteTitle"),
              OptionPane.YES_NO_OPTION);
      if (confirm != OptionPane.YES_OPTION) return false;
    }
    return doSave(proj, selectedFile);
  }
}
