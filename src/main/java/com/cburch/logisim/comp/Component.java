/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;

public interface Component extends Location.At {
  // listener methods
  default void addComponentListener(ComponentListener l) {
    // no-op implementation
  }

  boolean contains(Location pt);

  boolean contains(Location pt, Graphics g);

  void draw(ComponentDrawContext context);

  boolean endsAt(Location pt);

  // user interface methods
  default void expose(ComponentDrawContext context) {
    // no-op implementation
  }

  AttributeSet getAttributeSet();

  Bounds getBounds();

  Bounds getBounds(Graphics g);

  EndData getEnd(int index);

  // propagation methods
  List<EndData> getEnds(); // list of EndDatas
  // basic information methods

  ComponentFactory getFactory();

  default void setFactory(ComponentFactory fact) {
    // no-op implementation
  }

  /**
   * Retrieves information about a special-purpose feature for this component. This technique allows
   * future Logisim versions to add new features for components without requiring changes to
   * existing components. It also removes the necessity for the Component API to directly declare
   * methods for each individual feature. In most cases, the <code>key</code> is a <code>Class
   * </code> object corresponding to an interface, and the method should return an implementation of
   * that interface if it supports the feature.
   *
   * <p>As of this writing, possible values for <code>key</code> include: <code>Pokable.class</code>
   * , <code>CustomHandles.class</code>, <code>WireRepair.class</code>, <code>TextEditable.class
   * </code>, <code>MenuExtender.class</code>, <code>ToolTipMaker.class</code>, <code>
   * ExpressionComputer.class</code>, and <code>Loggable.class</code>.
   *
   * @param key an object representing a feature.
   * @return an object representing information about how the component supports the feature, or
   *     <code>null</code> if it does not support the feature.
   */
  Object getFeature(Object key);

  default void propagate(CircuitState state) {
    // no-op implementation
  }

  default void removeComponentListener(ComponentListener l) {
    // no-op implementation
  }
}
