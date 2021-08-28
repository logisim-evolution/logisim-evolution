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

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.Main;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.WindowClosable;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class LFrame extends JFrame implements WindowClosable {
  private static final long serialVersionUID = 1L;
  private static final String PATH = "resources/logisim/img/logisim-icon-";
  private static final int[] SIZES = {16, 32, 48, 64, 128};
  private static final int DEFAULT_SIZE = 48;
  private static List<Image> ICONS = null;
  private static Image DEFAULT_ICON = null;
  //A main window holds a circuit, always has menubar with Close, Save, etc.
  public static final int MAIN_WINDOW = 1;
  // A sub-window is either standalone or is associated with a project, always
  // has a menubar but without Close, Save, etc. If associated with a project,
  // the window will close when the project closes.
  public static final int SUB_WINDOW = 2;
  // A dialog is either standalone or is associated with a project, and doesn't
  // have a menubar except on MacOS where it is mostly empty. If associated with
  // a project, the window will close when the project closes.
  public static final int DIALOG = 3;

  protected final LogisimMenuBar menubar;
  protected final Project project;
  protected final int type;

  public static class MainWindow extends LFrame {
    private static final long serialVersionUID = 1L;

    public MainWindow(Project p) {
      super(MAIN_WINDOW, p, true);
      if (p == null)
        throw new IllegalArgumentException("project is null");
    }
  }

  public static class SubWindow extends LFrame {
    private static final long serialVersionUID = 1L;

    public SubWindow(Project p) { // may be null
      super(SUB_WINDOW, p, false);
    }
  }

  public static class SubWindowWithSimulation extends LFrame {
    private static final long serialVersionUID = 1L;

    public SubWindowWithSimulation(Project p) { // may be null
      super(SUB_WINDOW, p, true);
    }
  }

  public static class Dialog extends LFrame {
    private static final long serialVersionUID = 1L;

    public Dialog(Project p) { // may be null
      super(DIALOG, p, false);
    }
  }

  private LFrame(int t, Project p, boolean enableSim) {
    project = p;
    type = t;
    LFrame.attachIcon(this);
    if (type == MAIN_WINDOW) {
      menubar = new LogisimMenuBar(this, p, p, p);
      setJMenuBar(menubar);
    } else if (type == SUB_WINDOW || Main.RUNNING_ON_MAC) {
      // use null project so there will be no Close, Save, etc.
      menubar = new LogisimMenuBar(this, null, p, enableSim ? p : null);
      setJMenuBar(menubar);
    } else {
      menubar = null;
    }
    if (type != MAIN_WINDOW && project != null) {
      project.getFrame().addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          LFrame.this.dispose();
        }
      });
    }
  }

  public static void attachIcon(Window frame) {
    if (ICONS == null) {
      List<Image> loadedIcons = new ArrayList<>();
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
    } catch (Exception ignored) {
    }

    if (!success && frame instanceof JFrame && DEFAULT_ICON != null) {
      frame.setIconImage(DEFAULT_ICON);
    }
  }

  @Override
  public void requestClose() {
    WindowEvent closing = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    processWindowEvent(closing);
  }

  public Project getProject() {
    return project;
  }

  public LogisimMenuBar getLogisimMenuBar() {
    return menubar;
  }
}
