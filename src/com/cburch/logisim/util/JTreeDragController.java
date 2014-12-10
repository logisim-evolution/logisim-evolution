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

package com.cburch.logisim.util;

import java.awt.Point;

import javax.swing.JTree;

/* This comes from "Denis" at http://forum.java.sun.com/thread.jspa?forumID=57&threadID=296255 */

/*
 * My program use 4 classes. The application displays two trees.
 * You can drag (move by default or copy with ctrl pressed) a node from the left tree to the right one, from the right to the left and inside the same tree.
 * The rules for moving are :
 *   - you can't move the root
 *   - you can't move the selected node to its subtree (in the same tree).
 *   - you can't move the selected node to itself (in the same tree).
 *   - you can't move the selected node to its parent (in the same tree).
 *   - you can move a node to anywhere you want according to the 4 previous rules.
 *  The rules for copying are :
 *   - you can copy a node to anywhere you want.
 *
 * In the implementation I used DnD version of Java 1.3 because in 1.4 the DnD is too restrictive :
 * you can't do what you want (displaying the image of the node while dragging, changing the cursor
 * according to where you are dragging, etc...). In 1.4, the DnD is based on the 1.3 version but
 * it is too encapsulated.
 */

public interface JTreeDragController {
	public boolean canPerformAction(JTree target, Object draggedNode,
			int action, Point location);

	public boolean executeDrop(JTree tree, Object draggedNode,
			Object newParentNode, int action);
}
