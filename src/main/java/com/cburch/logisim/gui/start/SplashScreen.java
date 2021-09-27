/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.generated.BuildInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplashScreen extends JWindow {

  public static final int LIBRARIES = 0;
  public static final int TEMPLATE_CREATE = 1;
  public static final int TEMPLATE_OPEN = 2;
  public static final int TEMPLATE_LOAD = 3;
  public static final int TEMPLATE_CLOSE = 4;
  public static final int GUI_INIT = 5;
  public static final int FILE_CREATE = 6;
  public static final int FILE_LOAD = 7;
  public static final int PROJECT_CREATE = 8;
  public static final int FRAME_CREATE = 9;
  static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);
  private static final long serialVersionUID = 1L;
  private static final int PROGRESS_MAX = 3568;
  private static final boolean PRINT_TIMES = false;
  Marker[] markers =
      new Marker[] {
        new Marker(377, S.get("progressLibraries")),
        new Marker(990, S.get("progressTemplateCreate")),
        new Marker(1002, S.get("progressTemplateOpen")),
        new Marker(1002, S.get("progressTemplateLoad")),
        new Marker(1470, S.get("progressTemplateClose")),
        new Marker(1478, S.get("progressGuiInitialize")),
        new Marker(2114, S.get("progressFileCreate")),
        new Marker(2114, S.get("progressFileLoad")),
        new Marker(2383, S.get("progressProjectCreate")),
        new Marker(2519, S.get("progressFrameCreate")),
      };
  boolean inClose = false; // for avoiding mutual recursion
  final JProgressBar progress = new JProgressBar(0, PROGRESS_MAX);
  final long startTime = System.currentTimeMillis();
  public SplashScreen() {
    setName(BuildInfo.displayName);
    JPanel imagePanel = About.getImagePanel();
    imagePanel.setBorder(null);

    progress.setStringPainted(true);

    JPanel contents = new JPanel(new BorderLayout());
    contents.add(imagePanel, BorderLayout.NORTH);
    contents.add(progress, BorderLayout.CENTER);
    contents.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

    Color bg = imagePanel.getBackground();
    contents.setBackground(bg);
    setBackground(bg);
    setContentPane(contents);
  }

  public void close() {
    if (inClose) return;
    inClose = true;
    setVisible(false);
    inClose = false;
    if (PRINT_TIMES) {
      logger.info("{} closed", System.currentTimeMillis() - startTime);
    }
    markers = null;
  }

  public void setProgress(int markerId) {
    final Marker marker = markers == null ? null : markers[markerId];
    if (marker != null) {
      SwingUtilities.invokeLater(
          () -> {
            progress.setString(marker.message);
            progress.setValue(marker.count);
          });
      if (PRINT_TIMES) {
        logger.info("{} {}", System.currentTimeMillis() - startTime, marker.message);
      }
    } else {
      if (PRINT_TIMES) {
        logger.info("{} ??", System.currentTimeMillis() - startTime);
      }
    }
  }

  @Override
  public void setVisible(boolean value) {
    if (value) {
      pack();
      final var dim = getToolkit().getScreenSize();
      final var x = (int) (dim.getWidth() - getWidth()) / 2;
      final var y = (int) (dim.getHeight() - getHeight()) / 2;
      setLocation(x, y);
    }
    super.setVisible(value);
  }

  private static class Marker {
    final int count;
    final String message;

    Marker(int count, String message) {
      this.count = count;
      this.message = message;
    }
  }
}
