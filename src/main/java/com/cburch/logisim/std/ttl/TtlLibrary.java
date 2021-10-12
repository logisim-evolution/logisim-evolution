/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class TtlLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "TTL";

  private static final FactoryDescription[] DESCRIPTIONS = {
      new FactoryDescription(Ttl7400.class, S.getter("TTL7400"), "ttl.gif"),
      new FactoryDescription(Ttl7402.class, S.getter("TTL7402"), "ttl.gif"),
      new FactoryDescription(Ttl7404.class, S.getter("TTL7404"), "ttl.gif"),
      new FactoryDescription(Ttl7408.class, S.getter("TTL7408"), "ttl.gif"),
      new FactoryDescription(Ttl7410.class, S.getter("TTL7410"), "ttl.gif"),
      new FactoryDescription(Ttl7411.class, S.getter("TTL7411"), "ttl.gif"),
      new FactoryDescription(Ttl7413.class, S.getter("TTL7413"), "ttl.gif"),
      new FactoryDescription(Ttl7414.class, S.getter("TTL7414"), "ttl.gif"),
      new FactoryDescription(Ttl7418.class, S.getter("TTL7418"), "ttl.gif"),
      new FactoryDescription(Ttl7419.class, S.getter("TTL7419"), "ttl.gif"),
      new FactoryDescription(Ttl7420.class, S.getter("TTL7420"), "ttl.gif"),
      new FactoryDescription(Ttl7421.class, S.getter("TTL7421"), "ttl.gif"),
      new FactoryDescription(Ttl7424.class, S.getter("TTL7424"), "ttl.gif"),
      new FactoryDescription(Ttl7427.class, S.getter("TTL7427"), "ttl.gif"),
      new FactoryDescription(Ttl7430.class, S.getter("TTL7430"), "ttl.gif"),
      new FactoryDescription(Ttl7432.class, S.getter("TTL7432"), "ttl.gif"),
      new FactoryDescription(Ttl7434.class, S.getter("TTL7434"), "ttl.gif"),
      new FactoryDescription(Ttl7436.class, S.getter("TTL7436"), "ttl.gif"),
      new FactoryDescription(Ttl7442.class, S.getter("TTL7442"), "ttl.gif"),
      new FactoryDescription(Ttl7443.class, S.getter("TTL7443"), "ttl.gif"),
      new FactoryDescription(Ttl7444.class, S.getter("TTL7444"), "ttl.gif"),
      new FactoryDescription(Ttl7447.class, S.getter("TTL7447"), "ttl.gif"),
      new FactoryDescription(Ttl7451.class, S.getter("TTL7451"), "ttl.gif"),
      new FactoryDescription(Ttl7454.class, S.getter("TTL7454"), "ttl.gif"),
      new FactoryDescription(Ttl7458.class, S.getter("TTL7458"), "ttl.gif"),
      new FactoryDescription(Ttl7464.class, S.getter("TTL7464"), "ttl.gif"),
      new FactoryDescription(Ttl7474.class, S.getter("TTL7474"), "ttl.gif"),
      new FactoryDescription(Ttl7485.class, S.getter("TTL7485"), "ttl.gif"),
      new FactoryDescription(Ttl7486.class, S.getter("TTL7486"), "ttl.gif"),
      new FactoryDescription(Ttl74125.class, S.getter("TTL74125"), "ttl.gif"),
      new FactoryDescription(Ttl74139.class, S.getter("TTL74139"), "ttl.gif"),
      new FactoryDescription(Ttl74175.class, S.getter("TTL74175"), "ttl.gif"),
      new FactoryDescription(Ttl74161.class, S.getter("TTL74161"), "ttl.gif"),
      new FactoryDescription(Ttl74163.class, S.getter("TTL74163"), "ttl.gif"),
      new FactoryDescription(Ttl74165.class, S.getter("TTL74165"), "ttl.gif"),
      new FactoryDescription(Ttl74266.class, S.getter("TTL74266"), "ttl.gif"),
      new FactoryDescription(Ttl74273.class, S.getter("TTL74273"), "ttl.gif"),
      new FactoryDescription(Ttl74283.class, S.getter("TTL74283"), "ttl.gif"),
      new FactoryDescription(Ttl74377.class, S.getter("TTL74377"), "ttl.gif"),
  };

  static final Attribute<Boolean> VCC_GND =
      Attributes.forBoolean("VccGndPorts", S.getter("VccGndPorts"));
  static final Attribute<Boolean> DRAW_INTERNAL_STRUCTURE =
      Attributes.forBoolean("ShowInternalStructure", S.getter("ShowInternalStructure"));

  private List<Tool> tools = null;

  @Override
  public List<? extends Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(TtlLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
