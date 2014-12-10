/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.tools.key;

import java.util.HashMap;
import java.util.Map;

import com.cburch.logisim.data.Attribute;

public class KeyConfigurationResult {
	private KeyConfigurationEvent event;
	private Map<Attribute<?>, Object> attrValueMap;

	public KeyConfigurationResult(KeyConfigurationEvent event,
			Attribute<?> attr, Object value) {
		this.event = event;
		Map<Attribute<?>, Object> singleMap = new HashMap<Attribute<?>, Object>(
				1);
		singleMap.put(attr, value);
		this.attrValueMap = singleMap;
	}

	public KeyConfigurationResult(KeyConfigurationEvent event,
			Map<Attribute<?>, Object> values) {
		this.event = event;
		this.attrValueMap = values;
	}

	public Map<Attribute<?>, Object> getAttributeValues() {
		return attrValueMap;
	}

	public KeyConfigurationEvent getEvent() {
		return event;
	}
}
