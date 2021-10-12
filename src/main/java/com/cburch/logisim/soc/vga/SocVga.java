/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.vga;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElement.Path;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.AttributeSet;
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
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Font;
import java.awt.Graphics;

public class SocVga extends SocInstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "SocVga";

  public SocVga() {
    super(_ID, S.getter("SocVgaComponent"), SOC_SLAVE | SOC_SNIFFER | SOC_MASTER);
    setIcon(new ArithmeticIcon("SocVGA", 3));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new VgaAttributes();
  }

  @Override
  public void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    setTextField(instance);
  }

  public void setTextField(Instance instance) {
    instance.recomputeBounds();
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
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    return VgaState.getSize(VgaAttributes.getModeIndex(attrsBase.getValue(VgaAttributes.VGA_STATE).getCurrentMode()));
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return new VgaMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public void propagate(InstanceState state) {
    if (state.getData() == null)
      state.setData(state.getAttributeValue(VgaAttributes.VGA_STATE).getNewState());
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Bounds bds1 = painter.getBounds();
    Bounds bds2 = getOffsetBounds(painter.getAttributeSet());
    if (bds1.getWidth() != bds2.getWidth() || bds1.getHeight() != bds2.getHeight())
      setTextField(painter.getInstance());
    painter.drawBounds();
    painter.drawLabel();
    Graphics g = painter.getGraphics().create();
    Location loc = painter.getLocation();
    Bounds bds = painter.getBounds();
    g.translate(loc.getX(), loc.getY());
    Font f = g.getFont();
    g.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g, "SOC VGA", bds.getWidth() / 2, 10);
    g.setFont(f);
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(g, Bounds.create(VgaState.LEFT_MARGIN, bds.getHeight() - VgaState.BOTTOM_MARGIN + 1,
                bds.getWidth() - VgaState.LEFT_MARGIN - VgaState.RIGHT_MARGIN, VgaState.BOTTOM_MARGIN - 2));
    VgaState.VgaDisplayState data = (VgaState.VgaDisplayState) painter.getData();
    if (data != null)
      data.paint(g, painter.getCircuitState());
    g.dispose();
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(VgaAttributes.VGA_STATE);
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) {
    return attrs.getValue(VgaAttributes.VGA_STATE);
  }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
    return new SocVgaShape(x, y, path);
  }
}
