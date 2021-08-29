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

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

public class RandomState extends ClockState implements InstanceData {
  private static final long MULTIPLIER = 0x5DEECE66DL;
  private static final long ADDEND = 0xBL;
  private static final long MASK = (1L << 48) - 1;

  private long initialSeed;
  private long currentSeed;
  private int currentValue;
  private long resetValue;
  private Value oldResetValue;

  public RandomState(Object seed) {
    resetValue = initialSeed = currentSeed = getRandomSeed(seed);
    currentValue = (int) resetValue;
    oldResetValue = Value.UNKNOWN;
  }

  public void propagateReset(Value reset, Object seed) {
    if (oldResetValue == Value.FALSE && reset == Value.TRUE) {
      resetValue = getRandomSeed(seed);
    }
    oldResetValue = reset;
  }

  public void reset(Object seed) {
    initialSeed = resetValue;
    currentSeed = resetValue;
    currentValue = (int) resetValue;
  }
  
  public int getValue() {
    return currentValue;
  }

  private long getRandomSeed(Object seed) {
    var retValue = seed instanceof Integer ? (Integer) seed : 0L;
    if (retValue == 0) {
      retValue = (System.currentTimeMillis() ^ MULTIPLIER) & MASK;
      if (retValue == initialSeed) {
        retValue = (retValue + MULTIPLIER) & MASK;
      }
    }
    return retValue;
  }

  void step() {
    var newValue = currentSeed;
    newValue = (newValue * MULTIPLIER + ADDEND) & MASK;
    currentSeed = newValue;
    currentValue = (int) (newValue >> 12);
  }
}
