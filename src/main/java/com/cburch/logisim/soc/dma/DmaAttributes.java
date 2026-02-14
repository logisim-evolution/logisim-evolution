/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.dma;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.util.StringUtil;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;

/**
 * Attribute set for the SoC DMA engine component.
 *
 * <p>Provides three separate bus-select attributes so that control registers,
 * source reads, and destination writes can each be routed to a different bus.
 */
public class DmaAttributes extends AbstractAttributeSet {

  /* -------- hidden state attribute -------- */

  private static class DmaStateAttribute extends Attribute<DmaState> {
    @Override
    public DmaState parse(String value) {
      return null;
    }

    @Override
    public boolean isHidden() {
      return true;
    }
  }

  /* -------- bus-select attribute for master-only connections -------- */

  /**
   * A bus-select attribute similar to {@link SocSimulationManager#SOC_BUS_SELECT} but
   * designed for master-only connections.  Changing the bus does NOT trigger
   * slave/sniffer re-registration.
   */
  private static class MasterBusSelectAttribute extends Attribute<SocBusInfo> {

    private MasterBusSelectAttribute(String name, String displayKey) {
      super(name, S.getter(displayKey));
    }

    @Override
    public SocBusInfo parse(String value) {
      return new SocBusInfo(value);
    }

    @Override
    public java.awt.Component getCellEditor(Window source, SocBusInfo value) {
      MasterBusSelector selector = new MasterBusSelector(source, value, this);
      selector.mouseClicked(null);
      return selector;
    }

    @Override
    public String toDisplayString(SocBusInfo f) {
      return S.get("SocBusSelectAttrClick");
    }

    @Override
    public String toStandardString(SocBusInfo value) {
      return value.getBusId();
    }
  }

  /**
   * A click-to-select label widget that lets the user pick a bus from the
   * circuit's simulation manager.  Unlike the standard SocBusSelector, this
   * one only updates the bus ID without re-registering slaves/sniffers.
   */
  private static class MasterBusSelector extends JLabel implements BaseMouseListenerContract {
    private static final long serialVersionUID = 1L;

    private final SocBusInfo myValue;
    private final MasterBusSelectAttribute myAttr;
    private com.cburch.logisim.circuit.Circuit myCirc;

    MasterBusSelector(Window source, SocBusInfo value, MasterBusSelectAttribute attr) {
      super(S.get("SocBusSelectAttrClick"));
      myValue = value;
      myAttr = attr;
      myCirc = null;
      if (source instanceof Frame frame) {
        myCirc = frame.getProject().getCurrentCircuit();
      }
      addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (myCirc == null) return;
      SocSimulationManager socMan = myCirc.getSocSimulationManager();
      if (!socMan.hasSocBusses()) {
        OptionPane.showMessageDialog(
            null,
            S.get("SocManagerNoBusses"),
            S.get("SocBusSelectAttr"),
            OptionPane.ERROR_MESSAGE);
        return;
      }
      final var id = socMan.getGuiBusId();
      if (StringUtil.isNotEmpty(id)) {
        final var oldId = myValue.getBusId();
        final var comp = myValue.getComponent();
        if (comp == null) return;
        if (oldId == null || !oldId.equals(id)) {
          // No slave/sniffer re-registration needed for master-only bus connections
          final var newInfo = new SocBusInfo(id);
          newInfo.setSocSimulationManager(myValue.getSocSimulationManager(), comp);
          comp.getAttributeSet().setValue(myAttr, newInfo);
        }
      }
    }
  }

  /* -------- burst-size attribute (power of 2) -------- */

  private static final Integer[] BURST_OPTIONS = {1, 2, 4, 8, 16, 32, 64, 128, 256};

  private static class BurstSizeAttribute extends Attribute<Integer> {
    private BurstSizeAttribute() {
      super("BurstSize", S.getter("SocDmaBurstSize"));
    }

    @Override
    public Integer parse(String value) {
      int v = Integer.parseInt(value);
      // Clamp to nearest valid power-of-2
      for (int bs : BURST_OPTIONS) {
        if (bs >= v) return bs;
      }
      return BURST_OPTIONS[BURST_OPTIONS.length - 1];
    }

    @Override
    public java.awt.Component getCellEditor(Integer value) {
      final var combo = new com.cburch.logisim.gui.generic.ComboBox<>(BURST_OPTIONS);
      if (value == null) combo.setSelectedIndex(-1);
      else combo.setSelectedItem(value);
      return combo;
    }

    @Override
    public String toDisplayString(Integer value) {
      return value + " words/tick";
    }

    @Override
    public String toStandardString(Integer value) {
      return Integer.toString(value);
    }
  }

  /* -------- public attribute constants -------- */

  public static final Attribute<DmaState> DMA_STATE = new DmaStateAttribute();

  public static final Attribute<Integer> START_ADDRESS =
      Attributes.forHexInteger("StartAddress", S.getter("SocDmaStartAddress"));

  public static final Attribute<Integer> BURST_SIZE = new BurstSizeAttribute();

  /** Bus for MMIO control register access (DMA is a slave on this bus). */
  // Uses the standard SOC_BUS_SELECT for slave registration.

  /** Bus used for source (read) DMA transfers (master-only). */
  public static final Attribute<SocBusInfo> DMA_SRC_BUS =
      new MasterBusSelectAttribute("DmaSrcBus", "SocDmaSrcBus");

  /** Bus used for destination (write) DMA transfers (master-only). */
  public static final Attribute<SocBusInfo> DMA_DST_BUS =
      new MasterBusSelectAttribute("DmaDstBus", "SocDmaDstBus");

  /* -------- attribute list -------- */

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          START_ADDRESS,
          BURST_SIZE,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          SocSimulationManager.SOC_BUS_SELECT,
          DMA_SRC_BUS,
          DMA_DST_BUS,
          DMA_STATE);

  /* -------- instance fields -------- */

  private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
  private Boolean labelVisible = true;
  private DmaState state = new DmaState();

  /* -------- AbstractAttributeSet implementation -------- */

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    DmaAttributes d = (DmaAttributes) dest;
    d.labelFont = labelFont;
    d.labelVisible = labelVisible;
    d.state = new DmaState();
    state.copyInto(d.state);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    return attr == DMA_STATE;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return attr.isToSave() && attr != DMA_STATE;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == START_ADDRESS) return (V) state.getStartAddress();
    if (attr == BURST_SIZE) return (V) (Integer) state.getBurstSize();
    if (attr == StdAttr.LABEL) return (V) state.getLabel();
    if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (V) labelVisible;
    if (attr == SocSimulationManager.SOC_BUS_SELECT) return (V) state.getControlBus();
    if (attr == DMA_SRC_BUS) return (V) state.getSourceBus();
    if (attr == DMA_DST_BUS) return (V) state.getDestBus();
    if (attr == DMA_STATE) return (V) state;
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    V oldValue = getValue(attr);
    if (attr == START_ADDRESS) {
      if (state.setStartAddress((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == BURST_SIZE) {
      if (state.setBurstSize((Integer) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL) {
      if (state.setLabel((String) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == StdAttr.LABEL_FONT) {
      Font f = (Font) value;
      if (!labelFont.equals(f)) {
        labelFont = f;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == StdAttr.LABEL_VISIBILITY) {
      Boolean b = (Boolean) value;
      if (!b.equals(labelVisible)) {
        labelVisible = b;
        fireAttributeValueChanged(attr, value, oldValue);
      }
      return;
    }
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      if (state.setControlBus((SocBusInfo) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == DMA_SRC_BUS) {
      if (state.setSourceBus((SocBusInfo) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
    if (attr == DMA_DST_BUS) {
      if (state.setDestBus((SocBusInfo) value))
        fireAttributeValueChanged(attr, value, oldValue);
      return;
    }
  }
}
