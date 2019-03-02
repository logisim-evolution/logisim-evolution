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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class Analyzer extends LFrame {
	private class EditListener implements ActionListener, ChangeListener {
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			Component c = tabbedPane.getSelectedComponent();
			if (c instanceof JScrollPane) {
				c = ((JScrollPane) c).getViewport().getView();
			}
			if (!(c instanceof TabInterface))
				return;
			TabInterface tab = (TabInterface) c;
			if (src == LogisimMenuBar.CUT) {
				tab.copy();
				tab.delete();
			} else if (src == LogisimMenuBar.COPY) {
				tab.copy();
			} else if (src == LogisimMenuBar.PASTE) {
				tab.paste();
			} else if (src == LogisimMenuBar.DELETE) {
				tab.delete();
			} else if (src == LogisimMenuBar.SELECT_ALL) {
				tab.selectAll();
			}
		}

		private void enableItems(LogisimMenuBar menubar) {
			Component c = tabbedPane.getSelectedComponent();
			if (c instanceof JScrollPane) {
				c = ((JScrollPane) c).getViewport().getView();
			}
			boolean support = c instanceof TabInterface;
			menubar.setEnabled(LogisimMenuBar.CUT, support);
			menubar.setEnabled(LogisimMenuBar.COPY, support);
			menubar.setEnabled(LogisimMenuBar.PASTE, support);
			menubar.setEnabled(LogisimMenuBar.DELETE, support);
			menubar.setEnabled(LogisimMenuBar.SELECT_ALL, support);
		}

		private void register(LogisimMenuBar menubar) {
			menubar.addActionListener(LogisimMenuBar.CUT, this);
			menubar.addActionListener(LogisimMenuBar.COPY, this);
			menubar.addActionListener(LogisimMenuBar.PASTE, this);
			menubar.addActionListener(LogisimMenuBar.DELETE, this);
			menubar.addActionListener(LogisimMenuBar.SELECT_ALL, this);
			tabbedPane.addChangeListener(this);
			enableItems(menubar);
		}

		public void stateChanged(ChangeEvent e) {
			enableItems((LogisimMenuBar) getJMenuBar());

			Object selected = tabbedPane.getSelectedComponent();
			if (selected instanceof JScrollPane) {
				selected = ((JScrollPane) selected).getViewport().getView();
			}
			if (selected instanceof JPanel) {
				((JPanel)selected).requestFocus();
			}
			if (selected instanceof AnalyzerTab) {
				model.getOutputExpressions().enableUpdates();
				((AnalyzerTab) selected).updateTab();
			} else {
				model.getOutputExpressions().disableUpdates();
			}
		}
	}

	private class MyListener implements LocaleListener {
		public void localeChanged() {
			Analyzer.this.setTitle(S.get("analyzerWindowTitle"));
			tabbedPane.setTitleAt(INPUTS_TAB, S.get("inputsTab"));
			tabbedPane.setTitleAt(OUTPUTS_TAB, S.get("outputsTab"));
			tabbedPane.setTitleAt(TABLE_TAB, S.get("tableTab"));
			tabbedPane.setTitleAt(EXPRESSION_TAB, S.get("expressionTab"));
			tabbedPane.setTitleAt(MINIMIZED_TAB, S.get("minimizedTab"));
			tabbedPane
					.setToolTipTextAt(INPUTS_TAB, S.get("inputsTabTip"));
			tabbedPane.setToolTipTextAt(OUTPUTS_TAB,
					S.get("outputsTabTip"));
			tabbedPane.setToolTipTextAt(TABLE_TAB, S.get("tableTabTip"));
			tabbedPane.setToolTipTextAt(EXPRESSION_TAB,
					S.get("expressionTabTip"));
			tabbedPane.setToolTipTextAt(MINIMIZED_TAB,
					S.get("minimizedTabTip"));
			importTable.setText(S.get("importTableButton"));
			buildCircuit.setText(S.get("buildCircuitButton"));
			exportTable.setText(S.get("exportTableButton"));
			inputsPanel.localeChanged();
			outputsPanel.localeChanged();
			truthTablePanel.localeChanged();
			expressionPanel.localeChanged();
			minimizedPanel.localeChanged();
			importTable.localeChanged();
			buildCircuit.localeChanged();
			exportTable.localeChanged();
		}
	}

	public static void main(String[] args) {
		Analyzer frame = new Analyzer();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static final long serialVersionUID = 1L;
	// used by circuit analysis to select the relevant tab automatically.
	public static final int INPUTS_TAB = 0;
	public static final int OUTPUTS_TAB = 1;

	public static final int TABLE_TAB = 2;

	public static final int EXPRESSION_TAB = 3;

	public static final int MINIMIZED_TAB = 4;
	private MyListener myListener = new MyListener();
	private EditListener editListener = new EditListener();
	private AnalyzerModel model = new AnalyzerModel();

	private JTabbedPane tabbedPane = new JTabbedPane();
	private VariableTab inputsPanel;
	private VariableTab outputsPanel;
	private TableTab truthTablePanel;
	private ExpressionTab expressionPanel;
	private MinimizedTab minimizedPanel;

	private BuildCircuitButton buildCircuit;
	private ImportTableButton importTable;
	private ExportTableButton exportTable;
	
	Analyzer() {
		inputsPanel = new VariableTab(model.getInputs(), AnalyzerModel.MAX_INPUTS);
		outputsPanel = new VariableTab(model.getOutputs(), AnalyzerModel.MAX_OUTPUTS);
		inputsPanel.SetCompanion(outputsPanel, S.get("outputsTab"));
		outputsPanel.SetCompanion(inputsPanel, S.get("inputsTab"));
		truthTablePanel = new TableTab(model.getTruthTable());
		expressionPanel = new ExpressionTab(model);
		minimizedPanel = new MinimizedTab(model);
		importTable = new ImportTableButton(this, model);
		buildCircuit = new BuildCircuitButton(this, model);
		exportTable = new ExportTableButton(this, model);

		truthTablePanel.addMouseListener(new TruthTableMouseListener());

		tabbedPane = new JTabbedPane();
		addTab(INPUTS_TAB, inputsPanel);
		addTab(OUTPUTS_TAB, outputsPanel);
		addTab(TABLE_TAB, truthTablePanel);
		addTab(EXPRESSION_TAB, expressionPanel);
		addTab(MINIMIZED_TAB, minimizedPanel);

		Container contents = getContentPane();
		JPanel vertStrut = new JPanel(null);
		vertStrut.setPreferredSize(new Dimension(0, 300));
		JPanel horzStrut = new JPanel(null);
		horzStrut.setPreferredSize(new Dimension(450, 0));
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(importTable);
		buttonPanel.add(buildCircuit);
		buttonPanel.add(exportTable);
		contents.add(vertStrut, BorderLayout.WEST);
		contents.add(horzStrut, BorderLayout.NORTH);
		contents.add(tabbedPane, BorderLayout.CENTER);
		contents.add(buttonPanel, BorderLayout.SOUTH);

		DefaultRegistry registry = new DefaultRegistry(getRootPane());
		inputsPanel.registerDefaultButtons(registry);
		outputsPanel.registerDefaultButtons(registry);
		expressionPanel.registerDefaultButtons(registry);

		LocaleManager.addLocaleListener(myListener);
		myListener.localeChanged();

		LogisimMenuBar menubar = new LogisimMenuBar(this, null);
		setJMenuBar(menubar);
		editListener.register(menubar);
	}

	private void addTab(int index, final JComponent comp) {
		if (comp instanceof TableTab) {
			tabbedPane.insertTab("Untitled", null, comp, null, index);
			return;
		}
		final JScrollPane pane = new JScrollPane(comp,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pane.addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent arg0) {
			}

			public void componentMoved(ComponentEvent arg0) {
			}

			public void componentResized(ComponentEvent event) {
				int width = pane.getViewport().getWidth();
				comp.setSize(new Dimension(width, comp.getHeight()));
			}

			public void componentShown(ComponentEvent arg0) {
			}
		});
		tabbedPane.insertTab("Untitled", null, pane, null, index);
	}

	public AnalyzerModel getModel() {
		return model;
	}

	public void setSelectedTab(int index) {
		Object found = tabbedPane.getComponentAt(index);
		if (found instanceof AnalyzerTab) {
			model.getOutputExpressions().enableUpdates();
			((AnalyzerTab) found).updateTab();
		} else {
			model.getOutputExpressions().disableUpdates();
		}
		tabbedPane.setSelectedIndex(index);
	}
	
	public abstract static class PleaseWait<T> extends JDialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private SwingWorker<T, Void> worker;
		private java.awt.Component parent;
		public abstract T doInBackground() throws Exception;
		private boolean alreadyFinished = false;

		public PleaseWait(String title, java.awt.Component parent) {
			super(null, title, ModalityType.APPLICATION_MODAL);
			this.parent = parent;
			worker = new SwingWorker<T, Void>() {
				@Override
				protected T doInBackground() throws Exception {
					return PleaseWait.this.doInBackground();
				}
				@Override
				protected void done() {
					if (PleaseWait.this.isVisible())
						PleaseWait.this.dispose();
					else
						PleaseWait.this.alreadyFinished = true;
				}
			};
		}

		public T get() {
			worker.execute();
			JProgressBar progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(progressBar, BorderLayout.CENTER);
			panel.add(new JLabel(S.get("analyzePleaseWait")), BorderLayout.PAGE_START);
			add(panel);
			setPreferredSize(new Dimension(300, 70));
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			pack();
			setLocationRelativeTo(parent);
			try {
				try { return worker.get(300, TimeUnit.MILLISECONDS); }
				catch (TimeoutException e) { }
				if (!alreadyFinished)
					setVisible(true);
				return worker.get();
			} catch (Exception e) {
				return null;
			}
		}
	}
}
