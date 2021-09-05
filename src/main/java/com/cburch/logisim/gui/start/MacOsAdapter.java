/*
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

package com.cburch.logisim.gui.start;

import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Desktop;
import java.io.File;

class MacOsAdapter {

  private static boolean listenersAdded = false;

  /**
   * addListeners() adds listeners for external events for the Mac. It allows the program to work like
   * a normal Mac application with double-click opening of the .circ documents. Note that the .jar
   * file must be wrapped in a .app for this to be meaningful on the Mac. This code requires Java 9
   * or higher.
   */
  public static void addListeners() {
    if (listenersAdded || !MacCompatibility.isRunningOnMac()) {
      return;
    }
    if (Desktop.isDesktopSupported()) {
      listenersAdded = true;
      final var dt = Desktop.getDesktop();
      try {
        dt.setAboutHandler(
            e -> About.showAboutDialog(null));
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
        dt.setPreferencesHandler(
            e -> PreferencesFrame.showPreferences());
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
