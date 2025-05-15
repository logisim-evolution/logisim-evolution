/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class PlaRom extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "PlaRom";

  private static class ContentsAttribute extends Attribute<String> {
    private InstanceState state = null;
    private Instance instance = null;
    private CircuitState circ = null;

    private ContentsAttribute() {
      super("Contents", S.getter("romContentsAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, String value) {
      Project proj = null;
      if (source instanceof Frame) proj = ((Frame) source).getProject();
      PlaRomData data = null;
      if (this.state != null) data = getPlaRomData(state);
      else if (this.instance != null && this.circ != null) {
        data = getPlaRomData(instance, circ);
      }
      ContentsCell ret = new ContentsCell(data);
      // call mouse click function and open edit window
      ret.mouseClicked(null);
      // if something changed
      if (this.state != null
          && !data.getSavedData().equals(state.getAttributeValue(CONTENTS_ATTR))) {
        state.fireInvalidated();
        state.getAttributeSet().setValue(CONTENTS_ATTR, data.getSavedData());
        if (proj != null) proj.getLogisimFile().setDirty(true);
      } else if (this.instance != null
          && !data.getSavedData().equals(instance.getAttributeValue(CONTENTS_ATTR))) {
        instance.fireInvalidated();
        instance.getAttributeSet().setValue(CONTENTS_ATTR, data.getSavedData());
        if (proj != null) proj.getLogisimFile().setDirty(true);
      }
      return ret;
    }

    @Override
    public String parse(String value) {
      return value;
    }

    // i don't know other ways to do this, I'll try to change this when I'll know
    // better Burch's code
    void setData(Instance instance, CircuitState circ) {
      if (!instance.equals(this.instance)) this.instance = instance;
      if (!circ.equals(this.circ)) this.circ = circ;
    }

    void setData(InstanceState state) {
      if (!state.equals(this.state)) this.state = state;
    }

    @Override
    public String toDisplayString(String value) {
      return S.get("romContentsValue");
    }
  }

  public static class ContentsCell extends JLabel implements BaseMouseListenerContract {
    /** */
    private static final long serialVersionUID = -53754819096800664L;

    private final PlaRomData data;

    ContentsCell(PlaRomData data) {
      super(S.get("romContentsValue"));
      this.data = data;
      addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (data == null) return;
      if (data.editWindow() == 1) data.clearMatrixValues();
    }
  }

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return null;
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      PlaRomData data = getPlaRomData(state);
      return BitWidth.create(data.getOutputs());
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      return state.getPortValue(1);
    }
  }

  private static class PlaMenu implements ActionListener, MenuExtender {
    private JMenuItem edit, clear;
    private final Instance instance;
    private CircuitState circState;

    public PlaMenu(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      PlaRomData data = PlaRom.getPlaRomData(instance, circState);
      if (evt.getSource() == edit) {
        if (data.editWindow() == 1) data.clearMatrixValues();
      } else if (evt.getSource() == clear) data.clearMatrixValues();
      // if something changed
      if (!data.getSavedData().equals(instance.getAttributeValue(CONTENTS_ATTR))) {
        instance.fireInvalidated();
        instance.getAttributeSet().setValue(CONTENTS_ATTR, data.getSavedData());
        circState.getProject().getLogisimFile().setDirty(true);
      }
    }

    @Override
    public void configureMenu(JPopupMenu menu, Project proj) {
      this.circState = proj.getCircuitState();
      boolean enabled = circState != null;

      this.edit = createItem(enabled, S.get("ramEditMenuItem"));
      this.clear = createItem(enabled, S.get("ramClearMenuItem"));
      menu.addSeparator();
      menu.add(this.edit);
      menu.add(this.clear);
    }

    private JMenuItem createItem(boolean enabled, String label) {
      JMenuItem ret = new JMenuItem(label);
      ret.setEnabled(enabled);
      ret.addActionListener(this);
      return ret;
    }
  }

  private static final Attribute<Integer> ATTR_INPUTS =
      Attributes.forIntegerRange("inputs", S.getter("plaBitWidthIn"), 1, 32);

  private static final Attribute<Integer> ATTR_AND =
      Attributes.forIntegerRange("and", S.getter("PlaANDAttr"), 1, 32);

  private static final Attribute<Integer> ATTR_OUTPUTS =
      Attributes.forIntegerRange("outputs", S.getter("plaBitWidthOut"), 1, 32);

  private static final ContentsAttribute CONTENTS_ATTR = new ContentsAttribute();

  public static PlaRomData getPlaRomData(Instance instance, CircuitState state) {
    byte inputs = instance.getAttributeValue(ATTR_INPUTS).byteValue();
    byte outputs = instance.getAttributeValue(ATTR_OUTPUTS).byteValue();
    byte and = instance.getAttributeValue(ATTR_AND).byteValue();
    PlaRomData ret = (PlaRomData) instance.getData(state);
    if (ret == null) {
      ret = new PlaRomData(inputs, outputs, and);
      // if new, fill the content with the saved data
      ret.decodeSavedData(instance.getAttributeValue(CONTENTS_ATTR));
      instance.setData(state, ret);
    } else if (ret.updateSize(inputs, outputs, and)) {
      // if size updated, update the content attribute, written here because can't
      // access PlaRomData object from instanceAttributeChanged method
      instance.getAttributeSet().setValue(CONTENTS_ATTR, ret.getSavedData());
    }
    CONTENTS_ATTR.setData(instance, state);
    return ret;
  }

  public static PlaRomData getPlaRomData(InstanceState state) {
    byte inputs = state.getAttributeValue(ATTR_INPUTS).byteValue();
    byte outputs = state.getAttributeValue(ATTR_OUTPUTS).byteValue();
    byte and = state.getAttributeValue(ATTR_AND).byteValue();
    PlaRomData ret = (PlaRomData) state.getData();
    if (ret == null) {
      ret = new PlaRomData(inputs, outputs, and);
      // if new, fill the content with the saved data
      ret.decodeSavedData(state.getAttributeValue(CONTENTS_ATTR));
      state.setData(ret);
    } else if (ret.updateSize(inputs, outputs, and)) {
      // if size updated, update the content attribute, written here because can't
      // access PlaRomData object from instanceAttributeChanged method
      state.getAttributeSet().setValue(CONTENTS_ATTR, ret.getSavedData());
    }
    CONTENTS_ATTR.setData(state);
    return ret;
  }

  public PlaRom() {
    super(_ID, S.getter("PlaRomComponent"));
    setIcon(new ArithmeticIcon("PLA", 3));
    setAttributes(
        new Attribute[] {
          ATTR_INPUTS,
          ATTR_AND,
          ATTR_OUTPUTS,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          CONTENTS_ATTR,
          Mem.ATTR_SELECTION
        },
        new Object[] {4, 4, 4, "", StdAttr.DEFAULT_LABEL_FONT, true, "", Mem.SEL_LOW});
    setOffsetBounds(Bounds.create(0, -30, 60, 60));
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    Bounds bds = instance.getBounds();
    instance.addAttributeListener();
    updateports(instance);
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() + bds.getHeight() / 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER_OVERALL);
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return new PlaMenu(instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_INPUTS || attr == ATTR_OUTPUTS) updateports(instance);
    else instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    PlaRomData data = getPlaRomData(painter);
    Graphics g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawRoundBounds(Color.WHITE);
    Bounds bds = painter.getBounds();
    g.setFont(new Font("sans serif", Font.BOLD, 11));
    Object label = painter.getAttributeValue(StdAttr.LABEL);
    if (label == null || label.equals(""))
      GraphicsUtil.drawCenteredText(
          g, "PLA ROM", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 3);
    GraphicsUtil.drawCenteredText(
        g,
        data.getSizeString(),
        bds.getX() + bds.getWidth() / 2,
        bds.getY() + bds.getHeight() / 3 * 2 - 3);
    painter.drawPort(0);
    painter.drawPort(1);
    painter.drawPort(2, S.get("ramCSLabel"), Direction.SOUTH);
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    PlaRomData data = getPlaRomData(state);
    Value cs = state.getPortValue(2);
    boolean selection = state.getAttributeValue(Mem.ATTR_SELECTION) == Mem.SEL_HIGH;
    boolean ComponentActive = !(cs == Value.FALSE && selection || cs == Value.TRUE && !selection);
    if (!ComponentActive) {
      state.setPort(1, Value.createUnknown(BitWidth.create(data.getOutputs())), Mem.DELAY);
      return;
    }
    Value[] inputs = state.getPortValue(0).getAll();
    for (byte i = 0; i < inputs.length / 2; i++) { // reverse array
      Value temp = inputs[i];
      inputs[i] = inputs[inputs.length - i - 1];
      inputs[inputs.length - i - 1] = temp;
    }
    data.setInputsValue(inputs);
    state.setPort(1, Value.create(data.getOutputValues()), Mem.DELAY);
  }

  private void updateports(Instance instance) {
    byte inputbitwidth = instance.getAttributeValue(ATTR_INPUTS).byteValue();
    byte outputbitwidth = instance.getAttributeValue(ATTR_OUTPUTS).byteValue();
    Port[] ps = new Port[3];
    ps[0] = new Port(0, 0, Port.INPUT, inputbitwidth);
    ps[1] = new Port(60, 0, Port.OUTPUT, outputbitwidth);
    ps[2] = new Port(30, 30, Port.INPUT, 1); // chip select
    ps[0].setToolTip(S.getter("demultiplexerInTip"));
    ps[1].setToolTip(S.getter("multiplexerOutTip"));
    if (instance.getAttributeValue(Mem.ATTR_SELECTION) == Mem.SEL_HIGH)
      ps[2].setToolTip(S.getter("memCSTip", "0"));
    else ps[2].setToolTip(S.getter("memCSTip", "1"));
    instance.setPorts(ps);
  }
}
