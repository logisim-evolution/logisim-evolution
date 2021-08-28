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

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.instance.InstanceComponent;
import java.util.HashSet;
import java.util.Set;

public class simpleDRCContainer {

  public static final int LEVEL_NORMAL = 1;
  public static final int LEVEL_SEVERE = 2;
  public static final int LEVEL_FATAL = 3;
  public static final int MARK_NONE = 0;
  public static final int MARK_INSTANCE = 1;
  public static final int MARK_LABEL = 2;
  public static final int MARK_WIRE = 4;

  private final String Message;
  private final int SeverityLevel;
  private Set<Object> DRCComponents;
  private Circuit MyCircuit;
  private final int markType;
  private int listNumber;
  private final boolean suppressCount;

  public simpleDRCContainer(String Message, int level) {
    this.Message = Message;
    this.SeverityLevel = level;
    markType = MARK_NONE;
    listNumber = 0;
    suppressCount = false;
  }

  public simpleDRCContainer(String message, int level, boolean supressCount) {
    this.Message = message;
    this.SeverityLevel = level;
    markType = MARK_NONE;
    listNumber = 0;
    this.suppressCount = supressCount;
  }

  public simpleDRCContainer(Object message, int level) {
    this.Message = message.toString();
    this.SeverityLevel = level;
    markType = MARK_NONE;
    listNumber = 0;
    suppressCount = false;
  }

  public simpleDRCContainer(Object message, int level, boolean supressCount) {
    this.Message = message.toString();
    this.SeverityLevel = level;
    markType = MARK_NONE;
    listNumber = 0;
    this.suppressCount = supressCount;
  }

  public simpleDRCContainer(Circuit circ, Object message, int level, int markMask) {
    this.Message = message.toString();
    this.SeverityLevel = level;
    MyCircuit = circ;
    markType = markMask;
    listNumber = 0;
    suppressCount = false;
  }

  public simpleDRCContainer(Circuit circ, Object message, int level, int markMask, boolean supressCount) {
    this.Message = message.toString();
    this.SeverityLevel = level;
    MyCircuit = circ;
    markType = markMask;
    listNumber = 0;
    this.suppressCount = supressCount;
  }

  @Override
  public String toString() {
    return Message;
  }

  public int Severity() {
    return SeverityLevel;
  }

  public boolean DRCInfoPresent() {
    if (DRCComponents == null || MyCircuit == null) return false;
    return !DRCComponents.isEmpty();
  }

  public Circuit GetCircuit() {
    return MyCircuit;
  }

  public boolean HasCircuit() {
    return (MyCircuit != null);
  }

  public void AddMarkComponent(Object comp) {
    if (DRCComponents == null) DRCComponents = new HashSet<>();
    DRCComponents.add(comp);
  }

  public void AddMarkComponents(Set<?> set) {
    if (DRCComponents == null) DRCComponents = new HashSet<>();
    DRCComponents.addAll(set);
  }

  public void SetListNumber(int number) {
    listNumber = number;
  }

  public boolean SupressCount() {
    return this.suppressCount;
  }

  public int GetListNumber() {
    return listNumber;
  }

  public void MarkComponents() {
    if (!DRCInfoPresent()) return;
    for (Object obj : DRCComponents) {
      if (obj instanceof Wire) {
        Wire wire = (Wire) obj;
        if ((markType & MARK_WIRE) != 0) {
          wire.SetDRCHighlight(true);
        }
      } else if (obj instanceof Splitter) {
        Splitter split = (Splitter) obj;
        if ((markType & MARK_INSTANCE) != 0) {
          split.SetMarked(true);
        }
      } else if (obj instanceof InstanceComponent) {
        InstanceComponent comp = (InstanceComponent) obj;
        if ((markType & MARK_INSTANCE) != 0) comp.markInstance();
        if ((markType & MARK_LABEL) != 0) comp.markLabel();
      } else {
      }
    }
  }

  public void ClearMarks() {
    if (!DRCInfoPresent()) return;
    for (Object obj : DRCComponents) {
      if (obj instanceof Wire) {
        Wire wire = (Wire) obj;
        if ((markType & MARK_WIRE) != 0) {
          wire.SetDRCHighlight(false);
        }
      } else if (obj instanceof Splitter) {
        Splitter split = (Splitter) obj;
        if ((markType & MARK_INSTANCE) != 0) {
          split.SetMarked(false);
        }
      } else if (obj instanceof InstanceComponent) {
        InstanceComponent comp = (InstanceComponent) obj;
        comp.clearMarks();
      }
    }
  }
}
