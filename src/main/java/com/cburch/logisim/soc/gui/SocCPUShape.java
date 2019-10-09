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

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementWithPoker;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocUpStateInterface;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;

public class SocCPUShape extends DynamicElementWithPoker {

  public SocCPUShape(int x , int y , DynamicElement.Path p) {
    super(p, Bounds.create(x, y, CpuDrawSupport.simStateBounds.getWidth(), CpuDrawSupport.simStateBounds.getHeight()));
  }
  
  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    SocUpStateInterface data = state == null ? null : (SocUpStateInterface) getData(state);
    if (state == null || data == null || data.getSimState() == null) {
      g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
      GraphicsUtil.drawCenteredText(g, S.get("RV32ShapeSimControl"), bounds.getCenterX(), bounds.getCenterY());
    } else data.getSimState().paint(g, 0, 0, bounds);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
          new Attribute<?>[] { ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR });
  }

  @Override
  public String getDisplayName() {
    return "SocCpu";
  }

  @Override
  public Element toSvgElement(Document doc) {
return toSvgElement(doc.createElement("visible-soc-cpu"));
  }

  @Override
  public void performClickAction(InstanceState state, MouseEvent e) {
    if (mouseInside(state, e)) {
      CircuitState cstate = (CircuitState) state.getData();
      SocUpStateInterface data = state == null ? null : (SocUpStateInterface) getData(cstate);
      if (data != null) data.getSimState().buttonPressed();
    }
  }
  
}
