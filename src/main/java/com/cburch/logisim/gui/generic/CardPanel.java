/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CardPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private final ArrayList<ChangeListener> listeners;
  private String current;

  public CardPanel() {
    super(new CardLayout());
    listeners = new ArrayList<>();
    current = "";
  }

  public void addChangeListener(ChangeListener listener) {
    listeners.add(listener);
  }

  public void addView(String name, Component comp) {
    add(comp, name);
  }

  public String getView() {
    return current;
  }

  public void setView(String choice) {
    if (choice == null) choice = "";
    final var oldChoice = current;
    if (!oldChoice.equals(choice)) {
      current = choice;
      ((CardLayout) getLayout()).show(this, choice);
      final var e = new ChangeEvent(this);
      for (final var listener : listeners) {
        listener.stateChanged(e);
      }
    }
  }
}
