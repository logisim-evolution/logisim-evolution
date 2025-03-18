/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import java.util.WeakHashMap;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.hdl.HdlModel.PortDescription;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.util.StringGetter;

/**
 * Contains the code that refreshes the component's ports when the contents are changed, among other things.
 */
public abstract class HdlCircuitComponent<C extends HdlContent> extends GenericInterfaceComponent {
  static class HdlCircuitListener implements HdlModelListener {

    final Instance instance;

    HdlCircuitListener(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void contentSet(HdlModel source) {
      // ((InstanceState)
      // instance).getProject().getSimulator().getVhdlSimulator().fireInvalidated();
      instance.fireInvalidated();
      instance.recomputeBounds();
    }
  }

  protected final WeakHashMap<Instance, HdlCircuitListener> contentListeners;
  protected final Attribute<C> contentAttr;

  public HdlCircuitComponent(String name, StringGetter displayName, HdlGeneratorFactory generator, boolean requiresGlobalClock, Attribute<C> contentAttr) {
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
  protected final String getGIAttributesName(AttributeSet attrs) {
    final var content = attrs.getValue(contentAttr);
    return content.getName();
  }

  @Override
  protected final PortDescription[] getGIAttributesInputs(AttributeSet attrs) {
    final var content = attrs.getValue(contentAttr);
    return content.getInputs();
  }

  @Override
  protected final PortDescription[] getGIAttributesOutputs(AttributeSet attrs) {
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