/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static com.cburch.logisim.tools.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import lombok.val;

public class MenuTool extends Tool {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Menu Tool";

  private static class MenuComponent extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 1L;
    final Project proj;
    final Circuit circ;
    final Component comp;
    final JMenuItem del = new JMenuItem(S.get("compDeleteItem"));
    final JMenuItem attrs = new JMenuItem(S.get("compShowAttrItem"));
    final JMenuItem rotateRight = new JMenuItem(S.get("compRotateRight"));
    final JMenuItem rotateLeft = new JMenuItem(S.get("compRotateLeft"));

    MenuComponent(Project proj, Circuit circ, Component comp) {
      this.proj = proj;
      this.circ = circ;
      this.comp = comp;
      val canChange = proj.getLogisimFile().contains(circ);

      if (comp.getAttributeSet().containsAttribute(StdAttr.FACING)) {
        add(rotateRight);
        rotateRight.addActionListener(this);
        add(rotateLeft);
        rotateLeft.addActionListener(this);
      }

      add(del);
      del.addActionListener(this);
      del.setEnabled(canChange);
      add(attrs);
      attrs.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      val src = e.getSource();
      if (src == del) {
        val circ = proj.getCurrentCircuit();
        val xn = new CircuitMutation(circ);
        xn.remove(comp);
        proj.doAction(xn.toAction(S.getter("removeComponentAction", comp.getFactory().getDisplayGetter())));
      } else if (src == attrs) {
        proj.getFrame().viewComponentAttributes(circ, comp);
      } else if (src == rotateRight) {
        val circ = proj.getCurrentCircuit();
        val xn = new CircuitMutation(circ);
        val d = comp.getAttributeSet().getValue(StdAttr.FACING);
        xn.set(comp, StdAttr.FACING, d.getRight());
        proj.doAction(xn.toAction(S.getter("rotateComponentAction", comp.getFactory().getDisplayGetter())));
      } else if (src == rotateLeft) {
        val circ = proj.getCurrentCircuit();
        val xn = new CircuitMutation(circ);
        val d = comp.getAttributeSet().getValue(StdAttr.FACING);
        xn.set(comp, StdAttr.FACING, d.getLeft());
        proj.doAction(xn.toAction(S.getter("rotateComponentAction", comp.getFactory().getDisplayGetter())));
      }
    }
  }

  private static class MenuSelection extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 1L;
    final Project proj;
    final JMenuItem del = new JMenuItem(S.get("selDeleteItem"));
    final JMenuItem cut = new JMenuItem(S.get("selCutItem"));
    final JMenuItem copy = new JMenuItem(S.get("selCopyItem"));

    MenuSelection(Project proj) {
      this.proj = proj;
      val canChange = proj.getLogisimFile().contains(proj.getCurrentCircuit());
      add(del);
      del.addActionListener(this);
      del.setEnabled(canChange);
      add(cut);
      cut.addActionListener(this);
      cut.setEnabled(canChange);
      add(copy);
      copy.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      val src = e.getSource();
      val sel = proj.getSelection();
      if (src == del) {
        proj.doAction(SelectionActions.clear(sel));
      } else if (src == cut) {
        proj.doAction(SelectionActions.cut(sel));
      } else if (src == copy) {
        proj.doAction(SelectionActions.copy(sel));
      }
    }
  }

  public MenuTool() {}

  @Override
  public boolean equals(Object other) {
    return other instanceof MenuTool;
  }

  @Override
  public String getDescription() {
    return S.get("menuToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("menuTool");
  }

  @Override
  public int hashCode() {
    return MenuTool.class.hashCode();
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    val x = e.getX();
    val y = e.getY();
    val pt = Location.create(x, y);

    JPopupMenu menu;
    val proj = canvas.getProject();
    val sel = proj.getSelection();
    val in_sel = sel.getComponentsContaining(pt, g);
    if (!in_sel.isEmpty()) {
      val comp = in_sel.iterator().next();
      if (sel.getComponents().size() > 1) {
        menu = new MenuSelection(proj);
      } else {
        menu = new MenuComponent(proj, canvas.getCircuit(), comp);
        val extender = (MenuExtender) comp.getFeature(MenuExtender.class);
        if (extender != null) extender.configureMenu(menu, proj);
      }
    } else {
      val cl = canvas.getCircuit().getAllContaining(pt, g);
      if (!cl.isEmpty()) {
        val comp = cl.iterator().next();
        menu = new MenuComponent(proj, canvas.getCircuit(), comp);
        val extender = (MenuExtender) comp.getFeature(MenuExtender.class);
        if (extender != null) extender.configureMenu(menu, proj);
      } else {
        menu = null;
      }
    }

    if (menu != null) {
      canvas.showPopupMenu(menu, x, y);
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    val gfx = c.getGraphics();
    gfx.fillRect(x + 2, y + 1, 9, 2);
    gfx.drawRect(x + 2, y + 3, 15, 12);
    gfx.setColor(Color.lightGray);
    gfx.drawLine(x + 4, y + 2, x + 8, y + 2);
    for (var offsetY = y + 6; offsetY < y + 15; offsetY += 3) {
      gfx.drawLine(x + 4, offsetY, x + 14, offsetY);
    }
  }
}
