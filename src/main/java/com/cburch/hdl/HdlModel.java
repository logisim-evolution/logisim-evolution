/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.hdl;

import com.cburch.logisim.data.BitWidth;

/**
 * HdlModel is a base interface for objects that contain mutable text representing code
 *  written in an HDL. Listeners may be attached to appropriately handle updates.
 * Implementors should typically use HdlContent.
 * It should not be confused with the other HdlModel, located in
 *  the logisim.vhdl.base package.
 */
public interface HdlModel {
  // NOTE: silly members' names are mostly to avoid refactoring of the whole codebase due to record's
  // getters not using Bean naming convention (so i.e. `foo()` instead of `getFoo()`. We may change
  // that in future, but for now it looks stupid in this file only.
  public record PortDescription(String getName, String getType, int getWidthInt, BitWidth getWidth) {
    public PortDescription(String name, String type, int width) {
      this(name, type, width, BitWidth.create(width));
    }
  }

  /** Registers a listener for changes to the values. */
  void addHdlModelListener(HdlModelListener l);

  /** Compares the model's content with another model. */
  boolean compare(HdlModel model);

  /** Compares the model's content with a string. */
  boolean compare(String value);

  /** Gets the content of the HDL-IP component. */
  String getContent();

  /** Get the component's name. */
  String getName();

  /**
   * Get the component's input ports.
   */
  PortDescription[] getInputs();

  /**
   * Get the component's output ports.
   */
  PortDescription[] getOutputs();

  /** Unregisters a listener for changes to the values. */
  void removeHdlModelListener(HdlModelListener l);

  /** Sets the content of the component. */
  boolean setContent(String content);
}
