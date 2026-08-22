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
import com.cburch.logisim.gui.search.OmniSearchDialog;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.providers.MenuSearchProvider;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JMenuItem;

class MenuHelp extends Menu implements ActionListener {

  private static final long serialVersionUID = 1L;
  private static final String ENGLISH_HELP_SET = "doc/doc_en.hs";
  private final LogisimMenuBar menubar;
  private final JMenuItem search = new JMenuItem();
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

    search.addActionListener(this);
    tutorial.addActionListener(this);
    guide.addActionListener(this);
    library.addActionListener(this);
    about.addActionListener(this);
    www.addActionListener(this);

    // Keep the entry point out of its own results; finding "Find Action" is never what was wanted.
    search.putClientProperty(MenuSearchProvider.EXCLUDE_PROPERTY, Boolean.TRUE);
    search.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_SEARCH).getWithMask(0));

    /* add myself to hotkey sync */
    AppPreferences.gui_sync_objects.add(this);

    add(search);
    addSeparator();
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
    if (search.equals(src)) {
      showSearch();
    } else if (guide.equals(src)) {
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

  @Override
  public void hotkeyUpdate() {
    search.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_SEARCH).getWithMask(0));
  }

  @Override
  protected void computeEnabled() {
    setEnabled(true);
  }

  /** Opens the omni-search over the actions this window offers. */
  private void showSearch() {
    OmniSearchDialog.showDialog(
        new SearchContext(menubar.getParentFrame(), menubar, menubar.getBaseProject()));
  }

  private void disableHelp() {
    guide.setEnabled(false);
    tutorial.setEnabled(false);
    library.setEnabled(false);
    www.setEnabled(false);
  }

  private void loadBroker() {
    final var resolved = resolveHelpSet(MenuHelp.class.getClassLoader(), S.get("helpsetUrl"));
    if (resolved == null) {
      disableHelp();
      OptionPane.showMessageDialog(menubar.getParentFrame(), S.get("helpNotFoundError"));
      return;
    }
    if (helpSet == null || helpFrame == null || !resolved.path().equals(helpSetUrl)) {
      try {
        helpSetUrl = resolved.path();
        helpSet = new HelpSet(null, resolved.url());
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

  static ResolvedHelpSet resolveHelpSet(ClassLoader loader, String localizedHelpSet) {
    final var requested =
        localizedHelpSet == null || localizedHelpSet.isBlank()
            ? ENGLISH_HELP_SET
            : localizedHelpSet;
    var url = HelpSet.findHelpSet(loader, requested);
    if (url != null) {
      return new ResolvedHelpSet(requested, url);
    }
    if (!ENGLISH_HELP_SET.equals(requested)) {
      url = HelpSet.findHelpSet(loader, ENGLISH_HELP_SET);
      if (url != null) {
        return new ResolvedHelpSet(ENGLISH_HELP_SET, url);
      }
    }
    return null;
  }

  record ResolvedHelpSet(String path, URL url) {}

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
    search.setText(S.get("helpSearchItem"));
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
