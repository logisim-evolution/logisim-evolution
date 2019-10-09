/**
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

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;


import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElement.Path;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocUpMenuProvider;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.soc.gui.SocCPUShape;
import com.cburch.logisim.tools.MenuExtender;

public class Nios2 extends SocInstanceFactory implements DynamicElementProvider {

  public Nios2() {
    super("Nios2",S.getter("Nios2Component"),SocMaster);
    setIcon(new ArithmeticIcon("uP",2));
    setOffsetBounds(Bounds.create(0, 0, 640, 640));
    setInstancePoker(CpuDrawSupport.SimStatePoker.class);
  }

  @Override
  public boolean providesSubCircuitMenu() {
    return true;
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return SocUpMenuProvider.SOCUPMENUPROVIDER.getMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) { return new SocCPUShape(x,y,path); }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) { return null; }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }
}
