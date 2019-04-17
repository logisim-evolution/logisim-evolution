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
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;
import java.util.Map;

class AttrTableSelectionModel extends AttributeSetTableModel implements SelectionListener {
  private Canvas canvas;

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
      return S.fmt("selectionVarious", "" + totalCount);
    } else if (commonCount == 1) {
      return S.fmt("selectionOne", firstObject.getDisplayNameAndLabel());
    } else {
      return S.fmt("selectionMultiple", firstObject.getDisplayName(), "" + commonCount);
    }
  }

  //
  // SelectionListener method
  //
  public void selectionChanged(SelectionEvent e) {
    fireTitleChanged();
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    SelectionAttributes attrs = (SelectionAttributes) getAttributeSet();
    Map<AttributeMapKey, Object> oldVals;
    oldVals = new HashMap<AttributeMapKey, Object>();
    Map<AttributeMapKey, Object> newVals;
    newVals = new HashMap<AttributeMapKey, Object>();
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
