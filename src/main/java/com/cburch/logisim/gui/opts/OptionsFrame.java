/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.TableLayout;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class OptionsFrame extends LFrame.Dialog {
  private static final long serialVersionUID = 1L;
  private final MyListener myListener = new MyListener();
  private final WindowMenuManager windowManager = new WindowMenuManager();
  private final OptionsPanel[] panels;
  private final JTabbedPane tabbedPane;

  public OptionsFrame(Project project) {
    super(project);
    project.addLibraryListener(myListener);
    project.addProjectListener(
        event -> {
          final var action = event.getAction();
          if (action == ProjectEvent.ACTION_SET_STATE) {
            computeTitle();
          }
        });
    panels =
        new OptionsPanel[] {
          new SimulateOptions(this),
          new ToolbarOptions(this),
          new MouseOptions(this),
          new RevertPanel(this)
        };
    tabbedPane = new JTabbedPane();
    for (final var panel : panels) {
      tabbedPane.addTab(panel.getTitle(), null, panel, panel.getToolTipText());
    }

    final var contents = getContentPane();
    tabbedPane.setPreferredSize(
        new Dimension(AppPreferences.getScaled(450), AppPreferences.getScaled(300)));
    contents.add(tabbedPane, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
    setLocationRelativeTo(project.getFrame());
  }

  private void computeTitle() {
    final var file = project.getLogisimFile();
    final var name = (file == null) ? "???" : file.getName();
    final var title = S.get("optionsFrameTitle", name);
    setTitle(title);
  }

  public Options getOptions() {
    return project.getLogisimFile().getOptions();
  }

  OptionsPanel[] getPrefPanels() {
    return panels;
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
    private final MyListener myListener = new MyListener();
    private final JButton revert = new JButton();

    public RevertPanel(OptionsFrame window) {
      super(window);
      setLayout(new TableLayout(1));
      final var buttonPanel = new JPanel();
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

    private class MyListener implements ActionListener {
      @Override
      public void actionPerformed(ActionEvent event) {
        final var src = event.getSource();
        if (src == revert) {
          getProject().doAction(LogisimFileActions.revertDefaults());
        }
      }
    }
  }

  private class MyListener implements LibraryListener, LocaleListener {
    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        computeTitle();
        windowManager.localeChanged();
      }
    }

    @Override
    public void localeChanged() {
      computeTitle();
      for (var i = 0; i < panels.length; i++) {
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

    @Override
    public void localeChanged() {
      final var title = project.getLogisimFile().getDisplayName();
      setText(S.get("optionsFrameMenuItem", title));
    }
  }
}
