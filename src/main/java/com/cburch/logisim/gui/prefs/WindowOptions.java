/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.gui.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.TableLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
  private final PrefOptionList canvasPlacement;
  private final ZoomSlider zoomValue;
  private final JLabel lookfeelLabel;
  private final JLabel zoomLabel;
  private final JLabel importantA;
  private final JTextArea importantB;
  private final JPanel previewContainer;
  private final JComboBox<String> lookAndFeel;
  private final LookAndFeelInfo[] lookAndFeelInfos;
  private JPanel previewPanel;
  private int index = 0;

  private final ColorChooserButton canvasBgColor;
  private final JLabel canvasBgColorTitle;
  private final ColorChooserButton gridBgColor;
  private final JLabel gridBgColorTitle;
  private final ColorChooserButton gridDotColor;
  private final JLabel gridDotColorTitle;
  private final ColorChooserButton gridZoomedDotColor;
  private final JLabel gridZoomedDotColorTitle;

  protected final String cmdResetWindowLayout = "reset-window-layout";
  protected final String cmdResetGridColors = "reset-grid-colors";

  public WindowOptions(PreferencesFrame window) {
    super(window);

    final var listener = new SettingsChangeListener();
    final var panel = new JPanel(new TableLayout(2));

    checks =
        new PrefBoolean[] {
          new PrefBoolean(AppPreferences.SHOW_TICK_RATE, S.getter("windowTickRate")),
        };

    canvasPlacement =
        new PrefOptionList(
            AppPreferences.CANVAS_PLACEMENT,
            S.getter("windowCanvasLocation"),
            new PrefOption[] {
              new PrefOption(Direction.EAST.toString(), Direction.EAST.getDisplayGetter()),
              new PrefOption(Direction.WEST.toString(), Direction.WEST.getDisplayGetter())
            });

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

    panel.add(canvasPlacement.getJLabel());
    panel.add(canvasPlacement.getJComboBox());

    panel.add(toolbarPlacement.getJLabel());
    panel.add(toolbarPlacement.getJComboBox());

    canvasBgColorTitle = new JLabel(S.get("windowCanvasBgColor"));
    canvasBgColor = new ColorChooserButton(window, AppPreferences.CANVAS_BG_COLOR);
    panel.add(canvasBgColorTitle);
    panel.add(canvasBgColor);
    gridBgColorTitle = new JLabel(S.get("windowGridBgColor"));
    gridBgColor = new ColorChooserButton(window, AppPreferences.GRID_BG_COLOR);
    panel.add(gridBgColorTitle);
    panel.add(gridBgColor);
    gridDotColorTitle = new JLabel(S.get("windowGridDotColor"));
    gridDotColor = new ColorChooserButton(window, AppPreferences.GRID_DOT_COLOR);
    panel.add(gridDotColorTitle);
    panel.add(gridDotColor);
    gridZoomedDotColorTitle = new JLabel(S.get("windowGridZoomedDotColor"));
    gridZoomedDotColor = new ColorChooserButton(window, AppPreferences.GRID_ZOOMED_DOT_COLOR);
    panel.add(gridZoomedDotColorTitle);
    panel.add(gridZoomedDotColor);

    final var gridColorsResetButton = new JButton();
    gridColorsResetButton.addActionListener(listener);
    gridColorsResetButton.setActionCommand(cmdResetGridColors);
    gridColorsResetButton.setText(S.get("windowGridColorsReset"));
    panel.add(new JLabel());
    panel.add(gridColorsResetButton);

    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));

    importantA = new JLabel(S.get("windowToolbarPleaserestart"));
    importantA.setFont(importantA.getFont().deriveFont(Font.ITALIC));
    panel.add(importantA);

    importantB = new JTextArea(S.get("windowToolbarImportant"));
    importantB.setFont(importantB.getFont().deriveFont(Font.ITALIC));
    panel.add(importantB);

    zoomLabel = new JLabel(S.get("windowToolbarZoomfactor"));
    zoomValue = new ZoomSlider(JSlider.HORIZONTAL, 100, 300, (int) (AppPreferences.SCALE_FACTOR.get() * 100));

    panel.add(zoomLabel);
    panel.add(zoomValue);
    zoomValue.addChangeListener(listener);

    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));

    var index = 0;
    lookAndFeel = new JComboBox<>();
    lookAndFeel.setSize(50, 20);

    lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
    for (final var info : lookAndFeelInfos) {
      lookAndFeel.insertItemAt(info.getName(), index);
      if (info.getClassName().equals(AppPreferences.LookAndFeel.get())) {
        lookAndFeel.setSelectedIndex(index);
        this.index = index;
      }
      index++;
    }
    lookfeelLabel = new JLabel(S.get("windowToolbarLookandfeel"));
    panel.add(lookfeelLabel);
    panel.add(lookAndFeel);
    lookAndFeel.addActionListener(listener);

    final var previewLabel = new JLabel(S.get("windowToolbarPreview"));
    panel.add(previewLabel);
    previewContainer = new JPanel();
    panel.add(previewContainer);
    initThemePreviewer();

    setLayout(new TableLayout(1));
    final var but = new JButton();
    but.addActionListener(listener);
    but.setActionCommand(cmdResetWindowLayout);
    but.setText(S.get("windowToolbarReset"));
    add(but);
    for (final var check : checks) {
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
    for (final var check : checks) {
      check.localeChanged();
    }
    toolbarPlacement.localeChanged();
    zoomLabel.setText(S.get("windowToolbarZoomfactor"));
    lookfeelLabel.setText(S.get("windowToolbarLookandfeel"));
    importantA.setText(S.get("windowToolbarPleaserestart"));
    importantB.setText(S.get("windowToolbarImportant"));
  }

  private class SettingsChangeListener implements ChangeListener, ActionListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      final var source = (JSlider) e.getSource();
      if (!source.getValueIsAdjusting()) {
        int value = source.getValue();
        AppPreferences.SCALE_FACTOR.set((double) value / 100.0);
        final var nowOpen = Projects.getOpenProjects();
        for (final var proj : nowOpen) {
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(lookAndFeel)) {
        if (lookAndFeel.getSelectedIndex() != index) {
          index = lookAndFeel.getSelectedIndex();
          AppPreferences.LookAndFeel.set(lookAndFeelInfos[index].getClassName());
          initThemePreviewer();
        }
      } else if (e.getActionCommand().equals(cmdResetWindowLayout)) {
        AppPreferences.resetWindow();
        final var nowOpen = Projects.getOpenProjects();
        for (final var proj : nowOpen) {
          proj.getFrame().resetLayout();
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      } else if (e.getActionCommand().equals(cmdResetGridColors)) {
        //        AppPreferences.resetWindow();
        final var nowOpen = Projects.getOpenProjects();
        AppPreferences.setDefaultGridColors();
        for (final var proj : nowOpen) {
          proj.getFrame().repaint();
        }
      }
    }
  }
}
