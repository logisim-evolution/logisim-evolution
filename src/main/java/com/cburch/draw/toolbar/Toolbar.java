/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class Toolbar extends JPanel {
  public static final Object VERTICAL = new Object();
  public static final Object HORIZONTAL = new Object();
  private static final long serialVersionUID = 1L;
  private final JPanel subpanel;
  private final MyListener myListener;
  private ToolbarModel model;
  private Object orientation;
  private ToolbarButton curPressed;

  public Toolbar(ToolbarModel model) {
    super(new BorderLayout());
    this.subpanel = new JPanel();
    this.model = model;
    this.orientation = HORIZONTAL;
    this.myListener = new MyListener();
    this.curPressed = null;

    this.add(new JPanel(), BorderLayout.CENTER);
    setOrientation(HORIZONTAL);

    computeContents();
    if (model != null) model.addToolbarModelListener(myListener);
  }

  private void computeContents() {
    subpanel.removeAll();
    final var m = model;
    if (m != null) {
      for (ToolbarItem item : m.getItems()) {
        subpanel.add(new ToolbarButton(this, item));
      }
      subpanel.add(Box.createGlue());
    }
    revalidate();
  }

  Object getOrientation() {
    return orientation;
  }

  public void setOrientation(Object value) {
    int axis;
    String position;
    if (value.equals(HORIZONTAL)) {
      axis = BoxLayout.X_AXIS;
      position = BorderLayout.LINE_START;
    } else if (value.equals(VERTICAL)) {
      axis = BoxLayout.Y_AXIS;
      position = BorderLayout.NORTH;
    } else {
      throw new IllegalArgumentException();
    }
    this.remove(subpanel);
    subpanel.setLayout(new BoxLayout(subpanel, axis));
    this.add(subpanel, position);
    this.orientation = value;
  }

  ToolbarButton getPressed() {
    return curPressed;
  }

  void setPressed(ToolbarButton value) {
    final var oldValue = curPressed;
    if (oldValue != value) {
      curPressed = value;
      if (oldValue != null) oldValue.repaint();
      if (value != null) value.repaint();
    }
  }

  public ToolbarModel getToolbarModel() {
    return model;
  }

  public void setToolbarModel(ToolbarModel value) {
    final var oldValue = model;
    if (value != oldValue) {
      if (oldValue != null) oldValue.removeToolbarModelListener(myListener);
      if (value != null) value.addToolbarModelListener(myListener);
      model = value;
      computeContents();
    }
  }

  private class MyListener implements ToolbarModelListener {
    @Override
    public void toolbarAppearanceChanged(ToolbarModelEvent event) {
      repaint();
    }

    @Override
    public void toolbarContentsChanged(ToolbarModelEvent event) {
      computeContents();
    }
  }
}
