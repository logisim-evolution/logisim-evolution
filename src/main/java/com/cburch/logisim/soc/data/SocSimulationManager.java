/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.soc.bus.SocBusAttributes;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.JLabel;

public class SocSimulationManager implements SocBusMasterInterface {

  private static class SocBusSelector extends JLabel implements BaseMouseListenerContract {

    private static final long serialVersionUID = 1L;

    private Circuit myCirc;
    private final SocBusInfo myValue;

    public SocBusSelector(Window source, SocBusInfo value) {
      super(S.get("SocBusSelectAttrClick"));
      myCirc = null;
      this.repaint();
      if (source instanceof Frame) {
        myCirc = ((Frame) source).getProject().getCurrentCircuit();
      }
      myValue = value;
      addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (myCirc == null)
        return;
      SocSimulationManager socMan = myCirc.getSocSimulationManager();
      if (!socMan.hasSocBusses()) {
        OptionPane.showMessageDialog(
            null, S.get("SocManagerNoBusses"), S.get("SocBusSelectAttr"), OptionPane.ERROR_MESSAGE);
        return;
      }
      String id = socMan.getGuiBusId();
      if (id != null && !id.isEmpty()) {
        String oldId = myValue.getBusId();
        Component comp = myValue.getComponent();
        if (comp == null)
          return;
        if (oldId != null && !oldId.equals(id)) {
          myValue.getSocSimulationManager().reRegisterSlaveSniffer(oldId, id, comp);
          SocBusInfo newId = new SocBusInfo(id);
          newId.setSocSimulationManager(myValue.getSocSimulationManager(), comp);
          comp.getAttributeSet().setValue(SOC_BUS_SELECT, newId);
        }
      }
    }
  }


  private static class SocBusSelectAttribute extends Attribute<SocBusInfo> {
    private SocBusSelectAttribute() {
      super("SocBusSelection", S.getter("SocBusSelectAttr"));
    }

    @Override
    public SocBusInfo parse(String value) {
      return new SocBusInfo(value);
    }

    @Override
    public java.awt.Component getCellEditor(Window source, SocBusInfo value) {
      SocBusSelector ret = new SocBusSelector(source, value);
      ret.mouseClicked(null);
      return ret;
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

  public static final Attribute<SocBusInfo> SOC_BUS_SELECT = new SocBusSelectAttribute();
  private final HashMap<String, SocBusStateInfo> socBusses = new HashMap<>();
  private final ArrayList<Component> toBeChecked = new ArrayList<>();
  private CircuitState state;

  public String getSocBusDisplayString(String id) {
    if (id == null || id.isEmpty() || !socBusses.containsKey(id))
      return null;
    SocBusStateInfo bus = socBusses.get(id);
    Component c = bus.getComponent();
    String name = c == null ? null : c.getAttributeSet().getValue(StdAttr.LABEL);
    if ((name == null || name.isEmpty()) && c != null) {
      Location loc = c.getLocation();
      name = c.getFactory().getDisplayName() + "@" + loc.getX() + "," + loc.getY();
    }
    if (c == null)
      name = null;
    return name;
  }

  public boolean registerComponent(Component c) {
    if (!c.getFactory().isSocComponent())
      return false;
    SocInstanceFactory fact = (SocInstanceFactory) c.getFactory();
    if (fact.isSocUnknown())
      return false;
    if (fact.isSocBus()) {
      SocBusInfo ID = c.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID);
      if (socBusses.containsKey(ID.getBusId()))
        socBusses.get(ID.getBusId()).setComponent(c);
      else
        socBusses.put(ID.getBusId(), new SocBusStateInfo(this, c));
      c.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID).setSocSimulationManager(this, c);
    }
    if (c.getAttributeSet().containsAttribute(SOC_BUS_SELECT)) {
      c.getAttributeSet().getValue(SOC_BUS_SELECT).setSocSimulationManager(this, c);
      if (fact.isSocSlave() || fact.isSocSniffer()) {
        toBeChecked.add(c);
        Iterator<Component> iter = toBeChecked.iterator();
        while (iter.hasNext()) {
          Component comp = iter.next();
          if (comp.getAttributeSet().containsAttribute(SOC_BUS_SELECT)) {
            String id = comp.getAttributeSet().getValue(SOC_BUS_SELECT).getBusId();
            if (id != null && socBusses.containsKey(id)) {
              SocBusStateInfo binfo = socBusses.get(id);
              SocInstanceFactory factor = (SocInstanceFactory) comp.getFactory();
              if (factor.isSocSlave())
                binfo.registerSocBusSlave(factor.getSlaveInterface(comp.getAttributeSet()));
              if (factor.isSocSniffer())
                binfo.registerSocBusSniffer(factor.getSnifferInterface(comp.getAttributeSet()));
              iter.remove();
            } else if (id == null || id.isEmpty()) iter.remove();
          }
        }
      }
    }
    return true;
  }

  public boolean removeComponent(Component c) {
    if (!c.getFactory().isSocComponent())
      return false;
    SocInstanceFactory fact = (SocInstanceFactory) c.getFactory();
    if (fact.isSocUnknown())
      return false;
    if (fact.isSocBus()) {
      SocBusStateInfo info = socBusses.get(c.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID).getBusId());
      if (info != null)
        info.setComponent(null);
    }
    if (fact.isSocSlave() || fact.isSocSniffer()) {
      SocBusInfo binfo = c.getAttributeSet().getValue(SOC_BUS_SELECT);
      if (binfo != null)
        reRegisterSlaveSniffer(binfo.getBusId(), null, c);
    }
    return true;
  }

  public int nrOfSocBusses() {
    int result = 0;
    for (String s : socBusses.keySet())
      if (socBusses.get(s).getComponent() != null)
        result++;
    return result;
  }

  public boolean hasSocBusses() {
    return nrOfSocBusses() != 0;
  }

  public String getGuiBusId() {
    HashMap<String, String> busses = new HashMap<>();
    for (String id : socBusses.keySet()) {
      if (socBusses.get(id).getComponent() != null) busses.put(getSocBusDisplayString(id), id);
    }
    String res = (String) OptionPane.showInputDialog(
        null,
        S.get("SocBusManagerSelectBus"),
        S.get("SocBusSelectAttr"),
        OptionPane.PLAIN_MESSAGE,
        null,
        busses.keySet().toArray(),
        "");
    if (res != null && !res.isEmpty()) return busses.get(res);
    return "";
  }

  public SocBusStateInfo getSocBusState(String busId) {
    return socBusses.get(busId);
  }

  public void reRegisterSlaveSniffer(String oldId, String newId, Component comp) {
    SocInstanceFactory fact = (SocInstanceFactory) comp.getFactory();
    if (oldId != null && socBusses.containsKey(oldId)) {
      SocBusStateInfo binfo = socBusses.get(oldId);
      if (fact.isSocSlave())
        binfo.removeSocBusSlave(fact.getSlaveInterface(comp.getAttributeSet()));
      if (fact.isSocSniffer())
        binfo.removeSocBusSniffer(fact.getSnifferInterface(comp.getAttributeSet()));
    }
    if (newId != null && socBusses.containsKey(newId)) {
      SocBusStateInfo binfo = socBusses.get(newId);
      if (fact.isSocSlave())
        binfo.registerSocBusSlave(fact.getSlaveInterface(comp.getAttributeSet()));
      if (fact.isSocSniffer())
        binfo.registerSocBusSniffer(fact.getSnifferInterface(comp.getAttributeSet()));
    }
    toBeChecked.remove(comp);
  }

  public Object getdata(Component comp) {
    if (state == null)
      return null;
    return state.getData(comp);
  }

  public InstanceState getState(Component comp) {
    if (state == null)
      return null;
    return state.getInstanceState(comp);
  }


  @Override
  public void initializeTransaction(SocBusTransaction trans, String busId, CircuitState cState) {
    state = cState;
    SocBusStateInfo info = socBusses.get(busId);
    if (info == null || info.getComponent() == null) {
      trans.setError(SocBusTransaction.NO_SOC_BUS_CONNECTED_ERROR);
      return;
    }
    Iterator<Component> iter = toBeChecked.iterator();
    while (iter.hasNext()) {
      Component comp = iter.next();
      if (comp.getAttributeSet().containsAttribute(SOC_BUS_SELECT)) {
        String id = comp.getAttributeSet().getValue(SOC_BUS_SELECT).getBusId();
        if (id != null && socBusses.containsKey(id)) {
          SocBusStateInfo binfo = socBusses.get(id);
          SocInstanceFactory fact = (SocInstanceFactory) comp.getFactory();
          if (fact.isSocSlave())
            binfo.registerSocBusSlave(fact.getSlaveInterface(comp.getAttributeSet()));
          if (fact.isSocSniffer())
            binfo.registerSocBusSniffer(fact.getSnifferInterface(comp.getAttributeSet()));
        } else {
          SocBusInfo binfo = comp.getAttributeSet().getValue(SOC_BUS_SELECT);
          binfo.setBusId("");
          comp.getAttributeSet().setValue(SOC_BUS_SELECT, binfo);
        }
      }
      iter.remove();
    }
    info.initializeTransaction(trans, busId);
  }

}
