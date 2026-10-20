/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;

/**
 * Represents a category of components that appear in a circuit. This class and <code>Component
 * </code> share the same sort of relationship as the relation between <em>classes</em> and
 * <em>instances</em> in Java. Normally, there is only one ComponentFactory created for any
 * particular category.
 */
public interface ComponentFactory extends AttributeDefaultProvider {
  Object SHOULD_SNAP = new Object();
  Object TOOL_TIP = new Object();
  Object FACING_ATTRIBUTE_KEY = new Object();

  boolean activeOnHigh(AttributeSet attrs);

  AttributeSet createAttributeSet();

  Component createComponent(Location loc, AttributeSet attrs);

  void removeComponent(Circuit circ, Component c, CircuitState state);

  void drawGhost(ComponentDrawContext context, Color color, int x, int y, AttributeSet attrs);

  @Override
  Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver);

  StringGetter getDisplayGetter();

  String getDisplayName();

  /**
   * Retrieves special-purpose features for this factory. This technique allows for future Logisim
   * versions to add new features for components without requiring changes to existing components.
   * It also removes the necessity for the Component API to directly declare methods for each
   * individual feature. In most cases, the <code>key</code> is a <code>Class</code> object
   * corresponding to an interface, and the method should return an implementation of that interface
   * if it supports the feature.
   *
   * <p>As of this writing, possible values for <code>key</code> include: <code>TOOL_TIP</code>
   * (return a <code>String</code>) and <code>SHOULD_SNAP</code> (return a <code>Boolean</code>).
   *
   * @param key an object representing a feature.
   * @return an object representing information about how the component supports the feature, or
   *     <code>null</code> if it does not support the feature.
   */
  Object getFeature(Object key, AttributeSet attrs);

  HdlGeneratorFactory getHDLGenerator(AttributeSet attrs);

  String getHDLName(AttributeSet attrs);

  String getHDLTopName(AttributeSet attrs);

  String getName();

  Bounds getOffsetBounds(AttributeSet attrs);

  boolean hasThreeStateDrivers(AttributeSet attrs);

  boolean isHDLSupportedComponent(AttributeSet attrs);

  @Override
  boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver);

  boolean checkForGatedClocks(netlistComponent comp);

  int[] clockPinIndex(netlistComponent comp);

  void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attrs);

  boolean requiresGlobalClock();

  /* Added for HDL generation */
  boolean requiresNonZeroLabel();

  /* Added for Soc simulation core */
  boolean isSocComponent();
}
