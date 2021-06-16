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

package com.cburch.logisim;

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
import java.io.Writer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  public static void main(String[] args) {
    System.setProperty("apple.awt.application.name", APP_NAME);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        FlatLightLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();

        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        UIManager.put("ToolTip.font", new FontUIResource("SansSerif", Font.BOLD, AppPreferences.getScaled(12)));
      }
    } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
    Startup startup = Startup.parseArgs(args);
    if (startup == null) {
      System.exit(0);
    } else {
      try {
        startup.run();
      } catch (Throwable e) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        OptionPane.showMessageDialog(null, result.toString());
        System.exit(-1);
      }
    }
  }

  static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static final String APP_NAME = "Logisim-evolution";
  public static final LogisimVersion VERSION = new LogisimVersion(3, 5, 0);
  public static final int COPYRIGHT_YEAR = 2021;
  public static final String APP_DISPLAY_NAME = APP_NAME + " v" + VERSION;
  public static final String APP_URL = "https://github.com/logisim-evolution/";

  public static boolean ANALYZE = true;
  public static boolean headless = false;
  public static final boolean MacOS = MacCompatibility.isRunningOnMac();
  public static boolean hasGui() { return !headless; }

}
