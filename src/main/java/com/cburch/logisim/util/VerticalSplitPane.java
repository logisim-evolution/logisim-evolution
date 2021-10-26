/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.contracts.BaseLayoutManagerContract;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class VerticalSplitPane extends JPanel {
  private class MyDragbar extends HorizontalSplitPane.Dragbar {
    private static final long serialVersionUID = 1L;

    MyDragbar() {
      setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
    }

    @Override
    int getDragValue(MouseEvent e) {
      return getX() + e.getX() - VerticalSplitPane.this.getInsets().left;
    }

    @Override
    void setDragValue(int value) {
      final var in = VerticalSplitPane.this.getInsets();
      setFraction((double) value / (VerticalSplitPane.this.getWidth() - in.left - in.right));
      revalidate();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      // do nothing
    }
  }

  private class MyLayout implements BaseLayoutManagerContract {

    @Override
    public void layoutContainer(Container parent) {
      final var in = parent.getInsets();
      final var maxWidth = parent.getWidth() - (in.left + in.right);
      final var maxHeight = parent.getHeight() - (in.top + in.bottom);
      int split;
      if (fraction <= 0.0) {
        split = 0;
      } else if (fraction >= 1.0) {
        split = maxWidth;
      } else {
        split = (int) Math.round(maxWidth * fraction);
        split = Math.min(split, maxWidth - compRight.getMinimumSize().width);
        split = Math.max(split, compLeft.getMinimumSize().width);
      }

      compLeft.setBounds(in.left, in.top, split, maxHeight);
      compRight.setBounds(in.left + split, in.top, maxWidth - split, maxHeight);
      dragbar.setBounds(
          in.left + split - HorizontalSplitPane.DRAG_TOLERANCE,
          in.top,
          2 * HorizontalSplitPane.DRAG_TOLERANCE,
          maxHeight);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      if (fraction <= 0.0) return compRight.getMinimumSize();
      if (fraction >= 1.0) return compLeft.getMinimumSize();
      final var in = parent.getInsets();
      final var d0 = compLeft.getMinimumSize();
      final var d1 = compRight.getMinimumSize();
      return new Dimension(
          in.left + d0.width + d1.width + in.right,
          in.top + Math.max(d0.height, d1.height) + in.bottom);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      if (fraction <= 0.0) return compRight.getPreferredSize();
      if (fraction >= 1.0) return compLeft.getPreferredSize();
      final var in = parent.getInsets();
      final var d0 = compLeft.getPreferredSize();
      final var d1 = compRight.getPreferredSize();
      return new Dimension(
          in.left + d0.width + d1.width + in.right,
          in.top + Math.max(d0.height, d1.height) + in.bottom);
    }
  }

  private static final long serialVersionUID = 1L;

  protected JComponent compLeft;
  protected JComponent compRight;
  protected final MyDragbar dragbar = new MyDragbar();
  protected double fraction;

  protected VerticalSplitPane() {}

  public VerticalSplitPane(JComponent compLeft, JComponent compRight, double fraction) {
    init(compLeft, compRight, fraction);
  }

  protected MyLayout layout = new MyLayout();

  protected void init(JComponent compLeft, JComponent compRight, double fraction) {
    removeAll();

    this.compLeft = compLeft;
    this.compRight = compRight;
    this.fraction = fraction;

    setLayout(layout);
    add(dragbar); // above the other components
    add(compLeft);
    add(compRight);
  }

  public double getFraction() {
    return fraction;
  }

  public void setFraction(double value) {
    if (value < 0.0) value = 0.0;
    if (value > 1.0) value = 1.0;
    if (fraction != value) {
      fraction = value;
      revalidate();
    }
  }
}
