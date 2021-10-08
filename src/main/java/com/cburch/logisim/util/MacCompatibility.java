/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.awt.Desktop;
import javax.swing.JMenuBar;

public final class MacCompatibility {

  private static final boolean runningOnMac = System.getProperty("os.name").toLowerCase().contains("mac");
  private static boolean usingScreenMenuBar = runningOnMac;

  private MacCompatibility() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

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
