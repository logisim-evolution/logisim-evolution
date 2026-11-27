/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.PropertyChangeWeakSupport;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public final class Projects {

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
      mostRecentFrame = frame;
      try {
        frameLocations.put(frame, frame.getLocation());
      } catch (Exception ignored) {
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

  public static final String PROJECT_LIST_PROPERTY = "projectList";
  private static final WeakHashMap<Window, Point> frameLocations = new WeakHashMap<>();
  private static final MyListener myListener = new MyListener();
  private static final PropertyChangeWeakSupport propertySupport =
      new PropertyChangeWeakSupport(Projects.class);
  private static final ArrayList<Project> openProjects = new ArrayList<>();
  private static Frame mostRecentFrame = null;

  private Projects() {}

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
    var x = 0;
    var y = 0;

    final var topFrame = getTopFrame();
    if (topFrame != null) {
      x = topFrame.getX() + topFrame.getWidth() / 2;
      x -= width / 2;
      y = topFrame.getY() + topFrame.getHeight() / 2;
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
        final var frame = proj.getFrame();
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
    proj.getLogisimFile().stopAutosaveThread(true);
    propertySupport.firePropertyChange(PROJECT_LIST_PROPERTY, null, null);
  }

  public static void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  public static void removePropertyChangeListener(
      String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  static void windowCreated(Project proj, Frame frame, int offset) {
    if (frame == null) {
      return;
    }

    // Locate the lowest existing project window
    Rectangle lowest = null;

    for (final var p : openProjects) {
      final var f = p.getFrame();
      if (f == null || f == frame) continue;

      final var bounds = f.getBounds();
      if (lowest == null || bounds.y > lowest.y) {
        lowest = bounds;
      }
    }

    if (lowest != null) {
      // Position the new window slightly below and to the right, if the existing frame is not a blank project
      final int x = lowest.x + offset;
      final int y = lowest.y + offset;

      // Check whether the proposed window intersects any available screen
      final var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      final var bounds = new Rectangle(x, y, frame.getWidth(), frame.getHeight());

      boolean visible = false;

      for (final var device : ge.getScreenDevices()) {
        if (device.getDefaultConfiguration().getBounds().intersects(bounds)) {
          visible = true;
          break;
        }
      }

      // If completely off-screen, fall back to the primary monitor
      if (!visible) {
        final var screen = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        frame.setLocation(screen.x + offset, screen.y + offset);
      } else {
        frame.setLocation(x, y);
      }
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
}
