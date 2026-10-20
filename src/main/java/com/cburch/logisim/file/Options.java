/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;

public class Options {
  public static final AttributeOption GATE_UNDEFINED_IGNORE =
      new AttributeOption("ignore", S.getter("gateUndefinedIgnore"));
  public static final AttributeOption GATE_UNDEFINED_ERROR =
      new AttributeOption("error", S.getter("gateUndefinedError"));

  public static final Attribute<Integer> ATTR_SIM_LIMIT =
      Attributes.forInteger("simlimit", S.getter("simLimitOption"));
  public static final Attribute<Integer> ATTR_SIM_RAND =
      Attributes.forInteger("simrand", S.getter("simRandomOption"));
  public static final Attribute<AttributeOption> ATTR_GATE_UNDEFINED =
      Attributes.forOption(
          "gateUndefined",
          S.getter("gateUndefinedOption"),
          new AttributeOption[] {GATE_UNDEFINED_IGNORE, GATE_UNDEFINED_ERROR});

  public static final Integer SIM_RAND_DFLT = 32;

  private static final Attribute<?>[] ATTRIBUTES = {
    ATTR_GATE_UNDEFINED, ATTR_SIM_LIMIT, ATTR_SIM_RAND
  };
  private static final Object[] DEFAULTS = {GATE_UNDEFINED_IGNORE, 1000, 0};

  private final AttributeSet attrs;
  private final MouseMappings mmappings;
  private final ToolbarData toolbar;

  public Options() {
    attrs = AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    mmappings = new MouseMappings();
    toolbar = new ToolbarData();
  }

  public void copyFrom(Options other, LogisimFile dest) {
    AttributeSets.copy(other.attrs, this.attrs);
    this.toolbar.copyFrom(other.toolbar, dest);
    this.mmappings.copyFrom(other.mmappings, dest);
  }

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public MouseMappings getMouseMappings() {
    return mmappings;
  }

  public ToolbarData getToolbarData() {
    return toolbar;
  }
}
