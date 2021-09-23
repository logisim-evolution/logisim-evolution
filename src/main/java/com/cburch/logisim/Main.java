/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.MacCompatibility;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

public class Main {
  /**
   * Application entry point.
   *
   * @param args Optional arguments.
   */
  public static void main(String[] args) {
    System.setProperty("apple.awt.application.name", APP_NAME);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        FlatLightLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();

        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        UIManager.put("ToolTip.font",
                new FontUIResource("SansSerif", Font.BOLD, AppPreferences.getScaled(12)));
      }
    } catch (ClassNotFoundException
        | UnsupportedLookAndFeelException
        | IllegalAccessException
        | InstantiationException e) {
      e.printStackTrace();
    }

    final var startup = Startup.parseArgs(args);
    if (startup == null) System.exit(10);
    if (startup.shallQuit()) System.exit(0);

    try {
      startup.run();
    } catch (Throwable e) {
      final var strWriter = new StringWriter();
      final var printWriter = new PrintWriter(strWriter);
      e.printStackTrace(printWriter);
      OptionPane.showMessageDialog(null, strWriter.toString());
      System.exit(100);
    }
  }

  public static final String APP_NAME = BuildInfo.name;
  // @deprecated use BuildInfo instead
  public static final LogisimVersion VERSION = BuildInfo.version;
  public static final String APP_DISPLAY_NAME = APP_NAME + " v" + VERSION;
  public static final String APP_URL = "https://github.com/logisim-evolution/";

  public static final String JVM_VERSION = System.getProperty("java.vm.name") + " v" + System.getProperty("java.version");
  public static final String JVM_VENDOR = System.getProperty("java.vendor");

  public static boolean headless = false;
  public static final boolean RUNNING_ON_MAC = MacCompatibility.isRunningOnMac();

  // FloppyDisk unicode character: https://charbase.com/1f4be-unicode-floppy-disk
  public static final String DIRTY_MARKER = "\ud83d\udcbe";

  public static boolean hasGui() {
    return !headless;
  }
}
