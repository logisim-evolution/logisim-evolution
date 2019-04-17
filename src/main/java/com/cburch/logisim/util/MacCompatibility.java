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

import java.awt.Desktop;
import javax.swing.JMenuBar;

public class MacCompatibility {

  private static boolean runningOnMac =
      System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
  private static boolean usingScreenMenuBar = runningOnMac;

  public static boolean isRunningOnMac() {
    return runningOnMac;
  }

  public static boolean isAboutAutomaticallyPresent() {
    return runningOnMac;
  }

  public static boolean isPreferencesAutomaticallyPresent() {
    return runningOnMac;
  }

  public static boolean isQuitAutomaticallyPresent() {
    return runningOnMac;
  }

  public static boolean isSwingUsingScreenMenuBar() {
    return usingScreenMenuBar;
  }

  public static void setFramelessJMenuBar(JMenuBar menubar) {
    try {
      // DHH This method allows the app to run without a frame on the Mac. The menu will still show.
      if (runningOnMac) {
        Desktop.getDesktop().setDefaultMenuBar(menubar);
      }
    } catch (Exception t) {
      usingScreenMenuBar = false;
    }
  }
}
