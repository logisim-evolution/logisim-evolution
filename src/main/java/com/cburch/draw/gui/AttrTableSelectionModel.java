/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.actions.ModelChangeAttributeAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;

class AttrTableSelectionModel extends AttributeSetTableModel implements SelectionListener {
  private final Canvas canvas;

  public AttrTableSelectionModel(Canvas canvas) {
    super(new SelectionAttributes(canvas.getSelection()));
    this.canvas = canvas;
    canvas.getSelection().addSelectionListener(this);
  }

  @Override
  public String getTitle() {
    final var sel = canvas.getSelection();
    Class<? extends CanvasObject> commonClass = null;
    var commonCount = 0;
    CanvasObject firstObject = null;
    var totalCount = 0;
    for (final var obj : sel.getSelected()) {
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
    final var attrs = (SelectionAttributes) getAttributeSet();
    final var oldVals = new HashMap<AttributeMapKey, Object>();
    final var newVals = new HashMap<AttributeMapKey, Object>();
    for (final var ent : attrs.entries()) {
      final var key = new AttributeMapKey(attr, ent.getValue());
      oldVals.put(key, ent.getKey().getValue(attr));
      newVals.put(key, value);
    }
    final var model = canvas.getModel();
    canvas.doAction(new ModelChangeAttributeAction(model, oldVals, newVals));
    fireTitleChanged();
  }
}
