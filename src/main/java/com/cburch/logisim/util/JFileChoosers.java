/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.proj.Projects;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JFileChooser;

public final class JFileChoosers {

  public enum DirectoryScope {
    CIRCUITS,
    IMAGE_EXPORT,
    SOC_SOFTWARE
  }

  private static final String[] PROP_NAMES = {
    null, "user.home", "user.dir", "java.home", "java.io.tmpdir"
  };

  private static final Map<DirectoryScope, String> currentDirectories =
      Collections.synchronizedMap(new EnumMap<>(DirectoryScope.class));

  private JFileChoosers() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  private static PrefMonitor<String> getPrefMonitor(DirectoryScope scope) {
    return switch (scope) {
      case IMAGE_EXPORT -> AppPreferences.IMAGE_EXPORT_DIRECTORY;
      case SOC_SOFTWARE -> AppPreferences.SOC_DIRECTORY;
      default -> AppPreferences.DIALOG_DIRECTORY;
    };
  }

  /*
   * A user reported that JFileChooser's constructor sometimes resulted in
   * IOExceptions when Logisim is installed under a system administrator
   * account and then is attempted to run as a regular user. This class is an
   * attempt to be a bit more robust about which directory the JFileChooser
   * opens up under. (23 Feb 2010)
   */
  private static class LogisimFileChooser extends JFileChooser {
    private static final long serialVersionUID = 1L;
    private final DirectoryScope scope;

    LogisimFileChooser(DirectoryScope scope) {
      super();
      this.scope = (scope != null) ? scope : DirectoryScope.CIRCUITS;
    }

    LogisimFileChooser(File initSelected, DirectoryScope scope) {
      super(initSelected);
      this.scope = (scope != null) ? scope : DirectoryScope.CIRCUITS;
    }

    @Override
    public File getSelectedFile() {
      final var dir = getCurrentDirectory();
      if (dir != null) {
        JFileChoosers.setCurrentDirectory(scope, dir.toString());
      }
      return super.getSelectedFile();
    }

    private boolean shouldUseMacNativeDialog() {
      if (!MacCompatibility.isRunningOnMac()) return false;
      // Fall back to Swing when multiple format-selection filters
      final var choosable = getChoosableFileFilters();
      final boolean hasAcceptAll = isAcceptAllFileFilterUsed();
      return hasAcceptAll || choosable.length <= 1;
    }

    @Override
    public int showOpenDialog(java.awt.Component parent) {
      if (shouldUseMacNativeDialog()) {
        final var file = showMacFileDialog(parent, java.awt.FileDialog.LOAD);
        if (file != null) {
          setSelectedFile(file);
          return APPROVE_OPTION;
        }
        return CANCEL_OPTION;
      }
      return super.showOpenDialog(parent);
    }

    @Override
    public int showSaveDialog(java.awt.Component parent) {
      if (shouldUseMacNativeDialog()) {
        final int mode = (getFileSelectionMode() == DIRECTORIES_ONLY)
            ? java.awt.FileDialog.LOAD
            : java.awt.FileDialog.SAVE;
        final var file = showMacFileDialog(parent, mode);
        if (file != null) {
          setSelectedFile(file);
          return APPROVE_OPTION;
        }
        return CANCEL_OPTION;
      }
      return super.showSaveDialog(parent);
    }

    @Override
    public int showDialog(java.awt.Component parent, String approveButtonText) {
      if (shouldUseMacNativeDialog()) {
        final int mode = (getDialogType() == SAVE_DIALOG
            && getFileSelectionMode() != DIRECTORIES_ONLY)
            ? java.awt.FileDialog.SAVE
            : java.awt.FileDialog.LOAD;
        final var file = showMacFileDialog(parent, mode);
        if (file != null) {
          setSelectedFile(file);
          return APPROVE_OPTION;
        }
        return CANCEL_OPTION;
      }
      return super.showDialog(parent, approveButtonText);
    }

    private File showMacFileDialog(java.awt.Component parent, int mode) {
      java.awt.Window parentWindow = null;
      if (parent instanceof java.awt.Window win) {
        parentWindow = win;
      } else if (parent != null) {
        parentWindow = javax.swing.SwingUtilities.getWindowAncestor(parent);
      }
      if (parentWindow == null) {
        parentWindow = Projects.getTopFrame();
      }

      var title = getDialogTitle();
      if (title == null) {
        final var key = (mode == java.awt.FileDialog.LOAD)
            ? "FileChooser.openDialogTitleText"
            : "FileChooser.saveDialogTitleText";
        title = javax.swing.UIManager.getString(key);
      }
      final java.awt.FileDialog fileDialog;
      if (parentWindow instanceof java.awt.Frame frame) {
        fileDialog = new java.awt.FileDialog(frame, title, mode);
      } else if (parentWindow instanceof java.awt.Dialog dialog) {
        fileDialog = new java.awt.FileDialog(dialog, title, mode);
      } else {
        fileDialog = new java.awt.FileDialog((java.awt.Frame) null, title, mode);
      }

      final var curDir = getCurrentDirectory();
      if (curDir != null) {
        fileDialog.setDirectory(curDir.getAbsolutePath());
      }
      final var selFile = getSelectedFile();
      if (selFile != null) {
        fileDialog.setFile(selFile.getName());
      }

      final var filter = getFileFilter();
      if (filter != null && filter != getAcceptAllFileFilter()) {
        fileDialog.setFilenameFilter((dir, name) -> {
          final var f = new File(dir, name);
          return f.isDirectory() || filter.accept(f);
        });
      }

      final boolean dirMode = (getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY);
      if (dirMode) {
        System.setProperty("apple.awt.fileDialogForDirectories", "true");
      }
      try {
        fileDialog.setVisible(true);
      } finally {
        if (dirMode) {
          System.clearProperty("apple.awt.fileDialogForDirectories");
        }
      }

      if (fileDialog.getFile() != null) {
        final var selected = new File(fileDialog.getDirectory(), fileDialog.getFile());
        final var folder = dirMode ? selected : selected.getParentFile();
        if (folder != null) {
          setCurrentDirectory(folder);
          JFileChoosers.setCurrentDirectory(scope, folder.toString());
        }
        return selected;
      } else if (dirMode && fileDialog.getDirectory() != null) {
        final var selected = new File(fileDialog.getDirectory());
        setCurrentDirectory(selected);
        JFileChoosers.setCurrentDirectory(scope, selected.toString());
        return selected;
      }
      return null;
    }
  }

  public static JFileChooser create() {
    return create(DirectoryScope.CIRCUITS);
  }

  public static JFileChooser create(DirectoryScope scope) {
    if (scope == null) {
      scope = DirectoryScope.CIRCUITS;
    }
    RuntimeException first = null;
    for (final var prop : PROP_NAMES) {
      try {
        String dirname;
        if (prop == null) {
          dirname = getCurrentDirectory(scope);
        } else {
          dirname = System.getProperty(prop);
        }
        if (dirname == null || "".equals(dirname)) {
          return new LogisimFileChooser(scope);
        } else {
          final var dir = new File(dirname);
          if (dir.canRead()) {
            return new LogisimFileChooser(dir, scope);
          }
        }
      } catch (RuntimeException t) {
        if (first == null) first = t;
        final var u = t.getCause();
        if (!(u instanceof IOException)) throw t;
      }
    }
    throw first;
  }

  public static JFileChooser createFor(DirectoryScope scope) {
    return create(scope);
  }

  public static JFileChooser createAt(File openDirectory) {
    return createAt(openDirectory, DirectoryScope.CIRCUITS);
  }

  public static JFileChooser createAt(File openDirectory, DirectoryScope scope) {
    if (openDirectory == null) {
      return create(scope);
    } else {
      try {
        return new LogisimFileChooser(openDirectory, scope);
      } catch (RuntimeException t) {
        if (t.getCause() instanceof IOException) {
          try {
            return create(scope);
          } catch (RuntimeException ignored) {
          }
        }
        throw t;
      }
    }
  }

  public static JFileChooser createSelected(File selected) {
    return createSelected(selected, DirectoryScope.CIRCUITS);
  }

  public static JFileChooser createSelected(File selected, DirectoryScope scope) {
    if (selected == null) {
      return create(scope);
    } else if (selected.isDirectory()) {
      return createAt(selected, scope);
    } else {
      final var ret = createAt(selected.getParentFile(), scope);
      ret.setSelectedFile(selected);
      return ret;
    }
  }

  public static String getCurrentDirectory() {
    return getCurrentDirectory(DirectoryScope.CIRCUITS);
  }

  public static String getCurrentDirectory(DirectoryScope scope) {
    if (scope == null) {
      scope = DirectoryScope.CIRCUITS;
    }
    var dir = currentDirectories.get(scope);
    if (dir != null && !dir.isEmpty()) {
      return dir;
    }
    dir = getPrefMonitor(scope).get();
    if (dir != null && !dir.isEmpty()) {
      return dir;
    }
    if (scope != DirectoryScope.CIRCUITS) {
      return getCurrentDirectory(DirectoryScope.CIRCUITS);
    }
    return "";
  }

  public static void setCurrentDirectory(String dirname) {
    setCurrentDirectory(DirectoryScope.CIRCUITS, dirname);
  }

  public static void setCurrentDirectory(DirectoryScope scope, String dirname) {
    if (scope == null) {
      scope = DirectoryScope.CIRCUITS;
    }
    if (dirname != null && !dirname.isEmpty()) {
      currentDirectories.put(scope, dirname);
      getPrefMonitor(scope).set(dirname);
    }
  }

  public static void setCurrentDirectory(DirectoryScope scope, File dir) {
    if (dir != null) {
      setCurrentDirectory(scope, dir.getAbsolutePath());
    }
  }
}
