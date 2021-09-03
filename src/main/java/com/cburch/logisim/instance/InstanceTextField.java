/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.tools.TextEditable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import lombok.val;

public class InstanceTextField implements AttributeListener, TextFieldListener, TextEditable {
  private Canvas canvas;
  private final InstanceComponent comp;
  private TextField field;
  private Attribute<String> labelAttr;
  private Attribute<Font> fontAttr;
  private boolean isLabelVisible = true;
  private Color fontColor;
  private int fieldX;
  private int fieldY;
  private int halign;
  private int valign;

  InstanceTextField(InstanceComponent comp) {
    this.comp = comp;
    this.field = null;
    this.labelAttr = null;
    this.fontAttr = null;
    fontColor = StdAttr.DEFAULT_LABEL_COLOR;
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    val attr = e.getAttribute();
    if (attr == labelAttr) {
      updateField(comp.getAttributeSet());
    } else if (attr == fontAttr) {
      if (field != null) field.setFont((Font) e.getValue());
    } else if (attr == StdAttr.LABEL_COLOR) {
      fontColor = (Color) e.getValue();
    } else if (attr == StdAttr.LABEL_VISIBILITY) {
      isLabelVisible = (Boolean) e.getValue();
    }
  }

  private void createField(AttributeSet attrs, String text) {
    val font = attrs.getValue(fontAttr);
    field = new TextField(fieldX, fieldY, halign, valign, font);
    field.setText(text);
    field.addTextFieldListener(this);
  }

  void draw(Component comp, ComponentDrawContext context) {
    if (field != null && isLabelVisible) {
      val gfx = context.getGraphics().create();
      val currentColor = gfx.getColor();
      if (!context.isPrintView()) gfx.setColor(fontColor);
      field.draw(gfx);
      gfx.setColor(currentColor);
      gfx.dispose();
    }
  }

  Bounds getBounds(Graphics g) {
    return field == null || !isLabelVisible ? Bounds.EMPTY_BOUNDS : field.getBounds(g);
  }

  @Override
  public Action getCommitAction(Circuit circuit, String oldText, String newText) {
    val action = new SetAttributeAction(circuit, S.getter("changeLabelAction"));
    action.set(comp, labelAttr, newText);
    return action;
  }

  @Override
  public Caret getTextCaret(ComponentUserEvent event) {
    canvas = event.getCanvas();
    val gfx = canvas.getGraphics();

    // if field is absent, create it empty
    // and if it is empty, just return a caret at its beginning
    if (field == null) createField(comp.getAttributeSet(), "");
    val text = field.getText();
    if (text == null || text.equals("")) return field.getCaret(gfx, 0);

    var bds = field.getBounds(gfx);
    if (bds.getWidth() < 4 || bds.getHeight() < 4) {
      val loc = comp.getLocation();
      bds = bds.add(Bounds.create(loc).expand(2));
    }

    val x = event.getX();
    val y = event.getY();
    if (bds.contains(x, y)) return field.getCaret(gfx, x, y);

    return null;
  }

  private boolean shouldRegister() {
    return labelAttr != null || fontAttr != null;
  }

  @Override
  public void textChanged(TextFieldEvent e) {
    val prev = e.getOldText();
    val next = e.getText();
    if (!next.equals(prev)) {
      comp.getAttributeSet().setValue(labelAttr, next);
    }
  }

  void update(Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
    val wasReg = shouldRegister();
    this.labelAttr = labelAttr;
    this.fontAttr = fontAttr;
    this.fieldX = x;
    this.fieldY = y;
    this.halign = halign;
    this.valign = valign;
    val shouldReg = shouldRegister();
    var attrs = comp.getAttributeSet();
    if (attrs.containsAttribute(StdAttr.LABEL_VISIBILITY))
      isLabelVisible = attrs.getValue(StdAttr.LABEL_VISIBILITY);
    if (!wasReg && shouldReg) attrs.addAttributeListener(this);
    if (wasReg && !shouldReg) attrs.removeAttributeListener(this);

    updateField(attrs);
  }

  private void updateField(AttributeSet attrs) {
    val text = attrs.getValue(labelAttr);
    if (text == null || text.equals("")) {
      if (field != null) {
        field.removeTextFieldListener(this);
        field = null;
      }
    } else {
      if (field == null) {
        createField(attrs, text);
      } else {
        val font = attrs.getValue(fontAttr);
        if (font != null) field.setFont(font);
        field.setLocation(fieldX, fieldY, halign, valign);
        field.setText(text);
      }
    }
  }
}
