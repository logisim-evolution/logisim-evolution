/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.actions;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.util.ZOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelReorderAction extends ModelAction {
  private final List<ReorderRequest> requests;
  private final List<CanvasObject> objects;
  private final int type;

  public ModelReorderAction(CanvasModel model, List<ReorderRequest> requests) {
    super(model);
    this.requests = new ArrayList<>(requests);
    this.objects = new ArrayList<>(requests.size());
    for (ReorderRequest r : requests) {
      objects.add(r.getObject());
    }
    int typeIndex = 0; // 0 = mixed/unknown, -1 = to greater index, 1 = to
    // smaller index
    for (ReorderRequest r : requests) {
      int thisType;
      int from = r.getFromIndex();
      int to = r.getToIndex();
      thisType = Integer.compare(to, from);
      if (typeIndex == 2) {
        typeIndex = thisType;
      } else if (typeIndex != thisType) {
        typeIndex = 0;
        break;
      }
    }
    this.type = typeIndex;
  }

  public static ModelReorderAction createLower(
      CanvasModel model, Collection<? extends CanvasObject> objects) {
    List<ReorderRequest> reqs = new ArrayList<>();
    Map<CanvasObject, Integer> zmap = ZOrder.getZIndex(objects, model);
    for (Map.Entry<CanvasObject, Integer> entry : zmap.entrySet()) {
      CanvasObject obj = entry.getKey();
      int from = entry.getValue();
      CanvasObject above = ZOrder.getObjectBelow(obj, model, objects);
      if (above != null) {
        int to = ZOrder.getZIndex(above, model);
        if (objects.contains(above)) {
          to++;
        }
        reqs.add(new ReorderRequest(obj, from, to));
      }
    }
    if (reqs.isEmpty()) {
      return null;
    } else {
      reqs.sort(ReorderRequest.ASCENDING_FROM);
      repairRequests(reqs);
      return new ModelReorderAction(model, reqs);
    }
  }

  public static ModelReorderAction createLowerBottom(
      CanvasModel model, Collection<? extends CanvasObject> objects) {
    List<ReorderRequest> reqs = new ArrayList<>();
    Map<CanvasObject, Integer> zmap = ZOrder.getZIndex(objects, model);
    int to = 0;
    for (Map.Entry<CanvasObject, Integer> entry : zmap.entrySet()) {
      CanvasObject obj = entry.getKey();
      int from = entry.getValue();
      reqs.add(new ReorderRequest(obj, from, to));
    }
    if (reqs.isEmpty()) {
      return null;
    } else {
      reqs.sort(ReorderRequest.ASCENDING_FROM);
      repairRequests(reqs);
      return new ModelReorderAction(model, reqs);
    }
  }

  public static ModelReorderAction createRaise(
      CanvasModel model, Collection<? extends CanvasObject> objects) {
    List<ReorderRequest> reqs = new ArrayList<>();
    Map<CanvasObject, Integer> zmap = ZOrder.getZIndex(objects, model);
    for (Map.Entry<CanvasObject, Integer> entry : zmap.entrySet()) {
      CanvasObject obj = entry.getKey();
      int from = entry.getValue();
      CanvasObject above = ZOrder.getObjectAbove(obj, model, objects);
      if (above != null) {
        int to = ZOrder.getZIndex(above, model);
        if (objects.contains(above)) {
          to--;
        }
        reqs.add(new ReorderRequest(obj, from, to));
      }
    }
    if (reqs.isEmpty()) {
      return null;
    } else {
      reqs.sort(ReorderRequest.DESCENDING_FROM);
      repairRequests(reqs);
      return new ModelReorderAction(model, reqs);
    }
  }

  public static ModelReorderAction createRaiseTop(
      CanvasModel model, Collection<? extends CanvasObject> objects) {
    List<ReorderRequest> reqs = new ArrayList<>();
    Map<CanvasObject, Integer> zmap = ZOrder.getZIndex(objects, model);
    int to = model.getObjectsFromBottom().size() - 1;
    for (Map.Entry<CanvasObject, Integer> entry : zmap.entrySet()) {
      CanvasObject obj = entry.getKey();
      int from = entry.getValue();
      reqs.add(new ReorderRequest(obj, from, to));
    }
    if (reqs.isEmpty()) {
      return null;
    } else {
      reqs.sort(ReorderRequest.ASCENDING_FROM);
      repairRequests(reqs);
      return new ModelReorderAction(model, reqs);
    }
  }

  private static void repairRequests(List<ReorderRequest> reqs) {
    for (int i = 0, n = reqs.size(); i < n; i++) {
      ReorderRequest req = reqs.get(i);
      int from = req.getFromIndex();
      int to = req.getToIndex();
      for (int j = 0; j < i; j++) {
        ReorderRequest prev = reqs.get(j);
        int prevFrom = prev.getFromIndex();
        int prevTo = prev.getToIndex();
        if (prevFrom <= from && from < prevTo) {
          from--;
        } else if (prevTo <= from && from < prevFrom) {
          from++;
        }
        if (prevFrom <= to && to < prevTo) {
          to--;
        } else if (prevTo <= to && to < prevFrom) {
          to++;
        }
      }
      if (from != req.getFromIndex() || to != req.getToIndex()) {
        reqs.set(i, new ReorderRequest(req.getObject(), from, to));
      }
    }
    for (int i = reqs.size() - 1; i >= 0; i--) {
      ReorderRequest req = reqs.get(i);
      if (req.getFromIndex() == req.getToIndex()) {
        reqs.remove(i);
      }
    }
  }

  @Override
  void doSub(CanvasModel model) {
    model.reorderObjects(requests);
  }

  @Override
  public String getName() {
    if (type < 0) {
      return S.get("actionRaise", getShapesName(objects));
    } else if (type > 0) {
      return S.get("actionLower", getShapesName(objects));
    } else {
      return S.get("actionReorder", getShapesName(objects));
    }
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return objects;
  }

  public List<ReorderRequest> getReorderRequests() {
    return Collections.unmodifiableList(requests);
  }

  @Override
  void undoSub(CanvasModel model) {
    List<ReorderRequest> inv = new ArrayList<>(requests.size());
    for (int i = requests.size() - 1; i >= 0; i--) {
      ReorderRequest r = requests.get(i);
      inv.add(new ReorderRequest(r.getObject(), r.getToIndex(), r.getFromIndex()));
    }
    model.reorderObjects(inv);
  }
}
