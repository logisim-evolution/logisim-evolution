/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.icons.BreakpointIcon;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.util.AssemblerInterface;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

public class BreakpointPanel extends JPanel
    implements CaretListener, LocaleListener, ActionListener, BaseKeyListenerContract {

  private static final long serialVersionUID = 1L;
  private final RSyntaxTextArea asmWindow;
  private final RTextScrollPane debugScrollPane;
  private final JLabel lineIndicator;
  private final HashMap<Integer, Integer> debugLines;
  private final JButton addBreakPoint;
  private final JButton removeBreakPoint;
  private int oldCaretPos;
  private int currentLine;
  private int maxLines;

  public BreakpointPanel(String highLiter) {
    asmWindow = new RSyntaxTextArea(20, 60);
    asmWindow.setSyntaxEditingStyle(highLiter);
    asmWindow.setEditable(false);
    asmWindow.setPopupMenu(null);
    asmWindow.addCaretListener(this);
    asmWindow.addKeyListener(this);
    debugScrollPane = new RTextScrollPane(asmWindow);
    debugScrollPane.setLineNumbersEnabled(false);
    debugScrollPane.setIconRowHeaderEnabled(true);
    debugScrollPane.getGutter().setBookmarkIcon(new BreakpointIcon());
    debugScrollPane.getGutter().setBookmarkingEnabled(true);
    JPanel info = new JPanel();
    info.setLayout(new BorderLayout());
    lineIndicator = new JLabel();
    lineIndicator.setHorizontalAlignment(JLabel.CENTER);
    addBreakPoint = new JButton();
    addBreakPoint.addActionListener(this);
    removeBreakPoint = new JButton();
    removeBreakPoint.addActionListener(this);
    info.add(addBreakPoint, BorderLayout.WEST);
    info.add(lineIndicator, BorderLayout.CENTER);
    info.add(removeBreakPoint, BorderLayout.EAST);
    setLayout(new BorderLayout());
    add(info, BorderLayout.NORTH);
    add(debugScrollPane);
    oldCaretPos = -1;
    debugLines = new HashMap<>();
    localeChanged();
    LocaleManager.addLocaleListener(this);
  }

  public void loadProgram(
      CircuitState state,
      SocProcessorInterface pIf,
      ElfProgramHeader progInfo,
      ElfSectionHeader sectInfo,
      AssemblerInterface assembler) {
    debugLines.clear();
    debugScrollPane.getGutter().removeAllTrackingIcons();
    asmWindow.setText(assembler.getProgram(state, pIf, progInfo, sectInfo, debugLines));
    asmWindow.setCaretPosition(0);
  }

  public void gotoLine(int line) {
    Element root = asmWindow.getDocument().getDefaultRootElement();
    int curetPos = 0;
    while (root.getElementIndex(curetPos) != line && curetPos < root.getEndOffset()) curetPos++;
    asmWindow.setCaretPosition(curetPos);
  }

  public Map<Integer, Integer> getBreakPoints() {
    HashMap<Integer, Integer> breakPoints = new HashMap<>();
    for (int i : getBreakpointLines()) breakPoints.put(debugLines.get(i), i);
    return breakPoints;
  }

  @Override
  public void caretUpdate(CaretEvent e) {
    int caretPos = e.getDot();
    if (caretPos != oldCaretPos) {
      oldCaretPos = caretPos;
      Element root = asmWindow.getDocument().getDefaultRootElement();
      currentLine = root.getElementIndex(caretPos) + 1;
      maxLines = root.getElementCount();
      localeChanged();
      ArrayList<Integer> dlines = getBreakpointLines();
      if (!debugLines.containsKey(currentLine)) {
        addBreakPoint.setEnabled(false);
        removeBreakPoint.setEnabled(false);
      } else if (dlines.contains(currentLine)) {
        removeBreakPoint.setEnabled(true);
        addBreakPoint.setEnabled(false);
      } else {
        removeBreakPoint.setEnabled(false);
        addBreakPoint.setEnabled(true);
      }
    }
  }

  @Override
  public void localeChanged() {
    lineIndicator.setText(S.get("RV32imAsmLineIndicator", currentLine, maxLines));
    addBreakPoint.setText(S.get("RV32imSetBreakpoint"));
    removeBreakPoint.setText(S.get("RV32imRemoveBreakPoint"));
  }

  private ArrayList<Integer> getBreakpointLines() {
    ArrayList<Integer> lines = new ArrayList<>();
    GutterIconInfo[] bookmarks = debugScrollPane.getGutter().getBookmarks();
    Element root = asmWindow.getDocument().getDefaultRootElement();
    for (GutterIconInfo bookmark : bookmarks) {
      int pos = bookmark.getMarkedOffset();
      int line = root.getElementIndex(pos) + 1;
      if (debugLines.containsKey(line)) lines.add(line);
      else
        try {
          debugScrollPane.getGutter().toggleBookmark(line - 1);
        } catch (BadLocationException ignored) {
        }
    }
    return lines;
  }

  private void updateBreakpoint() {
    if (getBreakpointLines().contains(currentLine)) {
      try {
        debugScrollPane.getGutter().toggleBookmark(currentLine - 1);
      } catch (BadLocationException e) {
        return;
      }
      addBreakPoint.setEnabled(true);
      removeBreakPoint.setEnabled(false);
    } else {
      try {
        debugScrollPane.getGutter().toggleBookmark(currentLine - 1);
      } catch (BadLocationException e) {
        return;
      }
      addBreakPoint.setEnabled(false);
      removeBreakPoint.setEnabled(true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == addBreakPoint || e.getSource() == removeBreakPoint) updateBreakpoint();
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyChar() == 'b' && debugLines.containsKey(currentLine)) updateBreakpoint();
  }
}
