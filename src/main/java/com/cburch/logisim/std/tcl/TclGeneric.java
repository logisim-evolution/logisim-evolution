/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.hdl.VhdlContentComponent;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Window;
import java.util.WeakHashMap;

/**
 * The TclGeneric component is a standard TclComponent but who has its interface defined by a VHDL
 * entitiy. This way, you can create any TCL component you may need.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclGeneric extends TclComponent {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "TclGeneric";

  static class ContentAttribute extends Attribute<VhdlContentComponent> {

    public ContentAttribute() {
      super("content", S.getter("tclInterfaceDefinition"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, VhdlContentComponent value) {
      final var proj =
          source instanceof com.cburch.logisim.gui.main.Frame frame
              ? frame.getProject()
              : null;
      return TclGenericAttributes.getContentEditor(source, value, proj);
    }

    @Override
    public VhdlContentComponent parse(String value) {
      final var content = VhdlContentComponent.create();
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

    final Instance instance;

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

  private final WeakHashMap<Instance, TclGenericListener> contentListeners;

  public TclGeneric() {
    super(_ID, S.getter("tclGeneric"));

    contentListeners = new WeakHashMap<>();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    final var content = instance.getAttributeValue(CONTENT_ATTR);
    final var listener = new TclGenericListener(instance);

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
    final var content = attrs.getValue(CONTENT_ATTR);
    final var nbInputs = content.getInputsNumber();
    final var nbOutputs = content.getOutputsNumber();

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
    final var g = painter.getGraphics();
    final var content = painter.getAttributeValue(CONTENT_ATTR);
    var metric = g.getFontMetrics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    final var bds = painter.getBounds();
    final var x0 = bds.getX() + (bds.getWidth() / 2);
    final var y0 = bds.getY() + metric.getHeight() + 12;
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(content.getName(), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    final var glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      final var font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    final var inputs = content.getInputs();
    final var outputs = content.getOutputs();

    for (var i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getName(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (var i = 0; i < outputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(outputs[i].getName(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + WIDTH - 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_CENTER);

    painter.drawBounds();
    painter.drawPorts();
  }

  @Override
  void updatePorts(Instance instance) {
    final var content = instance.getAttributeValue(CONTENT_ATTR);
    HdlModel.PortDescription[] inputs = content.getInputs();
    HdlModel.PortDescription[] outputs = content.getOutputs();

    Port[] result = new Port[inputs.length + outputs.length];
    int resultIndex = 0;

    int i = 0;
    for (var desc : inputs) {
      result[resultIndex] =
          new Port(
              0,
              (i * PORT_GAP) + HEIGHT,
              desc.getType(),
              desc.getWidth());
      result[resultIndex].setToolTip(S.getter(desc.getName()));
      resultIndex++;
      i++;
    }

    i = 0;
    for (var desc : outputs) {
      result[resultIndex] =
          new Port(
              WIDTH,
              (i * PORT_GAP) + HEIGHT,
              desc.getType(),
              desc.getWidth());
      result[resultIndex].setToolTip(S.getter(desc.getName()));
      resultIndex++;
      i++;
    }

    instance.setPorts(result);
    setPorts(result);
  }
}
