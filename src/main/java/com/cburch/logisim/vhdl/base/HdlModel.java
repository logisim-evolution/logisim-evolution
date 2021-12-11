/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

public interface HdlModel {

  /** Registers a listener for changes to the values. */
  void addHdlModelListener(HdlModelListener l);

  /** Compares the model's content with another model. */
  boolean compare(HdlModel model);

  /** Compares the model's content with a string. */
  boolean compare(String value);

  /** Gets the content of the HDL-IP component. */
  String getContent();

  /** Get the component's name */
  String getName();

  /** Unregisters a listener for changes to the values. */
  void removeHdlModelListener(HdlModelListener l);

  /** Sets the content of the component. */
  boolean setContent(String content);

  /** Checks whether the content of the component is valid. */
  boolean isValid();

  /** Displays errors, if any. */
  void showErrors();

  /** Fire notification that the display has changed. */
  void displayChanged();
}
