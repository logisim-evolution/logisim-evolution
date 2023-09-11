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
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;

public final class JFileChoosers {

  private static final String[] PROP_NAMES = {
    null, "user.home", "user.dir", "java.home", "java.io.tmpdir"
  };

  private static String currentDirectory = "";

  private JFileChoosers() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
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

    LogisimFileChooser() {
      super();
    }

    LogisimFileChooser(File initSelected) {
      super(initSelected);
    }

    @Override
    public File getSelectedFile() {
      final var dir = getCurrentDirectory();
      if (dir != null) {
        JFileChoosers.currentDirectory = dir.toString();
      }
      return super.getSelectedFile();
    }
  }

  public static JFileChooser create() {
    RuntimeException first = null;
    for (final var prop : PROP_NAMES) {
      try {
        String dirname;
        if (prop == null) {
          dirname = currentDirectory;
          if ("".equals(dirname)) {
            dirname = AppPreferences.DIALOG_DIRECTORY.get();
          }
        } else {
          dirname = System.getProperty(prop);
        }
        if ("".equals(dirname)) {
          return new LogisimFileChooser();
        } else {
          final var dir = new File(dirname);
          if (dir.canRead()) {
            return new LogisimFileChooser(dir);
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

  public static JFileChooser createAt(File openDirectory) {
    if (openDirectory == null) {
      return create();
    } else {
      try {
        return new LogisimFileChooser(openDirectory);
      } catch (RuntimeException t) {
        if (t.getCause() instanceof IOException) {
          try {
            return create();
          } catch (RuntimeException ignored) {
          }
        }
        throw t;
      }
    }
  }

  public static JFileChooser createSelected(File selected) {
    if (selected == null) {
      return create();
    } else if (selected.isDirectory()) {
      return createAt(selected);
    } else {
      final var ret = createAt(selected.getParentFile());
      ret.setSelectedFile(selected);
      return ret;
    }
  }

  public static String getCurrentDirectory() {
    return currentDirectory;
  }
}
