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
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
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
  private final ArrayList<String> infoMessages;
  private FpgaCommanderTextWindow infoWindow;

  private final JList<Object> warnings;
  private final JComponent panelWarnings;
  private final FpgaCommanderListModel warningsList;
  private final FpgaCommanderListWindow warningsWindow;

  private final JList<Object> errors;
  private final JComponent panelErrors;
  private final FpgaCommanderListModel errorsList;
  private final FpgaCommanderListWindow errorsWindow;

  private FpgaCommanderTextWindow consoleWindow;
  private final JTextArea textAreaConsole;
  private final JComponent panelConsole;
  private final ArrayList<String> consoleMessages;

  private boolean drcTraceActive = false;
  private SimpleDrcContainer activeDrcContainer;

  private final Project myProject;

  public FpgaReportTabbedPane(Project myProject) {
    super();
    this.myProject = myProject;
    /* first we setup all info for the first tab, the Information window */
    infoMessages = new ArrayList<>();
    textAreaInfo = new JTextArea(10, 50);
    textAreaInfo.setForeground(Color.GRAY);
    textAreaInfo.setBackground(Color.BLACK);
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

    /* now we setup the Warning window */
    warningsList = new FpgaCommanderListModel(true);
    warnings = new JList<>();
    warnings.setBackground(Color.BLACK);
    warnings.setForeground(Color.ORANGE);
    warnings.setSelectionBackground(Color.ORANGE);
    warnings.setSelectionForeground(Color.BLACK);
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
    warningsWindow =
        new FpgaCommanderListWindow("FPGACommander: Warnings", Color.ORANGE, true, warningsList);
    warningsWindow.setSize(new Dimension(740, 400));
    warningsWindow.addWindowListener(this);
    warningsWindow.getListObject().addMouseListener(this);

    /* here we setup the Error window */
    errorsList = new FpgaCommanderListModel(false);
    errors = new JList<>();
    errors.setBackground(Color.BLACK);
    errors.setForeground(Color.RED);
    errors.setSelectionBackground(Color.RED);
    errors.setSelectionForeground(Color.BLACK);
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
    errorsWindow =
        new FpgaCommanderListWindow("FPGACommander: Errors", Color.RED, true, errorsList);
    errorsWindow.addWindowListener(this);
    errorsWindow.setSize(new Dimension(740, 400));
    errorsWindow.getListObject().addMouseListener(this);

    /* finally we define the console window */
    consoleMessages = new ArrayList<>();
    textAreaConsole = new JTextArea(10, 50);
    textAreaConsole.setForeground(Color.LIGHT_GRAY);
    textAreaConsole.setBackground(Color.BLACK);
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

    addMouseListener(this);
    setPreferredSize(new Dimension(700, 20 * fontSize));
  }

  private String getAllignmentSpaces(int index) {
    if (index < 9) {
      return "    " + index;
    } else if (index < 99) {
      return "   " + index;
    } else if (index < 999) {
      return "  " + index;
    } else if (index < 9999) {
      return " " + index;
    }
    return Integer.toString(index);
  }

  public void addInfo(Object message) {
    int nrOfInfos = infoMessages.size() + 1;
    infoMessages.add(getAllignmentSpaces(nrOfInfos) + "> " + message.toString() + "\n");
    if (infoWindow != null) {
      if (infoWindow.isVisible()) {
        updateInfoWindow();
        return;
      }
    }
    updateInfoTab();
  }

  private void updateInfoWindow() {
    final var line = new StringBuilder();
    for (final var mes : infoMessages) {
      line.append(mes);
    }
    infoWindow.set(line.toString(), infoMessages.size());
  }

  private void updateInfoTab() {
    final var line = new StringBuilder();
    for (final var mes : infoMessages) {
      line.append(mes);
    }
    textAreaInfo.setText(line.toString());
    final var idx = indexOfComponent(panelInfos);
    if (idx >= 0) {
      setSelectedIndex(idx);
      setTitleAt(idx, "Infos (" + infoMessages.size() + ")");
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

  public void addConsole(String Message) {
    consoleMessages.add(Message + "\n");
    if (consoleWindow != null)
      if (consoleWindow.isVisible()) {
        updateConsoleWindow();
      }
    updateConsoleTab();
  }

  private void updateConsoleWindow() {
    final var lines = new StringBuilder();
    for (final var mes : consoleMessages) {
      lines.append(mes);
    }
    consoleWindow.set(lines.toString(), 0);
  }

  private void updateConsoleTab() {
    final var lines = new StringBuilder();
    for (final var mes : consoleMessages) {
      lines.append(mes);
    }
    textAreaConsole.setText(lines.toString());
    final var idx = indexOfComponent(panelConsole);
    if (idx >= 0) {
      setSelectedIndex(idx);
      panelConsole.revalidate();
      panelConsole.repaint();
    }
  }

  public void clearConsole() {
    consoleMessages.clear();
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
    infoMessages.clear();
    int idx = indexOfComponent(panelInfos);
    if (idx >= 0) {
      setTitleAt(idx, "Infos (" + infoMessages.size() + ")");
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
    if (infoWindow != null) {
      if (infoWindow.isVisible()) {
        infoWindow.setVisible(false);
        add(panelInfos, infoTabIndex);
        updateInfoTab();
      }
    }
    if (warningsWindow != null) {
      if (warningsWindow.isVisible()) {
        warningsWindow.setVisible(false);
        add(panelWarnings, warningsTabIndex);
        setTitleAt(warningsTabIndex, "Warnings (" + warningsList.getCountNr() + ")");
        clearDrcTrace();
      }
    }
    if (errorsWindow != null)
      if (errorsWindow.isVisible()) {
        errorsWindow.setVisible(false);
        add(panelErrors, errorsTabIndex);
        setTitleAt(errorsTabIndex, "Errors (" + errorsList.getCountNr() + ")");
        clearDrcTrace();
      }
    if (consoleWindow != null)
      if (consoleWindow.isVisible()) {
        consoleWindow.setVisible(false);
        add(panelConsole, consoleTabIndex);
        updateConsoleTab();
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
    if (e.getClickCount() > 1) {

      if (e.getSource().equals(this)) {
        if (getComponentCount() > 0) {
          if (getSelectedComponent().equals(panelInfos)) {
            if (infoWindow != null) {
              infoWindow.setVisible(true);
              updateInfoWindow();
            } else {
              infoWindow = new FpgaCommanderTextWindow("FPGACommander: Infos", Color.GRAY, true);
              infoWindow.setVisible(true);
              updateInfoWindow();
              infoWindow.addWindowListener(this);
            }
            remove(getSelectedIndex());
          } else if (getSelectedComponent().equals(panelConsole)) {
            if (consoleWindow != null) {
              consoleWindow.setVisible(true);
              updateConsoleWindow();
            } else {
              consoleWindow =
                  new FpgaCommanderTextWindow("FPGACommander: Console", Color.LIGHT_GRAY, false);
              consoleWindow.setVisible(true);
              updateConsoleWindow();
              consoleWindow.addWindowListener(this);
            }
            remove(getSelectedIndex());
          } else if (getSelectedComponent().equals(panelWarnings)) {
            if (warningsWindow != null) {
              warningsWindow.setVisible(true);
              remove(getSelectedIndex());
            }
          } else if (getSelectedComponent().equals(panelErrors)) {
            if (errorsWindow != null) {
              errorsWindow.setVisible(true);
              remove(getSelectedIndex());
            }
          }
        }
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

  @Override
  public void windowClosing(WindowEvent e) {
    if (e.getSource().equals(infoWindow)) {
      add(panelInfos, infoTabIndex);
      updateInfoTab();
    }
    if (e.getSource().equals(consoleWindow)) {
      add(panelConsole, getComponentCount());
      updateConsoleTab();
    }
    if (e.getSource().equals(warningsWindow)) {
      var idx = getComponentCount();
      HashSet<Component> comps = new HashSet<>(Arrays.asList(getComponents()));
      if (comps.contains(panelConsole)) idx = indexOfComponent(panelConsole);
      if (comps.contains(panelErrors)) idx = indexOfComponent(panelErrors);
      add(panelWarnings, idx);
      setTitleAt(idx, "Warnings (" + warningsList.getCountNr() + ")");
      setSelectedIndex(idx);
      clearDrcTrace();
    }
    if (e.getSource().equals(errorsWindow)) {
      var idx = getComponentCount();
      final var comps = new HashSet<>(Arrays.asList(getComponents()));
      if (comps.contains(panelConsole)) idx = indexOfComponent(panelConsole);
      add(panelErrors, idx);
      setTitleAt(idx, "Errors (" + errorsList.getCountNr() + ")");
      setSelectedIndex(idx);
      clearDrcTrace();
    }
  }
}
