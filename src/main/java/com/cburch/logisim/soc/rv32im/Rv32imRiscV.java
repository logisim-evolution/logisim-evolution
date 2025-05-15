/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.rv32im;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElement.Path;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
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
import java.awt.Graphics2D;

public class Rv32imRiscV extends SocInstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Rv32im";

  public Rv32imRiscV() {
    super(_ID, S.getter("Rv32imComponent"), SOC_MASTER);
    setIcon(new ArithmeticIcon("uP", 2));
    setOffsetBounds(Bounds.create(0, 0, 640, 640));
    setInstancePoker(CpuDrawSupport.SimStatePoker.class);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new RV32imAttributes();
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
    int nrOfIrqs = instance.getAttributeValue(RV32imAttributes.RV32IM_STATE).getNrOfIrqs();
    final var ps = new Port[nrOfIrqs + 2];
    ps[0] = new Port(0, 610, Port.INPUT, 1);
    ps[0].setToolTip(S.getter("Rv32imResetInput"));
    ps[1] = new Port(0, 630, Port.INPUT, 1);
    ps[1].setToolTip(S.getter("Rv32imClockInput"));
    for (int i = 0; i < nrOfIrqs; i++) {
      ps[i + 2] = new Port(0, 10 + i * 10, Port.INPUT, 1);
      ps[i + 2].setToolTip(S.getter("Rv32imIrqInput", Integer.toString(i)));
    }
    instance.setPorts(ps);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    final var bds = instance.getBounds();
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
    if (attr == RV32imAttributes.NR_OF_IRQS) {
      updatePorts(instance);
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    }
    super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var loc = painter.getLocation();
    final var g2 = (Graphics2D) painter.getGraphics();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    painter.drawClock(1, Direction.EAST);
    painter.drawPort(0, "Reset", Direction.EAST);
    for (int i = 0;
        i < painter.getAttributeValue(RV32imAttributes.RV32IM_STATE).getNrOfIrqs();
        i++) {
      painter.drawPort(i + 2, "IRQ" + i, Direction.EAST);
    }
    final var f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "RISC V IM simulator", loc.getX() + 320, loc.getY() + 630);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter
        .getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(
            g2,
            Bounds.create(
                loc.getX() + CpuDrawSupport.busConBounds.getX(),
                loc.getY() + CpuDrawSupport.busConBounds.getY(),
                CpuDrawSupport.busConBounds.getWidth(),
                CpuDrawSupport.busConBounds.getHeight()));
    final var state = painter.getAttributeValue(RV32imAttributes.RV32IM_STATE);
    state.paint(
        loc.getX(),
        loc.getY(),
        g2,
        painter.getInstance(),
        painter.getAttributeValue(RV32imAttributes.RV32IM_STATE_VISIBLE),
        painter.getData());
  }

  @Override
  public void propagate(InstanceState state) {
    RV32imState.ProcessorState data = (RV32imState.ProcessorState) state.getData();
    if (data == null) {
      data =
          state.getAttributeValue(RV32imAttributes.RV32IM_STATE).getNewState(state.getInstance());
      state.setData(data);
    }
    if (state.getPortValue(0) == Value.TRUE) data.reset();
    else data.setClock(state.getPortValue(1), ((InstanceStateImpl) state).getCircuitState());
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
    return attrs.getValue(RV32imAttributes.RV32IM_STATE);
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
    return new SocCpuShape(x, y, path);
  }
}
