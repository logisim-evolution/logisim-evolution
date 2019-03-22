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

package com.cburch.logisim.std.tcl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlContentComponent;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import java.util.WeakHashMap;

/**
 * The TclGeneric component is a standard TclComponent but who has its interface defined by a VHDL
 * entitiy. This way, you can create any TCL component you may need.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclGeneric extends TclComponent {

  static class ContentAttribute extends Attribute<VhdlContentComponent> {

    public ContentAttribute() {
      super("content", S.getter("tclInterfaceDefinition"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, VhdlContentComponent value) {
      Project proj =
          source instanceof com.cburch.logisim.gui.main.Frame
              ? ((com.cburch.logisim.gui.main.Frame) source).getProject()
              : null;
      return TclGenericAttributes.getContentEditor(source, value, proj);
    }

    @Override
    public VhdlContentComponent parse(String value) {
      VhdlContentComponent content = VhdlContentComponent.create();
      if (!content.compare(value)) content.setContent(value);
      return content;
    }

    @Override
    public String toDisplayString(VhdlContentComponent value) {
      return S.get("tclInterfaceDefinitionValue");
    }

    @Override
    public String toStandardString(VhdlContentComponent value) {
      return value.getContent();
    }
  }

  static class TclGenericListener implements HdlModelListener {

    Instance instance;

    TclGenericListener(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void contentSet(HdlModel source) {
      instance.fireInvalidated();
      instance.recomputeBounds();
    }
  }

  static final Attribute<VhdlContentComponent> CONTENT_ATTR = new ContentAttribute();

  private WeakHashMap<Instance, TclGenericListener> contentListeners;

  public TclGeneric() {
    super("TclGeneric", S.getter("tclGeneric"));

    contentListeners = new WeakHashMap<Instance, TclGenericListener>();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    VhdlContentComponent content = instance.getAttributeValue(CONTENT_ATTR);
    TclGenericListener listener = new TclGenericListener(instance);

    contentListeners.put(instance, listener);
    content.addHdlModelListener(listener);

    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new TclGenericAttributes();
  }

  @Override
  public String getDisplayName() {
    return S.get("tclGeneric");
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    VhdlContentComponent content = attrs.getValue(CONTENT_ATTR);
    int nbInputs = content.getInputsNumber();
    int nbOutputs = content.getOutputsNumber();

    return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs) * PORT_GAP + HEIGHT);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == CONTENT_ATTR) {
      updatePorts(instance);
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    VhdlContentComponent content = painter.getAttributeValue(CONTENT_ATTR);
    FontMetrics metric = g.getFontMetrics();

    Bounds bds = painter.getBounds();
    int x0 = bds.getX() + (bds.getWidth() / 2);
    int y0 = bds.getY() + metric.getHeight() + 12;
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(content.getName(), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    String glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      Font font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(
          g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setColor(Color.GRAY);
    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    Port[] inputs = content.getInputs();
    Port[] outputs = content.getOutputs();

    for (int i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (int i = 0; i < outputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(outputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + WIDTH - 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_CENTER);

    painter.drawBounds();
    painter.drawPorts();
  }

  @Override
  void updatePorts(Instance instance) {
    VhdlContentComponent content = instance.getAttributeValue(CONTENT_ATTR);
    instance.setPorts(content.getPorts());
    setPorts(content.getPorts());
  }
}
