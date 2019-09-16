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

package com.cburch.logisim.soc.vga;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Font;
import java.awt.Graphics;

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

public class SocVga extends SocInstanceFactory implements DynamicElementProvider {

  public SocVga() {
    super("SocVga",S.getter("SocVgaComponent"),SocSlave|SocSniffer|SocMaster);
    setIcon(new ArithmeticIcon("SocVGA",3));
  }
  
  @Override
  public AttributeSet createAttributeSet() { return new VgaAttributes(); }
  
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
    GraphicsUtil.drawCenteredText(g, "SOC VGA", bds.getWidth()/2, 10);
    g.setFont(f);
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT).paint(g, 
            Bounds.create(VgaState.LEFT_MARGIN, bds.getHeight()-VgaState.BOTTOM_MARGIN+1, 
                bds.getWidth()-VgaState.LEFT_MARGIN-VgaState.RIGHT_MARGIN, VgaState.BOTTOM_MARGIN-2));
    VgaState.VgaDisplayState data = (VgaState.VgaDisplayState) painter.getData();
    if (data != null)
      data.paint(g,painter.getCircuitState());
    g.dispose();
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) { return attrs.getValue(VgaAttributes.VGA_STATE); }
  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) { return attrs.getValue(VgaAttributes.VGA_STATE); }
  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) { return null; }

  @Override
  public DynamicElement createDynamicElement(int x, int y, Path path) {
	return new SocVgaShape(x,y,path);
  }
}
