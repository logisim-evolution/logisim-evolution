/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.help.JHelp;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.InfoIcon;
import com.cburch.logisim.gui.icons.QuestionIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.WindowManagers;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.util.MacCompatibility;

public class GuiInterface implements AWTEventListener {

  private static ArrayList<File> filesToOpen;
  private static ArrayList<File> filesToPrint = new ArrayList<File>();
  private static boolean initialized = false;

  private String gateShape = null;
  private Point windowSize = null;
  private Point windowLocation = null;
  private File templFile = null;
  private boolean templEmpty = false;
  private boolean templPlain = false;
  private boolean showSplash = true;
  private boolean clearPreferences = false;
  private HashMap<File, File> substitutions;

  public GuiInterface(Startup startup) {
    assert startup.ui == Startup.UI.GUI;

    gateShape         = startup.gateShape;
    windowSize        = startup.windowSize;
    windowLocation    = startup.windowLocation;
    filesToOpen       = startup.filesToOpen;
    filesToPrint      = startup.filesToPrint;
    templFile         = startup.templFile;
    templEmpty        = startup.templEmpty;
    templPlain        = startup.templPlain;
    showSplash        = startup.showSplash;
    clearPreferences  = startup.clearPreferences;
    substitutions     = startup.substitutions;
  }

  public int run(Loader templLoader) {
    // Set up the Look&Feel to match the platform
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    // Initialize graphics acceleration if appropriate
    AppPreferences.handleGraphicsAcceleration();
  
    if (clearPreferences) AppPreferences.clear();

    if (gateShape != null) AppPreferences.GATE_SHAPE.set(gateShape);

    if (windowSize != null) {
      AppPreferences.WINDOW_WIDTH.set(windowSize.x);
      AppPreferences.WINDOW_HEIGHT.set(windowSize.y);
    }
    if (windowLocation != null) {
      AppPreferences.WINDOW_LOCATION.set(windowLocation.x + "," + windowLocation.y);
    }

    MacOsAdapter.addListeners();

    // kick off the progress monitor
    // (The values used for progress values are based on a single run where
    // I loaded a large file.)
    SplashScreen monitor = null;
    if (showSplash) {
      try {
        monitor = new SplashScreen();
        monitor.setVisible(true);
      } catch (Exception t) {
        monitor = null;
      }
    }

    Toolkit.getDefaultToolkit()
        .addAWTEventListener(this, AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
    // pre-load the two basic component libraries, just so that the time
    // taken is shown separately in the progress bar.
    if (monitor != null) {
      monitor.setProgress(SplashScreen.LIBRARIES);
    }
    templLoader.setParent(monitor);
    final var count = templLoader.getBuiltin().getLibrary(BaseLibrary._ID).getTools().size()
                    + templLoader.getBuiltin().getLibrary(GatesLibrary._ID).getTools().size();
    assert(count >= 0);

    // load in template
    if (monitor != null) {
      monitor.setProgress(SplashScreen.TEMPLATE_OPEN);
    }
    if (templFile != null) {
      AppPreferences.setTemplateFile(templFile);
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_CUSTOM);
    } else if (templEmpty) {
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_EMPTY);
    } else if (templPlain) {
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_PLAIN);
    }

    // now that the splash screen is almost gone, we do some last-minute
    // interface initialization
    if (monitor != null) {
      monitor.setProgress(SplashScreen.GUI_INIT);
    }
    WindowManagers.initialize();
    if (MacCompatibility.isSwingUsingScreenMenuBar()) {
        MacCompatibility.setFramelessJMenuBar(new LogisimMenuBar(null, null, null, null));
    } else {
        new LogisimMenuBar(null, null, null, null);
        // most of the time occupied here will be in loading menus, which
        // will occur eventually anyway; we might as well do it when the
        // monitor says we are
    }

    // Make ENTER and SPACE have the same effect for focused buttons.
    UIManager.getDefaults()
        .put(
            "Button.focusInputMap",
            new UIDefaults.LazyInputMap(
                new Object[] {
                    "ENTER", "pressed",
                    "released ENTER", "released",
                    "SPACE", "pressed",
                    "released SPACE", "released"
                }));

    // if user has double-clicked a file to open, we'll
    // use that as the file to open now.
    initialized = true;

    // load file
    if (filesToOpen.isEmpty()) {
      final var proj = ProjectActions.doNew(monitor);
      proj.setStartupScreen(true);
      if (monitor != null) {
        monitor.close();
        monitor = null;
      }
    } else {
      var numOpened = 0;
      var first = true;
      for (final var fileToOpen : filesToOpen) {
        try {
          ProjectActions.doOpen(monitor, fileToOpen, substitutions);
          numOpened++;
        } catch (LoadFailedException ex) {
          // FIXME: report error opening: fileToOpen.getName()
        }
        if (first) {
          first = false;
          if (monitor != null) {
            monitor.close();
            monitor = null;
          }
        }
      }
      if (numOpened == 0) return -1;
    }

    for (final var fileToPrint : filesToPrint) {
      doPrint(fileToPrint);
    }

    return 0;
  }

  private boolean hasIcon(Component comp) {
    var result = false;
    if (comp instanceof JOptionPane pane) {
      for (final var comp1 : pane.getComponents()) result |= hasIcon(comp1);
    } else if (comp instanceof JPanel panel) {
      for (final var comp1 : panel.getComponents()) result |= hasIcon(comp1);
    } else if (comp instanceof JLabel label) {
      return label.getIcon() != null;
    }
    return result;
  }
  
  static void doOpen(File file) {
    if (initialized) {
      ProjectActions.doOpen(null, null, file);
    } else {
      filesToOpen.add(file);
    }
  }

  static void doPrint(File file) {
    if (initialized) {
      final var toPrint = ProjectActions.doOpen(null, null, file);
      Print.doPrint(toPrint);
      toPrint.getFrame().dispose();
    } else {
      filesToPrint.add(file);
    }
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (event instanceof ContainerEvent containerEvent) {
      if (containerEvent.getID() == ContainerEvent.COMPONENT_ADDED) {
        final var container = containerEvent.getChild();
        if ((container instanceof JButton)
            || (container instanceof JCheckBox)
            || (container instanceof JComboBox)
            || (container instanceof JToolTip)
            || (container instanceof JLabel)
            || (container instanceof JFrame)
            || (container instanceof JMenuItem)
            || (container instanceof JRadioButton)
            || (container instanceof JRadioButtonMenuItem)
            || (container instanceof JProgressBar)
            || (container instanceof JSpinner)
            || (container instanceof JTabbedPane)
            || (container instanceof JTextField)
            || (container instanceof JTextArea)
            || (container instanceof JHelp)
            || (container instanceof JFileChooser)
            || ((container instanceof JScrollPane) && (!(container instanceof CanvasPane)))
            || (container instanceof JCheckBoxMenuItem)) {
          AppPreferences.setScaledFonts(((JComponent) container).getComponents());
          try {
            container.setFont(AppPreferences.getScaledFont(containerEvent.getChild().getFont()));
            container.revalidate();
            container.repaint();
          } catch (Exception ignored) {
          }
        }
        if (container instanceof final JOptionPane pane) {
          if (hasIcon(pane)) {
            switch (pane.getMessageType()) {
              case OptionPane.ERROR_MESSAGE -> pane.setIcon(new ErrorIcon());
              case OptionPane.QUESTION_MESSAGE -> pane.setIcon(new QuestionIcon());
              case OptionPane.INFORMATION_MESSAGE -> pane.setIcon(new InfoIcon());
              case OptionPane.WARNING_MESSAGE -> pane.setIcon(new WarningIcon());
            }
          }
        }
      }
    }
  }
}
