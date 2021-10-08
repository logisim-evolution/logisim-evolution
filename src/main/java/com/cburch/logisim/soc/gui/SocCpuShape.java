/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

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
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SocCpuShape extends DynamicElementWithPoker {

  public SocCpuShape(int x, int y, DynamicElement.Path p) {
    super(
        p,
        Bounds.create(
            x,
            y,
            CpuDrawSupport.simStateBounds.getWidth(),
            CpuDrawSupport.simStateBounds.getHeight()));
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    SocUpStateInterface data = state == null ? null : (SocUpStateInterface) getData(state);
    if (state == null || data == null || data.getSimState() == null) {
      g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
      GraphicsUtil.drawCenteredText(
          g, S.get("RV32ShapeSimControl"), bounds.getCenterX(), bounds.getCenterY());
    } else data.getSimState().paint(g, 0, 0, bounds);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
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
