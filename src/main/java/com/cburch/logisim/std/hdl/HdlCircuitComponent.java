/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModel.PortDescription;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.util.StringGetter;
import java.util.WeakHashMap;

/**
 * Contains the code that refreshes the component's ports when contents are changed,
 *  among other things.
 */
public abstract class HdlCircuitComponent<C extends HdlContent> extends GenericInterfaceComponent {
  static class HdlCircuitListener implements HdlModelListener {

    final Instance instance;

    HdlCircuitListener(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void contentSet(HdlModel source) {
      // This was commented in VhdlEntityListener before I moved it here.
      // ((InstanceState)
      // instance).getProject().getSimulator().getVhdlSimulator().fireInvalidated();
      instance.fireInvalidated();
      instance.recomputeBounds();
    }
  }

  protected final WeakHashMap<Instance, HdlCircuitListener> contentListeners;
  protected final Attribute<C> contentAttr;

  /**
   * Creates the HdlCircuitComponent.
   * This includes a lot of stuff.
   */
  public HdlCircuitComponent(String name, StringGetter displayName, HdlGeneratorFactory generator,
      boolean requiresGlobalClock, Attribute<C> contentAttr) {
    super(name, displayName, generator, requiresGlobalClock);
    this.contentListeners = new WeakHashMap<>();
    this.contentAttr = contentAttr;
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    final var content = instance.getAttributeValue(contentAttr);
    final var listener = new HdlCircuitListener(instance);
  
    contentListeners.put(instance, listener);
    content.addHdlModelListener(listener);
  
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  protected final String getGiAttributesName(AttributeSet attrs) {
    final var content = attrs.getValue(contentAttr);
    return content.getName();
  }

  @Override
  protected final PortDescription[] getGiAttributesInputs(AttributeSet attrs) {
    final var content = attrs.getValue(contentAttr);
    return content.getInputs();
  }

  @Override
  protected final PortDescription[] getGiAttributesOutputs(AttributeSet attrs) {
    final var content = attrs.getValue(contentAttr);
    return content.getOutputs();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == contentAttr) {
      updatePorts(instance);
    }
  }

}