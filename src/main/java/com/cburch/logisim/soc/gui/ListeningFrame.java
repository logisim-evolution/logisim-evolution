package com.cburch.logisim.soc.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

public class ListeningFrame extends JFrame implements WindowListener,LocaleListener,CircuitListener,ComponentListener{

  private static final long serialVersionUID = 1L;
  private StringGetter title;
  private String upName;
  private CircuitStateHolder.HierarchyInfo hierInfo;
  
  public ListeningFrame(String upName, StringGetter t, CircuitStateHolder.HierarchyInfo h) {
    LocaleManager.addLocaleListener(this);
    title = t;
    hierInfo = h;
    this.upName = upName+" ";
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
    if (hierInfo == null) setTitle(upName+title.toString());
    else setTitle(upName+title+" "+hierInfo.getName());
  }
  
  public String getParentTitle() { return upName+title+" "+hierInfo.getName(); }

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) { setVisible(false); dispose(); }

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowActivated(WindowEvent e) {}

  @Override
  public void windowDeactivated(WindowEvent e) {}

  @Override
  public void localeChanged() {updateTitle();}
  
  @Override
  public void circuitChanged(CircuitEvent event) {
    if (event.getAction() == CircuitEvent.ACTION_SET_NAME) updateTitle();
  }

  @Override
  public void componentInvalidated(ComponentEvent e) {}

  @Override
  public void endChanged(ComponentEvent e) {}

  @Override
  public void LabelChanged(ComponentEvent e) { updateTitle(); };
      
}
