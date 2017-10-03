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

package com.cburch.logisim.gui.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.instance.StdAttr;

class ComponentSelector extends JTree {
	private class CircuitNode implements TreeNode, CircuitListener,
			Comparator<Component> {
		private CircuitNode parent;
		private CircuitState circuitState;
		private Component subcircComp;
		private ArrayList<TreeNode> children;

		public CircuitNode(CircuitNode parent, CircuitState circuitState,
				Component subcircComp) {
			this.parent = parent;
			this.circuitState = circuitState;
			this.subcircComp = subcircComp;
			this.children = new ArrayList<TreeNode>();
			circuitState.getCircuit().addCircuitListener(this);
			computeChildren();
		}

		public Enumeration<TreeNode> children() {
			return Collections.enumeration(children);
		}

		public void circuitChanged(CircuitEvent event) {
			int action = event.getAction();
			DefaultTreeModel model = (DefaultTreeModel) getModel();
			if (action == CircuitEvent.ACTION_SET_NAME) {
				model.nodeChanged(this);
			} else {
				if (computeChildren()) {
					model.nodeStructureChanged(this);
				} else if (action == CircuitEvent.ACTION_INVALIDATE) {
					Object o = event.getData();
					for (int i = children.size() - 1; i >= 0; i--) {
						Object o2 = children.get(i);
						if (o2 instanceof ComponentNode) {
							ComponentNode n = (ComponentNode) o2;
							if (n.comp == o) {
								int[] changed = { i };
								children.remove(i);
								model.nodesWereRemoved(this, changed,
										new Object[] { n });
								children.add(i, new ComponentNode(this, n.comp));
								model.nodesWereInserted(this, changed);
							}
						}
					}
				}
			}
		}

		public int compare(Component a, Component b) {
			if (a != b) {
				String aName = a.getFactory().getDisplayName();
				String bName = b.getFactory().getDisplayName();
				int ret = aName.compareToIgnoreCase(bName);
				if (ret != 0)
					return ret;
			}
			return a.getLocation().toString()
					.compareTo(b.getLocation().toString());
		}

		// returns true if changed
		private boolean computeChildren() {
			ArrayList<TreeNode> newChildren = new ArrayList<TreeNode>();
			ArrayList<Component> subcircs = new ArrayList<Component>();
			for (Component comp : circuitState.getCircuit().getNonWires()) {
				if (comp.getFactory() instanceof SubcircuitFactory) {
					subcircs.add(comp);
				} else {
					Object o = comp.getFeature(Loggable.class);
					if (o != null) {
						ComponentNode toAdd = null;
						for (TreeNode o2 : children) {
							if (o2 instanceof ComponentNode) {
								ComponentNode n = (ComponentNode) o2;
								if (n.comp == comp) {
									toAdd = n;
									break;
								}
							}
						}
						if (toAdd == null)
							toAdd = new ComponentNode(this, comp);
						newChildren.add(toAdd);
					}
				}
			}
			Collections.sort(newChildren, new CompareByName());
			Collections.sort(subcircs, this);
			for (Component comp : subcircs) {
				SubcircuitFactory factory = (SubcircuitFactory) comp
						.getFactory();
				CircuitState state = factory.getSubstate(circuitState, comp);
				CircuitNode toAdd = null;
				for (TreeNode o : children) {
					if (o instanceof CircuitNode) {
						CircuitNode n = (CircuitNode) o;
						if (n.circuitState == state) {
							toAdd = n;
							break;
						}
					}
				}
				if (toAdd == null) {
					toAdd = new CircuitNode(this, state, comp);
				}
				newChildren.add(toAdd);
			}

			if (!children.equals(newChildren)) {
				children = newChildren;
				return true;
			} else {
				return false;
			}
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(int index) {
			return children.get(index);
		}

		public int getChildCount() {
			return children.size();
		}

		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		public TreeNode getParent() {
			return parent;
		}

		public boolean isLeaf() {
			return false;
		}

		@Override
		public String toString() {
			if (subcircComp != null) {
				String label = subcircComp.getAttributeSet().getValue(
						StdAttr.LABEL);
				if (label != null && !label.equals("")) {
					return label;
				}
			}
			String ret = circuitState.getCircuit().getName();
			if (subcircComp != null) {
				ret += subcircComp.getLocation();
			}
			return ret;
		}
	}

	private static class CompareByName implements Comparator<Object> {
		public int compare(Object a, Object b) {
			return a.toString().compareToIgnoreCase(b.toString());
		}
	}

	private class ComponentNode implements TreeNode {
		private CircuitNode parent;
		private Component comp;
		private OptionNode[] opts;

		public ComponentNode(CircuitNode parent, Component comp) {
			this.parent = parent;
			this.comp = comp;
			this.opts = null;

			Loggable log = (Loggable) comp.getFeature(Loggable.class);
			if (log != null) {
				Object[] opts = log.getLogOptions(parent.circuitState);
				if (opts != null && opts.length > 0) {
					this.opts = new OptionNode[opts.length];
					for (int i = 0; i < opts.length; i++) {
						this.opts[i] = new OptionNode(this, opts[i]);
					}
				}
			}
		}

		public Enumeration<OptionNode> children() {
			return Collections.enumeration(Arrays.asList(opts));
		}

		public boolean getAllowsChildren() {
			return false;
		}

		public TreeNode getChildAt(int index) {
			return opts[index];
		}

		public int getChildCount() {
			return opts == null ? 0 : opts.length;
		}

		public int getIndex(TreeNode n) {
			for (int i = 0; i < opts.length; i++) {
				if (opts[i] == n)
					return i;
			}
			return -1;
		}

		public TreeNode getParent() {
			return parent;
		}

		public boolean isLeaf() {
			return opts == null || opts.length == 0;
		}

		@Override
		public String toString() {
			Loggable log = (Loggable) comp.getFeature(Loggable.class);
			if (log != null) {
				String ret = log.getLogName(null);
				if (ret != null && !ret.equals(""))
					return ret;
			}
			return comp.getFactory().getDisplayName() + " "
					+ comp.getLocation();
		}
	}

	private class MyCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getTreeCellRendererComponent(JTree tree,
				Object value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {
			java.awt.Component ret = super.getTreeCellRendererComponent(tree,
					value, selected, expanded, leaf, row, hasFocus);
			if (ret instanceof JLabel && value instanceof ComponentNode) {
				ComponentNode node = (ComponentNode) value;
				ComponentIcon icon = new ComponentIcon(node.comp);
				if (node.getChildCount() > 0) {
					icon.setTriangleState(expanded ? ComponentIcon.TRIANGLE_OPEN
							: ComponentIcon.TRIANGLE_CLOSED);
				}
				((JLabel) ret).setIcon(icon);
			}
			return ret;
		}
	}

	private class OptionNode implements TreeNode {
		private ComponentNode parent;
		private Object option;

		public OptionNode(ComponentNode parent, Object option) {
			this.parent = parent;
			this.option = option;
		}

		public Enumeration<? extends TreeNode> children() {
			return Collections.enumeration(Collections.<TreeNode>emptySet());
		}

		public boolean getAllowsChildren() {
			return false;
		}

		public TreeNode getChildAt(int arg0) {
			return null;
		}

		public int getChildCount() {
			return 0;
		}

		public int getIndex(TreeNode n) {
			return -1;
		}

		public TreeNode getParent() {
			return parent;
		}

		public boolean isLeaf() {
			return true;
		}

		@Override
		public String toString() {
			return option.toString();
		}
	}

	private static final long serialVersionUID = 1L;

	private Model logModel;

	public ComponentSelector(Model logModel) {
		DefaultTreeModel model = new DefaultTreeModel(null);
		model.setAsksAllowsChildren(false);
		setModel(model);
		setRootVisible(false);
		setLogModel(logModel);
		setCellRenderer(new MyCellRenderer());
	}

	public List<SelectionItem> getSelectedItems() {
		TreePath[] sel = getSelectionPaths();
		if (sel == null || sel.length == 0)
			return Collections.emptyList();

		ArrayList<SelectionItem> ret = new ArrayList<SelectionItem>();
		for (int i = 0; i < sel.length; i++) {
			TreePath path = sel[i];
			Object last = path.getLastPathComponent();
			ComponentNode n = null;
			Object opt = null;
			if (last instanceof OptionNode) {
				OptionNode o = (OptionNode) last;
				n = o.parent;
				opt = o.option;
			} else if (last instanceof ComponentNode) {
				n = (ComponentNode) last;
				if (n.opts != null)
					n = null;
			}
			if (n != null) {
				int count = 0;
				for (CircuitNode cur = n.parent; cur != null; cur = cur.parent) {
					count++;
				}
				Component[] nPath = new Component[count - 1];
				CircuitNode cur = n.parent;
				for (int j = nPath.length - 1; j >= 0; j--) {
					nPath[j] = cur.subcircComp;
					cur = cur.parent;
				}
				ret.add(new SelectionItem(logModel, nPath, n.comp, opt));
			}
		}
		return ret.size() == 0 ? null : ret;
	}

	public boolean hasSelectedItems() {
		TreePath[] sel = getSelectionPaths();
		if (sel == null || sel.length == 0)
			return false;

		for (int i = 0; i < sel.length; i++) {
			Object last = sel[i].getLastPathComponent();
			if (last instanceof OptionNode) {
				return true;
			} else if (last instanceof ComponentNode) {
				if (((ComponentNode) last).opts == null)
					return true;
			}
		}
		return false;
	}

	public void localeChanged() {
		repaint();
	}

	public void setLogModel(Model value) {
		this.logModel = value;

		DefaultTreeModel model = (DefaultTreeModel) getModel();
		CircuitNode curRoot = (CircuitNode) model.getRoot();
		CircuitState state = logModel == null ? null : logModel
				.getCircuitState();
		if (state == null) {
			if (curRoot != null)
				model.setRoot(null);
			return;
		}
		if (curRoot == null || curRoot.circuitState != state) {
			curRoot = new CircuitNode(null, state, null);
			model.setRoot(curRoot);
		}
	}
}
