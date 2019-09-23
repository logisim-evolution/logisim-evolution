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

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

public class SocBus extends SocInstanceFactory {
	
  public static final SocBusMenuProvider MENU_PROVIDER = new SocBusMenuProvider();

  public SocBus() {
    super("SocBus",S.getter("SocBusComponent"),SocBus);
    setIcon(new ArithmeticIcon("SOCBus",3));
  }
  
  @Override
  public AttributeSet createAttributeSet() {
	return new SocBusAttributes();  
  }
  
  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(0, 0, 640, 
    		(attrs.getValue(SocBusAttributes.NrOfTracesAttr).getWidth()+1)*SocBusStateInfo.TraceHeight);
  }
  
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if (attr.equals(SocBusAttributes.NrOfTracesAttr)) {
       instance.recomputeBounds();
    }
  }
  
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    Port[] ps = new Port[1];
    ps[0] = new Port(0,10,Port.INPUT,1);
    ps[0].setToolTip(S.getter("Rv32imResetInput"));
    instance.setPorts(ps);
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
  public void paintInstance(InstancePainter painter) {
    painter.drawBounds();
    painter.drawLabel();
    painter.drawPort(0, "Reset", Direction.EAST);
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "SOC Bus Interconnect", loc.getX()+320, loc.getY()+10);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    SocBusInfo info = (SocBusInfo)painter.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    SocBusStateInfo data = info.getSocSimulationManager().getSocBusState(info.getBusId());
    if (data != null)
      data.paint(loc.getX(),loc.getY(),g2,painter.getInstance(),painter.getAttributeValue(SocBusAttributes.SOC_TRACE_VISABLE),painter.getData());
  }

  @Override
  public void propagate(InstanceState state) {
	SocBusInfo info = (SocBusInfo)state.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
    SocBusStateInfo data = info.getSocSimulationManager().getSocBusState(info.getBusId());
    SocBusStateInfo.SocBusState dat = (SocBusStateInfo.SocBusState) state.getData();
    if (dat == null)
      state.setData(data.getNewState(state.getInstance()));
    if (state.getPortValue(0)==Value.TRUE)
      dat.clear();
  }

  @Override
  public boolean providesSubCircuitMenu() {
    return true;
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return MENU_PROVIDER.getMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) { return null; }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }
  
}
