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

import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElement.Path;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
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
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.soc.data.SocUpMenuProvider;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.soc.gui.SocCPUShape;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

public class Nios2 extends SocInstanceFactory implements DynamicElementProvider {

  private static int CLOCK = 0;
  private static int RESET = 1;
  private static int DATAA = 2;
  private static int DATAB = 3;
  private static int START = 4;
  private static int N = 5;
  private static int A = 6;
  private static int READRA = 7;
  private static int B = 8;
  private static int READRB = 9;
  private static int C = 10;
  private static int WRITERC = 11;
  private static int RESULT = 12;
  private static int DONE = 13;
  private static int IRQSTART = 14;
  
  private static String[] pinName = {"clock","reset","dataa","datab","start","n","a","readra","b","readrb",
                                     "c","writerc","result","done"};

  public Nios2() {
    super("Nios2",S.getter("Nios2Component"),SocMaster);
    setIcon(new ArithmeticIcon("uP",2));
    setOffsetBounds(Bounds.create(0, 0, 640, 650));
    setInstancePoker(CpuDrawSupport.SimStatePoker.class);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new Nios2Attributes();
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

  private void updatePorts(Instance instance) {
    int NrOfIrqs = instance.getAttributeValue(Nios2Attributes.NIOS2_STATE).getNrOfIrqs();
    Port[] ps = new Port[NrOfIrqs+IRQSTART];
    ps[RESET] = new Port(0,620,Port.INPUT,1);
    ps[RESET].setToolTip(S.getter("Rv32imResetInput"));
    ps[CLOCK] = new Port(0,640,Port.INPUT,1);
    ps[CLOCK].setToolTip(S.getter("Rv32imClockInput"));
    ps[DATAA] = new Port(30,0,Port.OUTPUT,32);
    ps[DATAA].setToolTip(S.getter("Nios2Dataa"));
    ps[DATAB] = new Port(80,0,Port.OUTPUT,32);
    ps[DATAB].setToolTip(S.getter("Nios2Datab"));
    ps[START] = new Port(130,0,Port.OUTPUT,1);
    ps[START].setToolTip(S.getter("Nios2Start"));
    ps[N] = new Port(180,0,Port.OUTPUT,8);
    ps[N].setToolTip(S.getter("Nios2N"));
    ps[A] = new Port(230,0,Port.OUTPUT,5);
    ps[A].setToolTip(S.getter("Nios2A"));
    ps[READRA] = new Port(280,0,Port.OUTPUT,1);
    ps[READRA].setToolTip(S.getter("Nios2ReadRa"));
    ps[B] = new Port(330,0,Port.OUTPUT,5);
    ps[B].setToolTip(S.getter("Nios2A"));
    ps[READRB] = new Port(380,0,Port.OUTPUT,1);
    ps[READRB].setToolTip(S.getter("Nios2ReadRb"));
    ps[C] = new Port(430,0,Port.OUTPUT,5);
    ps[C].setToolTip(S.getter("Nios2A"));
    ps[WRITERC] = new Port(480,0,Port.OUTPUT,1);
    ps[WRITERC].setToolTip(S.getter("Nios2WriteRc"));
    ps[DONE] = new Port(560,0,Port.INPUT,1);
    ps[DONE].setToolTip(S.getter("Nios2Done"));
    ps[RESULT] = new Port(610,0,Port.INPUT,32);
    ps[RESULT].setToolTip(S.getter("Nios2Result"));
    for (int i = 0 ; i < NrOfIrqs ; i++) {
      ps[i+IRQSTART] = new Port(0,40+i*10,Port.INPUT,1);
      ps[i+IRQSTART].setToolTip(S.getter("Rv32imIrqInput", Integer.toString(i)));
    }
    instance.setPorts(ps);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
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
    if (attr == Nios2Attributes.NR_OF_IRQS) {
      updatePorts(instance);
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    }
    super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Location loc = painter.getLocation();
    Graphics2D g2 = (Graphics2D)painter.getGraphics();
    painter.drawBounds();
    painter.drawLabel();
    painter.drawClock(CLOCK, Direction.EAST);
    painter.drawPort(RESET, "Reset", Direction.EAST);
    for (int i = DATAA ; i < IRQSTART ; i++)
      painter.drawPort(i, pinName[i], Direction.NORTH);
    for (int i = 0 ; i < painter.getAttributeValue(Nios2Attributes.NIOS2_STATE).getNrOfIrqs() ; i++) {
      painter.drawPort(i+IRQSTART, "IRQ"+i, Direction.EAST);
    }
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "Nios2s simulator", loc.getX()+320, loc.getY()+640);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT).paint(g2, 
    		Bounds.create(loc.getX()+CpuDrawSupport.busConBounds.getX(), loc.getY()+CpuDrawSupport.busConBounds.getY()+10, 
    				CpuDrawSupport.busConBounds.getWidth(), CpuDrawSupport.busConBounds.getHeight()));
    Nios2State state = painter.getAttributeValue(Nios2Attributes.NIOS2_STATE);
    state.paint(loc.getX(), loc.getY()+10, g2,painter.getInstance(),painter.getAttributeValue(Nios2Attributes.NIOS_STATE_VISIBLE), painter.getData());
  }

  @Override
  public void propagate(InstanceState state) {
    Nios2State.ProcessorState data = (Nios2State.ProcessorState) state.getData(); 
	if (data == null) {
	  data = state.getAttributeValue(Nios2Attributes.NIOS2_STATE).getNewState(state.getInstance());
	  state.setData(data);
	}
	if (state.getPortValue(RESET) == Value.TRUE)
	  data.reset();
	else
	  data.setClock(state.getPortValue(CLOCK), ((InstanceStateImpl)state).getCircuitState());
	/* TODO: Implement correctly the CI of the NIOS2 */
	state.setPort(DATAA, Value.createKnown(32, 0), 5);
	state.setPort(DATAB, Value.createKnown(32, 0), 5);
	state.setPort(START, Value.createKnown(1, 0), 5);
	state.setPort(N, Value.createKnown(8, 0), 5);
	state.setPort(A, Value.createKnown(5, 0), 5);
	state.setPort(READRA, Value.createKnown(1, 0), 5);
	state.setPort(B, Value.createKnown(5, 0), 5);
	state.setPort(READRB, Value.createKnown(1, 0), 5);
	state.setPort(C, Value.createKnown(5, 0), 5);
	state.setPort(WRITERC, Value.createKnown(1, 0), 5);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) { return new SocCPUShape(x,y,path); }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) { return null; }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return null; }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return attrs.getValue(Nios2Attributes.NIOS2_STATE); }
}
