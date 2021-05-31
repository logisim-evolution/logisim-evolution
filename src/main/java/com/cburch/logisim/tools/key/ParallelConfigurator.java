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

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import java.util.HashMap;

public class ParallelConfigurator implements KeyConfigurator, Cloneable {
  public static ParallelConfigurator create(KeyConfigurator a, KeyConfigurator b) {
    return new ParallelConfigurator(new KeyConfigurator[] {a, b});
  }

  public static ParallelConfigurator create(KeyConfigurator[] configs) {
    return new ParallelConfigurator(configs);
  }

  private KeyConfigurator[] handlers;

  private ParallelConfigurator(KeyConfigurator[] handlers) {
    this.handlers = handlers;
  }

  @Override
  public ParallelConfigurator clone() {
    ParallelConfigurator ret;
    try {
      ret = (ParallelConfigurator) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
    int len = this.handlers.length;
    ret.handlers = new KeyConfigurator[len];
    for (int i = 0; i < len; i++) {
      ret.handlers[i] = this.handlers[i].clone();
    }
    return ret;
  }

  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    KeyConfigurator[] hs = handlers;
    if (event.isConsumed()) {
      return null;
    }
    KeyConfigurationResult first = null;
    HashMap<Attribute<?>, Object> map = null;
    for (KeyConfigurator h : hs) {
      KeyConfigurationResult result = h.keyEventReceived(event);
      if (result != null) {
        if (first == null) {
          first = result;
        } else if (map == null) {
          map = new HashMap<>(first.getAttributeValues());
          map.putAll(result.getAttributeValues());
        } else {
          map.putAll(result.getAttributeValues());
        }
      }
    }
    if (map != null) {
      return new KeyConfigurationResult(event, map);
    } else {
      return first;
    }
  }
}
