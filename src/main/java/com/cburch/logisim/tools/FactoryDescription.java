/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.IconsUtil;
import com.cburch.logisim.util.LibraryUtil;
import com.cburch.logisim.util.StringGetter;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows an object to be created holding all the information essential to showing a
 * ComponentFactory in the explorer window, but without actually loading the ComponentFactory unless
 * a program genuinely gets around to needing to use it. Note that for this to work, the
 * ComponentFactory class must be public, and it must include a public no-arguments constructor.
 */
public class FactoryDescription {

  static final Logger logger = LoggerFactory.getLogger(FactoryDescription.class);

  private final StringGetter displayName;
  private String iconName;
  private boolean iconLoadAttempted;
  private Icon icon;
  private final Class factoryClass;
  private boolean factoryLoadAttempted;
  private ComponentFactory factory;
  private StringGetter toolTip;

  public FactoryDescription(
      Class<? extends ComponentFactory> factoryClass, StringGetter displayName, Icon icon) {
    this(factoryClass, displayName);
  }

  public FactoryDescription(
      Class<? extends ComponentFactory> factoryClass, StringGetter displayName, String iconName) {
    this(factoryClass, displayName);
    this.iconName = iconName;
    this.iconLoadAttempted = false;
    this.icon = null;
  }

  public FactoryDescription(
      Class<? extends ComponentFactory> factoryClass, StringGetter displayName) {
    this.displayName = displayName;
    this.iconName = "???";
    this.iconLoadAttempted = true;
    this.icon = null;
    this.factoryClass = factoryClass;
    this.factoryLoadAttempted = false;
    this.factory = null;
    this.toolTip = null;
  }

  public static List<Tool> getTools(
      Class<? extends Library> base, FactoryDescription[] descriptions) {
    var tools = new Tool[descriptions.length];
    for (var i = 0; i < tools.length; i++) {
      tools[i] = new AddTool(base, descriptions[i]);
    }
    return Arrays.asList(tools);
  }

  public String getDisplayName() {
    return displayName.toString();
  }

  public ComponentFactory getFactory(Class<? extends Library> libraryClass) {
    final var ret = factory;
    if (factory != null || factoryLoadAttempted) {
      return ret;
    }

    var errorMsg = "";
    try {
      errorMsg = "Getting class loader";
      final var loader = this.factoryClass.getClassLoader();
      errorMsg = "Loading class";
      Class<?> factoryCls = loader.loadClass(this.factoryClass.getCanonicalName());
      errorMsg = "Creating instance";
      Object factoryValue = factoryCls.getDeclaredConstructor().newInstance();
      errorMsg = "Converting to ComponentFactory";
      factory = (ComponentFactory) factoryValue;
      factoryLoadAttempted = true;
      return factory;
    } catch (Exception t) {
      final var name = t.getClass().getName();
      final var m = t.getMessage();

      errorMsg += ": " + name;
      if (m != null) errorMsg += ": " + m;
    }

    logger.error("Error while {}", errorMsg);
    factory = null;
    factoryLoadAttempted = true;
    return null;
  }

  public Icon getIcon() {
    var ret = icon;
    if (ret == null && !iconLoadAttempted) {
      ret = IconsUtil.getIcon(iconName);
      icon = ret;
      iconLoadAttempted = true;
    }
    return ret;
  }

  /**
   * Returns unique library identifier.
   *
   * <p>As we want to have static _ID per library, generic implementation must look for it in the
   * current instance
   */
  public String getName() {
    return LibraryUtil.getName(factoryClass);
  }

  public String getToolTip() {
    StringGetter getter = toolTip;
    return getter == null ? null : getter.toString();
  }

  public boolean isFactoryLoaded() {
    return factoryLoadAttempted;
  }

  public FactoryDescription setToolTip(StringGetter getter) {
    toolTip = getter;
    return this;
  }
}
