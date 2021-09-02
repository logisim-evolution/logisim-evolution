/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.actions.ModelChangeAttributeAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;
import java.util.Map;

class AttrTableSelectionModel extends AttributeSetTableModel implements SelectionListener {
  private final Canvas canvas;

  public AttrTableSelectionModel(Canvas canvas) {
    super(new SelectionAttributes(canvas.getSelection()));
    this.canvas = canvas;
    canvas.getSelection().addSelectionListener(this);
  }

  @Override
  public String getTitle() {
    Selection sel = canvas.getSelection();
    Class<? extends CanvasObject> commonClass = null;
    int commonCount = 0;
    CanvasObject firstObject = null;
    int totalCount = 0;
    for (CanvasObject obj : sel.getSelected()) {
      if (firstObject == null) {
        firstObject = obj;
        commonClass = obj.getClass();
        commonCount = 1;
      } else if (obj.getClass() == commonClass) {
        commonCount++;
      } else {
        commonClass = null;
      }
      totalCount++;
    }

    if (firstObject == null) {
      return null;
    } else if (commonClass == null) {
      return S.get("selectionVarious", "" + totalCount);
    } else if (commonCount == 1) {
      return firstObject.getDisplayNameAndLabel();
    } else {
      return S.get("selectionMultiple", firstObject.getDisplayName(), "" + commonCount);
    }
  }

  //
  // SelectionListener method
  //
  @Override
  public void selectionChanged(SelectionEvent e) {
    fireTitleChanged();
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    SelectionAttributes attrs = (SelectionAttributes) getAttributeSet();
    Map<AttributeMapKey, Object> oldVals;
    oldVals = new HashMap<>();
    Map<AttributeMapKey, Object> newVals;
    newVals = new HashMap<>();
    for (Map.Entry<AttributeSet, CanvasObject> ent : attrs.entries()) {
      AttributeMapKey key = new AttributeMapKey(attr, ent.getValue());
      oldVals.put(key, ent.getKey().getValue(attr));
      newVals.put(key, value);
    }
    CanvasModel model = canvas.getModel();
    canvas.doAction(new ModelChangeAttributeAction(model, oldVals, newVals));
    fireTitleChanged();
  }
}
