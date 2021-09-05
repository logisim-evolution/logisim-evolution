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
import lombok.val;

public class Toolbar extends JPanel {
  public static final Object VERTICAL = new Object();
  public static final Object HORIZONTAL = new Object();
  private static final long serialVersionUID = 1L;
  private final JPanel subPanel;
  private final MyListener myListener;
  @Getter private ToolbarModel toolbarModel;
  @Getter private Object orientation;
  @Getter private ToolbarButton pressed;    // cur pressed

  public Toolbar(ToolbarModel model) {
    super(new BorderLayout());
    subPanel = new JPanel();
    toolbarModel = model;
    orientation = HORIZONTAL;
    myListener = new MyListener();
    pressed = null;

    add(new JPanel(), BorderLayout.CENTER);
    setOrientation(HORIZONTAL);

    computeContents();
    if (model != null) model.addToolbarModelListener(myListener);
  }

  private void computeContents() {
    subPanel.removeAll();
    val model = toolbarModel;
    if (model != null) {
      for (val item : model.getItems()) {
        subPanel.add(new ToolbarButton(this, item));
      }
      subPanel.add(Box.createGlue());
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
    remove(subPanel);
    subPanel.setLayout(new BoxLayout(subPanel, axis));
    add(subPanel, position);
    orientation = value;
  }

  void setPressed(ToolbarButton value) {
    val oldValue = pressed;
    if (oldValue != value) {
      pressed = value;
      if (oldValue != null) oldValue.repaint();
      if (value != null) value.repaint();
    }
  }

  public void setToolbarModel(ToolbarModel value) {
    val oldValue = toolbarModel;
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
