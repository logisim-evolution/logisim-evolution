/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.Icons;
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
 * a program genuinely gets around to needing to use it. Note that for this to work, the ComponentFactory
 * class must be public, and it must include a public no-arguments constructor.
 */
public class FactoryDescription {

  public static List<Tool> getTools(
      Class<? extends Library> base, FactoryDescription[] descriptions) {
    var tools = new Tool[descriptions.length];
    for (var i = 0; i < tools.length; i++) {
      tools[i] = new AddTool(base, descriptions[i]);
    }
    return Arrays.asList(tools);
  }

  static final Logger logger = LoggerFactory.getLogger(FactoryDescription.class);

  private final StringGetter displayName;
  private String iconName;
  private boolean iconLoadAttempted;
  private Icon icon;
  private final Class factoryClass;
  private boolean factoryLoadAttempted;
  private ComponentFactory factory;
  private StringGetter toolTip;


  public FactoryDescription(Class<? extends ComponentFactory> factoryClass, StringGetter displayName, Icon icon) {
    this(factoryClass, displayName);
  }

  public FactoryDescription(Class<? extends ComponentFactory> factoryClass, StringGetter displayName, String iconName) {
    this(factoryClass, displayName);
    this.iconName = iconName;
    this.iconLoadAttempted = false;
    this.icon = null;
  }

  public FactoryDescription(Class<? extends ComponentFactory> factoryClass, StringGetter displayName) {
    this.displayName = displayName;
    this.iconName = "???";
    this.iconLoadAttempted = true;
    this.icon = null;
    this.factoryClass = factoryClass;
    this.factoryLoadAttempted = false;
    this.factory = null;
    this.toolTip = null;
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
      ret = Icons.getIcon(iconName);
      icon = ret;
      iconLoadAttempted = true;
    }
    return ret;
  }

  /**
   * Returns unique library identifier.
   *
   * As we want to have static _ID per library, generic
   * implementation must look for it in the current instance
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
