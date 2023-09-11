/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.nios2;

import static com.cburch.logisim.soc.Strings.S;

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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.soc.data.SocUpMenuProvider;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.soc.gui.SocCpuShape;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Nios2 extends SocInstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Nios2";

  public static final int CLOCK = 0;
  public static final int RESET = 1;
  public static final int DATAA = 2;
  public static final int DATAB = 3;
  public static final int START = 4;
  public static final int N = 5;
  public static final int A = 6;
  public static final int READRA = 7;
  public static final int B = 8;
  public static final int READRB = 9;
  public static final int C = 10;
  public static final int WRITERC = 11;
  public static final int RESULT = 12;
  public static final int DONE = 13;
  public static final int IRQSTART = 14;

  private static final String[] pinName = {
    "clock", "reset", "dataa", "datab", "start", "n", "a", "readra", "b", "readrb", "c", "writerc",
    "result", "done"
  };

  public Nios2() {
    super(_ID, S.getter("Nios2Component"), SOC_MASTER);
    setIcon(new ArithmeticIcon("uP", 2));
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
    Port[] ps = new Port[NrOfIrqs + IRQSTART];
    ps[RESET] = new Port(0, 620, Port.INPUT, 1);
    ps[RESET].setToolTip(S.getter("Rv32imResetInput"));
    ps[CLOCK] = new Port(0, 640, Port.INPUT, 1);
    ps[CLOCK].setToolTip(S.getter("Rv32imClockInput"));
    ps[DATAA] = new Port(30, 0, Port.OUTPUT, 32);
    ps[DATAA].setToolTip(S.getter("Nios2Dataa"));
    ps[DATAB] = new Port(80, 0, Port.OUTPUT, 32);
    ps[DATAB].setToolTip(S.getter("Nios2Datab"));
    ps[START] = new Port(130, 0, Port.OUTPUT, 1);
    ps[START].setToolTip(S.getter("Nios2Start"));
    ps[N] = new Port(180, 0, Port.OUTPUT, 8);
    ps[N].setToolTip(S.getter("Nios2N"));
    ps[A] = new Port(230, 0, Port.OUTPUT, 5);
    ps[A].setToolTip(S.getter("Nios2A"));
    ps[READRA] = new Port(280, 0, Port.OUTPUT, 1);
    ps[READRA].setToolTip(S.getter("Nios2ReadRa"));
    ps[B] = new Port(330, 0, Port.OUTPUT, 5);
    ps[B].setToolTip(S.getter("Nios2B"));
    ps[READRB] = new Port(380, 0, Port.OUTPUT, 1);
    ps[READRB].setToolTip(S.getter("Nios2ReadRb"));
    ps[C] = new Port(430, 0, Port.OUTPUT, 5);
    ps[C].setToolTip(S.getter("Nios2C"));
    ps[WRITERC] = new Port(480, 0, Port.OUTPUT, 1);
    ps[WRITERC].setToolTip(S.getter("Nios2WriteRc"));
    ps[DONE] = new Port(560, 0, Port.INPUT, 1);
    ps[DONE].setToolTip(S.getter("Nios2Done"));
    ps[RESULT] = new Port(610, 0, Port.INPUT, 32);
    ps[RESULT].setToolTip(S.getter("Nios2Result"));
    for (int i = 0; i < NrOfIrqs; i++) {
      ps[i + IRQSTART] = new Port(0, 40 + i * 10, Port.INPUT, 1);
      ps[i + IRQSTART].setToolTip(S.getter("Rv32imIrqInput", Integer.toString(i)));
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
        bds.getY() + bds.getHeight() + 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_TOP);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == Nios2Attributes.NR_OF_IRQS) {
      updatePorts(instance);
      SocUpMenuProvider.SOCUPMENUPROVIDER.repaintStates(instance);
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
      SocUpMenuProvider.SOCUPMENUPROVIDER.repaintStates(instance);
    }
    super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Location loc = painter.getLocation();
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    painter.drawClock(CLOCK, Direction.EAST);
    painter.drawPort(RESET, "Reset", Direction.EAST);
    for (int i = DATAA; i < IRQSTART; i++) painter.drawPort(i, pinName[i], Direction.NORTH);
    for (int i = 0; i < painter.getAttributeValue(Nios2Attributes.NIOS2_STATE).getNrOfIrqs(); i++) {
      painter.drawPort(i + IRQSTART, "IRQ" + i, Direction.EAST);
    }
    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "Nios2s simulator", loc.getX() + 320, loc.getY() + 640);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter
        .getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(
            g2,
            Bounds.create(
                loc.getX() + CpuDrawSupport.busConBounds.getX(),
                loc.getY() + CpuDrawSupport.busConBounds.getY() + 10,
                CpuDrawSupport.busConBounds.getWidth(),
                CpuDrawSupport.busConBounds.getHeight()));
    Nios2State state = painter.getAttributeValue(Nios2Attributes.NIOS2_STATE);
    state.paint(
        loc.getX(),
        loc.getY() + 10,
        g2,
        painter.getInstance(),
        painter.getAttributeValue(Nios2Attributes.NIOS_STATE_VISIBLE),
        painter.getData());
  }

  @Override
  public void propagate(InstanceState state) {
    Nios2State.ProcessorState data = (Nios2State.ProcessorState) state.getData();
    if (data == null) {
      data = state.getAttributeValue(Nios2Attributes.NIOS2_STATE).getNewState(state.getInstance());
      state.setData(data);
    }
    if (state.getPortValue(RESET) == Value.TRUE) data.reset();
    else data.setClock(state.getPortValue(CLOCK), ((InstanceStateImpl) state).getCircuitState());
    /* update Irqs */
    int irqs = 0;
    for (int i = 0; i < state.getAttributeValue(Nios2Attributes.NR_OF_IRQS).getWidth(); i++) {
      if (state.getPortValue(i + IRQSTART) == Value.TRUE) irqs |= 1 << i;
    }
    data.setIpending(irqs);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
    return new SocCpuShape(x, y, path);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) {
    return attrs.getValue(Nios2Attributes.NIOS2_STATE);
  }
}
