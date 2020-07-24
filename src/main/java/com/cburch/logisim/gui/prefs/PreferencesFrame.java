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

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.fpga.prefs.FPGAOptions;
import com.cburch.logisim.fpga.prefs.SoftwaresOptions;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class PreferencesFrame extends LFrame {

  private class MyListener implements LocaleListener {
    public void localeChanged() {
      setTitle(S.get("preferencesFrameTitle"));
      for (int i = 0; i < panels.length; i++) {
        tabbedPane.setTitleAt(i, panels[i].getTitle());
        tabbedPane.setToolTipTextAt(i, panels[i].getToolTipText());
        panels[i].localeChanged();
      }
    }
  }

  private static class WindowMenuManager extends WindowMenuItemManager implements LocaleListener {
    private PreferencesFrame window = null;

    WindowMenuManager() {
      super(S.get("preferencesFrameMenuItem"), true);
      LocaleManager.addLocaleListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      if (create) {
        if (window == null) {
          window = new PreferencesFrame();
          window.setLocationRelativeTo(parent);
          frameOpened(window);
        }
      }
      return window;
    }

    public void localeChanged() {
      setText(S.get("preferencesFrameMenuItem"));
    }
  }

  public static void initializeManager() {
    MENU_MANAGER = new WindowMenuManager();
  }

  public static void showPreferences() {
    JFrame frame = MENU_MANAGER.getJFrame(true, null);
    frame.setVisible(true);
  }
  
  public static void showFPGAPreferences() {
	PreferencesFrame frame = (PreferencesFrame) MENU_MANAGER.getJFrame(true, null);
	frame.setFpgaTab();
    frame.setVisible(true);
  }

  private static final long serialVersionUID = 1L;

  private static WindowMenuManager MENU_MANAGER = null;

  private MyListener myListener = new MyListener();
  private OptionsPanel[] panels;
  private JTabbedPane tabbedPane;
  private int FpgaTabIdx = -1; 

  private PreferencesFrame() {
    super(false,null);

    panels = new OptionsPanel[] {
          new TemplateOptions(this),
          new IntlOptions(this),
          new WindowOptions(this),
          new LayoutOptions(this),
          new SimOptions(this),
          new ExperimentalOptions(this),
          new SoftwaresOptions(this),
          new FPGAOptions(this),
        };
    tabbedPane = new JTabbedPane();
    int intlIndex = -1;
    for (int index = 0; index < panels.length; index++) {
      OptionsPanel panel = panels[index];
      tabbedPane.addTab(panel.getTitle(), null, panel, panel.getToolTipText());
      if (panel instanceof IntlOptions) intlIndex = index;
      if (panel instanceof FPGAOptions) FpgaTabIdx = index;
    }

    Container contents = getContentPane();
    contents.add(new JScrollPane(tabbedPane), BorderLayout.CENTER);

    if (intlIndex >= 0) tabbedPane.setSelectedIndex(intlIndex);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
  }
  
  public void setFpgaTab() {
	if (FpgaTabIdx < 0) return;
    tabbedPane.setSelectedIndex(FpgaTabIdx);
  }
}
