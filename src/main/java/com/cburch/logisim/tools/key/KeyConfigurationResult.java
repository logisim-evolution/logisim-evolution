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
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.val;

@Data
public class KeyConfigurationResult {
  private final KeyConfigurationEvent event;
  private final Map<Attribute<?>, Object> attributeValues;

  public KeyConfigurationResult(KeyConfigurationEvent event, Attribute<?> attributeValues, Object value) {
    this.event = event;
    val singleMap = new HashMap<Attribute<?>, Object>(1);
    singleMap.put(attributeValues, value);
    this.attributeValues = singleMap;
  }

  public KeyConfigurationResult(KeyConfigurationEvent event, Map<Attribute<?>, Object> values) {
    this.event = event;
    this.attributeValues = values;
  }
}
