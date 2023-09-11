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

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.util.StringUtil;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class TextTool extends Tool {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Text Tool";

  private class MyListener implements CaretListener, CircuitListener {
    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getCircuit() != caretCircuit) {
        event.getCircuit().removeCircuitListener(this);
        return;
      }
      final var action = event.getAction();
      if (action == CircuitEvent.ACTION_REMOVE) {
        if (event.getData() == caretComponent) {
          caret.cancelEditing();
        }
      } else if (action == CircuitEvent.ACTION_CLEAR) {
        if (caretComponent != null) {
          caret.cancelEditing();
        }
      }
    }

    @Override
    public void editingCanceled(CaretEvent e) {
      if (e.getCaret() != caret) {
        e.getCaret().removeCaretListener(this);
        return;
      }
      caret.removeCaretListener(this);
      caretCircuit.removeCircuitListener(this);

      caretCircuit = null;
      caretComponent = null;
      caretCreatingText = false;
      caret = null;
    }

    @Override
    public void editingStopped(CaretEvent e) {
      if (e.getCaret() != caret) {
        e.getCaret().removeCaretListener(this);
        return;
      }
      caret.removeCaretListener(this);
      caretCircuit.removeCircuitListener(this);

      final var val = caret.getText();
      var isEmpty = StringUtil.isNullOrEmpty(val);
      Action a;
      final var proj = caretCanvas.getProject();
      if (caretCreatingText) {
        if (!isEmpty) {
          final var xn = new CircuitMutation(caretCircuit);
          xn.add(caretComponent);
          a = xn.toAction(S.getter("addComponentAction", Text.FACTORY.getDisplayGetter()));
        } else {
          // don't add the blank text field
          a = null;
        }
      } else {
        if (isEmpty && caretComponent.getFactory() instanceof Text) {
          final var xn = new CircuitMutation(caretCircuit);
          xn.add(caretComponent);
          a = xn.toAction(S.getter("removeComponentAction", Text.FACTORY.getDisplayGetter()));
        } else {
          Object obj = caretComponent.getFeature(TextEditable.class);
          if (obj == null) {
            // should never happen
            a = null;
          } else {
            final var editable = (TextEditable) obj;
            a = editable.getCommitAction(caretCircuit, e.getOldText(), e.getText());
          }
        }
      }

      caretCircuit = null;
      caretComponent = null;
      caretCreatingText = false;
      caret = null;

      if (a != null) proj.doAction(a);
    }
  }

  private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

  private final MyListener listener = new MyListener();
  private final AttributeSet attrs;
  private Caret caret = null;
  private boolean caretCreatingText = false;
  private Canvas caretCanvas = null;
  private Circuit caretCircuit = null;
  private Component caretComponent = null;

  public TextTool() {
    attrs = Text.FACTORY.createAttributeSet();
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return Text.FACTORY.getDefaultAttributeValue(attr, ver);
  }

  @Override
  public void deselect(Canvas canvas) {
    if (caret != null) {
      caret.stopEditing();
      caret = null;
    }
  }

  @Override
  public void draw(Canvas canvas, ComponentDrawContext context) {
    if (caret != null) caret.draw(context.getGraphics());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TextTool;
  }

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  @Override
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public String getDescription() {
    return S.get("textToolDesc");
  }

  @Override
  public String getDisplayName() {
    return S.get("textTool");
  }

  @Override
  public int hashCode() {
    return TextTool.class.hashCode();
  }

  @Override
  public void keyPressed(Canvas canvas, KeyEvent e) {
    if (caret != null) {
      caret.keyPressed(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void keyReleased(Canvas canvas, KeyEvent e) {
    if (caret != null) {
      caret.keyReleased(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void keyTyped(Canvas canvas, KeyEvent e) {
    if (caret != null) {
      caret.keyTyped(e);
      canvas.getProject().repaintCanvas();
    }
  }

  @Override
  public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
    final var proj = canvas.getProject();
    final var circ = canvas.getCircuit();

    if (!proj.getLogisimFile().contains(circ)) {
      if (caret != null) caret.cancelEditing();
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }

    // Maybe user is clicking within the current caret.
    if (caret != null) {
      caret.mouseDragged(e);
      proj.repaintCanvas();
    }
  }

  @Override
  public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
    final var proj = canvas.getProject();
    final var circ = canvas.getCircuit();

    /*
     * This is made to remove an annoying bug that do not unselect current selection
     */
    final var act = SelectionActions.dropAll(canvas.getSelection());
    canvas.getProject().doAction(act);

    if (!proj.getLogisimFile().contains(circ)) {
      if (caret != null) caret.cancelEditing();
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }

    // Maybe user is clicking within the current caret.
    if (caret != null) {
      if (caret.getBounds(g).contains(e.getX(), e.getY())) { // Yes
        caret.mousePressed(e);
        proj.repaintCanvas();
        return;
      } else {
        // No. End the current caret.
        caret.stopEditing();
      }
    }
    // caret will be null at this point

    // Otherwise search for a new caret.
    int x = e.getX();
    int y = e.getY();
    final var loc = Location.create(x, y, false);
    final var event = new ComponentUserEvent(canvas, x, y);

    // First search in selection.
    for (final var comp : proj.getSelection().getComponentsContaining(loc, g)) {
      final var editable = (TextEditable) comp.getFeature(TextEditable.class);
      if (editable != null) {
        caret = editable.getTextCaret(event);
        if (caret != null) {
          proj.getFrame().viewComponentAttributes(circ, comp);
          caretComponent = comp;
          caretCreatingText = false;
          break;
        }
      }
    }

    // Then search in circuit
    if (caret == null) {
      for (final var comp : circ.getAllContaining(loc, g)) {
        final var editable = (TextEditable) comp.getFeature(TextEditable.class);
        if (editable != null) {
          caret = editable.getTextCaret(event);
          if (caret != null) {
            proj.getFrame().viewComponentAttributes(circ, comp);
            caretComponent = comp;
            caretCreatingText = false;
            break;
          }
        }
      }
    }

    // if nothing found, create a new label
    if (caret == null) {
      if (loc.getX() < 0 || loc.getY() < 0) return;
      final var copy = (AttributeSet) attrs.clone();
      caretComponent = Text.FACTORY.createComponent(loc, copy);
      caretCreatingText = true;
      final var editable = (TextEditable) caretComponent.getFeature(TextEditable.class);
      if (editable != null) {
        caret = editable.getTextCaret(event);
        proj.getFrame().viewComponentAttributes(circ, caretComponent);
      }
    }

    if (caret != null) {
      caretCanvas = canvas;
      caretCircuit = canvas.getCircuit();
      caret.addCaretListener(listener);
      caretCircuit.addCircuitListener(listener);
    }
    proj.repaintCanvas();
  }

  @Override
  public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
    final var proj = canvas.getProject();
    final var circ = canvas.getCircuit();

    if (!proj.getLogisimFile().contains(circ)) {
      if (caret != null) caret.cancelEditing();
      canvas.setErrorMessage(S.getter("cannotModifyError"));
      return;
    }

    if (caret != null) {
      caret.mouseReleased(e);
      proj.repaintCanvas();
    }
  }

  @Override
  public void paintIcon(ComponentDrawContext c, int x, int y) {
    Text.FACTORY.paintIcon(c, x, y, null);
  }
}
