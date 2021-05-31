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

import com.cburch.logisim.data.AttributeSet;
import java.awt.event.KeyEvent;

public class KeyConfigurationEvent {
  public static final int KEY_PRESSED = 0;
  public static final int KEY_RELEASED = 1;
  public static final int KEY_TYPED = 2;

  private final int type;
  private final AttributeSet attrs;
  private final KeyEvent event;
  private final Object data;
  private boolean consumed;

  public KeyConfigurationEvent(int type, AttributeSet attrs, KeyEvent event, Object data) {
    this.type = type;
    this.attrs = attrs;
    this.event = event;
    this.data = data;
    this.consumed = false;
  }

  public void consume() {
    consumed = true;
  }

  public AttributeSet getAttributeSet() {
    return attrs;
  }

  public Object getData() {
    return data;
  }

  public KeyEvent getKeyEvent() {
    return event;
  }

  public int getType() {
    return type;
  }

  public boolean isConsumed() {
    return consumed;
  }
}
