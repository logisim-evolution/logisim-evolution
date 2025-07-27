/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.hex;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.hex.HexEditor;
import com.cburch.hex.HexModel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.MemContents;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HexFrame extends LFrame.SubWindow {
  private static final long serialVersionUID = 1L;
  private final WindowMenuManager windowManager = new WindowMenuManager();
  private final EditListener editListener = new EditListener();
  private final MyListener myListener = new MyListener();
  private final HexModel model;
  private final HexEditor editor;
  private final JButton open = new JButton();
  private final JButton save = new JButton();
  private final JButton close = new JButton();
  private final Instance instance;

  public HexFrame(Project project, Instance instance, HexModel model) {
    super(project);
    setDefaultCloseOperation(HIDE_ON_CLOSE);

    this.model = model;
    this.editor = new HexEditor(model);
    this.instance = instance;

    final var buttonPanel = new JPanel();
    buttonPanel.add(open);
    buttonPanel.add(save);
    buttonPanel.add(close);
    open.addActionListener(myListener);
    save.addActionListener(myListener);
    close.addActionListener(myListener);

    final var pref = editor.getPreferredSize();
    final var scroll =
        new JScrollPane(
            editor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
    //Fix: ensure minimum size for the hex editor so it can display 32b addresses.
    pref.width = Math.max(pref.width, 1280);
    // Fix: pref.height can be 0, so min(a, b) can return 0. I replaced it with max. Width will never be 0.
    pref.height = Math.min(pref.height, pref.width * 3 / 2);
    scroll.setPreferredSize(pref);
    scroll.getViewport().setBackground(editor.getBackground());

    Container contents = getContentPane();
    contents.add(scroll, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.SOUTH);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();

    Dimension size = getSize();
    Dimension screen = getToolkit().getScreenSize();
    if (size.width > screen.width || size.height > screen.height) {
      size.width = Math.min(size.width, screen.width);
      size.height = Math.min(size.height, screen.height);
      setSize(size);
    }

    editor.getCaret().addChangeListener(editListener);
    editor.getCaret().setDot(0, false);
    editListener.register(menubar);
    setLocationRelativeTo(project.getFrame());
  }

  public void closeAndDispose() {
    WindowEvent e = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    processWindowEvent(e);
    dispose();
  }

  @Override
  public void setVisible(boolean value) {
    if (value && !isVisible()) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }

  private class EditListener implements ActionListener, ChangeListener {
    private Clip clip = null;

    @Override
    public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();
      if (src == LogisimMenuBar.CUT) {
        getClip().copy();
        editor.delete();
      } else if (src == LogisimMenuBar.COPY) {
        getClip().copy();
      } else if (src == LogisimMenuBar.PASTE) {
        getClip().paste();
      } else if (src == LogisimMenuBar.DELETE) {
        editor.delete();
      } else if (src == LogisimMenuBar.SELECT_ALL) {
        editor.selectAll();
      }
    }

    private void enableItems(LogisimMenuBar menubar) {
      final var sel = editor.selectionExists();
      final var clip = true; // TODO editor.clipboardExists();
      menubar.setEnabled(LogisimMenuBar.CUT, sel);
      menubar.setEnabled(LogisimMenuBar.COPY, sel);
      menubar.setEnabled(LogisimMenuBar.PASTE, clip);
      menubar.setEnabled(LogisimMenuBar.DELETE, sel);
      menubar.setEnabled(LogisimMenuBar.SELECT_ALL, true);
    }

    private Clip getClip() {
      if (clip == null) clip = new Clip(editor);
      return clip;
    }

    private void register(LogisimMenuBar menubar) {
      menubar.addActionListener(LogisimMenuBar.CUT, this);
      menubar.addActionListener(LogisimMenuBar.COPY, this);
      menubar.addActionListener(LogisimMenuBar.PASTE, this);
      menubar.addActionListener(LogisimMenuBar.DELETE, this);
      menubar.addActionListener(LogisimMenuBar.SELECT_ALL, this);
      enableItems(menubar);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      enableItems((LogisimMenuBar) getJMenuBar());
    }
  }

  private class MyListener implements ActionListener, LocaleListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      if (src == open) {
        HexFile.open((MemContents) model, HexFrame.this, project, instance);
      } else if (src == save) {
        HexFile.save((MemContents) model, HexFrame.this, project, instance);
      } else if (src == close) {
        WindowEvent e = new WindowEvent(HexFrame.this, WindowEvent.WINDOW_CLOSING);
        HexFrame.this.processWindowEvent(e);
      }
    }

    @Override
    public void localeChanged() {
      setTitle(S.get("hexFrameTitle"));
      open.setText(S.get("openButton"));
      save.setText(S.get("saveButton"));
      close.setText(S.get("closeButton"));
    }
  }

  private class WindowMenuManager extends WindowMenuItemManager implements LocaleListener {
    WindowMenuManager() {
      super(S.get("hexFrameMenuItem"), false);
      LocaleManager.addLocaleListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return HexFrame.this;
    }

    @Override
    public void localeChanged() {
      setText(S.get("hexFrameMenuItem"));
    }
  }
}
