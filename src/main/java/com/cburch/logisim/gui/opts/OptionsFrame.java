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

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.TableLayout;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class OptionsFrame extends LFrame {
  private class MyListener implements LibraryListener, LocaleListener {
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        setTitle(computeTitle(file));
        windowManager.localeChanged();
      }
    }

    public void localeChanged() {
      setTitle(computeTitle(file));
      for (int i = 0; i < panels.length; i++) {
        tabbedPane.setTitleAt(i, panels[i].getTitle());
        tabbedPane.setToolTipTextAt(i, panels[i].getToolTipText());
        panels[i].localeChanged();
      }
      windowManager.localeChanged();
    }
  }

  private class WindowMenuManager extends WindowMenuItemManager implements LocaleListener {
    WindowMenuManager() {
      super(S.get("optionsFrameMenuItem"), false);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return OptionsFrame.this;
    }

    public void localeChanged() {
      String title = project.getLogisimFile().getDisplayName();
      setText(StringUtil.format(S.get("optionsFrameMenuItem"), title));
    }
  }

  private static String computeTitle(LogisimFile file) {
    String name = file == null ? "???" : file.getName();
    return StringUtil.format(S.get("optionsFrameTitle"), name);
  }

  private static final long serialVersionUID = 1L;
  private Project project;
  private LogisimFile file;
  private MyListener myListener = new MyListener();

  private WindowMenuManager windowManager = new WindowMenuManager();
  private OptionsPanel[] panels;
  private JTabbedPane tabbedPane;

  public OptionsFrame(Project project) {
    super(false,project);
    this.project = project;
    this.file = project.getLogisimFile();
    file.addLibraryListener(myListener);
    panels = new OptionsPanel[] { new SimulateOptions(this), 
        new ToolbarOptions(this), new MouseOptions(this),
        new RevertPanel(this)};
    tabbedPane = new JTabbedPane();
    for (int index = 0; index < panels.length; index++) {
      OptionsPanel panel = panels[index];
      tabbedPane.addTab(panel.getTitle(), null, panel, panel.getToolTipText());
    }

    Container contents = getContentPane();
    tabbedPane.setPreferredSize(new Dimension(AppPreferences.getScaled(450), AppPreferences.getScaled(300)));
    contents.add(tabbedPane, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
    setLocationRelativeTo(project.getFrame());
  }

  public LogisimFile getLogisimFile() {
    return file;
  }

  public Options getOptions() {
    return file.getOptions();
  }

  OptionsPanel[] getPrefPanels() {
    return panels;
  }

  public Project getProject() {
    return project;
  }

  @Override
  public void setVisible(boolean value) {
    if (value) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }
  
  static class RevertPanel extends OptionsPanel {
    private static final long serialVersionUID = 1L;

    private class MyListener implements ActionListener {
      public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        if (src == revert) {
          getProject().doAction(LogisimFileActions.revertDefaults());
        }
      }
    }

    private MyListener myListener = new MyListener();
    private JButton revert = new JButton();

    public RevertPanel(OptionsFrame window) {
      super(window);
      setLayout(new TableLayout(1));
      JPanel buttonPanel = new JPanel();
      buttonPanel.add(revert);
      revert.addActionListener(myListener);
      add(buttonPanel);
	}

    @Override
    public String getHelpText() {
       return S.get("revertHelp");
	}

    @Override
    public String getTitle() {
      return S.get("revertTitle");
    }

    @Override
    public void localeChanged() {
      revert.setText(S.get("revertButton"));
    }
  }
}
