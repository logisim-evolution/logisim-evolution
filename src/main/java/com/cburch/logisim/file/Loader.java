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

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.ZipClassLoader;
import com.cburch.logisim.vhdl.file.HdlFile;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.swing.JFileChooser;
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

  private static File determineBackupName(File base) {
    File dir = base.getParentFile();
    String name = base.getName();
    if (name.endsWith(LOGISIM_EXTENSION)) {
      name = name.substring(0, name.length() - LOGISIM_EXTENSION.length());
    }
    for (int i = 1; i <= 20; i++) {
      String ext = i == 1 ? ".bak" : (".bak" + i);
      File candidate = new File(dir, name + ext);
      if (!candidate.exists()) return candidate;
    }
    return null;
  }

  private static void recoverBackup(File backup, File dest) {
    if (backup != null && backup.exists()) {
      if (dest.exists()) dest.delete();
      backup.renameTo(dest);
    }
  }

  public static final String LOGISIM_EXTENSION = ".circ";

  public static final FileFilter LOGISIM_FILTER = new LogisimFileFilter();

  public static final FileFilter JAR_FILTER = new JarFileFilter();
  public static final FileFilter TXT_FILTER = new TxtFileFilter();
  public static final FileFilter TCL_FILTER = new TclFileFilter();
  public static final FileFilter VHDL_FILTER = new VhdlFileFilter();

  // fixed
  private Component parent;
  private Builtin builtin = new Builtin();
  // to be cleared with each new file
  private File mainFile = null;

  private Stack<File> filesOpening = new Stack<File>();

  private Map<File, File> substitutions = new HashMap<File, File>();

  public Loader(Component parent) {
    this.parent = parent;
    clear();
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
    File ref;
    if (!filesOpening.empty()) {
      ref = filesOpening.peek();
    } else {
      ref = mainFile;
    }
    return ref == null ? null : ref.getParentFile();
  }

  public String getDescriptor(Library lib) {
    return LibraryManager.instance.getDescriptor(this, lib);
  }

  //
  // helper methods
  //
  File getFileFor(String name, FileFilter filter) {
    // Determine the actual file name.
    File file = new File(name);
    if (!file.isAbsolute()) {
      File currentDirectory = getCurrentDirectory();
      if (currentDirectory != null) file = new File(currentDirectory, name);
    }
    while (!file.canRead()) {
      // It doesn't exist. Figure it out from the user.
      OptionPane.showMessageDialog(
          parent, StringUtil.format(S.get("fileLibraryMissingError"), file.getName()));
      JFileChooser chooser = createChooser();
      chooser.setFileFilter(filter);
      chooser.setDialogTitle(StringUtil.format(S.get("fileLibraryMissingTitle"), file.getName()));
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
    File ret = substitutions.get(source);
    return ret == null ? source : ret;
  }

  Library loadJarFile(File request, String className) throws LoadFailedException {
    File actual = getSubstitution(request);

    // Anyway, here's the line for this new version:
    ZipClassLoader loader = new ZipClassLoader(actual);

    // load library class from loader
    Class<?> retClass;
    try {
      retClass = loader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new LoadFailedException(StringUtil.format(S.get("jarClassNotFoundError"), className));
    }
    if (!(Library.class.isAssignableFrom(retClass))) {
      throw new LoadFailedException(StringUtil.format(S.get("jarClassNotLibraryError"), className));
    }

    // instantiate library
    Library ret;
    try {
      ret = (Library) retClass.newInstance();
    } catch (Exception e) {
      throw new LoadFailedException(
          StringUtil.format(S.get("jarLibraryNotCreatedError"), className));
    }
    return ret;
  }

  public Library loadJarLibrary(File file, String className) {
    File actual = getSubstitution(file);
    return LibraryManager.instance.loadJarLibrary(this, actual, className);
  }

  //
  // Library methods
  //
  public Library loadLibrary(String desc) {
    return LibraryManager.instance.loadLibrary(this, desc);
  }

  //
  // methods for LibraryManager
  //
  LogisimFile loadLogisimFile(File request) throws LoadFailedException {
    File actual = getSubstitution(request);
    for (File fileOpening : filesOpening) {
      if (fileOpening.equals(actual)) {
        throw new LoadFailedException(
            StringUtil.format(S.get("logisimCircularError"), toProjectName(actual)));
      }
    }

    LogisimFile ret = null;
    filesOpening.push(actual);
    try {
      ret = LogisimFile.load(actual, this);
    } catch (IOException e) {
      throw new LoadFailedException(
          StringUtil.format(S.get("logisimLoadError"), toProjectName(actual), e.toString()));
    } finally {
      filesOpening.pop();
    }
    if (ret != null) ret.setName(toProjectName(actual));
    return ret;
  }

  public Library loadLogisimLibrary(File file) {
    File actual = getSubstitution(file);
    LoadedLibrary ret = LibraryManager.instance.loadLogisimLibrary(this, actual);
    if (ret != null) {
      LogisimFile retBase = (LogisimFile) ret.getBase();
      showMessages(retBase);
    }
    return ret;
  }

  public LogisimFile openLogisimFile(File file) throws LoadFailedException {
    try {
      LogisimFile ret = loadLogisimFile(file);
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

  public LogisimFile openLogisimFile(InputStream reader) throws LoadFailedException, IOException {
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

  public boolean save(LogisimFile file, File dest) {
    Library reference = LibraryManager.instance.findReference(file, dest);
    if (reference != null) {
      OptionPane.showMessageDialog(
          parent,
          StringUtil.format(S.get("fileCircularError"), reference.getDisplayName()),
          S.get("fileSaveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return false;
    }

    File backup = determineBackupName(dest);
    boolean backupCreated = backup != null && dest.renameTo(backup);

    FileOutputStream fwrite = null;
    try {
      fwrite = new FileOutputStream(dest);
      file.write(fwrite, this, dest);
      file.setName(toProjectName(dest));

      File oldFile = getMainFile();
      setMainFile(dest);
      LibraryManager.instance.fileSaved(this, dest, oldFile, file);
    } catch (IOException e) {
      if (backupCreated) recoverBackup(backup, dest);
      if (dest.exists() && dest.length() == 0) dest.delete();
      OptionPane.showMessageDialog(
          parent,
          StringUtil.format(S.get("fileSaveError"), e.toString()),
          S.get("fileSaveErrorTitle"),
          OptionPane.ERROR_MESSAGE);
      return false;
    } finally {
      if (fwrite != null) {
        try {
          fwrite.close();
        } catch (IOException e) {
          if (backupCreated) recoverBackup(backup, dest);
          if (dest.exists() && dest.length() == 0) dest.delete();
          OptionPane.showMessageDialog(
              parent,
              StringUtil.format(S.get("fileSaveCloseError"), e.toString()),
              S.get("fileSaveErrorTitle"),
              OptionPane.ERROR_MESSAGE);
          return false;
        }
      }
    }

    if (!dest.exists() || dest.length() == 0) {
      if (backupCreated && backup != null && backup.exists()) {
        recoverBackup(backup, dest);
      } else {
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
      backup.delete();
    }
    return true;
  }

  private void setMainFile(File value) {
    mainFile = value;
  }

  public void setParent(Component value) {
    parent = value;
  }

  public void showError(String description) {
    if (!filesOpening.empty()) {
      File top = filesOpening.peek();
      String init = toProjectName(top) + ":";
      if (description.contains("\n")) {
        description = init + "\n" + description;
      } else {
        description = init + " " + description;
      }
    }

    if (description.contains("\n") || description.length() > 60) {
      int lines = 1;
      for (int pos = description.indexOf('\n');
          pos >= 0;
          pos = description.indexOf('\n', pos + 1)) {
        lines++;
      }
      lines = Math.max(4, Math.min(lines, 7));

      JTextArea textArea = new JTextArea(lines, 60);
      textArea.setEditable(false);
      textArea.setText(description);
      textArea.setCaretPosition(0);

      JScrollPane scrollPane = new JScrollPane(textArea);
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
    String message = source.getMessage();
    while (message != null) {
      OptionPane.showMessageDialog(
          parent, message, S.get("fileMessageTitle"), OptionPane.INFORMATION_MESSAGE);
      message = source.getMessage();
    }
  }

  private String toProjectName(File file) {
    String ret = file.getName();
    if (ret.endsWith(LOGISIM_EXTENSION)) {
      return ret.substring(0, ret.length() - LOGISIM_EXTENSION.length());
    } else {
      return ret;
    }
  }

  public String vhdlImportChooser(Component window) {
    JFileChooser chooser = createChooser();
    chooser.setFileFilter(Loader.VHDL_FILTER);
    chooser.setDialogTitle(com.cburch.logisim.vhdl.Strings.S.get("hdlOpenDialog"));
    int returnVal = chooser.showOpenDialog(window);
    if (returnVal != JFileChooser.APPROVE_OPTION) return null;
    File selected = chooser.getSelectedFile();
    if (selected == null) return null;
    try {
      String vhdl = HdlFile.load(selected);
      return vhdl;
    } catch (IOException e) {
      OptionPane.showMessageDialog(
          window, e.getMessage(), S.get("hexOpenErrorTitle"), OptionPane.ERROR_MESSAGE);
      return null;
    }
  }
}
