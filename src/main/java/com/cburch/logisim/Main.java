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

package com.cburch.logisim;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.prefs.AppPreferences;

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
  public static void main(String[] args) throws Exception {
    System.setProperty("apple.awt.application.name", "Logisim-evolution");
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        UIManager.put("ToolTip.font", new FontUIResource("SansSerif", Font.BOLD, AppPreferences.getScaled(12))); 
      }
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }
    Startup startup = Startup.parseArgs(args);
    if (startup == null) {
      System.exit(0);
    } else {
      // If the auto-updater actually performed an update, then quit the
      // program, otherwise continue with the execution
      if (!startup.autoUpdate()) {
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
  }

  static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static final LogisimVersion VERSION =
      LogisimVersion.get(3, 3, 6, LogisimVersion.FINAL_REVISION);

  public static final String VERSION_NAME = VERSION.toString();
  public static final int COPYRIGHT_YEAR = 2020;

  public static boolean ANALYZE = true;
  public static boolean headless = false;
  public static boolean hasGui() { return !headless; }

  /** URL for the automatic updater */
  public static final String UPDATE_URL =
      "https://raw.githubusercontent.com/reds-heig/logisim-evolution/develop/logisim_evolution_version.xml";
}
