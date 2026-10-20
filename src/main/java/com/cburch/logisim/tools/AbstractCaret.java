/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractCaret implements Caret {
  private final ArrayList<CaretListener> listeners = new ArrayList<>();
  private final List<CaretListener> listenersView;
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  public AbstractCaret() {
    listenersView = Collections.unmodifiableList(listeners);
  }

  // listener methods
  @Override
  public void addCaretListener(CaretListener e) {
    listeners.add(e);
  }

  @Override
  public Bounds getBounds(Graphics gfx) {
    return bounds;
  }

  protected List<CaretListener> getCaretListeners() {
    return listenersView;
  }

  // query/Graphics methods
  @Override
  public String getText() {
    return "";
  }

  @Override
  public void removeCaretListener(CaretListener e) {
    listeners.remove(e);
  }

  // configuration methods
  public void setBounds(Bounds value) {
    bounds = value;
  }
}
