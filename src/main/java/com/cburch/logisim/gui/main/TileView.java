/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Library;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Tile View combining labeled top category tabs and 3-column component cards.
 */
public class TileView extends JPanel {

  private static final long serialVersionUID = 1L;

  private final ToolboxModel model;
  private final CategoryTabs tabs;
  private final TileGrid grid;

  public TileView(ToolboxModel model) {
    super(new BorderLayout());
    this.model = model;
    setOpaque(false);

    grid = new TileGrid(model);
    final var gridHolder = new JPanel(new BorderLayout());
    gridHolder.setOpaque(false);
    gridHolder.add(grid, BorderLayout.NORTH);

    final var gridScroll = new JScrollPane(gridHolder);
    gridScroll.setBorder(BorderFactory.createEmptyBorder());
    gridScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    gridScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    gridScroll.getViewport().setScrollMode(javax.swing.JViewport.BLIT_SCROLL_MODE);
    gridScroll.getVerticalScrollBar().setUnitIncrement(AppPreferences.getScaled(24));
    gridScroll.getVerticalScrollBar().setBlockIncrement(AppPreferences.getScaled(96));
    gridScroll.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0;");

    tabs = new CategoryTabs(model, (String categoryName, Library library) -> {
      grid.setCategory(categoryName, library);
    });

    add(tabs, BorderLayout.NORTH);
    add(gridScroll, BorderLayout.CENTER);
  }

  public void selectCategory(String cat) {
    tabs.setActiveCategory(cat);
  }

  public String getActiveCategory() {
    return tabs.getActiveCategory();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (tabs != null) {
      tabs.updateUI();
    }
    if (grid != null) {
      grid.updateUI();
    }
  }
}
