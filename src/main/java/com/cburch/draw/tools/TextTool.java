/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelEditTextAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Text;
import com.cburch.draw.util.EditableLabelField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.TextIcon;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class TextTool extends AbstractTool {
  private static final TextIcon ICON = new TextIcon();
  private final DrawingAttributeSet attrs;
  private final EditableLabelField field;
  private final FieldListener fieldListener;
  private Text curText;
  private Canvas curCanvas;
  private boolean isTextNew;

  public TextTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
    curText = null;
    isTextNew = false;
    field = new EditableLabelField();

    fieldListener = new FieldListener();
    final var fieldInput = field.getInputMap();
    fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "commit");
    fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    final var fieldAction = field.getActionMap();
    fieldAction.put("commit", fieldListener);
    fieldAction.put("cancel", new CancelListener());
  }

  private void cancelText(Canvas canvas) {
    final var cur = curText;
    if (cur != null) {
      curText = null;
      cur.removeAttributeListener(fieldListener);
      canvas.remove(field);
      canvas.getSelection().clearSelected();
      canvas.repaint();
    }
  }

  private void commitText(Canvas canvas) {
    final var cur = curText;
    if (cur == null) {
      return;
    }
    cancelText(canvas);

    final var isNew = isTextNew;
    final var newText = field.getText();
    if (isNew) {
      if (!newText.equals("")) {
        cur.setText(newText);
        canvas.doAction(new ModelAddAction(canvas.getModel(), cur));
      }
    } else {
      final var oldText = cur.getText();
      if (newText.equals("")) {
        canvas.doAction(new ModelRemoveAction(canvas.getModel(), cur));
      } else if (!oldText.equals(newText)) {
        canvas.doAction(new ModelEditTextAction(canvas.getModel(), cur, newText));
      }
    }
  }

  @Override
  public void draw(Canvas canvas, Graphics gfx) {
    // actually, there's nothing to do here - it's handled by the field
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.ATTRS_TEXT_TOOL;
  }

  @Override
  public Cursor getCursor(Canvas canvas) {
    return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }

  @Override
  public void mousePressed(Canvas canvas, MouseEvent e) {
    if (curText != null) {
      commitText(canvas);
    }

    Text clicked = null;
    var found = false;
    final var mx = e.getX();
    final var my = e.getY();
    final var mloc = Location.create(mx, my);
    for (final var o : canvas.getModel().getObjectsFromTop()) {
      if (o instanceof Text && o.contains(mloc, true)) {
        clicked = (Text) o;
        found = true;
        break;
      }
    }
    if (!found) {
      clicked = attrs.applyTo(new Text(mx, my, ""));
    }

    curText = clicked;
    curCanvas = canvas;
    isTextNew = !found;
    clicked.getLabel().configureTextField(field, canvas.getZoomFactor());
    field.setText(clicked.getText());
    canvas.add(field);

    final var fieldLoc = field.getLocation();
    final var zoom = canvas.getZoomFactor();
    fieldLoc.x = (int) Math.round(mx * zoom - fieldLoc.x);
    fieldLoc.y = (int) Math.round(my * zoom - fieldLoc.y);
    final var caret = field.viewToModel2D(fieldLoc);
    if (caret >= 0) {
      field.setCaretPosition(caret);
    }
    field.requestFocus();

    canvas.getSelection().setSelected(clicked, true);
    canvas.getSelection().setHidden(Collections.singleton(clicked), true);
    clicked.addAttributeListener(fieldListener);
    canvas.repaint();
  }

  @Override
  public void toolDeselected(Canvas canvas) {
    commitText(canvas);
  }

  @Override
  public void toolSelected(Canvas canvas) {
    cancelText(canvas);
  }

  @Override
  public void zoomFactorChanged(Canvas canvas) {
    final var text = curText;
    if (text != null) {
      text.getLabel().configureTextField(field, canvas.getZoomFactor());
    }
  }

  private class CancelListener extends AbstractAction {
    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent e) {
      cancelText(curCanvas);
    }
  }

  private class FieldListener extends AbstractAction implements AttributeListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent e) {
      commitText(curCanvas);
    }

    @Override
    public void attributeListChanged(AttributeEvent e) {
      final var cur = curText;
      if (cur != null) {
        double zoom = curCanvas.getZoomFactor();
        cur.getLabel().configureTextField(field, zoom);
        curCanvas.repaint();
      }
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      attributeListChanged(e);
    }
  }
}
