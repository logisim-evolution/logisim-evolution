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

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.WindowClosable;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class LFrame extends JFrame implements WindowClosable {
  public static void attachIcon(Window frame) {
    if (ICONS == null) {
      List<Image> loadedIcons = new ArrayList<Image>();
      ClassLoader loader = LFrame.class.getClassLoader();
      for (int size : SIZES) {
        URL url = loader.getResource(PATH + size + ".png");
        if (url != null) {
          ImageIcon icon = new ImageIcon(url);
          loadedIcons.add(icon.getImage());
          if (size == DEFAULT_SIZE) {
            DEFAULT_ICON = icon.getImage();
          }
        }
      }
      ICONS = loadedIcons;
    }

    boolean success = false;
    try {
      if (ICONS != null && !ICONS.isEmpty()) {
        Method set = frame.getClass().getMethod("setIconImages", List.class);
        set.invoke(frame, ICONS);
        success = true;
      }
    } catch (Exception e) {
    }

    if (!success && frame instanceof JFrame && DEFAULT_ICON != null) {
      ((JFrame) frame).setIconImage(DEFAULT_ICON);
    }
  }

  private static final long serialVersionUID = 1L;
  private static final String PATH = "resources/logisim/img/logisim-icon-";
  private static final int[] SIZES = {16, 20, 24, 48, 64, 128};
  private static List<Image> ICONS = null;
  private static final int DEFAULT_SIZE = 48;

  private static Image DEFAULT_ICON = null;
  
  protected final LogisimMenuBar menubar;

  public LFrame(boolean toplevel, Project proj) {
    LFrame.attachIcon(this);
    if (toplevel || MacCompatibility.isRunningOnMac()) {
    	menubar = new LogisimMenuBar(this,proj);
    	setJMenuBar(menubar);
    } else {
    	menubar = null;
    }
  }

  @Override
  public void requestClose() {
    WindowEvent closing = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    processWindowEvent(closing);
  }
}
