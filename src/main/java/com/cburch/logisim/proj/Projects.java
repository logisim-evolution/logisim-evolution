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

package com.cburch.logisim.proj;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.PropertyChangeWeakSupport;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class Projects {

  private static class MyListener extends WindowAdapter {

    @Override
    public void windowActivated(WindowEvent event) {
      mostRecentFrame = (Frame) event.getSource();
    }

    @Override
    public void windowClosed(WindowEvent event) {
      final var frame = (Frame) event.getSource();
      final var proj = frame.getProject();

      if (frame == proj.getFrame()) {
        projectRemoved(proj, frame, this);
      }
      if (openProjects.isEmpty() && !MacCompatibility.isSwingUsingScreenMenuBar()) {
        ProjectActions.doQuit();
      }
    }

    @Override
    public void windowClosing(WindowEvent event) {
      final var frame = (Frame) event.getSource();
      if ((frame.getExtendedState() & Frame.ICONIFIED) == 0) {
        mostRecentFrame = frame;
        try {
          frameLocations.put(frame, frame.getLocationOnScreen());
        } catch (Exception ignored) {
        }
      }
    }

    @Override
    public void windowOpened(WindowEvent event) {
      final var frame = (Frame) event.getSource();
      final var proj = frame.getProject();

      if (frame == proj.getFrame() && !openProjects.contains(proj)) {
        openProjects.add(proj);
        propertySupport.firePropertyChange(PROJECT_LIST_PROPERTY, null, null);
      }
    }
  }

  //
  // PropertyChangeSource methods
  //
  public static void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  public static void addPropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  public static Project findProjectFor(File query) {
    for (final var proj : openProjects) {
      final var loader = proj.getLogisimFile().getLoader();
      if (loader == null) {
        continue;
      }
      final var f = loader.getMainFile();
      if (query.equals(f)) {
        return proj;
      }
    }
    return null;
  }

  public static Point getCenteredLoc(int width, int height) {
    int x = 0;
    int y = 0;

    if (getTopFrame() != null) {
      x = getTopFrame().getX() + getTopFrame().getWidth() / 2;
      x -= width / 2;
      y = getTopFrame().getY() + getTopFrame().getHeight() / 2;
      y -= height / 2;
    }
    return new Point(x, y);
  }

  public static Point getLocation(Window win) {
    Point ret = frameLocations.get(win);
    return ret == null ? null : (Point) ret.clone();
  }

  public static List<Project> getOpenProjects() {
    return Collections.unmodifiableList(openProjects);
  }

  public static Frame getTopFrame() {
    var ret = mostRecentFrame;
    if (ret == null) {
      Frame backup = null;
      for (final var proj : openProjects) {
        Frame frame = proj.getFrame();
        if (ret == null) {
          ret = frame;
        }
        if (ret.isVisible() && (ret.getExtendedState() & Frame.ICONIFIED) != 0) {
          backup = ret;
        }
      }
      if (ret == null) {
        ret = backup;
      }
    }
    return ret;
  }

  private static void projectRemoved(Project proj, Frame frame, MyListener listener) {
    frame.removeWindowListener(listener);
    openProjects.remove(proj);
    proj.getSimulator().shutDown();
    propertySupport.firePropertyChange(PROJECT_LIST_PROPERTY, null, null);
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  static void windowCreated(Project proj, Frame oldFrame, Frame frame) {
    if (oldFrame != null) {
      projectRemoved(proj, oldFrame, myListener);
    }

    if (frame == null) {
      return;
    }

    // locate the window
    Point lowest = null;
    for (final var p : openProjects) {
      final var f = p.getFrame();
      if (f == null) {
        continue;
      }
      final var loc = p.getFrame().getLocation();
      if (lowest == null || loc.y > lowest.y) {
        lowest = loc;
      }
    }
    if (lowest != null) {
      final var sz = frame.getToolkit().getScreenSize();
      int x = Math.min(lowest.x + 20, sz.width - 200);
      int y = Math.min(lowest.y + 20, sz.height - 200);
      if (x < 0) {
        x = 0;
      }
      if (y < 0) {
        y = 0;
      }
      frame.setLocation(x, y);
    }

    if (frame.isVisible() && !openProjects.contains(proj)) {
      openProjects.add(proj);
      propertySupport.firePropertyChange(PROJECT_LIST_PROPERTY, null, null);
    }
    frame.addWindowListener(myListener);
  }

  public static boolean windowNamed(String name) {
    for (final var proj : openProjects) {
      if (proj.getLogisimFile().getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  public static final String PROJECT_LIST_PROPERTY = "projectList";

  private static final WeakHashMap<Window, Point> frameLocations = new WeakHashMap<>();

  private static final MyListener myListener = new MyListener();

  private static final PropertyChangeWeakSupport propertySupport =
      new PropertyChangeWeakSupport(Projects.class);

  private static final ArrayList<Project> openProjects = new ArrayList<>();

  private static Frame mostRecentFrame = null;

  private Projects() {}
}
