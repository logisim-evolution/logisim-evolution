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

package com.cburch.logisim.soc.jtaguart;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Attribute;
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
import com.cburch.logisim.util.GraphicsUtil;

public class JtagUart extends SocInstanceFactory {

  public static final int ClockPin = 0;
  public static final int ResetPin = 1;
  public static final int IRQPin = 2;
  public static final int ReadEnablePin = 3;
  public static final int ClearKeyboardPin = 4;
  public static final int AvailablePin = 5;
  public static final int DataInPin = 6;
  public static final int DataOutPin = 7;
  public static final int WritePin = 8;
  public static final int ClearTtyPin = 9;

	
  public JtagUart() {
    super("SocJtagUart",S.getter("SocJtagUartComponent"),SocSlave);
    setIcon(new ArithmeticIcon("JtagUart",4));
    setOffsetBounds(Bounds.create(0, 0, 300, 60));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new JtagUartAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    Bounds bds = instance.getBounds();
    instance.setTextField(
            StdAttr.LABEL,
            StdAttr.LABEL_FONT,
            bds.getX() + bds.getWidth() + 2,
            bds.getY() + bds.getHeight()/2,
            GraphicsUtil.H_LEFT,
            GraphicsUtil.V_CENTER);
    Port[] ps = new Port[10];
    ps[ClockPin] = new Port(0,50,Port.INPUT,1);
    ps[ClockPin].setToolTip(S.getter("Rv32imClockInput"));
    ps[ResetPin] = new Port(0,30,Port.INPUT,1);
    ps[ResetPin].setToolTip(S.getter("Rv32imResetInput"));
    ps[IRQPin] = new Port(300,50,Port.OUTPUT,1);
    ps[IRQPin].setToolTip(S.getter("SocPioIrqOutput"));
    ps[ReadEnablePin] = new Port(10,0,Port.OUTPUT,1);
    ps[ReadEnablePin].setToolTip(S.getter("JtagUartKeybReadEnable"));
    ps[ClearKeyboardPin] = new Port(20,0,Port.OUTPUT,1);
    ps[ClearKeyboardPin].setToolTip(S.getter("JtagUartClearKeyb"));
    ps[AvailablePin] = new Port(130,0,Port.INPUT,1);
    ps[AvailablePin].setToolTip(S.getter("JtagUartKeybAvailable"));
    ps[DataInPin] = new Port(140,0,Port.INPUT,7);
    ps[DataInPin].setToolTip(S.getter("JtagUartKeybData"));
    ps[DataOutPin] = new Port(160,0,Port.OUTPUT,7);
    ps[DataOutPin].setToolTip(S.getter("JtagUartTtyData"));
    ps[WritePin] = new Port(180,0,Port.OUTPUT,1);
    ps[WritePin].setToolTip(S.getter("JtagUartTtyWrite"));
    ps[ClearTtyPin] = new Port(190,0,Port.OUTPUT,1);
    ps[ClearTtyPin].setToolTip(S.getter("JtagUartTtyClear"));
    instance.setPorts(ps);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    } else super.instanceAttributeChanged(instance, attr);
  }
  
  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    painter.drawBounds();
    painter.drawLabel();
    g2.drawLine(loc.getX(), loc.getY()+20, loc.getX()+200, loc.getY()+20);
    g2.drawLine(loc.getX()+150, loc.getY(), loc.getX()+150, loc.getY()+20);
    g2.drawLine(loc.getX()+200, loc.getY(), loc.getX()+200, loc.getY()+20);
    painter.drawPort(ResetPin, "Reset", Direction.EAST);
    painter.drawClock(ClockPin, Direction.EAST);
    painter.drawPort(IRQPin, "IRQ", Direction.WEST);
    for (int i = ReadEnablePin ; i < 10 ; i++) painter.drawPort(i);
    GraphicsUtil.drawCenteredText(g2, "Keyboard", loc.getX()+75, loc.getY()+10);
    GraphicsUtil.drawCenteredText(g2, "TTY", loc.getX()+175, loc.getY()+10);
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "Jtag Uart", loc.getX()+250, loc.getY()+10);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT).paint(g2, 
    		Bounds.create(loc.getX()+40, loc.getY()+41, 220, 18));
  }
  
  @Override
  public void propagate(InstanceState state) {
    JtagUartState myState = state.getAttributeValue(JtagUartAttributes.JTAG_STATE);
    myState.handleOperations(state);
  }
  
  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(JtagUartAttributes.JTAG_STATE);
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }

}
