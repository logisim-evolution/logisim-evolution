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
import java.awt.Font;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.bus.SocBusAttributes;
import com.cburch.logisim.soc.gui.BusTransactionInsertionGui;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class SocBusStateInfo extends JDialog implements SocBusMasterInterface,ActionListener,
        LocaleListener,WindowListener {

  private static final long serialVersionUID = 1L;
  public static final int TraceWidth = 630;
  public static final int TraceHeight = 30;
  
  private class TransactionInfo {
    SocBusTransaction request,response;
    
    public TransactionInfo(SocBusTransaction request, SocBusTransaction response) {
      this.request = request;
      this.response = response;
    }
    
    private void paintTraceInfo(Graphics2D g, SocBusTransaction t, boolean isRequest) {
      g.setColor(Color.BLACK);
      g.drawLine(0, 0, 0, TraceHeight-2);
      if (t.hasError()) {
        Font f = g.getFont();
        g.setColor(Color.RED);
        g.setFont(StdAttr.DEFAULT_LABEL_FONT);
        GraphicsUtil.drawCenteredText(g, t.getShortErrorMessage(), 117, (TraceHeight-2)>>1);
        g.setFont(f);
        g.setColor(Color.BLACK);
        return;
      }
      String title = isRequest ? S.get("SocBusStateMaster")+t.transactionInitiator() : 
      S.get("SocBusStateSlave")+t.transactionResponder();
      GraphicsUtil.drawCenteredText(g, title, 118, (TraceHeight-2)/4);
      g.drawRect(2, (TraceHeight-2)>>1, 92, (TraceHeight-2)>>1);
      g.drawLine(14, (TraceHeight-2)>>1, 14, (TraceHeight-2));
      GraphicsUtil.drawCenteredText(g, "A", 8, (3*(TraceHeight-2))/4);
      String Str = String.format("0x%08X", t.getAddress());
      GraphicsUtil.drawCenteredText(g, Str, 53, (3*(TraceHeight-2))/4);
      g.drawRect(98, (TraceHeight-2)>>1, 92, (TraceHeight-2)>>1);
      g.drawLine(110, (TraceHeight-2)>>1, 110, (TraceHeight-2));
      GraphicsUtil.drawCenteredText(g, "D", 104, (3*(TraceHeight-2))/4);
      if ((isRequest && t.isWriteTransaction())||
          (!isRequest && t.isReadTransaction())) {
    	String format = "0x%08X";
    	if (t.getAccessType() == SocBusTransaction.HalfWordAccess)
    	  format = "0x%04X";
    	if (t.getAccessType() == SocBusTransaction.ByteAccess)
      	  format = "0x%02X";
        Str = String.format(format, t.getData());
      }
      else
        Str = S.get("SocBusStateNoDataMax10chars");
      GraphicsUtil.drawCenteredText(g, Str, 148, (3*(TraceHeight-2))/4);
      if (!isRequest)
        return;
      if (t.isAtomicTransaction()) {
        g.setColor(Color.yellow);
        g.fillRect(203, (TraceHeight-2)>>1 , 10, (TraceHeight-2)>>1);
        g.setColor(Color.BLUE);
        GraphicsUtil.drawCenteredText(g, "A", 208, (3*(TraceHeight-2))/4);
        g.setColor(Color.BLACK);
      }
      if (t.isWriteTransaction()) {
        g.fillRect(214, (TraceHeight-2)>>1 , 10, (TraceHeight-2)>>1);
        g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(g, "W", 219, (3*(TraceHeight-2))/4);
        g.setColor(Color.BLACK);
      }
      if (t.isReadTransaction()) {
        g.fillRect(225, (TraceHeight-2)>>1 , 10, (TraceHeight-2)>>1);
        g.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(g, "R", 230, (3*(TraceHeight-2))/4);
        g.setColor(Color.BLACK);
      }
    }
    
    public void paint(int x , int y, Graphics2D g2, Long index) {
      Graphics2D g = (Graphics2D)g2.create();
      g.translate(x, y);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, TraceWidth-2, TraceHeight-2);
      g.setColor(Color.BLACK);
      g.drawRect(0, 0, TraceWidth-2, TraceHeight-2);
      GraphicsUtil.drawCenteredText(g, S.get("SocBusStateTraceIndex"), 79, (TraceHeight-2)/4);
      GraphicsUtil.drawCenteredText(g, index.toString(), 79, (3*(TraceHeight-2)/4));
      g.translate(158, 0);
      paintTraceInfo(g,request,true);
      g.translate(235, 0);
      paintTraceInfo(g,response,false);
      g.dispose();
    }
  }

  private SocSimulationManager socManager;
  private Component myComp;
  private ArrayList<SocBusSnifferInterface> sniffers;
  private LinkedList<TransactionInfo> trace;
  private long traceCountOffset;
  private Value oldReset;
  private JButton okButton;
  private JLabel title;
  private JScrollPane scroll;
  private SocMemMapModel memMap;
  private BusTransactionInsertionGui mygui;
  
  public SocBusStateInfo(SocSimulationManager man , Component comp ) {
    super();
    LocaleManager.addLocaleListener(this);
    socManager = man;
    myComp = comp;
    sniffers = new ArrayList<SocBusSnifferInterface>();
    trace = new LinkedList<TransactionInfo>();
    traceCountOffset = 0;
    oldReset = Value.UNKNOWN;
    memMap = new SocMemMapModel();
    String id = comp.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID).getBusId();
    mygui = new BusTransactionInsertionGui(this,id);
    mygui.setTitle(S.get("SocInsertTransWindowTitle")+getName());
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
    if (name == null || name.isBlank()) {
      Location loc = myComp.getLocation();
      name = myComp.getFactory().getDisplayName()+"@"+loc.getX()+","+loc.getY();
    }
    return name;
  }
  
  public void setReset( Value reset ) {
    if (oldReset.equals(Value.FALSE) && reset.equals(Value.TRUE)) {
      traceCountOffset = 0;
      trace.clear();
    }
    oldReset = reset;
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
  
  public BusTransactionInsertionGui getTransactionInsertionGui() {
    return mygui;
  }

  @Override
  public SocBusTransaction initializeTransaction(SocBusTransaction trans, String busId) {
    SocBusTransaction ret = trans.clone();
    int nrOfReponders = 0;
    int reponder = -1;
    ArrayList<SocBusSlaveInterface> slaves = memMap.getSlaves();
    if (slaves.isEmpty())
      ret.setError(SocBusTransaction.NoSlavesError);
    else if (trans.isReadTransaction()&&trans.isWriteTransaction()&&!trans.isAtomicTransaction())
      ret.setError(SocBusTransaction.NoneAtomicReadWriteError);
    else {
      for (int i = 0 ; i < slaves.size() ; i++) {
        if (slaves.get(i).canHandleTransaction(ret)) {
          nrOfReponders++;
          reponder = i;
        }
      }
      if (nrOfReponders == 0)
        ret.setError(SocBusTransaction.NoResponsError);
      else if (nrOfReponders != 1)
        ret.setError(SocBusTransaction.MultipleSlavesError);
      else
        ret = slaves.get(reponder).handleTransaction(trans);
    }
    if (!ret.hasError()&&!ret.isHidden()) {
      for (SocBusSnifferInterface sniffer : sniffers)
        sniffer.sniffTransaction(ret);
    }
    if (!ret.isHidden()) {
      cleanup();
      trace.add(new TransactionInfo(trans,ret));
      ((InstanceComponent) myComp).getInstance().fireInvalidated();
    }
    return ret;
  }
  
  public void cleanup() {
    while (trace.size()>=myComp.getAttributeSet().getValue(SocBusAttributes.NrOfTracesAttr).getWidth()) {
      trace.removeFirst();
      traceCountOffset++;
    }
  }
  
  public void paint(int x , int y , Graphics2D g2, Instance inst, boolean visible) {
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
    else if (trace.isEmpty()) {
      GraphicsUtil.drawCenteredText(g, S.get("SocBusNoTrace"), 320, height/2);
    } else {
      for (int i = 0 ; i < trace.size() ; i++) {
        trace.get(i).paint(1, 1+i*30, g, traceCountOffset+i);
      }
    }
    g.dispose();
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
