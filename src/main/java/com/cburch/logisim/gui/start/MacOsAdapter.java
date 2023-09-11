/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Desktop;
import java.io.File;

class MacOsAdapter {

  private static boolean listenersAdded = false;

  /**
   * addListeners() adds listeners for external events for the Mac. It allows the program to work
   * like a normal Mac application with double-click opening of the .circ documents. Note that the
   * .jar file must be wrapped in a .app for this to be meaningful on the Mac. This code requires
   * Java 9 or higher.
   */
  public static void addListeners() {
    if (listenersAdded || !MacCompatibility.isRunningOnMac()) {
      return;
    }
    if (Desktop.isDesktopSupported()) {
      listenersAdded = true;
      final var dt = Desktop.getDesktop();
      try {
        dt.setAboutHandler(e -> About.showAboutDialog(null));
      } catch (Exception ignored) {
        // can fail, but just ignore it.
      }
      try {
        dt.setQuitHandler(
            (e, response) -> {
              ProjectActions.doQuit();
              response.performQuit();
            });
      } catch (Exception ignored) {
        // can fail, but just ignore it.
      }
      try {
        dt.setPreferencesHandler(e -> PreferencesFrame.showPreferences());
      } catch (Exception ignored) {
        // can fail, but just ignore it.
      }
      try {
        dt.setPrintFileHandler(
            e -> {
              for (File f : e.getFiles()) {
                Startup.doPrint(f);
              }
            });
      } catch (Exception ignored) {
        // can fail, but just ignore it.
      }
      try {
        dt.setOpenFileHandler(
            e -> {
              for (File f : e.getFiles()) {
                Startup.doOpen(f);
              }
            });
      } catch (Exception ignored) {
        // can fail, but just ignore it.
      }
    }
  }
}
