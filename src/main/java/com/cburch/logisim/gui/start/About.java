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

package com.cburch.logisim.gui.start;

import com.cburch.logisim.Main;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class About {
  static final int PADDING = 20;
  static final int PANEL_WIDTH = 600;
  static final int LOGO_HEIGHT = 200;
  static final int SCROLLER_HEIGHT = 200;

  private static final String LOGO_IMG = "resources/logisim/img/logisim-evolution-logo.png";

  private About() {}

  public static AboutPanel getImagePanel() {
    return new AboutPanel();
  }

  public static void showAboutDialog(JFrame owner) {
    AboutPanel imgPanel = new AboutPanel(true);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(imgPanel);
    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

    OptionPane.showMessageDialog(owner, panel, Main.APP_DISPLAY_NAME, OptionPane.PLAIN_MESSAGE);
  }

  private static class AboutPanel extends JPanel implements AncestorListener {
    private static final long serialVersionUID = 1L;
    private AboutCredits credits = null;
    private PanelThread thread = null;

    public AboutPanel() {
      this(false);
    }

    public AboutPanel(boolean includeCredits) {
      setLayout(null);

      final var prefWidth = PANEL_WIDTH + 2 * PADDING;
      var prefHeight = LOGO_HEIGHT + 2 * PADDING;
      if (includeCredits) {
        prefHeight += SCROLLER_HEIGHT;
      }

      setPreferredSize(new Dimension(prefWidth, prefHeight));
      setBackground(Color.WHITE);
      addAncestorListener(this);

      final var logo = new JLabel(new ImageIcon(getClass().getClassLoader().getResource(LOGO_IMG)));
      logo.setBounds(0, 20, prefWidth, LOGO_HEIGHT);
      add(logo);

      if (includeCredits) {
        credits = new AboutCredits(PANEL_WIDTH, SCROLLER_HEIGHT);
        credits.setBounds(0, prefHeight / 2, prefWidth, SCROLLER_HEIGHT);
        add(credits);
      }
    }

    @Override
    public void ancestorAdded(AncestorEvent arg0) {
      if (credits != null) {
        if (thread == null) {
          thread = new PanelThread(this);
        }
        thread.start();
      }
    }

    @Override
    public void ancestorMoved(AncestorEvent arg0) {}

    @Override
    public void ancestorRemoved(AncestorEvent arg0) {
      if (thread != null) {
        thread.running = false;
      }
    }
  } // AboutPanel

  private static class PanelThread extends UniquelyNamedThread {
    private final AboutPanel panel;
    private boolean running = true;

    PanelThread(AboutPanel panel) {
      super("About-PanelThread");
      this.panel = panel;
    }

    @Override
    public void run() {
      while (running) {
        panel.repaint();
        try {
          Thread.sleep(20);
        } catch (InterruptedException ignored) {
          // Let's pretend nothing happened.
        }
      }
    }
  } // PanelThread
}
