package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.icons.symbolic.ChevronDownIcon;
import com.cburch.logisim.gui.icons.symbolic.PauseIcon;
import com.cburch.logisim.gui.icons.symbolic.PlayIcon;
import com.cburch.logisim.gui.icons.symbolic.RedoIcon;
import com.cburch.logisim.gui.icons.symbolic.ResetIcon;
import com.cburch.logisim.gui.icons.symbolic.SidebarToggleIcon;
import com.cburch.logisim.gui.icons.symbolic.StepIcon;
import com.cburch.logisim.gui.icons.symbolic.TickFullIcon;
import com.cburch.logisim.gui.icons.symbolic.TickHalfIcon;
import com.cburch.logisim.gui.icons.symbolic.UndoIcon;
import com.cburch.logisim.gui.icons.symbolic.VectorBaseIcon;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.ProjectEvent;

/**
 * Unified HeaderBar for Logisim-Evolution.
 * Unified control panel for workspace state, simulation, zoom, and navigation.
 */
public class UnifiedHeaderBar extends JPanel implements LocaleListener, ProjectListener {

  private static final Color COLOR_RUNNING = new Color(51, 209, 122); // #33d17a
  private static final Color COLOR_PAUSED_DARK = new Color(229, 165, 10); // Amber
  private static final Color COLOR_PAUSED_LIGHT = new Color(184, 125, 0); // Contrast amber on white
  private static final Color COLOR_HALTED = new Color(246, 97, 81); // #f66151

  private final Frame frame;
  private final Project project;

  private static final int SIM_ICON_SIZE = 18;

  // Icons
  private final PlayIcon playIcon = new PlayIcon(SIM_ICON_SIZE);
  private final PauseIcon pauseIcon = new PauseIcon(SIM_ICON_SIZE);

  // Left Controls
  private JButton sidebarButton;
  private JButton undoButton;
  private JButton redoButton;

  // Center Controls
  private JPanel centerPanel;
  private JButton circuitPickerButton;

  // Right Controls
  private JPanel leftPanel;
  private JPanel rightPanel;
  private JLabel simStatusLabel;
  private JToggleButton playPauseButton;
  private JButton stepButton;
  private JButton tickHalfButton;
  private JButton tickFullButton;
  private JButton freqButton;
  private JButton zoomButton;

  public UnifiedHeaderBar(Frame frame, Project project) {
    super(null); // Custom absolute-centered layout manager
    setLayout(new HeaderBarLayout());
    this.frame = frame;
    this.project = project;

    initComponents();
    initStyle();
    attachListeners();
    updateState();
    LocaleManager.addLocaleListener(this);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        handleResponsiveLayout();
      }
    });
  }

  private void initStyle() {
    Color bg = UIManager.getColor("Surface.chrome");
    if (bg == null) {
      bg = UIManager.getColor("Chrome.barBackground");
    }
    if (bg == null) {
      bg = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x30, 0x33, 0x38) : new Color(0xE6, 0xE9, 0xEE);
    }
    setBackground(bg);
    Color borderColor = UIManager.getColor("Surface.borderStrong");
    if (borderColor == null) {
      borderColor = UIManager.getColor("Chrome.borderColor");
    }
    if (borderColor == null) {
      borderColor = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x4D, 0x51, 0x5A) : new Color(0xC4, 0xC8, 0xD0);
    }
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
        BorderFactory.createEmptyBorder(4, 10, 4, 20)));
  }

  private void initComponents() {
    // -------------------------------------------------------------------------
    // LEFT: Sidebar toggle + Undo / Redo
    // -------------------------------------------------------------------------
    leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    leftPanel.setOpaque(false);

    sidebarButton = createPillButton(new SidebarToggleIcon(), "Toggle Sidebar (Ctrl+B)");
    sidebarButton.addActionListener(e -> frame.toggleSidebar());

    undoButton = createPillButton(new UndoIcon(), "Undo (Ctrl+Z)");
    undoButton.addActionListener(e -> {
      if (project != null)
        project.undoAction();
    });

    redoButton = createPillButton(new RedoIcon(), "Redo (Ctrl+Y)");
    redoButton.addActionListener(e -> {
      if (project != null)
        project.redoAction();
    });

    leftPanel.add(sidebarButton);
    leftPanel.add(Box.createHorizontalStrut(6));
    leftPanel.add(undoButton);
    leftPanel.add(redoButton);
    add(leftPanel);

    // -------------------------------------------------------------------------
    // CENTER: Circuit Hierarchy Breadcrumb & Switcher
    // -------------------------------------------------------------------------
    centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    centerPanel.setOpaque(false);

    circuitPickerButton = new JButton("main", new ChevronDownIcon()) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (project != null && project.isFileDirty()) {
          final var g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          Color warn = UIManager.getColor("Chrome.warning");
          if (warn == null) {
            warn = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0xE5, 0xA5, 0x0A) : new Color(0xB8, 0x7D, 0x00);
          }
          g2.setColor(warn);
          int dotSize = AppPreferences.getScaled(6);
          Insets insets = getInsets();
          int textW = getFontMetrics(getFont()).stringWidth(getText());
          int x = (insets != null ? insets.left : 0) + textW + AppPreferences.getScaled(4);
          int y = (getHeight() - dotSize) / 2;
          g2.fillOval(x, y, dotSize, dotSize);
          g2.dispose();
        }
      }
    };
    circuitPickerButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, AppPreferences.getScaled(13)));
    circuitPickerButton.setHorizontalTextPosition(SwingConstants.LEFT);
    circuitPickerButton.setIconTextGap(AppPreferences.getScaled(14));
    circuitPickerButton.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; margin: 3,12,3,8; borderWidth: 1; borderColor: $Component.borderColor; background: $Group.background;");
    circuitPickerButton.addActionListener(e -> showCircuitMenu(circuitPickerButton));

    centerPanel.add(circuitPickerButton);
    add(centerPanel);

    // -------------------------------------------------------------------------
    // RIGHT: Simulation Controls, Frequency, and Zoom Controls
    // -------------------------------------------------------------------------
    rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    rightPanel.setOpaque(false);

    // Simulation controls container
    JPanel simPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    simPill.setOpaque(false);
    simPill.putClientProperty(FlatClientProperties.STYLE,
        "arc: 6; background: $Group.background; border: 1,1,1,1,$Component.borderColor,1,6;");

    simStatusLabel = new JLabel("● Paused");
    simStatusLabel.setFont(simStatusLabel.getFont().deriveFont(Font.BOLD, (float) AppPreferences.getScaled(11)));
    simStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
    simStatusLabel.setForeground(getPausedColor());

    playIcon.setColor(COLOR_RUNNING);
    pauseIcon.setColor(COLOR_HALTED);

    playPauseButton = new JToggleButton(playIcon);
    stylePillItem(playPauseButton);
    playPauseButton.setToolTipText("Play / Pause Auto-Ticking (Ctrl+K)");
    playPauseButton.addActionListener(e -> {
      Simulator sim = project.getSimulator();
      if (sim != null) {
        sim.setAutoTicking(!sim.isAutoTicking());
        updateSimControls();
      }
    });

    stepButton = new JButton(new StepIcon(SIM_ICON_SIZE));
    stylePillItem(stepButton);
    stepButton.setToolTipText("Single Step Propagation (F7)");
    stepButton.addActionListener(e -> {
      Simulator sim = project.getSimulator();
      if (sim != null)
        sim.step();
    });

    tickHalfButton = new JButton(new TickHalfIcon(SIM_ICON_SIZE));
    stylePillItem(tickHalfButton);
    tickHalfButton.setToolTipText("Step Half-Tick (Ctrl+Shift+T)");
    tickHalfButton.addActionListener(e -> {
      Simulator sim = project.getSimulator();
      if (sim != null)
        sim.tick(1);
    });

    tickFullButton = new JButton(new TickFullIcon(SIM_ICON_SIZE));
    stylePillItem(tickFullButton);
    tickFullButton.setToolTipText("Tick Clock Once (Ctrl+T)");
    tickFullButton.addActionListener(e -> {
      Simulator sim = project.getSimulator();
      if (sim != null)
        sim.tick(2);
    });

    freqButton = new JButton("16 Hz", new ChevronDownIcon());
    freqButton.setHorizontalTextPosition(SwingConstants.LEFT);
    stylePillItem(freqButton);
    freqButton.setToolTipText("Clock Frequency & Simulation Options");
    freqButton.addActionListener(e -> showFrequencyMenu(freqButton));

    simPill.add(simStatusLabel);
    simPill.add(playPauseButton);
    simPill.add(stepButton);
    simPill.add(tickHalfButton);
    simPill.add(tickFullButton);
    simPill.add(freqButton);

    // Zoom popover trigger button
    zoomButton = new JButton("100%", new ChevronDownIcon());
    zoomButton.setHorizontalTextPosition(SwingConstants.LEFT);
    zoomButton.putClientProperty(FlatClientProperties.STYLE, "arc: 6; margin: 4,10,4,8; background: $Group.background; borderWidth: 1; borderColor: $Component.borderColor;");
    zoomButton.setToolTipText("Zoom Factor");
    zoomButton.addActionListener(e -> showZoomMenu(zoomButton));

    rightPanel.add(simPill);
    rightPanel.add(zoomButton);
    add(rightPanel);
  }

  private Color getPausedColor() {
    return AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? COLOR_PAUSED_DARK : COLOR_PAUSED_LIGHT;
  }

  private JButton createPillButton(VectorBaseIcon icon, String tooltip) {
    JButton btn = new JButton(icon);
    btn.setToolTipText(tooltip);
    btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
    btn.putClientProperty(FlatClientProperties.STYLE, "arc: 6; margin: 4,8,4,8; background: $Group.background; borderWidth: 1; borderColor: $Component.borderColor;");
    return btn;
  }

  private void stylePillItem(JButton btn) {
    btn.putClientProperty(FlatClientProperties.STYLE,
        "arc: 4; borderWidth: 0; background: null; margin: 4,8,4,8;");
  }

  private void stylePillItem(JToggleButton btn) {
    btn.putClientProperty(FlatClientProperties.STYLE,
        "arc: 4; borderWidth: 0; background: null; margin: 4,8,4,8;");
  }

  private void handleResponsiveLayout() {
    int w = getWidth();
    if (w < 820) {
      simStatusLabel.setText("●");
      freqButton.setText("");
    } else {
      updateSimControls();
    }
  }

  private void attachListeners() {
    Simulator sim = project.getSimulator();
    if (sim != null) {
      sim.addSimulatorListener(new Simulator.Listener() {
        @Override
        public void propagationCompleted(Simulator.Event e) {
          SwingUtilities.invokeLater(UnifiedHeaderBar.this::updateSimControls);
        }

        @Override
        public void simulatorReset(Simulator.Event e) {
          SwingUtilities.invokeLater(UnifiedHeaderBar.this::updateSimControls);
        }

        @Override
        public void simulatorStateChanged(Simulator.Event e) {
          SwingUtilities.invokeLater(UnifiedHeaderBar.this::updateSimControls);
        }
      });
    }

    project.addProjectListener(this);

    Circuit circ = project.getCurrentCircuit();
    if (circ != null) {
      circ.addCircuitListener(new CircuitListener() {
        @Override
        public void circuitChanged(CircuitEvent event) {
          SwingUtilities.invokeLater(UnifiedHeaderBar.this::updateUndoRedo);
        }
      });
    }
  }

  public void updateState() {
    updateTitle();
    updateSimControls();
    updateUndoRedo();
  }

  public void updateTitle() {
    if (circuitPickerButton == null || project == null)
      return;
    Circuit current = project.getCurrentCircuit();
    String name = (current != null) ? current.getName() : "main";
    circuitPickerButton.setText(name);
    circuitPickerButton.repaint();
  }

  public void updateSimControls() {
    if (playPauseButton == null || project == null)
      return;
    Simulator sim = project.getSimulator();
    if (sim != null) {
      boolean ticking = sim.isAutoTicking();
      boolean oscillating = sim.isOscillating();

      playPauseButton.setSelected(ticking);
      if (ticking) {
        pauseIcon.setColor(COLOR_HALTED);
        playPauseButton.setIcon(pauseIcon);
        playPauseButton.setToolTipText("Pause Auto-Ticking (Ctrl+K)");
      } else {
        playIcon.setColor(COLOR_RUNNING);
        playPauseButton.setIcon(playIcon);
        playPauseButton.setToolTipText("Run Auto-Ticking (Ctrl+K)");
      }

      String detailedMsg = sim.getSingleStepMessage();
      if (oscillating) {
        simStatusLabel.setText(getWidth() < 820 ? "●" : "● Oscillating");
        simStatusLabel.setForeground(COLOR_HALTED);
        simStatusLabel.setToolTipText(detailedMsg.isEmpty() ? "Circuit oscillation detected" : detailedMsg);
      } else if (ticking) {
        simStatusLabel.setText(getWidth() < 820 ? "●" : "● Running");
        simStatusLabel.setForeground(COLOR_RUNNING);
        simStatusLabel.setToolTipText("Simulation clock is actively running");
      } else {
        simStatusLabel.setText(getWidth() < 820 ? "●" : "● Paused");
        simStatusLabel.setForeground(getPausedColor());
        simStatusLabel.setToolTipText(detailedMsg.isEmpty() ? "Simulator paused: step with F7" : detailedMsg);
      }

      if (getWidth() >= 820) {
        double hz = sim.getTickFrequency();
        if (hz >= 1000.0) {
          freqButton.setText(String.format("%.0f kHz", hz / 1000.0));
        } else {
          freqButton.setText(String.format("%.0f Hz", hz));
        }
      }
    }
  }

  public void updateUndoRedo() {
    if (undoButton == null || redoButton == null || project == null)
      return;
    final var last = project.getLastAction();
    final boolean canUndo = (last != null);
    undoButton.setEnabled(canUndo);
    if (canUndo) {
      undoButton.setToolTipText("Undo " + last.getName() + " · Ctrl+Z");
    } else {
      undoButton.setToolTipText("Nothing to undo · Ctrl+Z");
    }

    final var next = project.getCanRedo() ? project.getLastRedoAction() : null;
    final boolean canRedo = (next != null);
    redoButton.setEnabled(canRedo);
    if (canRedo) {
      redoButton.setToolTipText("Redo " + next.getName() + " · Ctrl+Y");
    } else {
      redoButton.setToolTipText("Nothing to redo · Ctrl+Y");
    }
  }

  public void setZoomText(String text) {
    zoomButton.setText(text);
  }

  // ---------------------------------------------------------------------------
  // Dropdown Popups
  // ---------------------------------------------------------------------------
  private void showCircuitMenu(JButton parent) {
    JPopupMenu menu = new JPopupMenu();
    for (Circuit c : project.getLogisimFile().getCircuits()) {
      JMenuItem item = new JMenuItem(c.getName());
      if (c == project.getCurrentCircuit()) {
        item.setFont(item.getFont().deriveFont(Font.BOLD));
      }
      item.addActionListener(e -> project.setCurrentCircuit(c));
      menu.add(item);
    }
    menu.show(parent, 0, parent.getHeight() + 2);
  }

  private void showFrequencyMenu(JButton parent) {
    JPopupMenu menu = new JPopupMenu();
    Simulator sim = project.getSimulator();
    if (sim == null)
      return;

    // Clock Control Actions
    JMenuItem resetItem = new JMenuItem("Reset Simulation (Ctrl+R)", new ResetIcon());
    resetItem.addActionListener(e -> {
      sim.reset();
      updateSimControls();
    });
    menu.add(resetItem);

    menu.add(new JSeparator());

    JMenuItem tickFullItem = new JMenuItem("Tick Clock Once (Ctrl+T)", new TickFullIcon());
    tickFullItem.addActionListener(e -> sim.tick(2));
    menu.add(tickFullItem);

    JMenuItem tickHalfItem = new JMenuItem("Step Half-Tick (Ctrl+Shift+T)", new TickHalfIcon());
    tickHalfItem.addActionListener(e -> sim.tick(1));
    menu.add(tickHalfItem);

    JCheckBoxMenuItem autoTickItem = new JCheckBoxMenuItem("Continuous Ticking (Ctrl+K)", sim.isAutoTicking());
    autoTickItem.addActionListener(e -> {
      sim.setAutoTicking(!sim.isAutoTicking());
      updateSimControls();
    });
    menu.add(autoTickItem);

    menu.add(new JSeparator());

    // Frequency Presets
    double[] freqs = { 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1000.0, 2000.0, 4000.0 };
    double currentHz = sim.getTickFrequency();

    for (double hz : freqs) {
      String label = (hz >= 1000.0)
          ? String.format("%.0f kHz", hz / 1000.0)
          : String.format(hz < 1 ? "%.2f Hz" : "%.0f Hz", hz);
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, Math.abs(currentHz - hz) < 0.01);
      item.addActionListener(e -> {
        sim.setTickFrequency(hz);
        updateSimControls();
      });
      menu.add(item);
    }

    menu.show(parent, 0, parent.getHeight() + 2);
  }

  private void showZoomMenu(JButton parent) {
    JPopupMenu popover = new JPopupMenu();
    popover.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
        BorderFactory.createEmptyBorder(6, 8, 6, 8)));

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(false);

    // Top: Editable Percentage Field
    JPanel topRow = new JPanel(new BorderLayout(6, 0));
    topRow.setOpaque(false);
    JLabel zoomLabel = new JLabel("Zoom:");
    zoomLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground;");

    double curFactor = (frame.getZoomModel() != null) ? frame.getZoomModel().getZoomFactor() : 1.0;
    int curPercent = (int) Math.round(curFactor * 100.0);

    JTextField percentField = new JTextField(curPercent + "%", 6);
    percentField.setHorizontalAlignment(JTextField.RIGHT);
    percentField.putClientProperty(FlatClientProperties.STYLE, "arc: 6;");

    topRow.add(zoomLabel, BorderLayout.WEST);
    topRow.add(percentField, BorderLayout.CENTER);
    panel.add(topRow);
    panel.add(Box.createVerticalStrut(6));

    // Continuous Slider: 20% to 500%
    JSlider slider = new JSlider(20, 500, Math.min(500, Math.max(20, curPercent)));
    slider.putClientProperty(FlatClientProperties.STYLE, "thumbSize: 14,14;");
    panel.add(slider);
    panel.add(Box.createVerticalStrut(8));

    // Connect slider & percentField
    Runnable applyZoom = () -> {
      String text = percentField.getText().replace("%", "").trim();
      try {
        double val = Double.parseDouble(text);
        if (val >= 20 && val <= 1000) {
          if (frame.getZoomModel() != null) {
            frame.getZoomModel().setZoomFactor(val / 100.0);
            setZoomText(String.format("%.0f%%", val));
          }
        }
      } catch (NumberFormatException ignored) {
        // Keep current value
      }
    };

    percentField.addActionListener(e -> {
      applyZoom.run();
      popover.setVisible(false);
    });

    slider.addChangeListener(e -> {
      int val = slider.getValue();
      percentField.setText(val + "%");
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(val / 100.0);
        setZoomText(val + "%");
      }
    });

    popover.add(panel);
    popover.add(new JSeparator());

    // Presets with keyboard shortcuts
    addZoomPresetItem(popover, "Zoom In", "Ctrl + +", () -> {
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(frame.getZoomModel().getZoomFactor() * 1.2);
        setZoomText(String.format("%.0f%%", frame.getZoomModel().getZoomFactor() * 100.0));
      }
    });
    addZoomPresetItem(popover, "Zoom Out", "Ctrl + -", () -> {
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(frame.getZoomModel().getZoomFactor() / 1.2);
        setZoomText(String.format("%.0f%%", frame.getZoomModel().getZoomFactor() * 100.0));
      }
    });
    addZoomPresetItem(popover, "100% (Reset)", "Ctrl + 0", () -> {
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(1.0);
        setZoomText("100%");
      }
    });
    addZoomPresetItem(popover, "Fit to Screen", "Ctrl + Shift + F", () -> {
      if (frame.getZoomControl() != null) {
        frame.getZoomControl().zoomButton.doClick();
      }
    });
    addZoomPresetItem(popover, "50%", "", () -> {
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(0.5);
        setZoomText("50%");
      }
    });
    addZoomPresetItem(popover, "200%", "", () -> {
      if (frame.getZoomModel() != null) {
        frame.getZoomModel().setZoomFactor(2.0);
        setZoomText("200%");
      }
    });

    popover.show(parent, parent.getWidth() - popover.getPreferredSize().width, parent.getHeight() + 2);
    percentField.requestFocusInWindow();
    percentField.selectAll();
  }

  private void addZoomPresetItem(JPopupMenu menu, String label, String shortcut, Runnable action) {
    JMenuItem item = new JMenuItem(label);
    if (!shortcut.isEmpty()) {
      if (shortcut.equals("Ctrl + +")) {
        item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_EQUALS,
            java.awt.event.KeyEvent.CTRL_DOWN_MASK));
      } else if (shortcut.equals("Ctrl + -")) {
        item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_MINUS,
            java.awt.event.KeyEvent.CTRL_DOWN_MASK));
      } else if (shortcut.equals("Ctrl + 0")) {
        item.setAccelerator(
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, java.awt.event.KeyEvent.CTRL_DOWN_MASK));
      } else if (shortcut.equals("Ctrl + Shift + F")) {
        item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
            java.awt.event.KeyEvent.CTRL_DOWN_MASK | java.awt.event.KeyEvent.SHIFT_DOWN_MASK));
      }
    }
    item.addActionListener(e -> action.run());
    menu.add(item);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (circuitPickerButton != null) {
      initStyle();
      updateState();
    }
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    int act = event.getAction();
    if (act == ProjectEvent.ACTION_SET_CURRENT
        || act == ProjectEvent.ACTION_SET_FILE
        || act == ProjectEvent.ACTION_SET_STATE) {
      if (SwingUtilities.isEventDispatchThread()) {
        updateState();
      } else {
        SwingUtilities.invokeLater(this::updateState);
      }
    } else if (act == ProjectEvent.ACTION_COMPLETE
        || act == ProjectEvent.ACTION_MERGE
        || act == ProjectEvent.UNDO_COMPLETE
        || act == ProjectEvent.REDO_COMPLETE) {
      if (SwingUtilities.isEventDispatchThread()) {
        updateUndoRedo();
      } else {
        SwingUtilities.invokeLater(this::updateUndoRedo);
      }
    }
  }

  public JButton getUndoButton() {
    return undoButton;
  }

  public JButton getRedoButton() {
    return redoButton;
  }

  @Override
  public void localeChanged() {
    updateState();
  }

  // ---------------------------------------------------------------------------
  // Custom Absolute-Centered Layout Manager
  // ---------------------------------------------------------------------------
  private class HeaderBarLayout implements java.awt.LayoutManager {
    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(java.awt.Container parent) {
      Insets insets = parent.getInsets();
      int leftW = leftPanel != null ? leftPanel.getPreferredSize().width : 0;
      int centerW = centerPanel != null ? centerPanel.getPreferredSize().width : 0;
      int rightW = rightPanel != null ? rightPanel.getPreferredSize().width : 0;
      int h = Math.max(leftPanel != null ? leftPanel.getPreferredSize().height : 0,
              Math.max(centerPanel != null ? centerPanel.getPreferredSize().height : 0,
                       rightPanel != null ? rightPanel.getPreferredSize().height : 0));
      return new Dimension(leftW + centerW + rightW + insets.left + insets.right + 40, h + insets.top + insets.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(java.awt.Container parent) {
      return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(java.awt.Container parent) {
      Insets insets = parent.getInsets();
      int totalW = parent.getWidth();
      int totalH = parent.getHeight();
      int availH = totalH - insets.top - insets.bottom;

      // 1. Layout Left Panel
      int leftW = 0;
      if (leftPanel != null) {
        Dimension ld = leftPanel.getPreferredSize();
        leftW = ld.width;
        int ly = insets.top + (availH - ld.height) / 2;
        leftPanel.setBounds(insets.left, ly, leftW, ld.height);
      }

      // 2. Layout Right Panel
      int rightW = 0;
      if (rightPanel != null) {
        Dimension rd = rightPanel.getPreferredSize();
        rightW = rd.width;
        int rx = totalW - insets.right - rightW;
        int ry = insets.top + (availH - rd.height) / 2;
        rightPanel.setBounds(rx, ry, rightW, rd.height);
      }

      // 3. Center circuit switcher across total bar width
      if (centerPanel != null) {
        Dimension cd = centerPanel.getPreferredSize();
        int cx = (totalW - cd.width) / 2;
        int cy = insets.top + (availH - cd.height) / 2;

        // Prevent overlap if window gets too narrow
        int minLeft = insets.left + leftW + 8;
        int maxRight = totalW - insets.right - rightW - cd.width - 8;
        if (cx < minLeft) {
          cx = minLeft;
        } else if (cx > maxRight) {
          cx = maxRight;
        }
        centerPanel.setBounds(cx, cy, cd.width, cd.height);
      }
    }
  }
}
