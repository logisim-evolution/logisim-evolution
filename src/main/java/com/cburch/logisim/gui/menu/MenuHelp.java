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

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;
import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

class MenuHelp extends JMenu implements ActionListener {

  private static final long serialVersionUID = 1L;
  private LogisimMenuBar menubar;
  private JMenuItem tutorial = new JMenuItem();
  private JMenuItem guide = new JMenuItem();
  private JMenuItem library = new JMenuItem();
  private JMenuItem about = new JMenuItem();
  private HelpSet helpSet;
  private String helpSetUrl = "";
  private JHelp helpComponent;
  private LFrame helpFrame;

  public MenuHelp(LogisimMenuBar menubar) {
    this.menubar = menubar;

    tutorial.addActionListener(this);
    guide.addActionListener(this);
    library.addActionListener(this);
    about.addActionListener(this);

    add(tutorial);
    add(guide);
    add(library);
    if (!MacCompatibility.isAboutAutomaticallyPresent()) {
      addSeparator();
      add(about);
    }
  }

  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == guide) {
      showHelp("guide");
    } else if (src == tutorial) {
      showHelp("tutorial");
    } else if (src == library) {
      showHelp("libs");
    } else if (src == about) {
      About.showAboutDialog(menubar.getParentWindow());
    }
  }

  private void disableHelp() {
    guide.setEnabled(false);
    tutorial.setEnabled(false);
    library.setEnabled(false);
  }

  private void loadBroker() {
    String helpUrl = S.get("helpsetUrl");
    if (helpUrl == null) {
      helpUrl = "doc/doc_en.hs";
    }
    if (helpSet == null || helpFrame == null || !helpUrl.equals(helpSetUrl)) {
      ClassLoader loader = MenuHelp.class.getClassLoader();
      try {
        URL hsURL = HelpSet.findHelpSet(loader, helpUrl);
        if (hsURL == null) {
          disableHelp();
          OptionPane.showMessageDialog(menubar.getParentWindow(), S.get("helpNotFoundError"));
          return;
        }
        helpSetUrl = helpUrl;
        helpSet = new HelpSet(null, hsURL);
        helpComponent = new JHelp(helpSet);
        if (helpFrame == null) {
          helpFrame = new LFrame(false,null);
          helpFrame.setTitle(S.get("helpWindowTitle"));
          helpFrame.getContentPane().add(helpComponent);
          helpFrame.setPreferredSize(
              new Dimension(
                  (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() >> 1,
                  (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() >> 1));
          helpFrame.pack();
        } else {
          helpFrame.getContentPane().removeAll();
          helpFrame.getContentPane().add(helpComponent);
          helpComponent.revalidate();
        }
      } catch (Exception e) {
        disableHelp();
        e.printStackTrace();
        OptionPane.showMessageDialog(menubar.getParentWindow(), S.get("helpUnavailableError"));
        return;
      }
    }
  }

  public void localeChanged() {
    this.setText(S.get("helpMenu"));
    if (helpFrame != null) {
      helpFrame.setTitle(S.get("helpWindowTitle"));
    }
    tutorial.setText(S.get("helpTutorialItem"));
    guide.setText(S.get("helpGuideItem"));
    library.setText(S.get("helpLibraryItem"));
    about.setText(S.get("helpAboutItem"));
    if (helpFrame != null) {
      helpFrame.setLocale(Locale.getDefault());
      loadBroker();
    }
  }

  private void showHelp(String target) {
    loadBroker();
    try {
      helpComponent.setCurrentID(target);
      helpFrame.toFront();
      helpFrame.setVisible(true);
    } catch (Exception e) {
      disableHelp();
      e.printStackTrace();
      OptionPane.showMessageDialog(menubar.getParentWindow(), S.get("helpDisplayError"));
    }
  }
}
