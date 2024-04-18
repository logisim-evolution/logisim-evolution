/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LineBuffer;
import com.cburch.logisim.util.ZipClassLoader;
import com.cburch.logisim.vhdl.file.HdlFile;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class Loader implements LibraryLoader {
  private static class JarFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".jar");
    }

    @Override
    public String getDescription() {
      return S.get("jarFileFilter");
    }
  }

  private static class TxtFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".txt");
    }

    @Override
    public String getDescription() {
      return S.get("txtFileFilter");
    }
  }

  private static class VhdlFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".vhd") || f.getName().endsWith(".vhdl");
    }

    @Override
    public String getDescription() {
      return S.get("vhdlFileFilter");
    }
  }

  private static class LogisimFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(LOGISIM_EXTENSION);
    }

    @Override
    public String getDescription() {
      return S.get("logisimFileFilter");
    }
  }

  private static class LogisimDirectoryFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory();
    }

    @Override
    public String getDescription() {
      return S.get("logisimDirectoryFilter");
    }
  }

  private static class TclFileFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".tcl");
    }

    @Override
    public String getDescription() {
      return S.get("tclFileFilter");
    }
  }

  public static final String LOGISIM_EXTENSION = ".circ";
  public static final String LOGISIM_LIBRARY_DIR = "library";
  public static final String LOGISIM_CIRCUIT_DIR = "circuit";
  public static final String LOGISIM_UNNAMED_AUTOSAVE_PREFIX = ".logisim-unnamed-autosave_";
  public static final String LOGISIM_UNNAMED_AUTOSAVE_SUFFIX = ".circ.autosave";
  public static final FileFilter LOGISIM_FILTER = new LogisimFileFilter();
  public static final FileFilter LOGISIM_DIRECTORY = new LogisimDirectoryFilter();
  public static final FileFilter JAR_FILTER = new JarFileFilter();
  public static final FileFilter TXT_FILTER = new TxtFileFilter();
  public static final FileFilter TCL_FILTER = new TclFileFilter();
  public static final FileFilter VHDL_FILTER = new VhdlFileFilter();

  private Component parent;
  private final Builtin builtin = new Builtin();
  // to be cleared with each new file
  private File mainFile = null;
  private File autosaveFile = null;
  private final Stack<File> filesOpening = new Stack<>();
  private Map<File, File> substitutions = new HashMap<>();

  public Loader(Component parent) {
    this.parent = parent;
    clear();
  }

  private static File determineBackupName(File base) {
    final var dir = base.getParentFile();
    var name = base.getName();
    if (name.endsWith(LOGISIM_EXTENSION)) {
      name = name.substring(0, name.length() - LOGISIM_EXTENSION.length());
    }
    for (var i = 1; i <= 20; i++) {
      final var ext = i == 1 ? ".bak" : (".bak" + i);
      final var candidate = new File(dir, name + ext);
      if (!candidate.exists()) return candidate;
    }
    return null;
  }

  // Determine the autosave file name for any .circ file as .<basename>.circ.autosave
  private static File determineAutosaveName(File base) {
    if (base == null) {
      String timestamp =
          DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
      final var candidate = new File(System.getProperty("user.home") + File.separator
          + LOGISIM_UNNAMED_AUTOSAVE_PREFIX + timestamp + LOGISIM_UNNAMED_AUTOSAVE_SUFFIX);
      if (!candidate.exists()) return candidate;
      return null;
    }
    if (!base.getName().endsWith(LOGISIM_EXTENSION)) return null;
    final var dir = base.getParentFile();
    final var name = "." + base.getName() + ".autosave";
    return new File(dir, name);
  }

  static Optional<File> findAutosaveFile(File base) {
    final var as = determineAutosaveName(base);
    if (as == null || !as.exists()) return Optional.empty();
    return Optional.of(as);
  }

  private static void recoverBackup(File backup, File dest) {
    if (backup != null && backup.exists()) {
      // FIXME: recovery will fail if delete() failed
      if (dest.exists()) dest.delete();
      // FIXME: renameTo() can fail. We need to tell the user if so
      backup.renameTo(dest);
    }
  }

  //
  // more substantive methods accessed from outside this package
  //
  public void clear() {
    filesOpening.clear();
    mainFile = null;
  }

  public JFileChooser createChooser() {
    return JFileChoosers.createAt(getCurrentDirectory());
  }

  public Builtin getBuiltin() {
    return builtin;
  }

  // used here and in LibraryManager only, also in MemMenu
  public File getCurrentDirectory() {
    final var ref = (!filesOpening.empty()) ? filesOpening.peek() : mainFile;
    return ref == null ? null : ref.getParentFile();
  }

  @Override
  public String getDescriptor(Library lib) {
    return LibraryManager.instance.getDescriptor(this, lib);
  }

  //
  // helper methods
  //
  File getFileFor(String name, FileFilter filter) {
    // Determine the actual file name.
    var file = new File(name);
    if (!file.isAbsolute()) {
      final var currentDirectory = getCurrentDirectory();
      if (currentDirectory != null) file = new File(currentDirectory, name);
    }
    while (!file.canRead()) {
      // It doesn't exist. Figure it out from the user.
      OptionPane.showMessageDialog(
          parent, String.format(S.get("fileLibraryMissingError"), file.getName()));
      final var chooser = createChooser();
      chooser.setFileFilter(filter);
      chooser.setDialogTitle(S.get("fileLibraryMissingTitle", file.getName()));
      int action = chooser.showDialog(parent, S.get("fileLibraryMissingButton"));
      if (action != JFileChooser.APPROVE_OPTION) {
        throw new LoaderException(S.get("fileLoadCanceledError"));
      }
      file = chooser.getSelectedFile();
    }
    return file;
  }

  //
  // file chooser related methods
  //
  public File getMainFile() {
    return mainFile;
  }

  private File getSubstitution(File source) {
    final var ret = substitutions.get(source);
    return ret == null ? source : ret;
  }

  Library loadJarFile(File request, String className) throws LoadFailedException {
    final var actual = getSubstitution(request);

    // Anyway, here's the line for this new version:
    final var loader = new ZipClassLoader(actual);

    // load library class from loader
    Class<?> retClass;
    try {
      retClass = loader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new LoadFailedException(S.get("jarClassNotFoundError", className));
    }
    if (!(Library.class.isAssignableFrom(retClass))) {
      throw new LoadFailedException(S.get("jarClassNotLibraryError", className));
    }

    // instantiate library
    Library ret;
    try {
      ret = (Library) retClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new LoadFailedException(S.get("jarLibraryNotCreatedError", className));
    }
    return ret;
  }

  public Library loadJarLibrary(File file, String className) {
    final var actual = getSubstitution(file);
    return LibraryManager.instance.loadJarLibrary(this, actual, className);
  }

  //
  // Library methods
  //
  @Override
  public Library loadLibrary(String desc) {
    return LibraryManager.instance.loadLibrary(this, desc);
  }

  //
  // methods for LibraryManager
  //
  LogisimFile loadLogisimFile(File request) throws LoadFailedException {
    final var actual = getSubstitution(request);
    for (final var fileOpening : filesOpening) {
      if (fileOpening.equals(actual)) {
        throw new LoadFailedException(S.get("logisimCircularError", toProjectName(actual)));
      }
    }

    LogisimFile ret = null;
    filesOpening.push(actual);
    try {
      ret = LogisimFile.load(actual, this);
    } catch (IOException e) {
      throw new LoadFailedException(S.get("logisimLoadError", toProjectName(actual), e.toString()));
    } finally {
      filesOpening.pop();
    }
    if (ret != null) ret.setName(toProjectName(actual));
    return ret;
  }

  public Library loadLogisimLibrary(File file) {
    final var actual = getSubstitution(file);
    final var ret = LibraryManager.instance.loadLogisimLibrary(this, actual);
    if (ret != null) {
      LogisimFile retBase = (LogisimFile) ret.getBase();
      showMessages(retBase);
    }
    return ret;
  }

  public LogisimFile openLogisimFile(File file) throws LoadFailedException {
    try {
      final var ret = loadLogisimFile(file);
      if (ret != null) setMainFile(file);
      else throw new LoadFailedException("File could not be opened");
      showMessages(ret);
      return ret;
    } catch (LoaderException e) {
      throw new LoadFailedException(e.getMessage(), e.isShown());
    }
  }

  public LogisimFile openLogisimFile(File file, Map<File, File> substitutions)
      throws LoadFailedException {
    this.substitutions = substitutions;
    try {
      return openLogisimFile(file);
    } finally {
      this.substitutions = Collections.emptyMap();
    }
  }

  public LogisimFile openLogisimFile(InputStream reader) throws IOException {
    LogisimFile ret = null;
    try {
      ret = LogisimFile.load(reader, this);
    } catch (LoaderException e) {
      return null;
    }
    showMessages(ret);
    return ret;
  }

  public void reload(LoadedLibrary lib) {
    LibraryManager.instance.reload(this, lib);
  }

  public boolean export(LogisimFile file, String homeDirectory) {
    try {
      final var mainCircFile =
          LineBuffer.format(
              "{{1}}{{2}}{{3}}{{2}}{{4}}",
              homeDirectory, File.separator, LOGISIM_CIRCUIT_DIR, getMainFile().getName());
      final var libraryHome =
          String.format("%s%s%s", homeDirectory, File.separator, LOGISIM_LIBRARY_DIR);
      final var fwrite = new FileOutputStream(mainCircFile);
      file.write(fwrite, this, libraryHome);
    } catch (IOException e) {
      // TODO: give an error message to the user #1136
      System.err.println("Unable to export file");
      return false;
    }
    return true;
  }

  public boolean save(LogisimFile file, File dest) {
    file.interruptAutosaveThread(); // Notify autosave thread of save
    final var reference = LibraryManager.instance.findReference(file, dest);
    if (reference != null) {
      OptionPane.showMessageDialog(
          parent,
          S.get("fileCircularError", reference.getDisplayName()),
          S.get("fileSaveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }

    final var backup = determineBackupName(dest);
    final var backupCreated = (backup != null) && dest.renameTo(backup);

    FileOutputStream fwrite = null;
    final var oldFile = getMainFile();
    try {
      setMainFile(dest);
      fwrite = new FileOutputStream(dest);
      file.write(fwrite, this, dest, null);
      file.setName(toProjectName(dest));
      LibraryManager.instance.fileSaved(this, dest, oldFile, file);
    } catch (IOException e) {
      setMainFile(oldFile);
      if (backupCreated) recoverBackup(backup, dest);
      if (dest.exists() && dest.length() == 0) {
        // FIXME: delete can fail. Ensure we will not have snowball effect here!
        dest.delete();
      }
      OptionPane.showMessageDialog(
          parent,
          S.get("fileSaveError", e.toString()),
          S.get("fileSaveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return false;
    } finally {
      if (fwrite != null) {
        try {
          fwrite.close();
        } catch (IOException e) {
          if (backupCreated) recoverBackup(backup, dest);
          if (dest.exists() && dest.length() == 0) {
            // FIXME: delete can fail. Ensure we will not have snowball effect here!
            dest.delete();
          }
          OptionPane.showMessageDialog(
              parent,
              S.get("fileSaveCloseError", e.toString()),
              S.get("fileSaveErrorTitle"),
              OptionPane.ERROR_MESSAGE);
          return false;
        }
      }
    }

    if (!dest.exists() || dest.length() == 0) {
      if (backupCreated && backup.exists()) {
        recoverBackup(backup, dest);
      } else {
        // FIXME: delete can fail. Ensure we will not have snowball effect here!
        dest.delete();
      }
      OptionPane.showMessageDialog(
          parent,
          S.get("fileSaveZeroError"),
          S.get("fileSaveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }

    if (backupCreated && backup.exists()) {
      // FIXME: delete can fail. Ensure we will not have snowball effect here!
      backup.delete();
    }
    if (autosaveFile != null && autosaveFile.exists()) {
      deleteAutosave();
    }
    return true;
  }

  /**
   * Method to perform autosaves. Essentially does the same as save()
   * but without any failsafes, if saving fails it simply fails.
   *
   * @param file The file that should be autosaved
   *
   * @return True if writing was successful, else false;
   */
  public boolean autosave(LogisimFile file) {
    final var oldAutosave = autosaveFile;
    autosaveFile = determineAutosaveName(mainFile);
    if (autosaveFile == null) {
      return false;
    }
    FileOutputStream fwrite = null;
    try {
      fwrite = new FileOutputStream(autosaveFile);
      file.write(fwrite, this, autosaveFile, null);
      fwrite.close();
    } catch (IOException e) {
      return false;
    }
    if (oldAutosave != null && !oldAutosave.equals(autosaveFile)) {
      oldAutosave.delete();
    }
    return true;
  }

  /**
   * Method to delete the latest autosave.
   *
   * @return True if deletion was successful,
   *     false if the file is null or deletion failed
   */
  public boolean deleteAutosave() {
    return autosaveFile != null ? autosaveFile.delete() : false;
  }

  private void setMainFile(File value) {
    mainFile = value;
  }

  public void setParent(Component value) {
    parent = value;
  }

  @Override
  public void showError(String description) {
    if (!filesOpening.empty()) {
      final var top = filesOpening.peek();
      final var init = toProjectName(top) + ":";
      final var sep = description.contains("\n") ? "\n" : " ";
      description = init + sep + description;
    }

    if (description.contains("\n") || description.length() > 60) {
      var lines = 1;
      for (var pos = description.indexOf('\n');
          pos >= 0;
          pos = description.indexOf('\n', pos + 1)) {
        lines++;
      }
      lines = Math.max(4, Math.min(lines, 7));

      final var textArea = new JTextArea(lines, 60);
      textArea.setEditable(false);
      textArea.setText(description);
      textArea.setCaretPosition(0);

      final var scrollPane = new JScrollPane(textArea);
      scrollPane.setPreferredSize(new Dimension(350, 150));
      OptionPane.showMessageDialog(
          parent, scrollPane, S.get("fileErrorTitle"), OptionPane.ERROR_MESSAGE);
    } else {
      OptionPane.showMessageDialog(
          parent, description, S.get("fileErrorTitle"), OptionPane.ERROR_MESSAGE);
    }
  }

  private void showMessages(LogisimFile source) {
    if (source == null) return;
    var message = source.getMessage();
    while (message != null) {
      OptionPane.showMessageDialog(
          parent, message, S.get("fileMessageTitle"), OptionPane.INFORMATION_MESSAGE);
      message = source.getMessage();
    }
  }

  /**
   * Method to show an OptionDialog showing Options and returning the result.
   *
   * @param message The message to be shown in the dialog
   * @param title The title of the dialog
   * @param options The Options to be available for selection
   * @param initialSelection The index of the default selected Option
   *
   * @return The index of the selected Option
   */
  public int showOptions(String message, String title, String[] options, int initialSelection) {
    return OptionPane.showOptionDialog(
            parent,
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[initialSelection % options.length]);
  }

  private String toProjectName(File file) {
    final var ret = file.getName();

    return (ret.endsWith(LOGISIM_EXTENSION))
        ? ret.substring(0, ret.length() - LOGISIM_EXTENSION.length())
        : ret;
  }

  public String vhdlImportChooser(Component window) {
    final var chooser = createChooser();
    chooser.setFileFilter(Loader.VHDL_FILTER);
    chooser.setDialogTitle(com.cburch.logisim.vhdl.Strings.S.get("hdlOpenDialog"));
    final var returnVal = chooser.showOpenDialog(window);
    if (returnVal != JFileChooser.APPROVE_OPTION) return null;
    final var selected = chooser.getSelectedFile();
    if (selected == null) return null;
    try {
      return HdlFile.load(selected);
    } catch (IOException e) {
      OptionPane.showMessageDialog(
          window, e.getMessage(), S.get("hexOpenErrorTitle"), OptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  public void setAutosavePath(File f) {
    autosaveFile = new File(f.getParent(), f.getName());
  }
}
