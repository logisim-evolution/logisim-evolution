/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.fpga.data.FpgaCommanderListModel;
import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultCaret;

public class FpgaReportTabbedPane extends JTabbedPane
    implements BaseMouseListenerContract, BaseWindowListenerContract {

  /** */
  private static final long serialVersionUID = 1L;

  private static final int fontSize = 12;
  private static final GridLayout consolesLayout = new GridLayout(1, 1);
  private static final int infoTabIndex = 0;
  private static final int warningsTabIndex = 1;
  private static final int errorsTabIndex = 2;
  private static final int consoleTabIndex = 3;

  private final JTextArea textAreaInfo;
  private final JComponent panelInfos;
  private int infoMessagesCount;
  private final FpgaCommanderTextWindow infoWindow;

  private final JList<Object> warnings;
  private final JComponent panelWarnings;
  private final FpgaCommanderListModel warningsList;
  private final FpgaCommanderListWindow warningsWindow;

  private final JList<Object> errors;
  private final JComponent panelErrors;
  private final FpgaCommanderListModel errorsList;
  private final FpgaCommanderListWindow errorsWindow;

  private final FpgaCommanderTextWindow consoleWindow;
  private final JTextArea textAreaConsole;
  private final JComponent panelConsole;
  private final ConcurrentLinkedQueue<String> consoleMessagesQueue = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean consoleUpdatePending = new AtomicBoolean(false);

  private boolean drcTraceActive = false;
  private SimpleDrcContainer activeDrcContainer;

  private final Project myProject;

  public FpgaReportTabbedPane(Project myProject) {
    super();
    this.myProject = myProject;
    /* first we setup all info for the first tab, the Information window */
    infoMessagesCount = 0;
    textAreaInfo = new JTextArea(10, 50);
    textAreaInfo.setForeground(UIManager.getColor("TextArea.foreground"));
    textAreaInfo.setBackground(UIManager.getColor("TextArea.background"));
    textAreaInfo.setFont(new Font("monospaced", Font.PLAIN, fontSize));
    textAreaInfo.setEditable(false);
    textAreaInfo.setText(null);
    var caret = (DefaultCaret) textAreaInfo.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    final var textMessages = new JScrollPane(textAreaInfo);
    textMessages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textMessages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panelInfos = new JPanel();
    panelInfos.setLayout(consolesLayout);
    panelInfos.add(textMessages);
    panelInfos.setName("Infos (0)");
    add(panelInfos, infoTabIndex);
    infoWindow = new FpgaCommanderTextWindow("FPGACommander: Infos", Color.BLACK, true);
    infoWindow.addWindowListener(this);

    /* now we setup the Warning window */
    warningsList = new FpgaCommanderListModel(true);
    warnings = new JList<>();
    warnings.setBackground(UIManager.getColor("List.background"));
    warnings.setForeground(Color.ORANGE);
    warnings.setSelectionBackground(Color.ORANGE);
    warnings.setSelectionForeground(UIManager.getColor("List.background"));
    warnings.setFont(new Font("monospaced", Font.PLAIN, fontSize));
    warnings.setModel(warningsList);
    warnings.setCellRenderer(warningsList.getMyRenderer());
    warnings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    warnings.addMouseListener(this);
    final var textWarnings = new JScrollPane(warnings);
    textWarnings.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textWarnings.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panelWarnings = new JPanel();
    panelWarnings.setLayout(consolesLayout);
    panelWarnings.add(textWarnings);
    panelWarnings.setName("Warnings (0)");
    add(panelWarnings, warningsTabIndex);
    warningsWindow = new FpgaCommanderListWindow("FPGACommander: Warnings", Color.ORANGE, true, warningsList);
    warningsWindow.addWindowListener(this);
    warningsWindow.getListObject().addMouseListener(this);

    /* here we setup the Error window */
    errorsList = new FpgaCommanderListModel(false);
    errors = new JList<>();
    errors.setBackground(UIManager.getColor("List.background"));
    errors.setForeground(Color.RED);
    errors.setSelectionBackground(Color.RED);
    errors.setSelectionForeground(UIManager.getColor("List.background"));
    errors.setFont(new Font("monospaced", Font.PLAIN, fontSize));
    errors.setModel(errorsList);
    errors.setCellRenderer(errorsList.getMyRenderer());
    errors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    errors.addMouseListener(this);
    final var textErrors = new JScrollPane(errors);
    textErrors.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textErrors.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panelErrors = new JPanel();
    panelErrors.setLayout(consolesLayout);
    panelErrors.add(textErrors);
    panelErrors.setName("Errors (0)");
    add(panelErrors, errorsTabIndex);
    errorsWindow = new FpgaCommanderListWindow("FPGACommander: Errors", Color.RED, true, errorsList);
    errorsWindow.addWindowListener(this);
    errorsWindow.getListObject().addMouseListener(this);

    /* finally we define the console window */
    textAreaConsole = new JTextArea(10, 50);
    textAreaConsole.setForeground(UIManager.getColor("TextArea.foreground"));
    textAreaConsole.setBackground(UIManager.getColor("TextArea.background"));
    textAreaConsole.setFont(new Font("monospaced", Font.PLAIN, fontSize));
    textAreaConsole.setEditable(false);
    textAreaConsole.setText(null);
    caret = (DefaultCaret) textAreaConsole.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    final var textConsole = new JScrollPane(textAreaConsole);
    textConsole.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    textConsole.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panelConsole = new JPanel();
    panelConsole.setLayout(consolesLayout);
    panelConsole.add(textConsole);
    panelConsole.setName("Console");
    add(panelConsole, consoleTabIndex);
    consoleWindow = new FpgaCommanderTextWindow("FPGACommander: Console", Color.BLACK, false);
    consoleWindow.addWindowListener(this);

    addMouseListener(this);
    setPreferredSize(new Dimension(700, 20 * fontSize));
  }

  private String getAllignmentSpaces(int index) {
    if (index <= 9) {
      return "    " + index;
    } else if (index <= 99) {
      return "   " + index;
    } else if (index <= 999) {
      return "  " + index;
    } else if (index <= 9999) {
      return " " + index;
    }
    return Integer.toString(index);
  }

  public void addInfo(Object message) {
    final var line = getAllignmentSpaces(++infoMessagesCount) + "> " + message.toString() + "\n";
    infoWindow.append(line, infoMessagesCount);
    textAreaInfo.append(line);
    final var idx = indexOfComponent(panelInfos);
    if (idx >= 0) {
      setSelectedIndex(idx);
      setTitleAt(idx, "Infos (" + infoMessagesCount + ")");
      panelInfos.revalidate();
      panelInfos.repaint();
    }
  }

  public void addWarning(Object message) {
    warningsList.add(message);
    final var idx = indexOfComponent(panelWarnings);
    if (idx >= 0) {
      setSelectedIndex(idx);
      setTitleAt(idx, "Warnings (" + warningsList.getCountNr() + ")");
      panelWarnings.revalidate();
      panelWarnings.repaint();
    }
  }

  public void addErrors(Object message) {
    errorsList.add(message);
    final var idx = indexOfComponent(panelErrors);
    if (idx >= 0) {
      setSelectedIndex(idx);
      setTitleAt(idx, "Errors (" + errorsList.getCountNr() + ")");
      panelErrors.revalidate();
      panelErrors.repaint();
    }
  }

  public void addConsole(String message) {
    consoleMessagesQueue.add(message + "\n");

    if (consoleUpdatePending.compareAndSet(false, true)) {
        SwingUtilities.invokeLater(this::flushConsoleMessages);
    }
  }

  private void flushConsoleMessages() {
    try {
      String message;
      while ((message = consoleMessagesQueue.poll()) != null) {
        consoleWindow.append(message, 0);
        textAreaConsole.append(message);
      }
      textAreaConsole.setCaretPosition(textAreaConsole.getDocument().getLength());
    } finally {
      consoleUpdatePending.set(false);
      if (!consoleMessagesQueue.isEmpty() && consoleUpdatePending.compareAndSet(false, true)) {
        SwingUtilities.invokeLater(this::flushConsoleMessages);
      }
    }
  }

  public void clearConsole() {
    consoleMessagesQueue.clear();
    textAreaConsole.setText(null);
    consoleWindow.clear();
    // Select the console tab at the same time
    final var idx = indexOfComponent(panelConsole);
    if (idx >= 0) {
      setSelectedIndex(idx);
      panelConsole.revalidate();
      panelConsole.repaint();
    }
  }

  public void clearDrcTrace() {
    if (drcTraceActive) {
      activeDrcContainer.clearMarks();
      drcTraceActive = false;
      if (myProject != null) myProject.repaintCanvas();
    }
  }

  public void clearAllMessages() {
    clearDrcTrace();
    textAreaInfo.setText(null);
    infoWindow.clear();
    infoMessagesCount = 0;
    int idx = indexOfComponent(panelInfos);
    if (idx >= 0) {
      setSelectedIndex(idx);
    }
    warningsList.clear();
    idx = indexOfComponent(panelWarnings);
    if (idx >= 0) setTitleAt(idx, "Warnings (" + warningsList.getCountNr() + ")");
    errorsList.clear();
    idx = indexOfComponent(panelErrors);
    if (idx >= 0) setTitleAt(idx, "Errors (" + errorsList.getCountNr() + ")");
    final var rect = getBounds();
    rect.x = 0;
    rect.y = 0;
    if (EventQueue.isDispatchThread()) paintImmediately(rect);
    else repaint(rect);
  }

  public void closeOpenWindows() {
    if (infoWindow.isVisible()) {
      infoWindow.setVisible(false);
      add(panelInfos, infoTabIndex);
      setTitleAt(infoTabIndex, "Info (" + infoMessagesCount + ")");
    }
    if (warningsWindow.isVisible()) {
      warningsWindow.setVisible(false);
      add(panelWarnings, warningsTabIndex);
      setTitleAt(warningsTabIndex, "Warnings (" + warningsList.getCountNr() + ")");
      clearDrcTrace();
    }
    if (errorsWindow.isVisible()) {
      errorsWindow.setVisible(false);
      add(panelErrors, errorsTabIndex);
      setTitleAt(errorsTabIndex, "Errors (" + errorsList.getCountNr() + ")");
      clearDrcTrace();
    }
    if (consoleWindow.isVisible()) {
      consoleWindow.setVisible(false);
      add(panelConsole, consoleTabIndex);
    }
  }

  private void generateDrcTrace(SimpleDrcContainer dc) {
    drcTraceActive = true;
    activeDrcContainer = dc;
    if (dc.hasCircuit())
      if (myProject != null && !myProject.getCurrentCircuit().equals(dc.getCircuit()))
        myProject.setCurrentCircuit(dc.getCircuit());
    dc.markComponents();
    if (myProject != null) myProject.repaintCanvas();
  }

  @Override
  public void mouseClicked(MouseEvent mouseEvent) {
    // do nothing
  }

  /* Here the mouse events are handled */
  @Override
  public void mousePressed(MouseEvent e) {
    if (e.getClickCount() > 1 && e.getSource().equals(this) && getTabCount() > 0) {
      final var currComp = getSelectedComponent();
      if (currComp.equals(panelInfos)) {
        infoWindow.display(this);
        remove(currComp);
      } else if (currComp.equals(panelConsole)) {
        consoleWindow.display(this);
        remove(currComp);
      } else if (currComp.equals(panelWarnings)) {
        warningsWindow.display(this);
        remove(currComp);
      } else if (currComp.equals(panelErrors)) {
        errorsWindow.display(this);
        remove(currComp);
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    final var sourceIsWarningsWindow =
        warningsWindow != null && e.getSource().equals(warningsWindow.getListObject());
    final var sourceIsErrorsWindow =
        errorsWindow != null && e.getSource().equals(errorsWindow.getListObject());
    if (e.getSource().equals(errors) || sourceIsErrorsWindow) {
      clearDrcTrace();
      var idx = -1;
      if (e.getSource().equals(errors)) idx = errors.getSelectedIndex();
      else if (sourceIsErrorsWindow) idx = errorsWindow.getListObject().getSelectedIndex();
      if (idx >= 0) {
        if (errorsList.getElementAt(idx) instanceof SimpleDrcContainer) {
          generateDrcTrace((SimpleDrcContainer) errorsList.getElementAt(idx));
        }
      }
    } else if (e.getSource().equals(warnings) || sourceIsWarningsWindow) {
      clearDrcTrace();
      var idx = -1;
      if (e.getSource().equals(warnings)) idx = warnings.getSelectedIndex();
      else if (sourceIsWarningsWindow) idx = warningsWindow.getListObject().getSelectedIndex();
      if (idx >= 0)
        if (warningsList.getElementAt(idx) instanceof SimpleDrcContainer)
          generateDrcTrace((SimpleDrcContainer) warningsList.getElementAt(idx));
    }
  }

  private void restore(Component panel, String title, int wanted) {
    if (indexOfComponent(panel) >= 0) return; // already a tab (double close)
    final var idx = Math.max(0, Math.min(wanted, getTabCount()));
    insertTab(title, null, panel, null, idx);
    setSelectedIndex(idx);
  }

  private int before(Component... cs) {
    for (final var c : cs) {
      final var i = indexOfComponent(c); // -1 if not a tab
      if (i >= 0) return i;
    }
    return getTabCount();
  }

  @Override
  public void windowClosing(WindowEvent e) {
    final var src = e.getSource();
    if (src.equals(infoWindow)) {
      restore(panelInfos, "Info (" + infoMessagesCount + ")", infoTabIndex);
    } else if (src.equals(consoleWindow)) {
      restore(panelConsole, "Console", getTabCount());
    } else if (src.equals(warningsWindow)) {
      restore(panelWarnings, "Warnings (" + warningsList.getCountNr() + ")", before(panelErrors, panelConsole));
      clearDrcTrace();
    } else if (src.equals(errorsWindow)) {
      restore(panelErrors, "Errors (" + errorsList.getCountNr() + ")", before(panelConsole));
      clearDrcTrace();
    }
  }
}
