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

package com.cburch.logisim.soc.pio;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

public class SocPio extends SocInstanceFactory {

  public static final int ResetIndex = 0;
  public static final int IRQIndex = 1;

  public SocPio() {
    super("SocPio",S.getter("SocPioComponent"),SocSlave);
    setIcon(new ArithmeticIcon("SocPIO",3));
    setOffsetBounds(Bounds.create(0, 0, 380, 120));
  }
  
  @Override
  public AttributeSet createAttributeSet() {
    return new PioAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    Bounds bds = instance.getBounds();
    instance.setTextField(
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            bds.getX() + bds.getWidth() + 2,
            bds.getY() + bds.getHeight()/2,
            GraphicsUtil.H_LEFT,
            GraphicsUtil.V_CENTER);
  }

  private void updatePorts(Instance instance) {
    int nrBits = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
    int nrOfPorts = nrBits;
    AttributeOption dir = instance.getAttributeValue(PioAttributes.PIO_DIRECTION);
    boolean hasIrq = hasIrqPin(instance.getAttributeSet()); 
    if (dir==PioAttributes.PORT_INOUT)
      nrOfPorts *= 2;
    int index = hasIrq ? 2 : 1;
    nrOfPorts += index;
    Port[] ps = new Port[nrOfPorts];
    if (hasIrq) {
      ps[IRQIndex] = new Port(20,0,Port.OUTPUT,1);
      ps[IRQIndex].setToolTip(S.getter("SocPioIrqOutput"));
    }
    ps[ResetIndex] = new Port(0,110,Port.INPUT,1);
    ps[ResetIndex].setToolTip(S.getter("SocPioResetInput"));
    if (dir == PioAttributes.PORT_INPUT || dir == PioAttributes.PORT_INOUT) {
      for (int b = 0 ; b < nrBits ; b++) {
        ps[index+b] = new Port(370-b*10,120,Port.INPUT,1);
        ps[index+b].setToolTip(S.getter("SocPioInputPinx", Integer.toString(b)));
      }
      index += nrBits;
    }
    if (dir == PioAttributes.PORT_INOUT || dir == PioAttributes.PORT_OUTPUT || dir == PioAttributes.PORT_BIDIR) {
      String PortType = (dir == PioAttributes.PORT_BIDIR) ? Port.INOUT : Port.OUTPUT;
      for (int b = 0 ; b < nrBits ; b++) {
        ps[index+b] = new Port(370-b*10,0,PortType,1);
        if (dir == PioAttributes.PORT_BIDIR)
          ps[index+b].setToolTip(S.getter("SocPioBidirPinx", Integer.toString(b)));
        else
          ps[index+b].setToolTip(S.getter("SocPioOutputPinx", Integer.toString(b)));
      }
    }
    instance.setPorts(ps);
  }
  
  private boolean hasIrqPin(AttributeSet attrs) {
    if (!attrs.containsAttribute(PioAttributes.PIO_GEN_IRQ))
      return false;
    return attrs.getValue(PioAttributes.PIO_GEN_IRQ);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    } else 
    if (attr == StdAttr.WIDTH || attr == PioAttributes.PIO_DIRECTION ||
        attr == PioAttributes.PIO_GEN_IRQ) {
      updatePorts(instance);
    }
    super.instanceAttributeChanged(instance, attr);
  }
  
  private void paintPins(InstancePainter painter, Graphics2D g2, Location loc) {
    painter.drawPort(ResetIndex, "Reset", Direction.EAST);
    int nrBits = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
    AttributeOption dir = painter.getAttributeValue(PioAttributes.PIO_DIRECTION);
    int index = 1;
    if (hasIrqPin(painter.getAttributeSet())) {
      index++;
      painter.drawPort(IRQIndex, "IRQ", Direction.NORTH);
    }
    if (dir == PioAttributes.PORT_INPUT || dir == PioAttributes.PORT_INOUT) {
      for (int b = 0 ; b < nrBits ; b++) 
        painter.drawPort(index+b);
      if (!painter.isPrintView()) {
        index += nrBits;
        g2.drawRect(loc.getX()+40, loc.getY()+80, 340, 40);
        GraphicsUtil.drawCenteredText(g2, S.get("SocPioInputs"), loc.getX()+210, loc.getY()+95);
        GraphicsUtil.drawCenteredText(g2, "0", loc.getX()+370, loc.getY()+110);
        if (nrBits > 9) {
          GraphicsUtil.drawCenteredText(g2, Integer.toString(nrBits-1), loc.getX()+380-nrBits*10, loc.getY()+110);
        } else {
          for (int b = 1 ;  b < nrBits ; b++)
            GraphicsUtil.drawCenteredText(g2, Integer.toString(b), loc.getX()+370-b*10, loc.getY()+110);
        }
      }
    }
    if (dir == PioAttributes.PORT_INOUT || dir == PioAttributes.PORT_OUTPUT || dir == PioAttributes.PORT_BIDIR) {
      for (int b = 0 ; b < nrBits ; b++) 
        painter.drawPort(index+b);
      if (!painter.isPrintView()) {
        String name = (dir == PioAttributes.PORT_BIDIR) ? S.get("SocPioBidirs") : S.get("SocPioOutputs");
        g2.drawRect(loc.getX()+40, loc.getY(), 340, 40);
        GraphicsUtil.drawCenteredText(g2, name, loc.getX()+210, loc.getY()+25);
        GraphicsUtil.drawCenteredText(g2, "0", loc.getX()+370, loc.getY()+10);
        if (nrBits > 9) {
          GraphicsUtil.drawCenteredText(g2, Integer.toString(nrBits-1), loc.getX()+380-nrBits*10, loc.getY()+10);
        } else {
          for (int b = 1 ;  b < nrBits ; b++)
            GraphicsUtil.drawCenteredText(g2, Integer.toString(b), loc.getX()+370-b*10, loc.getY()+10);
        }
      }
    }
  }

  @Override
  public void propagate(InstanceState state) {
    PioState myState = state.getAttributeValue(PioAttributes.PIO_STATE);
    myState.handleOperations(state,false);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    painter.drawBounds();
    painter.drawLabel();
    paintPins(painter,g2,loc);
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "SOC parallel IO", loc.getX()+210, loc.getY()+50);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT).paint(g2, 
    		Bounds.create(loc.getX()+45, loc.getY()+61, 330, 18));
  }
  
  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return new PioMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(PioAttributes.PIO_STATE);
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }
}
