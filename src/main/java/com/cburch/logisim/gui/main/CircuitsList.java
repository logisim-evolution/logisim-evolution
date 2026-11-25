/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.gui.icons.symbolic.CircuitGlyphIcon;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Modern circuits list section in the redesigned left explorer.
 * Displays circuits with semantic hierarchy: 14% tint + 3px accent bar for active circuit,
 * solid accent for armed tool, and unified section header styling.
 */
public class CircuitsList extends JPanel implements ToolboxModel.Listener {

  private static final long serialVersionUID = 1L;

  public enum SimStatus {
    RUNNING,
    PAUSED,
    ERROR,
    IDLE
  }

  private final ToolboxModel model;
  private final JPanel headerBox;
  private final JLabel headerLabel;
  private final JPanel itemsPanel;
  private final JScrollPane scrollPane;
  private Tool hoveredTool = null;
  private volatile SimStatus currentSimStatus = SimStatus.IDLE;

  public CircuitsList(ToolboxModel model) {
    super(new BorderLayout());
    this.model = model;
    setOpaque(false);

    // Section header with subtle rule above
    headerBox = new JPanel(new BorderLayout());
    headerBox.setOpaque(false);
    Color borderSubtle = getSeparatorBorderColor();
    headerBox.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, borderSubtle),
        BorderFactory.createEmptyBorder(4, 10, 3, 10)));

    headerLabel = new JLabel("CIRCUITS");
    headerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, AppPreferences.getScaled(11)));
    headerLabel.setForeground(getMutedHeaderColor());
    headerBox.add(headerLabel, BorderLayout.WEST);

    add(headerBox, BorderLayout.NORTH);

    itemsPanel = new JPanel();
    itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
    itemsPanel.setOpaque(false);
    itemsPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));

    scrollPane = new JScrollPane(itemsPanel);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.putClientProperty("JScrollPane.smoothScrolling", true);

    add(scrollPane, BorderLayout.CENTER);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        final var circuits = model != null ? model.getCircuits() : null;
        if (circuits != null && circuits.size() > 5) {
          revalidate();
        }
      }
    });

    updateBorder();
    model.addListener(this);
    initSimulatorListener();
    refresh();
  }

  private void initSimulatorListener() {
    if (model.getProject() != null && model.getProject().getSimulator() != null) {
      final var sim = model.getProject().getSimulator();
      currentSimStatus = computeSimStatus(sim);
      sim.addSimulatorListener(new Simulator.StatusListener() {
        @Override
        public void simulatorReset(Simulator.Event e) {
          checkAndApplySimStatus(sim);
        }

        @Override
        public void simulatorStateChanged(Simulator.Event e) {
          checkAndApplySimStatus(sim);
        }
      });
    }
  }

  private SimStatus computeSimStatus(Simulator sim) {
    if (sim == null) return SimStatus.IDLE;
    if (sim.isOscillating()) return SimStatus.ERROR;
    if (sim.isAutoTicking()) return SimStatus.RUNNING;
    return SimStatus.PAUSED;
  }

  private void checkAndApplySimStatus(Simulator sim) {
    final var newStatus = computeSimStatus(sim);
    if (newStatus != currentSimStatus) {
      currentSimStatus = newStatus;
      if (SwingUtilities.isEventDispatchThread()) {
        repaint();
      } else {
        SwingUtilities.invokeLater(CircuitsList.this::repaint);
      }
    }
  }

  private Color getMutedHeaderColor() {
    Color fgMuted = UIManager.getColor("Surface.mutedText");
    if (fgMuted == null) fgMuted = UIManager.getColor("Chrome.mutedText");
    if (fgMuted == null) {
      fgMuted = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x9E, 0x9E, 0x9E) : new Color(0x5A, 0x5D, 0x66);
    }
    return fgMuted;
  }

  private static Color getSeparatorBorderColor() {
    Color c = UIManager.getColor("Separator.foreground");
    if (c == null) {
      c = UIManager.getColor("Component.borderColor");
    }
    if (c == null) {
      c = UIManager.getColor("Surface.borderSubtle");
    }
    if (c == null) {
      c = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
          ? new Color(0x59, 0x5C, 0x5E)
          : new Color(0xD5, 0xDA, 0xE2);
    }
    return c;
  }

  private void updateBorder() {
    Color divColor = getSeparatorBorderColor();
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, divColor));
    if (headerBox != null) {
      headerBox.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(1, 0, 0, 0, divColor),
          BorderFactory.createEmptyBorder(6, 12, 4, 12)));
    }
  }

  @Override
  public Dimension getPreferredSize() {
    final var superSize = super.getPreferredSize();
    final var circuits = model != null ? model.getCircuits() : null;
    final int count = circuits != null ? circuits.size() : 0;

    final int headerH = headerBox != null ? headerBox.getPreferredSize().height : AppPreferences.getScaled(26);
    final int rowH = AppPreferences.getScaled(24);
    final int spacing = AppPreferences.getScaled(2);
    final int padding = AppPreferences.getScaled(10);
    final int triggerCount = 5;

    int computedH;
    if (count <= triggerCount) {
      computedH = headerH + count * (rowH + spacing) + padding;
    } else {
      final int baseH = headerH + triggerCount * (rowH + spacing) + padding;
      final double extraLog = AppPreferences.getScaled(22) * Math.log(1.0 + (count - triggerCount));

      // Viewport-relative ceiling: allocate up to 40% of parent height (if not, fallback to scaled 230)
      final var parent = getParent();
      final int parentH = (parent != null && parent.getHeight() > 0)
          ? parent.getHeight()
          : (getRootPane() != null ? (int) (getRootPane().getHeight() * 0.5) : 0);
      final int maxCap = (parentH > 0)
          ? Math.max(baseH, (int) (parentH * 0.40))
          : AppPreferences.getScaled(230);

      computedH = (int) Math.min(baseH + extraLog, maxCap);
    }
    return new Dimension(superSize.width, computedH);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    updateBorder();
    if (headerLabel != null) {
      headerLabel.setForeground(getMutedHeaderColor());
    }
    if (model != null) {
      refresh();
    }
  }

  public void refresh() {
    itemsPanel.removeAll();
    final var circuits = model.getCircuits();
    headerLabel.setText("CIRCUITS (" + circuits.size() + ")");

    for (final var tool : circuits) {
      itemsPanel.add(new CircuitRow(tool));
      itemsPanel.add(Box.createVerticalStrut(AppPreferences.getScaled(2)));
    }

    final int triggerCount = 5;
    if (scrollPane != null) {
      scrollPane.setVerticalScrollBarPolicy(
          circuits.size() > triggerCount
              ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
              : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    }

    revalidate();
    repaint();
  }

  @Override
  public void toolArmed(Tool tool) {
    repaint();
  }

  @Override
  public void circuitChanged(Circuit circuit) {
    refresh();
  }

  @Override
  public void structureChanged() {
    refresh();
  }

  @Override
  public void filterChanged(String query) {
    if (query == null || query.isBlank()) {
      for (final var comp : itemsPanel.getComponents()) {
        comp.setVisible(true);
      }
    } else {
      final var q = query.toLowerCase();
      for (final var comp : itemsPanel.getComponents()) {
        if (comp instanceof CircuitRow row) {
          row.setVisible(row.tool.getDisplayName().toLowerCase().contains(q));
        }
      }
    }
    revalidate();
    repaint();
  }

  @Override
  public void viewModeChanged(String newMode) {
    // Handled by card layout
  }

  private class CircuitRow extends JPanel {
    private static final long serialVersionUID = 1L;
    private final AddTool tool;
    private final CircuitGlyphIcon icon;
    private final JLabel label;

    CircuitRow(AddTool tool) {
      super(new BorderLayout());
      this.tool = tool;
      setOpaque(false);

      final var rowH = AppPreferences.getScaled(24);
      setPreferredSize(new Dimension(-1, rowH));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));

      final var isActive = model.isCircuitActive(tool);
      final var isArmed = (model.getArmedTool() == tool);
      icon = new CircuitGlyphIcon(isActive, AppPreferences.getScaled(16));

      label = new JLabel(tool.getDisplayName(), icon, SwingConstants.LEFT) {
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          final var curActive = model.isCircuitActive(tool);
          if (curActive) {
            final var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final var curArmed = (model.getArmedTool() == tool);
            final var isDarkTheme = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());

            Color dotColor = switch (currentSimStatus) {
              case ERROR -> new Color(246, 97, 81); // Halted / Oscillation Red (#f66151)
              case RUNNING -> new Color(51, 209, 122); // Auto-ticking Running Green (#33d17a)
              case PAUSED -> isDarkTheme ? new Color(229, 165, 10) : new Color(184, 125, 0); // Paused Amber
              case IDLE -> isDarkTheme ? new Color(0x4C, 0xAF, 0x50) : new Color(0x2E, 0x7D, 0x32);
            };

            int dotSize = AppPreferences.getScaled(6);
            Insets insets = getInsets();
            int iconW = getIcon() != null ? getIcon().getIconWidth() + getIconTextGap() : 0;
            int textW = getFontMetrics(getFont()).stringWidth(getText());
            int x = (insets != null ? insets.left : 0) + iconW + textW + AppPreferences.getScaled(6);
            int y = (getHeight() - dotSize) / 2;

            // Subtle halo around dot for crisp contrast
            Color halo = curArmed
                ? new Color(255, 255, 255, 60)
                : new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 50);
            g2.setColor(halo);
            g2.fillOval(x - 1, y - 1, dotSize + 2, dotSize + 2);

            g2.setColor(dotColor);
            g2.fillOval(x, y, dotSize, dotSize);
            g2.dispose();
          }
        }
      };
      label.setIconTextGap(AppPreferences.getScaled(8));
      label.setFont(new Font(Font.SANS_SERIF, isActive ? Font.BOLD : Font.PLAIN, AppPreferences.getScaled(12)));
      label.setBorder(BorderFactory.createEmptyBorder(0, AppPreferences.getScaled(10), 0, AppPreferences.getScaled(10)));

      updateRowColors(isActive, isArmed);
      add(label, BorderLayout.CENTER);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          hoveredTool = tool;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          if (hoveredTool == tool) {
            hoveredTool = null;
            repaint();
          }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            final var factory = tool.getFactory(false);
            if (factory instanceof SubcircuitFactory sub) {
              model.setCurrentCircuit(sub.getSubcircuit());
            } else if (factory instanceof VhdlEntity vhdl) {
              model.setCurrentHdl(vhdl.getContent());
            }
          } else {
            model.armTool(tool);
          }
        }

        @Override
        public void mousePressed(MouseEvent e) {
          if (e.isPopupTrigger()) showMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (e.isPopupTrigger()) showMenu(e);
        }

        private void showMenu(MouseEvent e) {
          final var fact = tool.getFactory(false);
          if (fact instanceof SubcircuitFactory sub) {
            final var menu = Popups.forCircuit(model.getProject(), tool, sub.getSubcircuit());
            if (menu != null) menu.show(CircuitRow.this, e.getX(), e.getY());
          } else if (fact instanceof VhdlEntity vhdl) {
            final var menu = Popups.forVhdl(model.getProject(), tool, vhdl.getContent());
            if (menu != null) menu.show(CircuitRow.this, e.getX(), e.getY());
          }
        }
      });
    }

    private void updateRowColors(boolean isActive, boolean isArmed) {
      Color fgMain = UIManager.getColor("Label.foreground");
      if (fgMain == null) fgMain = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(245, 245, 245) : new Color(38, 40, 44);

      if (icon != null) {
        icon.setCurrentCircuit(isActive);
        icon.setColor(fgMain);
      }

      if (isArmed || isActive) {
        label.setForeground(fgMain);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
      } else {
        label.setForeground(fgMain);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      final var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int arc = AppPreferences.getScaled(6);
      int marginX = AppPreferences.getScaled(4);
      int innerW = w - 2 * marginX;
      int innerH = h - 2;

      final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
      Color selectionBorderColor = isDark ? new Color(255, 255, 255, 140) : new Color(0, 0, 0, 140);

      final var isActive = model.isCircuitActive(tool);
      final var isHovered = (hoveredTool == tool);
      final var isArmed = (model.getArmedTool() == tool);

      updateRowColors(isActive, isArmed);

      if (isArmed) {
        // Selected/clicked circuit gets a crisp neutral monochrome outlined rectangle border
        g2.setColor(selectionBorderColor);
        g2.setStroke(new java.awt.BasicStroke(AppPreferences.getScaled(1.2f)));
        g2.drawRoundRect(marginX, 1, innerW - 1, innerH - 1, arc, arc);
      } else if (isHovered) {
        // Smooth theme-aware hover highlight
        Color hoverBg = isDark ? new Color(255, 255, 255, 14) : new Color(0, 0, 0, 12);
        g2.setColor(hoverBg);
        g2.fillRoundRect(marginX, 1, innerW, innerH, arc, arc);
      }

      g2.dispose();
      super.paintComponent(g);
    }
  }
}
