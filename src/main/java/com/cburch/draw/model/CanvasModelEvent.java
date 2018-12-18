/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.draw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CanvasModelEvent extends EventObject {
	public static CanvasModelEvent forAdd(CanvasModel source,
			Collection<? extends CanvasObject> affected) {
		return new CanvasModelEvent(source, ACTION_ADDED, affected);
	}

	public static CanvasModelEvent forChangeAttributes(CanvasModel source,
			Map<AttributeMapKey, Object> oldValues,
			Map<AttributeMapKey, Object> newValues) {
		return new CanvasModelEvent(source, ACTION_ATTRIBUTES_CHANGED,
				oldValues, newValues);
	}

	public static CanvasModelEvent forChangeText(CanvasModel source,
			CanvasObject obj, String oldText, String newText) {
		return new CanvasModelEvent(source, ACTION_TEXT_CHANGED,
				Collections.singleton(obj), oldText, newText);
	}

	public static CanvasModelEvent forDeleteHandle(CanvasModel source,
			Handle handle) {
		return new CanvasModelEvent(source, ACTION_HANDLE_DELETED, handle);
	}

	public static CanvasModelEvent forInsertHandle(CanvasModel source,
			Handle desired) {
		return new CanvasModelEvent(source, ACTION_HANDLE_INSERTED, desired);
	}

	public static CanvasModelEvent forMoveHandle(CanvasModel source,
			HandleGesture gesture) {
		return new CanvasModelEvent(source, ACTION_HANDLE_MOVED, gesture);
	}

	public static CanvasModelEvent forRemove(CanvasModel source,
			Collection<? extends CanvasObject> affected) {
		return new CanvasModelEvent(source, ACTION_REMOVED, affected);
	}

	public static CanvasModelEvent forReorder(CanvasModel source,
			Collection<ReorderRequest> requests) {
		return new CanvasModelEvent(true, source, ACTION_REORDERED, requests);
	}

	public static CanvasModelEvent forTranslate(CanvasModel source,
			Collection<? extends CanvasObject> affected, int dx, int dy) {
		return new CanvasModelEvent(source, ACTION_TRANSLATED, affected, 0, 0);
	}

	private static final long serialVersionUID = 1L;

	public static final int ACTION_ADDED = 0;

	public static final int ACTION_REMOVED = 1;

	public static final int ACTION_TRANSLATED = 2;

	public static final int ACTION_REORDERED = 3;

	public static final int ACTION_HANDLE_MOVED = 4;

	public static final int ACTION_HANDLE_INSERTED = 5;

	public static final int ACTION_HANDLE_DELETED = 6;

	public static final int ACTION_ATTRIBUTES_CHANGED = 7;

	public static final int ACTION_TEXT_CHANGED = 8;

	private int action;
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
	private CanvasModelEvent(boolean dummy, CanvasModel source, int action,
			Collection<ReorderRequest> requests) {
		this(source, action, Collections.<CanvasObject> emptySet());

		List<CanvasObject> affected;
		affected = new ArrayList<CanvasObject>(requests.size());
		for (ReorderRequest r : requests) {
			affected.add(r.getObject());
		}
		this.affected = affected;

		this.reorderRequests = Collections.unmodifiableCollection(requests);
	}

	private CanvasModelEvent(CanvasModel source, int action,
			Collection<? extends CanvasObject> affected) {
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

	private CanvasModelEvent(CanvasModel source, int action,
			Collection<? extends CanvasObject> affected, int dx, int dy) {
		this(source, action, affected);

		this.deltaX = dx;
		this.deltaY = dy;
	}

	private CanvasModelEvent(CanvasModel source, int action,
			Collection<? extends CanvasObject> affected, String oldText,
			String newText) {
		this(source, action, affected);
		this.oldText = oldText;
		this.newText = newText;
	}

	private CanvasModelEvent(CanvasModel source, int action, Handle handle) {
		this(source, action, Collections.singleton(handle.getObject()));

		this.handle = handle;
	}

	private CanvasModelEvent(CanvasModel source, int action,
			HandleGesture gesture) {
		this(source, action, gesture.getHandle());

		this.gesture = gesture;
	}

	private CanvasModelEvent(CanvasModel source, int action,
			Map<AttributeMapKey, Object> oldValues,
			Map<AttributeMapKey, Object> newValues) {
		this(source, action, Collections.<CanvasObject> emptySet());

		Set<CanvasObject> affected;
		affected = new HashSet<CanvasObject>(newValues.size());
		for (AttributeMapKey key : newValues.keySet()) {
			affected.add(key.getObject());
		}
		this.affected = affected;

		Map<AttributeMapKey, Object> oldValuesCopy;
		oldValuesCopy = new HashMap<AttributeMapKey, Object>(oldValues);
		Map<AttributeMapKey, Object> newValuesCopy;
		newValuesCopy = new HashMap<AttributeMapKey, Object>(newValues);

		this.oldValues = Collections.unmodifiableMap(oldValuesCopy);
		this.newValues = Collections.unmodifiableMap(newValuesCopy);
	}

	public int getAction() {
		return action;
	}

	public Collection<? extends CanvasObject> getAffected() {
		Collection<? extends CanvasObject> ret = affected;
		if (ret == null) {
			Map<AttributeMapKey, Object> newVals = newValues;
			if (newVals != null) {
				Set<CanvasObject> keys = new HashSet<CanvasObject>();
				for (AttributeMapKey key : newVals.keySet()) {
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
