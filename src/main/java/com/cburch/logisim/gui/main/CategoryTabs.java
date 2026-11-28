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
import com.cburch.logisim.gui.icons.symbolic.CategoryModuleIcon;
import com.cburch.logisim.gui.icons.symbolic.CircuitGlyphIcon;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

/**
 * Labeled horizontal category tabs along the top of the Tile view.
 * Displays category name + count (e.g. "Gates (12)", "Memory (8)", "Circuits (2)").
 */
public class CategoryTabs extends JPanel implements ToolboxModel.Listener {

  private static final long serialVersionUID = 1L;

  public interface TabSelectionListener {
    void categorySelected(String categoryName, Library library);
  }

  private final ToolboxModel model;
  private final TabSelectionListener selectionListener;
  private final JPanel tabsContainer;
  private final JScrollPane scrollPane;
  private final List<CategoryTabButton> tabButtons = new ArrayList<>();
  private String activeCategory = "Gates";

  public CategoryTabs(ToolboxModel model, TabSelectionListener selectionListener) {
    super(new BorderLayout());
    this.model = model;
    this.selectionListener = selectionListener;
    setOpaque(false);
    setPreferredSize(new Dimension(-1, AppPreferences.getScaled(36)));
    setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));

    tabsContainer = new JPanel();
    tabsContainer.setLayout(new BoxLayout(tabsContainer, BoxLayout.X_AXIS));
    tabsContainer.setOpaque(false);

    scrollPane = new JScrollPane(tabsContainer);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    scrollPane.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0;");

    // Enable smooth mouse wheel horizontal scrolling
    scrollPane.addMouseWheelListener((MouseWheelEvent e) -> {
      final var hBar = scrollPane.getHorizontalScrollBar();
      if (hBar != null) {
        int amount = e.getWheelRotation() * AppPreferences.getScaled(24);
        hBar.setValue(hBar.getValue() + amount);
      }
    });

    add(scrollPane, BorderLayout.CENTER);

    model.addListener(this);
    buildTabs();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (tabsContainer != null) {
      buildTabs();
    }
  }

  public String getActiveCategory() {
    return activeCategory;
  }

  public void setActiveCategory(String cat) {
    this.activeCategory = cat;
    for (final var btn : tabButtons) {
      btn.setSelected(btn.categoryName.equalsIgnoreCase(cat));
    }
  }

  public void buildTabs() {
    tabsContainer.removeAll();
    tabButtons.clear();

    Color fgMuted = new Color(154, 160, 168);

    // 1. Circuits Tab
    int circCount = model.getCircuits().size();
    final var circIcon = new CircuitGlyphIcon(false);
    circIcon.setColor(fgMuted);
    addTab("Circuits", circCount, circIcon, null);

    // 2. Standard Libraries & Imported Libraries
    for (final var lib : model.getLibraries()) {
      if (lib.isHidden()) continue;
      String libName = lib.getDisplayName();
      int count = lib.getTools().size();
      final var moduleIcon = new CategoryModuleIcon(16);
      addTab(libName, count, moduleIcon, lib);
    }

    tabsContainer.add(Box.createHorizontalGlue());
    setActiveCategory(activeCategory);
    revalidate();
    repaint();
  }

  private void addTab(String name, int count, Icon icon, Library lib) {
    final var btn = new CategoryTabButton(name, count, icon, lib);
    tabButtons.add(btn);
    tabsContainer.add(btn);
    tabsContainer.add(Box.createHorizontalStrut(AppPreferences.getScaled(4)));
  }

  @Override
  public void toolArmed(Tool tool) {}

  @Override
  public void circuitChanged(Circuit circuit) {
    buildTabs();
  }

  @Override
  public void structureChanged() {
    buildTabs();
  }

  @Override
  public void filterChanged(String query) {}

  @Override
  public void viewModeChanged(String newMode) {}

  private class CategoryTabButton extends JButton {
    private static final long serialVersionUID = 1L;
    final String categoryName;
    final Library library;
    private boolean isSelected = false;
    private boolean isHovered = false;

    CategoryTabButton(String name, int count, Icon icon, Library lib) {
      super();
      this.categoryName = name;
      this.library = lib;

      setLayout(new BorderLayout());
      setOpaque(false);
      putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
      setFocusPainted(false);
      setBorder(BorderFactory.createEmptyBorder(AppPreferences.getScaled(4), AppPreferences.getScaled(8), AppPreferences.getScaled(4), AppPreferences.getScaled(8)));

      final var content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
      content.setOpaque(false);

      if (icon != null) {
        final var iconLabel = new JLabel(icon);
        content.add(iconLabel);
        content.add(Box.createHorizontalStrut(AppPreferences.getScaled(4)));
      }

      final var textLabel = new JLabel(name);
      textLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(12)));
      content.add(textLabel);

      if (count > 0) {
        content.add(Box.createHorizontalStrut(AppPreferences.getScaled(4)));
        final var countLabel = new JLabel("(" + count + ")");
        countLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(10)));
        countLabel.setForeground(new Color(154, 160, 168));
        content.add(countLabel);
      }

      add(content, BorderLayout.CENTER);

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          isHovered = true;
          repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
          isHovered = false;
          repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          setActiveCategory(categoryName);
          if (selectionListener != null) {
            selectionListener.categorySelected(categoryName, library);
          }
        }
      });
    }

    public void setSelected(boolean sel) {
      this.isSelected = sel;
      repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
      final var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int arc = AppPreferences.getScaled(6);

      Color accent = UIManager.getColor("Component.accentColor");
      if (accent == null) accent = new Color(53, 132, 228);

      if (isSelected) {
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setColor(accent);
        g2.setStroke(new java.awt.BasicStroke(AppPreferences.getScaled(1.5f)));
        g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
      } else if (isHovered) {
        Color hoverBg = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
            ? new Color(255, 255, 255, 14)
            : new Color(0, 0, 0, 12);
        g2.setColor(hoverBg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
      }

      g2.dispose();
      super.paintComponent(g);
    }
  }
}
