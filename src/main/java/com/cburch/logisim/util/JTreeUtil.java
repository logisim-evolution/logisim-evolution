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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

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

public class JTreeUtil {
	private static class TransferableNode implements Transferable {
		private Object node;
		private DataFlavor[] flavors = { NODE_FLAVOR };

		public TransferableNode(Object nd) {
			node = nd;
		}

		public synchronized Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException {
			if (flavor == NODE_FLAVOR) {
				return node;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}

		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return Arrays.asList(flavors).contains(flavor);
		}
	}

	/*
	 * This class is the most important. It manages all the DnD behavior. It is
	 * abstract because it contains two abstract methods: public abstract
	 * boolean canPerformAction(JTree target, Object draggedNode, int action,
	 * Point location); public abstract boolean executeDrop(DNDTree tree, Object
	 * draggedNode, Object newParentNode, int action); we have to override to
	 * give the required behavior of DnD in your tree.
	 */
	private static class TreeTransferHandler implements DragGestureListener,
			DragSourceListener, DropTargetListener {
		private JTree tree;
		private JTreeDragController controller;
		private DragSource dragSource; // dragsource
		private Rectangle rect2D = new Rectangle();
		private boolean drawImage;

		protected TreeTransferHandler(JTree tree,
				JTreeDragController controller, int action, boolean drawIcon) {
			this.tree = tree;
			this.controller = controller;
			drawImage = drawIcon;
			dragSource = new DragSource();
			dragSource.createDefaultDragGestureRecognizer(tree, action, this);
		}

		private final void clearImage() {
			tree.paintImmediately(rect2D.getBounds());
		}

		/* Methods for DragSourceListener */
		public void dragDropEnd(DragSourceDropEvent dsde) {
			/*
			 * if (dsde.getDropSuccess() && dsde.getDropAction() ==
			 * DnDConstants.ACTION_MOVE && draggedNodeParent != null) {
			 * ((DefaultTreeModel) tree.getModel())
			 * .nodeStructureChanged(draggedNodeParent); }
			 */
		}

		public final void dragEnter(DragSourceDragEvent dsde) {
			int action = dsde.getDropAction();
			if (action == DnDConstants.ACTION_COPY) {
				dsde.getDragSourceContext().setCursor(
						DragSource.DefaultCopyDrop);
			} else {
				if (action == DnDConstants.ACTION_MOVE) {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveDrop);
				} else {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveNoDrop);
				}
			}
		}

		public final void dragEnter(DropTargetDragEvent dtde) {
			Point pt = dtde.getLocation();
			int action = dtde.getDropAction();
			if (drawImage) {
				paintImage(pt);
			}
			if (controller.canPerformAction(tree, draggedNode, action, pt)) {
				dtde.acceptDrag(action);
			} else {
				dtde.rejectDrag();
			}
		}

		public final void dragExit(DragSourceEvent dse) {
			dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
		}

		public final void dragExit(DropTargetEvent dte) {
			if (drawImage) {
				clearImage();
			}
		}

		/* Methods for DropTargetListener */

		/* Methods for DragGestureListener */
		public final void dragGestureRecognized(DragGestureEvent dge) {
			TreePath path = tree.getSelectionPath();
			if (path != null) {
				draggedNode = path.getLastPathComponent();
				if (drawImage) {
					Rectangle pathBounds = tree.getPathBounds(path); // getpathbounds
																		// of
																		// selectionpath
					JComponent lbl = (JComponent) tree.getCellRenderer()
							.getTreeCellRendererComponent(
									tree,
									draggedNode,
									false,
									tree.isExpanded(path),
									tree.getModel().isLeaf(
											path.getLastPathComponent()), 0,
									false);// returning the label
					lbl.setBounds(pathBounds);// setting bounds to lbl
					image = new BufferedImage(lbl.getWidth(), lbl.getHeight(),
							java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);// buffered
																			// image
																			// reference
																			// passing
																			// the
																			// label's
																			// ht
																			// and
																			// width
					Graphics2D graphics = image.createGraphics();// creating
																	// the
																	// graphics
																	// for
																	// buffered
																	// image
					graphics.setComposite(AlphaComposite.getInstance(
							AlphaComposite.SRC_OVER, 0.5f)); // Sets the
																// Composite for
																// the
																// Graphics2D
																// context
					lbl.setOpaque(false);
					lbl.paint(graphics); // painting the graphics to label
					graphics.dispose();
				}
				dragSource.startDrag(dge, DragSource.DefaultMoveNoDrop, image,
						new Point(0, 0), new TransferableNode(draggedNode),
						this);
			}
		}

		public final void dragOver(DragSourceDragEvent dsde) {
			int action = dsde.getDropAction();
			if (action == DnDConstants.ACTION_COPY) {
				dsde.getDragSourceContext().setCursor(
						DragSource.DefaultCopyDrop);
			} else {
				if (action == DnDConstants.ACTION_MOVE) {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveDrop);
				} else {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveNoDrop);
				}
			}
		}

		public final void dragOver(DropTargetDragEvent dtde) {
			Point pt = dtde.getLocation();
			int action = dtde.getDropAction();
			autoscroll(tree, pt);
			if (drawImage) {
				paintImage(pt);
			}
			if (controller.canPerformAction(tree, draggedNode, action, pt)) {
				dtde.acceptDrag(action);
			} else {
				dtde.rejectDrag();
			}
		}

		public final void drop(DropTargetDropEvent dtde) {
			try {
				if (drawImage) {
					clearImage();
				}
				int action = dtde.getDropAction();
				Transferable transferable = dtde.getTransferable();
				Point pt = dtde.getLocation();
				if (transferable.isDataFlavorSupported(NODE_FLAVOR)
						&& controller.canPerformAction(tree, draggedNode,
								action, pt)) {
					TreePath pathTarget = tree.getPathForLocation(pt.x, pt.y);
					Object node = transferable.getTransferData(NODE_FLAVOR);
					Object newParentNode = pathTarget.getLastPathComponent();
					if (controller.executeDrop(tree, node, newParentNode,
							action)) {
						dtde.acceptDrop(action);
						dtde.dropComplete(true);
						return;
					}
				}
				dtde.rejectDrop();
				dtde.dropComplete(false);
			} catch (Exception e) {
				dtde.rejectDrop();
				dtde.dropComplete(false);
			}
		}

		public final void dropActionChanged(DragSourceDragEvent dsde) {
			int action = dsde.getDropAction();
			if (action == DnDConstants.ACTION_COPY) {
				dsde.getDragSourceContext().setCursor(
						DragSource.DefaultCopyDrop);
			} else {
				if (action == DnDConstants.ACTION_MOVE) {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveDrop);
				} else {
					dsde.getDragSourceContext().setCursor(
							DragSource.DefaultMoveNoDrop);
				}
			}
		}

		public final void dropActionChanged(DropTargetDragEvent dtde) {
			Point pt = dtde.getLocation();
			int action = dtde.getDropAction();
			if (drawImage) {
				paintImage(pt);
			}
			if (controller.canPerformAction(tree, draggedNode, action, pt)) {
				dtde.acceptDrag(action);
			} else {
				dtde.rejectDrag();
			}
		}

		private final void paintImage(Point pt) {
			tree.paintImmediately(rect2D.getBounds());
			rect2D.setRect((int) pt.getX(), (int) pt.getY(), image.getWidth(),
					image.getHeight());
			tree.getGraphics().drawImage(image, (int) pt.getX(),
					(int) pt.getY(), tree);
		}
	}

	private static void autoscroll(JTree tree, Point cursorLocation) {
		Insets insets = DEFAULT_INSETS;
		Rectangle outer = tree.getVisibleRect();
		Rectangle inner = new Rectangle(outer.x + insets.left, outer.y
				+ insets.top, outer.width - (insets.left + insets.right),
				outer.height - (insets.top + insets.bottom));
		if (!inner.contains(cursorLocation)) {
			Rectangle scrollRect = new Rectangle(
					cursorLocation.x - insets.left, cursorLocation.y
							- insets.top, insets.left + insets.right,
					insets.top + insets.bottom);
			tree.scrollRectToVisible(scrollRect);
		}
	}

	public static void configureDragAndDrop(JTree tree,
			JTreeDragController controller) {
		tree.setAutoscrolls(true);
		new TreeTransferHandler(tree, controller,
				DnDConstants.ACTION_COPY_OR_MOVE, true);
	}

	private static final Insets DEFAULT_INSETS = new Insets(20, 20, 20, 20);

	private static final DataFlavor NODE_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType, "Node");

	private static Object draggedNode;

	private static BufferedImage image = null; // buff image
}
