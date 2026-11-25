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
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.gui.icons.symbolic.CircuitGlyphIcon;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlEntity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Responsive 3-column card grid displaying components as visual schematic cards
 * with 44px icon tiles, full non-truncated names, and prominent selected states.
 */
public class TileGrid extends JPanel implements ToolboxModel.Listener {

  private static final long serialVersionUID = 1L;

  private final ToolboxModel model;
  private String currentCategory = "Gates";
  private Library currentLibrary = null;
  private Tool hoveredTool = null;

  public TileGrid(ToolboxModel model) {
    super();
    this.model = model;
    setOpaque(true);
    Color bg = UIManager.getColor("Panel.background");
    setBackground(bg != null ? bg : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(31, 33, 37) : new Color(248, 248, 250)));
    setBorder(BorderFactory.createEmptyBorder(6, 8, 16, 8));

    model.addListener(this);
    setCategory("Gates", findLibrary("Gates"));
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Color bg = UIManager.getColor("Panel.background");
    setBackground(bg != null ? bg : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(31, 33, 37) : new Color(248, 248, 250)));
    if (model != null) {
      refresh();
    }
  }

  private Library findLibrary(String name) {
    for (final var lib : model.getLibraries()) {
      if (lib.getDisplayName().equalsIgnoreCase(name)) {
        return lib;
      }
    }
    return null;
  }

  public void setCategory(String categoryName, Library lib) {
    this.currentCategory = categoryName;
    this.currentLibrary = (lib != null) ? lib : findLibrary(categoryName);
    refresh();
  }

  public void refresh() {
    removeAll();

    final var searchQuery = model.getSearchQuery();
    final var isSearching = (searchQuery != null && !searchQuery.isBlank());

    List<ToolItem> items = new ArrayList<>();

    if (isSearching) {
      final var q = searchQuery.toLowerCase();
      // Search across circuits
      for (final var tool : model.getCircuits()) {
        if (matches(tool, "Circuits", q)) {
          items.add(new ToolItem(tool, "Circuits"));
        }
      }
      // Search across all libraries
      for (final var lib : model.getLibraries()) {
        collectMatchingTools(lib, lib.getDisplayName(), q, items);
      }
    } else {
      if ("Circuits".equalsIgnoreCase(currentCategory)) {
        for (final var tool : model.getCircuits()) {
          items.add(new ToolItem(tool, "Circuits"));
        }
      } else if (currentLibrary != null) {
        for (final var tool : currentLibrary.getTools()) {
          items.add(new ToolItem(tool, currentLibrary.getDisplayName()));
        }
      }
    }

    if (items.isEmpty()) {
      setLayout(new BorderLayout());
      final var emptyMsg = isSearching
          ? "<html><center>No components match '<b>" + escapeHtml(searchQuery) + "</b>'.<br><small style='color:#888'>Press Esc to clear.</small></center></html>"
          : "<html><center>No components in this category.</center></html>";
      final var emptyLabel = new JLabel(emptyMsg, JLabel.CENTER);
      emptyLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(12)));
      emptyLabel.setForeground(new Color(154, 160, 168));
      emptyLabel.setBorder(BorderFactory.createEmptyBorder(32, 16, 32, 16));
      add(emptyLabel, BorderLayout.CENTER);
    } else {
      int gap = AppPreferences.getScaled(8);
      // Fixed 3 columns layout
      setLayout(new GridLayout(0, 3, gap, gap));

      for (final var item : items) {
        final var tile = new ComponentTile(item.tool, item.libraryName, isSearching);
        add(tile);
      }
    }

    revalidate();
    repaint();
  }

  private void collectMatchingTools(Library lib, String libName, String query, List<ToolItem> out) {
    if (lib == null || lib.isHidden()) return;
    for (final var tool : lib.getTools()) {
      if (matches(tool, libName, query)) {
        out.add(new ToolItem(tool, libName));
      }
    }
    for (final var sub : lib.getLibraries()) {
      collectMatchingTools(sub, libName, query, out);
    }
  }

  private boolean matches(Tool tool, String libName, String query) {
    if (tool == null) return false;
    final var name = tool.getDisplayName();
    final var desc = tool.getDescription();
    if (name != null && name.toLowerCase().contains(query)) return true;
    if (desc != null && desc.toLowerCase().contains(query)) return true;
    return libName != null && libName.toLowerCase().contains(query);
  }

  private static String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
    refresh();
  }

  @Override
  public void viewModeChanged(String newMode) {}

  private static class ToolItem {
    final Tool tool;
    final String libraryName;

    ToolItem(Tool tool, String libraryName) {
      this.tool = tool;
      this.libraryName = libraryName;
    }
  }

  private class ComponentTile extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Tool tool;
    private final String libName;
    private final boolean showLibTag;

    ComponentTile(Tool tool, String libName, boolean showLibTag) {
      super(new BorderLayout());
      this.tool = tool;
      this.libName = libName;
      this.showLibTag = showLibTag;

      setOpaque(false);
      int cardH = AppPreferences.getScaled(80);
      setPreferredSize(new Dimension(-1, cardH));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, cardH));
      setMinimumSize(new Dimension(AppPreferences.getScaled(60), cardH));
      setToolTipText("<html><b>" + tool.getDisplayName() + "</b> (" + libName + ")<br>" + tool.getDescription() + "</html>");

      // 1. Icon Container
      final Icon icon;
      if (tool instanceof AddTool addTool && addTool.getFactory(false) instanceof SubcircuitFactory sub) {
        final var circ = sub.getSubcircuit();
        boolean isViewed = (circ != null && model.getCurrentCircuit() == circ);
        icon = new CircuitGlyphIcon(isViewed, false, tool.getDisplayName(), AppPreferences.getScaled(40));
      } else if (tool instanceof AddTool addTool && addTool.getFactory(false) instanceof VhdlEntity) {
        boolean isViewed = model.isCircuitActive(addTool);
        icon = new CircuitGlyphIcon(isViewed, true, tool.getDisplayName(), AppPreferences.getScaled(40));
      } else {
        icon = ComponentGlyphCache.getIcon(tool, AppPreferences.getScaled(40));
      }

      final var iconLabel = new JLabel(icon, JLabel.CENTER);
      iconLabel.setPreferredSize(new Dimension(-1, AppPreferences.getScaled(46)));
      iconLabel.setBorder(BorderFactory.createEmptyBorder(AppPreferences.getScaled(4), 2, 0, 2));
      add(iconLabel, BorderLayout.CENTER);

      // 2. Full Name Label (Never Truncated, wraps cleanly)
      final var textPanel = new JPanel(new BorderLayout());
      textPanel.setOpaque(false);
      textPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, AppPreferences.getScaled(6), 4));

      final var isArmed = (model.getArmedTool() == tool);
      Color fgMain = isArmed
          ? Color.WHITE
          : (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(230, 232, 235) : new Color(31, 30, 29));

      final var sq = model.getSearchQuery();
      String rawName = tool.getDisplayName();
      String formattedName;
      if (sq != null && !sq.isBlank() && rawName.toLowerCase().contains(sq.toLowerCase())) {
        int idx = rawName.toLowerCase().indexOf(sq.toLowerCase());
        String b = escapeHtml(rawName.substring(0, idx));
        String m = escapeHtml(rawName.substring(idx, idx + sq.length()));
        String a = escapeHtml(rawName.substring(idx + sq.length()));
        formattedName = b + "<span style='color: #3584e4; font-weight: bold;'>" + m + "</span>" + a;
      } else {
        formattedName = escapeHtml(rawName);
      }
      final var nameLabel = new JLabel("<html><center>" + formattedName + "</center></html>", JLabel.CENTER);
      nameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(11)));
      nameLabel.setForeground(fgMain);
      textPanel.add(nameLabel, BorderLayout.CENTER);

      if (showLibTag) {
        final var libLabel = new JLabel(libName, JLabel.CENTER);
        libLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(9)));
        libLabel.setForeground(new Color(154, 160, 168));
        textPanel.add(libLabel, BorderLayout.SOUTH);
      }

      add(textPanel, BorderLayout.SOUTH);

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
            if (tool instanceof AddTool addTool) {
              final var fact = addTool.getFactory(false);
              if (fact instanceof SubcircuitFactory sub) {
                model.setCurrentCircuit(sub.getSubcircuit());
              } else if (fact instanceof VhdlEntity vhdl) {
                model.setCurrentHdl(vhdl.getContent());
              }
            }
          } else {
            model.armTool(tool);
          }
        }

        @Override
        public void mousePressed(MouseEvent e) {
          if (e.isPopupTrigger()) showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (e.isPopupTrigger()) showPopup(e);
        }

        private void showPopup(MouseEvent e) {
          if (tool instanceof AddTool addTool) {
            final var fact = addTool.getFactory(false);
            if (fact instanceof SubcircuitFactory sub) {
              final var menu = Popups.forCircuit(model.getProject(), addTool, sub.getSubcircuit());
              if (menu != null) menu.show(ComponentTile.this, e.getX(), e.getY());
            } else if (fact instanceof VhdlEntity vhdl) {
              final var menu = Popups.forVhdl(model.getProject(), addTool, vhdl.getContent());
              if (menu != null) menu.show(ComponentTile.this, e.getX(), e.getY());
            }
          }
        }
      });
    }

    @Override
    protected void paintComponent(Graphics g) {
      final var g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int arc = AppPreferences.getScaled(8);

      final var isArmed = (model.getArmedTool() == tool);
      final var isHovered = (hoveredTool == tool);

      Color accent = UIManager.getColor("Component.accentColor");
      if (accent == null) accent = new Color(53, 132, 228);

      Color cardBg = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
          ? new Color(38, 41, 46)
          : new Color(255, 255, 255);
      Color cardBorder = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
          ? new Color(50, 54, 61)
          : new Color(224, 228, 232);

      if (isArmed) {
        // Selected card: 2px accent border and accent fill
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setColor(accent);
        g2.setStroke(new java.awt.BasicStroke(AppPreferences.getScaled(2.0f)));
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
      } else {
        g2.setColor(cardBg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        if (isHovered) {
          Color hoverBg = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
              ? new Color(255, 255, 255, 14)
              : new Color(0, 0, 0, 10);
          g2.setColor(hoverBg);
          g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

          Color strongBorder = UIManager.getColor("Component.focusedBorderColor");
          if (strongBorder == null) strongBorder = accent;
          g2.setColor(strongBorder);
          g2.setStroke(new java.awt.BasicStroke(AppPreferences.getScaled(1.5f)));
          g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        } else {
          g2.setColor(cardBorder);
          g2.setStroke(new java.awt.BasicStroke(AppPreferences.getScaled(1.0f)));
          g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
        }
      }

      g2.dispose();
      super.paintComponent(g);
    }
  }
}
