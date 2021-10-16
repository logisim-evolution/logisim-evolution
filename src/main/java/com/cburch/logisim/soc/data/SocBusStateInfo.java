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

import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.circuit.ComponentDataGuiProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
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
import com.cburch.logisim.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class SocBusStateInfo extends JDialog
    implements ActionListener, LocaleListener, BaseWindowListenerContract {

  private static final long serialVersionUID = 1L;

  public static final int TRACE_WIDTH = 630;
  public static final int TRACE_HEIGHT = 30;
  public static final int BLOCK_WIDTH = 238;

  public interface SocBusStateListener {
    void fireCanged(SocBusState item);
  }

  public static class SocBusState implements InstanceData, Cloneable, ComponentDataGuiProvider {

    public static class SocBusStateTrace extends JPanel {
      private static final long serialVersionUID = 1L;
      private final SocBusTransaction action;
      private final long index;
      private final TraceWindowTableModel model;

      public SocBusStateTrace(SocBusTransaction action, long index, TraceWindowTableModel model) {
        this.action = action;
        this.index = index;
        this.model = model;
      }

      public SocBusTransaction getTransaction() {
        return action;
      }

      @Override
      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(AppPreferences.getScaledFont(g2.getFont()));
        if (action == null) {
          if (index == 0)
            GraphicsUtil.drawCenteredText(
                g2, S.get("SocBusNoTrace"), getWidth() / 2, getHeight() / 2);
          g2.dispose();
          return;
        }
        int boxWidth = action.paint(g2, index, model.getBoxWidth());
        g2.dispose();
        if (boxWidth != model.getBoxWidth()) model.setBoxWidth(boxWidth);
      }
    }

    private static final int NR_OF_TRACES_TO_KEEP = 10000;
    private final LinkedList<SocBusTransaction> trace;
    private long startTraceIndex;
    private final SocBusStateInfo parent;
    private final Instance instance;
    private final ArrayList<SocBusStateListener> listeners;

    public SocBusState(SocBusStateInfo parent, Instance instance) {
      trace = new LinkedList<>();
      startTraceIndex = 0;
      this.parent = parent;
      this.instance = instance;
      SocBus.MENU_PROVIDER.registerBusState(this, instance);
      listeners = new ArrayList<>();
    }

    @Override
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

    public void paint(Graphics2D g, Bounds b) {
      if (trace.isEmpty()) {
        GraphicsUtil.drawCenteredText(g, S.get("SocBusNoTrace"), b.getCenterX(), b.getCenterY());
        return;
      }
      long nrOfTraces = b.getHeight() / TRACE_HEIGHT;
      if (nrOfTraces > trace.size())
        nrOfTraces = trace.size();
      int startIndex = trace.size() - 1;
      for (int i = 0; i < nrOfTraces; i++) {
        SocBusTransaction t = trace.get(startIndex - i);
        t.paint(b.getX() + 1, b.getY() + 1 + i * TRACE_HEIGHT, g, startTraceIndex + startIndex - i);
      }
    }

    public int getNrOfEntires() {
      return trace.size();
    }

    public void registerListener(SocBusStateListener l) {
      if (!listeners.contains(l)) listeners.add(l);
    }

    public void deregisterListener(SocBusStateListener l) {
      listeners.remove(l);
    }

    public SocBusStateTrace getEntry(int index, TraceWindowTableModel model) {
      if (index < 0 || index >= trace.size()) {
        if (index == 0)
          return new SocBusStateTrace(null, 0, model);
        return null;
      }
      long indx = startTraceIndex + trace.size() - index - 1;
      return new SocBusStateTrace(trace.get(trace.size() - index - 1), indx, model);
    }

    @Override
    public void destroy() {
      if (parent != null && parent.isVisible())
        parent.setVisible(false);
      SocBus.MENU_PROVIDER.deregisterBusState(this, instance);
    }
  }

  private final SocSimulationManager socManager;
  private Component myComp;
  private final ArrayList<SocBusSnifferInterface> sniffers;
  private final JButton okButton;
  private final JLabel title;
  private final JScrollPane scroll;
  private final SocMemMapModel memMap;

  public SocBusStateInfo(SocSimulationManager man, Component comp) {
    super();
    LocaleManager.addLocaleListener(this);
    socManager = man;
    myComp = comp;
    sniffers = new ArrayList<>();
    memMap = new SocMemMapModel();
    setTitle(S.get("SocMemMapWindowTitle") + getName());
    setLayout(new BorderLayout());
    title = new JLabel(S.get("SocMemoryMapTitle"), JLabel.CENTER);
    add(title, BorderLayout.NORTH);
    JTable table =
        new JTable(memMap) {
          private static final long serialVersionUID = 1L;

          @Override
          public TableCellRenderer getCellRenderer(int row, int column) {
            return memMap.getCellRender();
          }
        };
    table.getTableHeader().setDefaultRenderer(memMap.getHeaderRenderer());
    table.setFillsViewportHeight(true);
    table.setRowHeight(AppPreferences.getScaled(20));
    table.addMouseListener(memMap);
    scroll = new JScrollPane(table);
    scroll.setPreferredSize(
        new Dimension(AppPreferences.getScaled(320), AppPreferences.getScaled(240)));
    add(scroll, BorderLayout.CENTER);
    okButton = new JButton(S.get("SocMemoryMapOk"));
    add(okButton, BorderLayout.SOUTH);
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
    sniffers.remove(sniffer);
  }

  public List<SocBusSlaveInterface> getSlaves() {
    return memMap.getSlaves();
  }

  @Override
  public String getName() {
    var name = myComp.getAttributeSet().getValue(StdAttr.LABEL);
    if (StringUtil.isNullOrEmpty(name)) {
      final var loc = myComp.getLocation();
      name = myComp.getFactory().getDisplayName() + "@" + loc.getX() + "," + loc.getY();
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
    final var slaves = memMap.getSlaves();
    if (slaves.isEmpty()) trans.setError(SocBusTransaction.NO_SLAVES_ERROR);
    else if (trans.isReadTransaction()
        && trans.isWriteTransaction()
        && !trans.isAtomicTransaction()) trans.setError(SocBusTransaction.NONE_ATOMIC_READ_WRITE_ERROR);
    else {
      for (int i = 0; i < slaves.size(); i++) {
        if (slaves.get(i).canHandleTransaction(trans)) {
          nrOfReponders++;
          reponder = i;
        }
      }
      if (nrOfReponders == 0)
        trans.setError(SocBusTransaction.NO_RESPONS_ERROR);
      else if (nrOfReponders != 1)
        trans.setError(SocBusTransaction.MULTIPLE_SLAVES_ERROR);
      else
        slaves.get(reponder).handleTransaction(trans);
    }
    if (!trans.hasError() && !trans.isHidden()) {
      for (SocBusSnifferInterface sniffer : sniffers)
        sniffer.sniffTransaction(trans);
    }
    if (!trans.isHidden()) {
      final var data = getRegPropagateState();
      if (data != null) {
        data.addTransaction(trans);
        if (myComp.getAttributeSet().getValue(SocBusAttributes.SOC_TRACE_VISIBLE))
          ((InstanceComponent) myComp).getInstance().fireInvalidated();
      }
    }
  }

  public void paint(
      int x, int y, Graphics2D g2, Instance inst, boolean visible, InstanceData info) {
    Graphics2D g = (Graphics2D) g2.create();
    g.translate(x + 5, y + 25);
    int nrOfTraces = inst.getAttributeValue(SocBusAttributes.NrOfTracesAttr).getWidth();
    int height = nrOfTraces * TRACE_HEIGHT;
    g.setColor(Color.YELLOW);
    g.fillRect(0, 0, TRACE_WIDTH, height);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, TRACE_WIDTH, height);
    if (!visible)
      GraphicsUtil.drawCenteredText(g, S.get("SocHiddenForFasterSimulation"), 320, height / 2);
    else {
      if (info != null)
        ((SocBusState) info).paint(g, Bounds.create(0, 0, 640, height));
    }
    g.dispose();
  }

  public SocBusState getNewState(Instance instance) {
    return new SocBusState(this, instance);
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
  public void windowClosing(WindowEvent e) {
    setVisible(false);
  }
}
