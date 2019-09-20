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

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.circuit.ComponentDataGuiProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.bus.SocBus;
import com.cburch.logisim.soc.bus.SocBusAttributes;
import com.cburch.logisim.soc.gui.TraceWindowTableModel;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class SocBusStateInfo extends JDialog implements ActionListener,LocaleListener,WindowListener {

  private static final long serialVersionUID = 1L;
  public static final int TraceWidth = 630;
  public static final int TraceHeight = 30;
  public static final int BlockWidth = 238;
  
  public interface SocBusStateListener {
    public void fireCanged(SocBusState item);
  }
  
  public class SocBusState implements InstanceData,Cloneable,ComponentDataGuiProvider {
    
    public class SocBusStateTrace extends JPanel {
      private static final long serialVersionUID = 1L;
	  private SocBusTransaction action;
      private long index;
      private TraceWindowTableModel model;
      
      public SocBusStateTrace(SocBusTransaction action, long index, TraceWindowTableModel model) {
        this.action = action;
        this.index = index;
        this.model = model;
      }
      
      public SocBusTransaction getTransaction() { return action; }
      
      @Override
      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setFont(AppPreferences.getScaledFont(g2.getFont()));
        if (action == null) {
          if (index == 0)
            GraphicsUtil.drawCenteredText(g2, S.get("SocBusNoTrace"), getWidth()/2, getHeight()/2);
          g2.dispose();
          return;
        }
        int boxWidth = action.paint(g2, index, model.getBoxWidth());
        g2.dispose();
        if (boxWidth != model.getBoxWidth()) model.setBoxWidth(boxWidth);
      }
    }
    
	private static final int NR_OF_TRACES_TO_KEEP = 10000;
    private LinkedList<SocBusTransaction> trace;
    private long startTraceIndex;
    private SocBusStateInfo parrent;
    private Instance instance;
    private ArrayList<SocBusStateListener> listeners;
    
    public SocBusState(SocBusStateInfo parrent, Instance instance) {
      trace = new LinkedList<SocBusTransaction>();
      startTraceIndex = 0;
      this.parrent = parrent;
      this.instance = instance;
      SocBus.MENU_PROVIDER.registerBusState(this, instance);
      listeners = new ArrayList<SocBusStateListener>();
    }
 
    public SocBusState clone() {
      try {
        return (SocBusState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void addTransaction(SocBusTransaction t) {
      while (trace.size() >= NR_OF_TRACES_TO_KEEP) {
        startTraceIndex++;
        trace.removeFirst();
      }
      trace.addLast(t);
      for (SocBusStateListener l : listeners) l.fireCanged(this);
    }
    
    public void clear() {
      if (trace.size() == 0)
        return;
      trace.clear();
      startTraceIndex = 0;
      for (SocBusStateListener l : listeners) l.fireCanged(this);
    }
    
    public void paint(Graphics2D g , Bounds b) {
      if (trace.isEmpty()) {
        GraphicsUtil.drawCenteredText(g, S.get("SocBusNoTrace"), b.getCenterX(), b.getCenterY());
        return;
      }
      long nrOfTraces = b.getHeight()/TraceHeight;
      if (nrOfTraces > trace.size())
        nrOfTraces = trace.size();
      int startIndex = trace.size()-1;
      for (int i = 0 ; i < nrOfTraces; i++) {
        SocBusTransaction t = trace.get(startIndex-i);
        t.paint(b.getX()+1, b.getY()+1+i*TraceHeight, g, startTraceIndex+startIndex-i);
      }
    }
    
    public int getNrOfEntires() { return trace.size(); }
    public void registerListener(SocBusStateListener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void deregisterListener(SocBusStateListener l) { if (listeners.contains(l)) listeners.remove(l); }

    public SocBusStateTrace getEntry(int index, TraceWindowTableModel model) {
      if (index < 0 || index >= trace.size()) {
    	if (index == 0)
    	  return new SocBusStateTrace(null,0,model);
        return null;
      }
      long indx = startTraceIndex+trace.size()-index-1;
      return new SocBusStateTrace(trace.get(trace.size()-index-1),indx,model);
    }

    @Override
    public void destroy() {
      if (parrent != null && parrent.isVisible())
        parrent.setVisible(false);
      SocBus.MENU_PROVIDER.deregisterBusState(this, instance);
    }
  }
  
  private SocSimulationManager socManager;
  private Component myComp;
  private ArrayList<SocBusSnifferInterface> sniffers;
  private JButton okButton;
  private JLabel title;
  private JScrollPane scroll;
  private SocMemMapModel memMap;
  
  public SocBusStateInfo(SocSimulationManager man , Component comp ) {
    super();
    LocaleManager.addLocaleListener(this);
    socManager = man;
    myComp = comp;
    sniffers = new ArrayList<SocBusSnifferInterface>();
    memMap = new SocMemMapModel();
    setTitle(S.get("SocMemMapWindowTitle")+getName());
    setLayout(new BorderLayout());
    title = new JLabel(S.get("SocMemoryMapTitle"),JLabel.CENTER);
    add(title,BorderLayout.NORTH);
    JTable table = new JTable(memMap) {
      private static final long serialVersionUID = 1L;
      public TableCellRenderer getCellRenderer(int row, int column) { return memMap.getCellRender(); }
    };
    table.getTableHeader().setDefaultRenderer(memMap.getHeaderRenderer());
    table.setFillsViewportHeight(true);
    table.setRowHeight(AppPreferences.getScaled(20));
    table.addMouseListener(memMap);
    scroll = new JScrollPane(table);
    scroll.setPreferredSize(new Dimension(AppPreferences.getScaled(320),AppPreferences.getScaled(240)));
    add(scroll,BorderLayout.CENTER);
    okButton = new JButton(S.get("SocMemoryMapOk"));
    add(okButton,BorderLayout.SOUTH);
    okButton.addActionListener(this);
    pack();
  }
  
  public void registerSocBusSlave(SocBusSlaveInterface slave) {
    memMap.registerSocBusSlave(slave);
  }
  
  public void removeSocBusSlave(SocBusSlaveInterface slave) {
    memMap.removeSocBusSlave(slave);
  }
  
  public void registerSocBusSniffer(SocBusSnifferInterface sniffer) {
    if (!sniffers.contains(sniffer))
      sniffers.add(sniffer);
  }
  
  public void removeSocBusSniffer(SocBusSnifferInterface sniffer) {
    if (sniffers.contains(sniffer))
      sniffers.remove(sniffer);
  }
  
  public ArrayList<SocBusSlaveInterface> getSlaves() {
    return memMap.getSlaves();
  }
  
  public String getName() {
    String name = myComp.getAttributeSet().getValue(StdAttr.LABEL);
    if (name == null || name.isEmpty()) {
      Location loc = myComp.getLocation();
      name = myComp.getFactory().getDisplayName()+"@"+loc.getX()+","+loc.getY();
    }
    return name;
  }
  
  public SocSimulationManager getSocSimulationManager() {
    return socManager;
  }
  
  public Component getComponent() {
    return myComp;
  }
  
  public void setComponent(Component comp) {
    myComp = comp;
  }
  
  public void initializeTransaction(SocBusTransaction trans, String busId) {
    int nrOfReponders = 0;
    int reponder = -1;
    ArrayList<SocBusSlaveInterface> slaves = memMap.getSlaves();
    if (slaves.isEmpty())
      trans.setError(SocBusTransaction.NoSlavesError);
    else if (trans.isReadTransaction()&&trans.isWriteTransaction()&&!trans.isAtomicTransaction())
      trans.setError(SocBusTransaction.NoneAtomicReadWriteError);
    else {
      for (int i = 0 ; i < slaves.size() ; i++) {
        if (slaves.get(i).canHandleTransaction(trans)) {
          nrOfReponders++;
          reponder = i;
        }
      }
      if (nrOfReponders == 0)
        trans.setError(SocBusTransaction.NoResponsError);
      else if (nrOfReponders != 1)
        trans.setError(SocBusTransaction.MultipleSlavesError);
      else
        slaves.get(reponder).handleTransaction(trans);
    }
    if (!trans.hasError()&&!trans.isHidden()) {
      for (SocBusSnifferInterface sniffer : sniffers)
        sniffer.sniffTransaction(trans);
    }
    if (!trans.isHidden()) {
      SocBusState data = getRegPropagateState();
      if (data != null) {
        data.addTransaction(trans);
        if (myComp.getAttributeSet().getValue(SocBusAttributes.SOC_TRACE_VISABLE))
          ((InstanceComponent) myComp).getInstance().fireInvalidated();
      }
    }
  }
  
  public void paint(int x , int y , Graphics2D g2, Instance inst, boolean visible,InstanceData info) {
    Graphics2D g = (Graphics2D) g2.create();
    g.translate(x+5, y+25);
    int nrOfTraces = inst.getAttributeValue(SocBusAttributes.NrOfTracesAttr).getWidth();
    int height = nrOfTraces*TraceHeight;
    g.setColor(Color.YELLOW);
    g.fillRect(0, 0, TraceWidth, height);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, TraceWidth, height);
    if (!visible) 
      GraphicsUtil.drawCenteredText(g, S.get("SocHiddenForFasterSimulation"), 320, height/2);
    else {
      if (info != null)
        ((SocBusState)info).paint(g, Bounds.create(0,0,640,height));
    }
    g.dispose();
  }
  
  public SocBusState getNewState(Instance instance) {
    return new SocBusState(this,instance);
  }
  
  public SocBusState getRegPropagateState() {
    return (SocBusState) socManager.getdata(myComp);
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == okButton)
      setVisible(false);
  }

  @Override
  public void localeChanged() {
    okButton.setText(S.get("SocMemoryMapOk"));
  }

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) {
    setVisible(false);
  }

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
  
}
