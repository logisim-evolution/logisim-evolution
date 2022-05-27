/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class CanvasModelEvent extends EventObject {
  public static final int ACTION_ADDED = 0;
  public static final int ACTION_REMOVED = 1;
  public static final int ACTION_TRANSLATED = 2;
  public static final int ACTION_REORDERED = 3;
  public static final int ACTION_HANDLE_MOVED = 4;
  public static final int ACTION_HANDLE_INSERTED = 5;
  public static final int ACTION_HANDLE_DELETED = 6;
  public static final int ACTION_ATTRIBUTES_CHANGED = 7;
  public static final int ACTION_TEXT_CHANGED = 8;
  private static final long serialVersionUID = 1L;
  private final int action;
  private Collection<? extends CanvasObject> affected;
  private int deltaX;
  private int deltaY;
  private Map<AttributeMapKey, Object> oldValues;
  private Map<AttributeMapKey, Object> newValues;
  private Collection<ReorderRequest> reorderRequests;
  private Handle handle;
  private HandleGesture gesture;
  private String oldText;
  private String newText;

  // the boolean parameter is just because the compiler insists upon it to
  // avoid an erasure conflict with the first constructor
  private CanvasModelEvent(
      boolean dummy, CanvasModel source, int action, Collection<ReorderRequest> requests) {
    this(source, action, Collections.emptySet());

    final var affected = new ArrayList<CanvasObject>(requests.size());
    for (final var r : requests) {
      affected.add(r.getObject());
    }
    this.affected = affected;

    this.reorderRequests = Collections.unmodifiableCollection(requests);
  }

  private CanvasModelEvent(
      CanvasModel source, int action, Collection<? extends CanvasObject> affected) {
    super(source);

    this.action = action;
    this.affected = affected;
    this.deltaX = 0;
    this.deltaY = 0;
    this.oldValues = null;
    this.newValues = null;
    this.reorderRequests = null;
    this.handle = null;
    this.gesture = null;
    this.oldText = null;
    this.newText = null;
  }

  private CanvasModelEvent(
      CanvasModel source, int action, Collection<? extends CanvasObject> affected, int dx, int dy) {
    this(source, action, affected);

    this.deltaX = dx;
    this.deltaY = dy;
  }

  private CanvasModelEvent(
      CanvasModel source,
      int action,
      Collection<? extends CanvasObject> affected,
      String oldText,
      String newText) {
    this(source, action, affected);
    this.oldText = oldText;
    this.newText = newText;
  }

  private CanvasModelEvent(CanvasModel source, int action, Handle handle) {
    this(source, action, Collections.singleton(handle.getObject()));

    this.handle = handle;
  }

  private CanvasModelEvent(CanvasModel source, int action, HandleGesture gesture) {
    this(source, action, gesture.getHandle());

    this.gesture = gesture;
  }

  private CanvasModelEvent(
      CanvasModel source,
      int action,
      Map<AttributeMapKey, Object> oldValues,
      Map<AttributeMapKey, Object> newValues) {
    this(source, action, Collections.emptySet());

    final var affected = new HashSet<CanvasObject>(newValues.size());
    for (final var key : newValues.keySet()) {
      affected.add(key.getObject());
    }
    this.affected = affected;

    final var oldValuesCopy = new HashMap<>(oldValues);
    final var newValuesCopy = new HashMap<>(newValues);

    this.oldValues = Collections.unmodifiableMap(oldValuesCopy);
    this.newValues = Collections.unmodifiableMap(newValuesCopy);
  }

  public static CanvasModelEvent forAdd(
      CanvasModel source, Collection<? extends CanvasObject> affected) {
    return new CanvasModelEvent(source, ACTION_ADDED, affected);
  }

  public static CanvasModelEvent forChangeAttributes(
      CanvasModel source,
      Map<AttributeMapKey, Object> oldValues,
      Map<AttributeMapKey, Object> newValues) {
    return new CanvasModelEvent(source, ACTION_ATTRIBUTES_CHANGED, oldValues, newValues);
  }

  public static CanvasModelEvent forChangeText(
      CanvasModel source, CanvasObject obj, String oldText, String newText) {
    return new CanvasModelEvent(
        source, ACTION_TEXT_CHANGED, Collections.singleton(obj), oldText, newText);
  }

  public static CanvasModelEvent forDeleteHandle(CanvasModel source, Handle handle) {
    return new CanvasModelEvent(source, ACTION_HANDLE_DELETED, handle);
  }

  public static CanvasModelEvent forInsertHandle(CanvasModel source, Handle desired) {
    return new CanvasModelEvent(source, ACTION_HANDLE_INSERTED, desired);
  }

  public static CanvasModelEvent forMoveHandle(CanvasModel source, HandleGesture gesture) {
    return new CanvasModelEvent(source, ACTION_HANDLE_MOVED, gesture);
  }

  public static CanvasModelEvent forRemove(
      CanvasModel source, Collection<? extends CanvasObject> affected) {
    return new CanvasModelEvent(source, ACTION_REMOVED, affected);
  }

  public static CanvasModelEvent forReorder(
      CanvasModel source, Collection<ReorderRequest> requests) {
    return new CanvasModelEvent(true, source, ACTION_REORDERED, requests);
  }

  public static CanvasModelEvent forTranslate(
      CanvasModel source, Collection<? extends CanvasObject> affected) {
    return new CanvasModelEvent(source, ACTION_TRANSLATED, affected, 0, 0);
  }

  public int getAction() {
    return action;
  }

  public Collection<? extends CanvasObject> getAffected() {
    var ret = affected;
    if (ret == null) {
      final var newVals = newValues;
      if (newVals != null) {
        final var keys = new HashSet<CanvasObject>();
        for (final var key : newVals.keySet()) {
          keys.add(key.getObject());
        }
        ret = Collections.unmodifiableCollection(keys);
        affected = ret;
      }
    }
    return affected;
  }

  public int getDeltaX() {
    return deltaX;
  }

  public int getDeltaY() {
    return deltaY;
  }

  public Handle getHandle() {
    return handle;
  }

  public HandleGesture getHandleGesture() {
    return gesture;
  }

  public String getNewText() {
    return newText;
  }

  public Map<AttributeMapKey, Object> getNewValues() {
    return newValues;
  }

  public String getOldText() {
    return oldText;
  }

  public Map<AttributeMapKey, Object> getOldValues() {
    return oldValues;
  }

  public Collection<ReorderRequest> getReorderRequests() {
    return reorderRequests;
  }
}
