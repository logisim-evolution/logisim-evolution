package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class JAdjustableScroll extends JScrollPane {
  public int preferredWidth = 700;
  public int preferredHeight = 200;

  public JAdjustableScroll(Component view) {
    super(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(preferredWidth, preferredHeight);
  }
}
