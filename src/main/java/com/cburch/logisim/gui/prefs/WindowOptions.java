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

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.gui.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.TableLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class WindowOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final PrefBoolean[] checks;
  private final PrefOptionList toolbarPlacement;
  private final ZoomSlider ZoomValue;
  private final JLabel lookfeelLabel;
  private final JLabel ZoomLabel;
  private final JLabel Importanta;
  private final JTextArea Importantb;
  private final JPanel previewContainer;
  private final JComboBox<String> LookAndFeel;
  private final LookAndFeelInfo[] LFInfos;
  private JPanel previewPanel;
  private int Index = 0;

  private final ColorChooserButton CanvasBgColor;
  private final JLabel CanvasBgColorTitle;
  private final ColorChooserButton GridBgColor;
  private final JLabel GridBgColorTitle;
  private final ColorChooserButton GridDotColor;
  private final JLabel GridDotColorTitle;
  private final ColorChooserButton GridZoomedDotColor;
  private final JLabel GridZoomedDotColorTitle;
  
  private final JButton gridColorsResetButton;
  private final JButton but;
  protected final String cmdResetWindowLayout = "reset-window-layout";
  protected final String cmdResetGridColors = "reset-grid-colors";

  public WindowOptions(PreferencesFrame window) {
    super(window);

    SettingsChangeListener listener = new SettingsChangeListener();

    checks =
        new PrefBoolean[] {
          new PrefBoolean(AppPreferences.SHOW_TICK_RATE, S.getter("windowTickRate")),
        };

    toolbarPlacement =
        new PrefOptionList(
            AppPreferences.TOOLBAR_PLACEMENT,
            S.getter("windowToolbarLocation"),
            new PrefOption[] {
              new PrefOption(Direction.NORTH.toString(), Direction.NORTH.getDisplayGetter()),
              new PrefOption(Direction.SOUTH.toString(), Direction.SOUTH.getDisplayGetter()),
              new PrefOption(Direction.EAST.toString(), Direction.EAST.getDisplayGetter()),
              new PrefOption(Direction.WEST.toString(), Direction.WEST.getDisplayGetter()),
              new PrefOption(AppPreferences.TOOLBAR_HIDDEN, S.getter("windowToolbarHidden"))
            });

    JPanel panel = new JPanel(new TableLayout(2));
    panel.add(toolbarPlacement.getJLabel());
    panel.add(toolbarPlacement.getJComboBox());


    CanvasBgColorTitle = new JLabel(S.get("windowCanvasBgColor"));
    CanvasBgColor = new ColorChooserButton(window, AppPreferences.CANVAS_BG_COLOR);
    panel.add(CanvasBgColorTitle);
    panel.add(CanvasBgColor);
    GridBgColorTitle = new JLabel(S.get("windowGridBgColor"));
    GridBgColor = new ColorChooserButton(window, AppPreferences.GRID_BG_COLOR);
    panel.add(GridBgColorTitle);
    panel.add(GridBgColor);
    GridDotColorTitle = new JLabel(S.get("windowGridDotColor"));
    GridDotColor = new ColorChooserButton(window, AppPreferences.GRID_DOT_COLOR);
    panel.add(GridDotColorTitle);
    panel.add(GridDotColor);
    GridZoomedDotColorTitle = new JLabel(S.get("windowGridZoomedDotColor"));
    GridZoomedDotColor = new ColorChooserButton(window, AppPreferences.GRID_ZOOMED_DOT_COLOR);
    panel.add(GridZoomedDotColorTitle);
    panel.add(GridZoomedDotColor);

    gridColorsResetButton = new JButton();
    gridColorsResetButton.addActionListener(listener);
    gridColorsResetButton.setActionCommand(cmdResetGridColors);
    gridColorsResetButton.setText(S.get("windowGridColorsReset"));
    panel.add(new JLabel());
    panel.add(gridColorsResetButton);

    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));

    Importanta = new JLabel(S.get("windowToolbarPleaserestart"));
    Importanta.setFont(Importanta.getFont().deriveFont(Font.ITALIC));
    panel.add(Importanta);

    Importantb = new JTextArea(S.get("windowToolbarImportant"));
    Importantb.setFont(Importantb.getFont().deriveFont(Font.ITALIC));
    panel.add(Importantb);

    ZoomLabel = new JLabel(S.get("windowToolbarZoomfactor"));
    ZoomValue =
        new ZoomSlider(
            JSlider.HORIZONTAL, 100, 300, (int) (AppPreferences.SCALE_FACTOR.get() * 100));

    panel.add(ZoomLabel);
    panel.add(ZoomValue);
    ZoomValue.addChangeListener(listener);

    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));

    int index = 0;
    LookAndFeel = new JComboBox<>();
    LookAndFeel.setSize(50, 20);

    LFInfos = UIManager.getInstalledLookAndFeels();
    for (LookAndFeelInfo info : LFInfos) {
      LookAndFeel.insertItemAt(info.getName(), index);
      if (info.getClassName().equals(AppPreferences.LookAndFeel.get())) {
        LookAndFeel.setSelectedIndex(index);
        Index = index;
      }
      index++;
    }
    lookfeelLabel = new JLabel(S.get("windowToolbarLookandfeel"));
    panel.add(lookfeelLabel);
    panel.add(LookAndFeel);
    LookAndFeel.addActionListener(listener);

    JLabel previewLabel = new JLabel(S.get("windowToolbarPreview"));
    panel.add(previewLabel);
    previewContainer = new JPanel();
    panel.add(previewContainer);
    initThemePreviewer();

    setLayout(new TableLayout(1));
    but = new JButton();
    but.addActionListener(listener);
    but.setActionCommand("reset");
    but.setText(S.get("windowToolbarReset"));
    add(but);
    for (PrefBoolean check : checks) {
      add(check);
    }
    add(panel);
  }

  private void initThemePreviewer() {
    if (previewPanel != null) previewContainer.remove(previewPanel);
    javax.swing.LookAndFeel previousLF = UIManager.getLookAndFeel();
    try {
      UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
      previewPanel = new JPanel();
      previewPanel.add(new JButton("Preview"));
      previewPanel.add(new JCheckBox("Preview"));
      previewPanel.add(new JRadioButton("Preview"));
      previewPanel.add(new JComboBox<>(new String[]{"Preview 1", "Preview 2"}));
      previewContainer.add(previewPanel);
      UIManager.setLookAndFeel(previousLF);
    } catch (IllegalAccessException
        | UnsupportedLookAndFeelException
        | InstantiationException
        | ClassNotFoundException ignored) {
    }
    previewContainer.repaint();
    previewContainer.revalidate();
  }

  @Override
  public String getHelpText() {
    return S.get("windowHelp");
  }

  @Override
  public String getTitle() {
    return S.get("windowTitle");
  }

  @Override
  public void localeChanged() {
    for (PrefBoolean check : checks) {
      check.localeChanged();
    }
    toolbarPlacement.localeChanged();
    ZoomLabel.setText(S.get("windowToolbarZoomfactor"));
    lookfeelLabel.setText(S.get("windowToolbarLookandfeel"));
    Importanta.setText(S.get("windowToolbarPleaserestart"));
    Importantb.setText(S.get("windowToolbarImportant"));
    gridColorsResetButton.setText(S.get("windowGridColorsReset"));
    but.setText(S.get("windowToolbarReset"));
  }

  private class SettingsChangeListener implements ChangeListener, ActionListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      JSlider source = (JSlider) e.getSource();
      if (!source.getValueIsAdjusting()) {
        int value = source.getValue();
        AppPreferences.SCALE_FACTOR.set((double) value / 100.0);
        List<Project> nowOpen = Projects.getOpenProjects();
        for (Project proj : nowOpen) {
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(LookAndFeel)) {
        if (LookAndFeel.getSelectedIndex() != Index) {
          Index = LookAndFeel.getSelectedIndex();
          AppPreferences.LookAndFeel.set(LFInfos[Index].getClassName());
          initThemePreviewer();
        }
      } else if (e.getActionCommand().equals(cmdResetWindowLayout)) {
        AppPreferences.resetWindow();
        List<Project> nowOpen = Projects.getOpenProjects();
        for (Project proj : nowOpen) {
          proj.getFrame().resetLayout();
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      } else if (e.getActionCommand().equals(cmdResetGridColors)) {
        //        AppPreferences.resetWindow();
        List<Project> nowOpen = Projects.getOpenProjects();
        AppPreferences.setDefaultGridColors();
        for (Project proj : nowOpen) {
          proj.getFrame().repaint();
        }
      }
    }
  }
}
