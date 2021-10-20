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
import lombok.Getter;

public class Toolbar extends JPanel {
  public static final Object VERTICAL = new Object();
  public static final Object HORIZONTAL = new Object();
  private static final long serialVersionUID = 1L;
  private final JPanel subpanel;
  private final MyListener myListener;
  @Getter private ToolbarModel toolbarModel;
  @Getter private Object orientation;
  @Getter private ToolbarButton pressed;

  public Toolbar(ToolbarModel toolbarModel) {
    super(new BorderLayout());
    this.subpanel = new JPanel();
    this.toolbarModel = toolbarModel;
    this.orientation = HORIZONTAL;
    this.myListener = new MyListener();
    this.pressed = null;

    this.add(new JPanel(), BorderLayout.CENTER);
    setOrientation(HORIZONTAL);

    computeContents();
    if (toolbarModel != null) toolbarModel.addToolbarModelListener(myListener);
  }

  private void computeContents() {
    subpanel.removeAll();
    final var m = toolbarModel;
    if (m != null) {
      for (ToolbarItem item : m.getItems()) {
        subpanel.add(new ToolbarButton(this, item));
      }
      subpanel.add(Box.createGlue());
    }
    revalidate();
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

  void setPressed(ToolbarButton value) {
    final var oldValue = pressed;
    if (oldValue != value) {
      pressed = value;
      if (oldValue != null) oldValue.repaint();
      if (value != null) value.repaint();
    }
  }

  public void setToolbarModel(ToolbarModel value) {
    final var oldValue = toolbarModel;
    if (value != oldValue) {
      if (oldValue != null) oldValue.removeToolbarModelListener(myListener);
      if (value != null) value.addToolbarModelListener(myListener);
      toolbarModel = value;
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
