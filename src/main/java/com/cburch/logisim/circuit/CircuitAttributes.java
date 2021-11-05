/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.appear.CircuitAppearanceEvent;
import com.cburch.logisim.circuit.appear.CircuitAppearanceListener;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.SyntaxChecker;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

public class CircuitAttributes extends AbstractAttributeSet {
  
  private static class defaultStaticAttributeProvider implements AttributeDefaultProvider {

    @Override
    public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
      final var ret = AttributeSets.fixedSet(STATIC_ATTRS, STATIC_DEFAULTS);
      ret.setValue(APPEARANCE_ATTR, StdAttr.APPEAR_CLASSIC);
      return ret.getValue(attr);
    }

    @Override
    public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
      return false;
    }
    
  }

  private class MyListener implements AttributeListener, CircuitAppearanceListener {
    private final Circuit source;

    private MyListener(Circuit s) {
      source = s;
    }

    @Override
    public void attributeListChanged(AttributeEvent e) {
      // Do nothing
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      @SuppressWarnings("unchecked")
      Attribute<Object> a = (Attribute<Object>) e.getAttribute();
      fireAttributeValueChanged(a, e.getValue(), e.getOldValue());
    }

    @Override
    public void circuitAppearanceChanged(CircuitAppearanceEvent e) {
      final var factory = (SubcircuitFactory) subcircInstance.getFactory();
      if (e.isConcerning(CircuitAppearanceEvent.PORTS)) {
        factory.computePorts(subcircInstance);
      }
      if (e.isConcerning(CircuitAppearanceEvent.BOUNDS)) {
        subcircInstance.recomputeBounds();
      }
      subcircInstance.fireInvalidated();
      if (source != null && !source.getAppearance().isDefaultAppearance())
        source.getStaticAttributes().setValue(APPEARANCE_ATTR, APPEAR_CUSTOM);
    }
  }

  private static class StaticListener implements AttributeListener {
    private final Circuit source;

    private StaticListener(Circuit s) {
      source = s;
    }

    @Override
    public void attributeListChanged(AttributeEvent e) {
      // Do nothing
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      if (e.getAttribute() == NAME_ATTR) {
        final var NewName = (String) e.getValue();
        final var OldName = e.getOldValue() == null ? "ThisShouldNotHappen" : (String) e.getOldValue();
        if (!NewName.equals(OldName)) {
          if (NewName.isEmpty()) {
            OptionPane.showMessageDialog(null, S.get("EmptyNameError"), "", OptionPane.ERROR_MESSAGE);
            e.getSource().setValue(NAME_ATTR, OldName);
            source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
            return;
          } else if (!SyntaxChecker.isVariableNameAcceptable(NewName, true)) {
            e.getSource().setValue(NAME_ATTR, OldName);
            source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
            return;
          } else {
            for (final var component : source.getNonWires()) {
              if (component.getFactory() instanceof Pin) {
                final var label = component.getAttributeSet().getValue(StdAttr.LABEL).toUpperCase();
                if (!label.isEmpty() && label.equals(NewName.toUpperCase())) {
                  final var msg = S.get("CircuitSameInputOutputLabel");
                  OptionPane.showMessageDialog(null, "\"" + NewName + "\" : " + msg);
                  e.getSource().setValue(NAME_ATTR, OldName);
                  source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
                  return;
                }
              }
            }
            source.fireEvent(CircuitEvent.ACTION_CHECK_NAME, OldName);
            source.fireEvent(CircuitEvent.ACTION_SET_NAME, NewName);
          }
        }
      }
    }
  }
  
  public static void copyStaticAttributes(AttributeSet destination, AttributeSet source) {
    destination.setValue(CIRCUIT_LABEL_ATTR, source.getValue(CIRCUIT_LABEL_ATTR));
    destination.setValue(CIRCUIT_LABEL_FACING_ATTR, source.getValue(CIRCUIT_LABEL_FACING_ATTR));
    destination.setValue(CIRCUIT_LABEL_FONT_ATTR, source.getValue(CIRCUIT_LABEL_FONT_ATTR));
    destination.setValue(APPEARANCE_ATTR, source.getValue(APPEARANCE_ATTR));
    destination.setValue(NAMED_CIRCUIT_BOX_FIXED_SIZE, source.getValue(NAMED_CIRCUIT_BOX_FIXED_SIZE));
    destination.setValue(SIMULATION_FREQUENCY, source.getValue(SIMULATION_FREQUENCY));
    destination.setValue(DOWNLOAD_FREQUENCY, source.getValue(DOWNLOAD_FREQUENCY));
    destination.setValue(DOWNLOAD_BOARD, source.getValue(DOWNLOAD_BOARD));
  }

  static AttributeSet createBaseAttrs(Circuit source, String name) {
    final var ret = AttributeSets.fixedSet(STATIC_ATTRS, STATIC_DEFAULTS);
    ret.setValue(APPEARANCE_ATTR, AppPreferences.getDefaultCircuitAppearance());
    ret.setValue(CircuitAttributes.NAME_ATTR, name);
    ret.addAttributeListener(new StaticListener(source));
    return ret;
  }

  public static final Attribute<String> NAME_ATTR =
      Attributes.forString("circuit", S.getter("circuitName"));

  public static final Attribute<Direction> LABEL_LOCATION_ATTR =
      Attributes.forDirection("labelloc", S.getter("circuitLabelLocAttr"));

  public static final Attribute<String> CIRCUIT_LABEL_ATTR =
      Attributes.forString("clabel", S.getter("circuitLabelAttr"));

  public static final Attribute<Direction> CIRCUIT_LABEL_FACING_ATTR =
      Attributes.forDirection("clabelup", S.getter("circuitLabelDirAttr"));

  public static final Attribute<Font> CIRCUIT_LABEL_FONT_ATTR =
      Attributes.forFont("clabelfont", S.getter("circuitLabelFontAttr"));
  public static final Attribute<Boolean> CIRCUIT_IS_VHDL_BOX =
      Attributes.forBoolean("circuitvhdl", S.getter("circuitIsVhdl"));
  public static final Attribute<Boolean> NAMED_CIRCUIT_BOX_FIXED_SIZE =
      Attributes.forBoolean("circuitnamedboxfixedsize", S.getter("circuitNamedBoxFixedSize"));
  public static final AttributeOption APPEAR_CLASSIC = StdAttr.APPEAR_CLASSIC;
  public static final AttributeOption APPEAR_FPGA = StdAttr.APPEAR_FPGA;
  public static final AttributeOption APPEAR_EVOLUTION = StdAttr.APPEAR_EVOLUTION;
  public static final AttributeOption APPEAR_CUSTOM =
      new AttributeOption("custom", S.getter("circuitCustomAppearance"));
  public static final Attribute<AttributeOption> APPEARANCE_ATTR =
      Attributes.forOption(
          "appearance",
          S.getter("circuitAppearanceAttr"),
          new AttributeOption[] {APPEAR_CLASSIC, APPEAR_FPGA, APPEAR_EVOLUTION, APPEAR_CUSTOM});
  public static final Attribute<Double> SIMULATION_FREQUENCY = Attributes.forDouble("simulationFrequency");
  public static final Attribute<Double> DOWNLOAD_FREQUENCY = Attributes.forDouble("downloadFrequency");
  public static final Attribute<String> DOWNLOAD_BOARD = Attributes.forString("downloadBoard");
  public static final defaultStaticAttributeProvider DEFAULT_STATIC_ATTRIBUTES = new defaultStaticAttributeProvider();

  private static final Attribute<?>[] STATIC_ATTRS = {
    NAME_ATTR,
    CIRCUIT_LABEL_ATTR,
    CIRCUIT_LABEL_FACING_ATTR,
    CIRCUIT_LABEL_FONT_ATTR,
    APPEARANCE_ATTR,
    NAMED_CIRCUIT_BOX_FIXED_SIZE,
    SIMULATION_FREQUENCY,
    DOWNLOAD_FREQUENCY,
    DOWNLOAD_BOARD
  };

  private static final Object[] STATIC_DEFAULTS = {
    "", "", Direction.EAST, StdAttr.DEFAULT_LABEL_FONT, APPEAR_CLASSIC, false, -1d, -1d, ""
  };

  private static final List<Attribute<?>> INSTANCE_ATTRS =
      Arrays.asList(
          StdAttr.FACING,
          StdAttr.LABEL,
          LABEL_LOCATION_ATTR,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          NAME_ATTR,
          CIRCUIT_LABEL_ATTR,
          CIRCUIT_LABEL_FACING_ATTR,
          CIRCUIT_LABEL_FONT_ATTR,
          APPEARANCE_ATTR);

  private final Circuit source;
  private Instance subcircInstance;
  private Direction facing;
  private String label;
  private Direction labelLocation;
  private Font labelFont;
  private Boolean labelVisible;
  private MyListener listener;
  private Instance[] pinInstances;
  private boolean nameReadOnly;

  public CircuitAttributes(Circuit source) {
    this.source = source;
    subcircInstance = null;
    facing = source.getAppearance().getFacing();
    label = "";
    labelLocation = Direction.NORTH;
    labelFont = StdAttr.DEFAULT_LABEL_FONT;
    labelVisible = true;
    pinInstances = new Instance[0];
    nameReadOnly = false;
    DOWNLOAD_FREQUENCY.setHidden(true);
    SIMULATION_FREQUENCY.setHidden(true);
    DOWNLOAD_BOARD.setHidden(true);
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    final var other = (CircuitAttributes) dest;
    other.subcircInstance = null;
    other.listener = null;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return INSTANCE_ATTRS;
  }

  public Direction getFacing() {
    return facing;
  }

  public Instance[] getPinInstances() {
    return pinInstances;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> E getValue(Attribute<E> attr) {
    if (attr == StdAttr.FACING) return (E) facing;
    if (attr == StdAttr.LABEL) return (E) label;
    if (attr == StdAttr.LABEL_FONT) return (E) labelFont;
    if (attr == StdAttr.LABEL_VISIBILITY) return (E) labelVisible;
    if (attr == LABEL_LOCATION_ATTR) return (E) labelLocation;
    else return source.getStaticAttributes().getValue(attr);
  }
  
  public static void copyInto(AttributeSet source, AttributeSet destination) {
    destination.setValue(StdAttr.FACING, source.getValue(StdAttr.FACING));
    destination.setValue(StdAttr.LABEL, source.getValue(StdAttr.LABEL));
    destination.setValue(StdAttr.LABEL_FONT, source.getValue(StdAttr.LABEL_FONT));
    destination.setValue(StdAttr.LABEL_VISIBILITY, source.getValue(StdAttr.LABEL_VISIBILITY));
    destination.setValue(LABEL_LOCATION_ATTR, source.getValue(LABEL_LOCATION_ATTR));
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    for (Attribute<?> aStatic : STATIC_ATTRS) {
      if (aStatic == attr)
        return false;
    }
    return attr.isToSave();
  }

  void setPinInstances(Instance[] value) {
    pinInstances = value;
  }

  void setSubcircuit(Instance value) {
    subcircInstance = value;
    if (subcircInstance != null && listener == null) {
      listener = new MyListener(source);
      source.getStaticAttributes().addAttributeListener(listener);
      source.getAppearance().addCircuitAppearanceListener(listener);
    }
  }

  @Override
  public void setReadOnly(Attribute<?> attr, boolean value) {
    if (attr == NAME_ATTR) {
      nameReadOnly = value;
    }
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    if (attr == NAME_ATTR) {
      return nameReadOnly;
    }
    return false;
  }

  @Override
  public <E> void setValue(Attribute<E> attr, E value) {
    if (attr == StdAttr.FACING) {
      final var val = (Direction) value;
      if (facing.equals(val)) return;
      facing = val;
      fireAttributeValueChanged(StdAttr.FACING, val, null);
      if (subcircInstance != null) subcircInstance.recomputeBounds();
    } else if (attr == StdAttr.LABEL) {
      final var val = (String) value;
      final var oldval = label;
      if (label.equals(val)) return;
      label = val;
      fireAttributeValueChanged(StdAttr.LABEL, val, oldval);
    } else if (attr == StdAttr.LABEL_FONT) {
      final var val = (Font) value;
      if (labelFont.equals(val)) return;
      labelFont = val;
      fireAttributeValueChanged(StdAttr.LABEL_FONT, val, null);
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      final var val = (Boolean) value;
      if (labelVisible == value) return;
      labelVisible = val;
      fireAttributeValueChanged(StdAttr.LABEL_VISIBILITY, val, null);
    } else if (attr == LABEL_LOCATION_ATTR) {
      final var val = (Direction) value;
      if (labelLocation.equals(val)) return;
      labelLocation = val;
      fireAttributeValueChanged(LABEL_LOCATION_ATTR, val, null);
    } else {
      source.getStaticAttributes().setValue(attr, value);
      if (attr == NAME_ATTR) {
        source.fireEvent(CircuitEvent.ACTION_SET_NAME, value);
      }
    }
  }
}
