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

public class HexFrame extends LFrame {
  private class EditListener implements ActionListener, ChangeListener {
    private Clip clip = null;

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
      boolean sel = editor.selectionExists();
      boolean clip = true; // TODO editor.clipboardExists();
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

    public void stateChanged(ChangeEvent e) {
      enableItems((LogisimMenuBar) getJMenuBar());
    }
  }

  private class MyListener implements ActionListener, LocaleListener {
    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      if (src == open) {
        HexFile.open((MemContents)model, HexFrame.this, proj, instance);
      } else if (src == save) {
        HexFile.save((MemContents)model, HexFrame.this, proj, instance);
      } else if (src == close) {
        WindowEvent e = new WindowEvent(HexFrame.this, WindowEvent.WINDOW_CLOSING);
        HexFrame.this.processWindowEvent(e);
      }
    }

    public void localeChanged() {
      setTitle(S.get("hexFrameTitle"));
      open.setText(S.get("openButton"));
      save.setText(S.get("saveButton"));
      close.setText(S.get("closeButton"));
    }
  }
  
  public void closeAndDispose() {
    WindowEvent e = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    processWindowEvent(e);
    dispose();
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

    public void localeChanged() {
      setText(S.get("hexFrameMenuItem"));
    }
  }

  private static final long serialVersionUID = 1L;

  private WindowMenuManager windowManager = new WindowMenuManager();
  private EditListener editListener = new EditListener();
  private MyListener myListener = new MyListener();
  private HexModel model;
  private HexEditor editor;
  private JButton open = new JButton();
  private JButton save = new JButton();
  private JButton close = new JButton();
  private Instance instance;
  private Project proj;

  public HexFrame(Project proj, Instance instance, HexModel model) {
	super(false,proj);
	setDefaultCloseOperation(HIDE_ON_CLOSE);

	LogisimMenuBar menubar = new LogisimMenuBar(this, proj);
    setJMenuBar(menubar);
    
    this.model = model;
    this.editor = new HexEditor(model);
    this.instance = instance;
    this.proj = proj;

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(open);
    buttonPanel.add(save);
    buttonPanel.add(close);
    open.addActionListener(myListener);
    save.addActionListener(myListener);
    close.addActionListener(myListener);

    Dimension pref = editor.getPreferredSize();
    JScrollPane scroll =
        new JScrollPane(
            editor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
    setLocationRelativeTo(proj.getFrame());
  }

  @Override
  public void setVisible(boolean value) {
    if (value && !isVisible()) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }
}
