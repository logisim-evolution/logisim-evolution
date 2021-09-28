package com.cburch.logisim.soc.gui;

import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

public class ListeningFrame extends JFrame implements BaseWindowListenerContract, LocaleListener, CircuitListener, ComponentListener {

  private static final long serialVersionUID = 1L;
  private final StringGetter title;
  private final String upName;
  private final CircuitStateHolder.HierarchyInfo hierInfo;

  public ListeningFrame(String upName, StringGetter t, CircuitStateHolder.HierarchyInfo h) {
    LocaleManager.addLocaleListener(this);
    title = t;
    hierInfo = h;
    this.upName = upName + " ";
    if (h != null) {
      h.registerCircuitListener(this);
      h.registerComponentListener(this);
    }
    updateTitle();
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  public ListeningFrame(StringGetter t) {
    LocaleManager.addLocaleListener(this);
    title = t;
    this.upName = "";
    hierInfo = null;
    updateTitle();
  }

  private void updateTitle() {
    if (hierInfo == null) setTitle(upName + title.toString());
    else setTitle(upName + title + " " + hierInfo.getName());
  }

  public String getParentTitle() {
    return upName + title + " " + hierInfo.getName();
  }

  @Override
  public void windowClosing(WindowEvent e) {
    setVisible(false);
    dispose();
  }

  @Override
  public void localeChanged() {
    updateTitle();
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    if (event.getAction() == CircuitEvent.ACTION_SET_NAME) updateTitle();
  }

  @Override
  public void labelChanged(ComponentEvent e) {
    updateTitle();
  }
}
