/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Locale;
import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

class MenuHelp extends JMenu implements ActionListener {

  private static final long serialVersionUID = 1L;
  private final LogisimMenuBar menubar;
  private final JMenuItem tutorial = new JMenuItem();
  private final JMenuItem guide = new JMenuItem();
  private final JMenuItem library = new JMenuItem();
  private final JMenuItem about = new JMenuItem();
  private final JMenuItem www = new JMenuItem();
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
    www.addActionListener(this);

    add(tutorial);
    add(guide);
    add(library);
    if (browserIntegrationSupported()) {
      addSeparator();
      add(www);
    }
    if (!MacCompatibility.isAboutAutomaticallyPresent()) {
      addSeparator();
      add(about);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var src = e.getSource();
    if (guide.equals(src)) {
      showHelp("guide");
    } else if (tutorial.equals(src)) {
      showHelp("tutorial");
    } else if (library.equals(src)) {
      showHelp("libs");
    } else if (about.equals(src)) {
      About.showAboutDialog(menubar.getParentFrame());
    } else if (www.equals(src)) {
      openProjectWebsite();
    }
  }

  private void disableHelp() {
    guide.setEnabled(false);
    tutorial.setEnabled(false);
    library.setEnabled(false);
    www.setEnabled(false);
  }

  private void loadBroker() {
    var helpUrl = S.get("helpsetUrl");
    if (helpUrl == null) {
      helpUrl = "doc/doc_en.hs";
    }
    if (helpSet == null || helpFrame == null || !helpUrl.equals(helpSetUrl)) {
      final var loader = MenuHelp.class.getClassLoader();
      try {
        final var hsUrl = HelpSet.findHelpSet(loader, helpUrl);
        if (hsUrl == null) {
          disableHelp();
          OptionPane.showMessageDialog(menubar.getParentFrame(), S.get("helpNotFoundError"));
          return;
        }
        helpSetUrl = helpUrl;
        helpSet = new HelpSet(null, hsUrl);
        helpComponent = new JHelp(helpSet);
        if (helpFrame == null) {
          helpFrame = new LFrame.Dialog(null);
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
        OptionPane.showMessageDialog(menubar.getParentFrame(), S.get("helpUnavailableError"));
      }
    }
  }

  // On Linux this feature depends on Gnome, so may not be
  // working on all distros (i.e. KDE).
  private boolean browserIntegrationSupported() {
    return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
  }

  public void openProjectWebsite() {
    if (!browserIntegrationSupported()) return;
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(BuildInfo.url));
      }
    } catch (Exception e) {
      e.printStackTrace();
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
    www.setText(S.get("helpProjectWebsite"));
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
      OptionPane.showMessageDialog(menubar.getParentFrame(), S.get("helpDisplayError"));
    }
  }
}
