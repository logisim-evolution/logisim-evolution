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

package com.cburch.logisim.soc.memory;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.util.GraphicsUtil;

public class SocMemory extends SocInstanceFactory {

  public SocMemory() {
    super("Socmem",S.getter("SocMemoryComponent"),SocSlave);
    setIcon(new ArithmeticIcon("SocMem",3));
    setOffsetBounds(Bounds.create(0, 0, 320, 60));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new SocMemoryAttributes();
  }
  
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    Bounds bds = instance.getBounds();
    instance.setTextField(
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            bds.getX() + bds.getWidth() / 2,
            bds.getY() - 3,
            GraphicsUtil.H_CENTER,
            GraphicsUtil.V_BASELINE);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT || attr == SocMemoryAttributes.MEM_SIZE ||
        attr == SocMemoryAttributes.START_ADDRESS) {
      instance.fireInvalidated();
    }
    super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void propagate(InstanceState state) {
    SocMemoryState.SocMemoryInfo data = (SocMemoryState.SocMemoryInfo) state.getData();
    if (data == null) 
      state.setData(state.getAttributeValue(SocMemoryAttributes.SOCMEM_STATE).getNewState());
  }
  
  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    painter.drawBounds();
    painter.drawLabel();
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "SOC Memory", loc.getX()+160, loc.getY()+10);
    g2.setFont(f);
    GraphicsUtil.drawCenteredText(g2, S.get("SocMemBase")+String.format("0x%08X", painter.getAttributeValue(SocMemoryAttributes.START_ADDRESS)), loc.getX()+80, loc.getY()+30);
    GraphicsUtil.drawCenteredText(g2, S.get("SocMemSizeStr")+getSizeString(painter.getAttributeValue(SocMemoryAttributes.MEM_SIZE)), loc.getX()+240, loc.getY()+30);
    if (painter.isPrintView()) return;
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT).paint(g2, 
    		Bounds.create(loc.getX()+5, loc.getY()+40, 310, 18));
  }
  
  private String getSizeString(BitWidth addr) {
    long size = (long) Math.pow(2, addr.getWidth());
    if (size >= 1048576) {
      size /= 1048576;
      return size+"MB";
    }
    size /= 1024;
    return size+"kB";
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(SocMemoryAttributes.SOCMEM_STATE);
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }
}
