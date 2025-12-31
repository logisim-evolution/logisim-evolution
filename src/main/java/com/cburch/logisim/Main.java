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
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
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
    // Initialize and print debug/log level information
    com.cburch.logisim.util.Debug.printStartupMessage();
    
    System.setProperty("apple.awt.application.name", BuildInfo.name);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        FlatLightLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();

        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        UIManager.put(
            "ToolTip.font",
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

  public static boolean headless = false;

  // FloppyDisk unicode character: https://charbase.com/1f4be-unicode-floppy-disk
  public static final String DIRTY_MARKER = "\ud83d\udcbe";

  public static boolean hasGui() {
    return !headless;
  }
}
