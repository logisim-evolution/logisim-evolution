/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

/**
 * The SmartScroller will attempt to keep the viewport positioned based on the users interaction
 * with the scrollbar. The normal behaviour is to keep the viewport positioned to see new data as it
 * is dynamically added.
 *
 * <p>Assuming vertical scrolling and data is added to the bottom:
 *
 * <p>- when the viewport is at the bottom and new data is added, then automatically scroll the
 * viewport to the bottom - when the viewport is not at the bottom and new data is added, then do
 * nothing with the viewport
 *
 * <p>Assuming vertical scrolling and data is added to the top:
 *
 * <p>- when the viewport is at the top and new data is added, then do nothing with the viewport -
 * when the viewport is not at the top and new data is added, then adjust the viewport to the
 * relative position it was at before the data was added
 *
 * <p>Similiar logic would apply for horizontal scrolling.
 *
 * <p>http://www.camick.com/java/source/SmartScroller.java
 */
public class SmartScroller implements AdjustmentListener {
  public static final int HORIZONTAL = 0;
  public static final int VERTICAL = 1;

  public static final int START = 0;
  public static final int END = 1;

  private final int viewportPosition;

  private final JScrollBar scrollBar;
  private boolean adjustScrollBar = true;

  private int previousValue = -1;
  private int previousMaximum = -1;

  /**
   * Convenience constructor. Scroll direction is VERTICAL and viewport position is at the END.
   *
   * @param scrollPane the scroll pane to monitor
   */
  public SmartScroller(JScrollPane scrollPane) {
    this(scrollPane, VERTICAL, END);
  }

  /**
   * Convenience constructor. Scroll direction is VERTICAL.
   *
   * @param scrollPane the scroll pane to monitor
   * @param viewportPosition valid values are START and END
   */
  public SmartScroller(JScrollPane scrollPane, int viewportPosition) {
    this(scrollPane, VERTICAL, viewportPosition);
  }

  /**
   * Specify how the SmartScroller will function.
   *
   * @param scrollPane the scroll pane to monitor
   * @param scrollDirection indicates which JScrollBar to monitor. Valid values are HORIZONTAL and
   *     VERTICAL.
   * @param viewportPosition indicates where the viewport will normally be positioned as data is
   *     added. Valid values are START and END
   */
  public SmartScroller(JScrollPane scrollPane, int scrollDirection, int viewportPosition) {
    if (scrollDirection != HORIZONTAL && scrollDirection != VERTICAL)
      throw new IllegalArgumentException("invalid scroll direction specified");

    if (viewportPosition != START && viewportPosition != END)
      throw new IllegalArgumentException("invalid viewport position specified");

    this.viewportPosition = viewportPosition;

    if (scrollDirection == HORIZONTAL) scrollBar = scrollPane.getHorizontalScrollBar();
    else scrollBar = scrollPane.getVerticalScrollBar();

    scrollBar.addAdjustmentListener(this);

    // Turn off automatic scrolling for text components

    Component view = scrollPane.getViewport().getView();

    if (view instanceof JTextComponent textComponent) {
      final var caret = (DefaultCaret) textComponent.getCaret();
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }
  }

  @Override
  public void adjustmentValueChanged(final AdjustmentEvent e) {
    SwingUtilities.invokeLater(
        () -> checkScrollBar(e));
  }

  /*
   * Analyze every adjustment event to determine when the viewport needs to be
   * repositioned.
   */
  private void checkScrollBar(AdjustmentEvent e) {
    // The scroll bar listModel contains information needed to determine
    // whether the viewport should be repositioned or not.

    final var scrollBar = (JScrollBar) e.getSource();
    final var listModel = scrollBar.getModel();
    var value = listModel.getValue();
    final var extent = listModel.getExtent();
    final var maximum = listModel.getMaximum();

    final var valueChanged = previousValue != value;
    final var maximumChanged = previousMaximum != maximum;

    // Check if the user has manually repositioned the scrollbar

    if (valueChanged && !maximumChanged) {
      if (viewportPosition == START) adjustScrollBar = value != 0;
      else adjustScrollBar = value + extent >= maximum;
    }

    // Reset the "value" so we can reposition the viewport and
    // distinguish between a user scroll and a program scroll.
    // (ie. valueChanged will be false on a program scroll)

    if (adjustScrollBar && viewportPosition == END) {
      // Scroll the viewport to the end.
      scrollBar.removeAdjustmentListener(this);
      value = maximum - extent;
      scrollBar.setValue(value);
      scrollBar.addAdjustmentListener(this);
    }

    if (adjustScrollBar && viewportPosition == START) {
      // Keep the viewport at the same relative viewportPosition
      scrollBar.removeAdjustmentListener(this);
      value = value + maximum - previousMaximum;
      scrollBar.setValue(value);
      scrollBar.addAdjustmentListener(this);
    }

    previousValue = value;
    previousMaximum = maximum;
  }
}
