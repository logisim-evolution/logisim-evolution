/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.EditPopup;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import java.util.HashMap;
import java.util.Map;

public class AppearanceEditPopup extends EditPopup implements EditHandler.Listener {
  private static final long serialVersionUID = 1L;
  private final AppearanceCanvas canvas;
  private final EditHandler handler;
  private final Map<LogisimMenuItem, Boolean> enabled;

  public AppearanceEditPopup(AppearanceCanvas canvas) {
    super(true);
    this.canvas = canvas;
    handler = new AppearanceEditHandler(canvas);
    handler.setListener(this);
    enabled = new HashMap<>();
    handler.computeEnabled();
    initialize();
  }

  @Override
  public void enableChanged(EditHandler handler, LogisimMenuItem action, boolean value) {
    enabled.put(action, value);
  }

  @Override
  protected void fire(LogisimMenuItem item) {
    if (item == LogisimMenuBar.CUT) {
      handler.cut();
    } else if (item == LogisimMenuBar.COPY) {
      handler.copy();
    } else if (item == LogisimMenuBar.DELETE) {
      handler.delete();
    } else if (item == LogisimMenuBar.DUPLICATE) {
      handler.duplicate();
    } else if (item == LogisimMenuBar.RAISE) {
      handler.raise();
    } else if (item == LogisimMenuBar.LOWER) {
      handler.lower();
    } else if (item == LogisimMenuBar.RAISE_TOP) {
      handler.raiseTop();
    } else if (item == LogisimMenuBar.LOWER_BOTTOM) {
      handler.lowerBottom();
    } else if (item == LogisimMenuBar.ADD_CONTROL) {
      handler.addControlPoint();
    } else if (item == LogisimMenuBar.REMOVE_CONTROL) {
      handler.removeControlPoint();
    }
  }

  @Override
  protected boolean isEnabled(LogisimMenuItem item) {
    final var value = enabled.get(item);
    return value != null && value;
  }

  @Override
  protected boolean shouldShow(LogisimMenuItem item) {
    return (item == LogisimMenuBar.ADD_CONTROL || item == LogisimMenuBar.REMOVE_CONTROL)
        ? canvas.getSelection().getSelectedHandle() != null
        : true;
  }
}
