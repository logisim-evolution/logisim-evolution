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

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class TTL extends Library {
  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("7400", S.getter("TTL7400"), "ttl.gif", Ttl7400.class),
    new FactoryDescription("7402", S.getter("TTL7402"), "ttl.gif", Ttl7402.class),
    new FactoryDescription("7404", S.getter("TTL7404"), "ttl.gif", Ttl7404.class),
    new FactoryDescription("7408", S.getter("TTL7408"), "ttl.gif", Ttl7408.class),
    new FactoryDescription("7410", S.getter("TTL7410"), "ttl.gif", Ttl7410.class),
    new FactoryDescription("7411", S.getter("TTL7411"), "ttl.gif", Ttl7411.class),
    new FactoryDescription("7413", S.getter("TTL7413"), "ttl.gif", Ttl7413.class),
    new FactoryDescription("7414", S.getter("TTL7414"), "ttl.gif", Ttl7414.class),
    new FactoryDescription("7418", S.getter("TTL7418"), "ttl.gif", Ttl7418.class),
    new FactoryDescription("7419", S.getter("TTL7419"), "ttl.gif", Ttl7419.class),
    new FactoryDescription("7420", S.getter("TTL7420"), "ttl.gif", Ttl7420.class),
    new FactoryDescription("7421", S.getter("TTL7421"), "ttl.gif", Ttl7421.class),
    new FactoryDescription("7424", S.getter("TTL7424"), "ttl.gif", Ttl7424.class),
    new FactoryDescription("7427", S.getter("TTL7427"), "ttl.gif", Ttl7427.class),
    new FactoryDescription("7430", S.getter("TTL7430"), "ttl.gif", Ttl7430.class),
    new FactoryDescription("7432", S.getter("TTL7432"), "ttl.gif", Ttl7432.class),
    new FactoryDescription("7434", S.getter("TTL7434"), "ttl.gif", Ttl7434.class),
    new FactoryDescription("7436", S.getter("TTL7436"), "ttl.gif", Ttl7436.class),
    new FactoryDescription("7442", S.getter("TTL7442"), "ttl.gif", Ttl7442.class),
    new FactoryDescription("7443", S.getter("TTL7443"), "ttl.gif", Ttl7443.class),
    new FactoryDescription("7444", S.getter("TTL7444"), "ttl.gif", Ttl7444.class),
    new FactoryDescription("7447", S.getter("TTL7447"), "ttl.gif", Ttl7447.class),
    new FactoryDescription("7451", S.getter("TTL7451"), "ttl.gif", Ttl7451.class),
    new FactoryDescription("7454", S.getter("TTL7454"), "ttl.gif", Ttl7454.class),
    new FactoryDescription("7458", S.getter("TTL7458"), "ttl.gif", Ttl7458.class),
    new FactoryDescription("7464", S.getter("TTL7464"), "ttl.gif", Ttl7464.class),
    new FactoryDescription("7474", S.getter("TTL7474"), "ttl.gif", Ttl7474.class),
    new FactoryDescription("7485", S.getter("TTL7485"), "ttl.gif", Ttl7485.class),
    new FactoryDescription("7486", S.getter("TTL7486"), "ttl.gif", Ttl7486.class),
    new FactoryDescription("74125", S.getter("TTL74125"),"ttl.gif", Ttl74125.class),
    new FactoryDescription("74139", S.getter("TTL74139"),"ttl.gif", Ttl74139.class),
    new FactoryDescription("74175", S.getter("TTL74175"), "ttl.gif", Ttl74175.class),
    new FactoryDescription("74161", S.getter("TTL74161"), "ttl.gif", Ttl74161.class),
    new FactoryDescription("74165", S.getter("TTL74165"), "ttl.gif", Ttl74165.class),
    new FactoryDescription("74266", S.getter("TTL74266"), "ttl.gif", Ttl74266.class),
    new FactoryDescription("74273", S.getter("TTL74273"), "ttl.gif", Ttl74273.class),
    new FactoryDescription("74283", S.getter("TTL74283"), "ttl.gif", Ttl74283.class),
    new FactoryDescription("74377", S.getter("TTL74377"), "ttl.gif", Ttl74377.class),
  };

  static final Attribute<Boolean> VCC_GND =
      Attributes.forBoolean("VccGndPorts", S.getter("VccGndPorts"));
  static final Attribute<Boolean> DRAW_INTERNAL_STRUCTURE =
      Attributes.forBoolean("ShowInternalStructure", S.getter("ShowInternalStructure"));

  private List<Tool> tools = null;

  public TTL() {}

  @Override
  public String getName() {
    return "TTL";
  }

  @Override
  public List<? extends Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(TTL.class, DESCRIPTIONS);
    }
    return tools;
  }

  @Override
  public boolean removeLibrary(String name) {
    // TODO Auto-generated method stub
    return false;
  }
}
