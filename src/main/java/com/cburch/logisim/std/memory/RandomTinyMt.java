/*
 * This file is part of logisim-evolution.
 * 
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

/* 
 * The algorithms used in this file are based on the TinyMt algorithm V1.2
 * that can be found here: http://www.math.sci.hiroshima-u.ac.jp/m-mat/MT/XSADD/index.html
 * 
 * Copyright notice of the original code:
 * 
 * @brief XORSHIFT-ADD: 128-bit internal state pseudorandom number generator.
 *
 * @author Mutsuo Saito (Manieth Corp.)
 * @author Makoto Matsumoto (Hiroshima University)
 *
 * Copyright (c) 2014
 * Mutsuo Saito, Makoto Matsumoto, Manieth Corp.,
 * and Hiroshima University.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

public class RandomTinyMt extends ClockState implements InstanceData {

  private static final int NR_OF_LOOPS = 8;
  private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;
  private long[] state;
  private Value oldResetValue;
    
  public RandomTinyMt(int seed) {
    state = new long[4];
    reset(seed);
  }
  
  public boolean isResetCondition(Value reset) {
    final var isResetCondition = oldResetValue == Value.FALSE && reset == Value.TRUE; 
    oldResetValue = reset;
    return isResetCondition;
  }

  private void periodCertification() {
    if (state[0] == 0 && state[1] == 0 && state[2] == 0 && state[3] == 0) {
      state[0] = 88;
      state[1] = 83;
      state[2] = 65;
      state[3] = 68;
    }
  }
  
  public void step() {
    var newValue = state[0];
    newValue ^= (newValue << 15) & UNSIGNED_INT_MASK;
    newValue ^= (newValue >> 18) & UNSIGNED_INT_MASK;
    newValue ^= (state[3] << 11) & UNSIGNED_INT_MASK;
    state[0] = state[1];
    state[1] = state[2];
    state[2] = state[3];
    state[3] = newValue;
    periodCertification();
  }
  
  public long getValue() {
    final var value1 = (state[1] << 32) | state[0];
    final var value2 = (state[3] << 32) | state[2];
    return value1 ^ value2;
  }
    
  public void reset(int seed) {
    state[0] = seed;
    state[1] = state[2] = state[3] = 0;
    for (var currentLoop = 0 ; currentLoop < NR_OF_LOOPS; currentLoop++) {
      var poliValue = 1812433253L + currentLoop;
      final var selectedStateValue = state[(currentLoop - 1) & 3]; 
      poliValue *= (selectedStateValue ^ (selectedStateValue >> 30));
      poliValue &= UNSIGNED_INT_MASK;
      state[currentLoop & 3] ^= poliValue;
    }
    periodCertification();
    for (var currentLoop = 0 ; currentLoop < NR_OF_LOOPS; currentLoop++) step();
  }
}
