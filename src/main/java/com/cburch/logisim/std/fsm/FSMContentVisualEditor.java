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

package com.cburch.logisim.std.fsm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.statemachine.codegen.FSMVHDLCodeGen;
import com.cburch.logisim.statemachine.editor.FSMEditorWindow;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.cburch.logisim.statemachine.parser.FSMSerializer;
import com.cburch.logisim.statemachine.validation.FSMValidation;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class FSMContentVisualEditor extends JDialog implements JInputDialog, IFSMEditor {

	public FSMContent getContent() {
		return content;
	}

	public void setContent(FSMContent content) {
		this.content = content;
		editor.repaint();
	}

	private class FrameListener extends WindowAdapter implements ActionListener, LocaleListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			if (source == open) {

				JFileChooser chooser = JFileChoosers.createAt(getDefaultImportFile(null));
				chooser.setDialogTitle(Strings.get("openButton"));
				int choice = chooser.showOpenDialog(FSMContentVisualEditor.this);
				if (choice == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						FSMFile.open(f, FSMContentVisualEditor.this);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(FSMContentVisualEditor.this, e.getMessage(),
								Strings.get("hexOpenErrorTitle"), JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			if (source == save) {
				validate();
				JFileChooser chooser = JFileChoosers.createSelected(getDefaultExportFile(null));
				chooser.setDialogTitle(Strings.get("saveButton"));
				int choice = chooser.showSaveDialog(FSMContentVisualEditor.this);
				if (choice == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						FSMFile.save(f, FSMContentVisualEditor.this);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(FSMContentVisualEditor.this,e.getLocalizedMessage()+"\n"+e.getStackTrace(),
								"Error while saving FSM", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			if (source == validate) {
				validate();
				

			}
			if (source == fix) {
				FSM fsm = content.getFsm();
				int id =0;
				for (State s : fsm.getStates()) {
					for (Transition t : new ArrayList<Transition>(s.getTransition())) {
						if (t.getDst()==null) {
							s.getTransition().remove(t);
						}
					}
					int extended = 1 << fsm.getWidth();
					String newCode= Integer.toBinaryString(extended | id++).substring( 1 );
					s.setCode("\""+newCode+"\"");
				}
			}
			if (source == close) {
				if (validate())
					dispose();
				
			}
			if (source == gen) {
				JFileChooser chooser = JFileChoosers.createSelected( new File(content.getName() + ".vhd"));
				chooser.setDialogTitle("Export VHDL");
				int choice = chooser.showSaveDialog(FSMContentVisualEditor.this);
				if (choice == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						new FSMVHDLCodeGen().export(getContent().getFsm(), f);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(FSMContentVisualEditor.this, e.getMessage(),
								"Error while saving file", JOptionPane.ERROR_MESSAGE);
					}
				}
				dispose();
			}
		}

		private boolean validate() {
			FSMValidation validator = new FSMValidation(content.getFsm());
			
			validator.validate(content.getFsm());
			int nbErrors = validator.getErrors().size();
			int nbWarnings = validator.getWarnings().size();
			if ((nbErrors > 0) || (nbWarnings > 0)) {
				StringBuffer message = new StringBuffer("Validation results :\n\n");
				for (String err : validator.getErrors()) {
					message.append("Error :" + err + "\n");
				}
				for (String err : validator.getWarnings()) {
					message.append("Warning:" + err + "\n");
				}
				JOptionPane.showMessageDialog(FSMContentVisualEditor.this,message.toString() ,
						"Error in FSM model", JOptionPane.ERROR_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(FSMContentVisualEditor.this, "No Errors/Warning detected\nValidation successfull",
						"No Errors/Warning detected\nValidation successfull", JOptionPane.INFORMATION_MESSAGE);
			}
			try {
				FSMSerializer.saveAsString(content.getFsm());
			} catch (Exception e) {
				nbErrors++;
				JOptionPane.showMessageDialog(FSMContentVisualEditor.this,e.getMessage(),
						"Error in FSM model", JOptionPane.ERROR_MESSAGE);
			}
			return (nbErrors==0);
		}

		@Override
		public void localeChanged() {
			setTitle(Strings.get("fsmFrameTitle"));
			open.setText(Strings.get("openButton"));
			save.setText(Strings.get("saveButton"));
			gen.setText("Export VHDL");
			fix.setText("Reassign codes");
			validate.setText(Strings.get("validateButton"));
			close.setText(Strings.get("closeButton"));
		}
		@Override
		public void windowClosing(WindowEvent e) {
			if (validate()) {
				dispose();
				//close();
			} else {
				JOptionPane.showMessageDialog(FSMContentVisualEditor.this,"There are errors in your FSM, fix them before exiting the editor","Exit",JOptionPane.YES_NO_OPTION);
//				if(confirm  == JOptionPane.YES_OPTION) {
//				          setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//				} else if (confirm== JOptionPane.NO_OPTION) {
//				          setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//				}
			}
		}

	}

	private class ModelListener implements FSMModelListener {

		@Override
		public void contentSet(FSMContent source) {
			validate.setEnabled(false);
		}

	}

	public static boolean confirmImport(Component parent) {
		String[] options = { Strings.get("importOption"), Strings.get("cancelOption") };
		return JOptionPane.showOptionDialog(parent, Strings.get("importMessage"), Strings.get("importTitle"), 0,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]) == 0;
	}

	private static final long serialVersionUID = 1L;

	private static final String EXPORT_DIR = null;

	private final int APPLET_WIDTH = 800, APPLET_HEIGHT = 700;

	private FrameListener frameListener = new FrameListener();
	private ModelListener modelListener = new ModelListener();

	private FSMContent content;
	private Project project;
	private FSMEditorWindow editor;

	private JButton open = new JButton();
	private JButton save = new JButton();
	private JButton fix = new JButton();
	private JButton gen = new JButton();
	private JButton validate = new JButton();
	private JButton close = new JButton();

	public FSMContentVisualEditor(Dialog parent, Project proj, FSMContent model) {
		super(parent, Strings.get("hdlFrameTitle"), true);
		configure(proj, model);
		setContent(content);
	}

	public FSMContentVisualEditor(Frame parent, Project proj, FSMContent model) {
		super(parent, Strings.get("hdlFrameTitle"), true);
		configure(proj, model);
		setContent(content);
	}

	private void configure(Project proj, FSMContent model) {
		this.project = proj;
		this.content = model;
		this.content.addFSMModelListener(modelListener);
		this.addWindowListener(frameListener);

		setSize(APPLET_WIDTH, APPLET_HEIGHT);
		setMinimumSize(new Dimension(APPLET_WIDTH, APPLET_HEIGHT));
		System.out.println(this.toString() + ":" + getWidth() + "x" + getHeight());
		
		Panel buttonsPanel = new Panel();
		buttonsPanel.add(open);
		buttonsPanel.add(save);
		buttonsPanel.add(gen);
		buttonsPanel.add(validate);
		buttonsPanel.add(fix);
		buttonsPanel.add(close);

		open.addActionListener(frameListener);
		save.addActionListener(frameListener);
		gen.addActionListener(frameListener);
		fix.addActionListener(frameListener);
		close.addActionListener(frameListener);
		validate.addActionListener(frameListener);
		

		editor = new FSMEditorWindow(this);

		add(buttonsPanel, BorderLayout.SOUTH);
		add(editor, BorderLayout.CENTER);

		LocaleManager.addLocaleListener(frameListener);
		frameListener.localeChanged();
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		pack();

	}

	private File getDefaultExportFile(File defaultFile) {
		File projectFile = project.getLogisimFile().getLoader().getMainFile();
		if (projectFile == null) {
			if (defaultFile == null)
				return new File(content.getName() + ".fsm");
			return defaultFile;
		}

		File compFolder;
		try {
			compFolder = new File(FileUtil.correctPath(projectFile.getParentFile().getCanonicalPath()) + EXPORT_DIR);
			if (!compFolder.exists() || (compFolder.exists() && !compFolder.isDirectory()))
				compFolder.mkdir();
			return new File(FileUtil.correctPath(compFolder.getCanonicalPath()) + content.getName() + ".fsm");
		} catch (IOException ex) {
			return defaultFile;
		}
	}

	private File getDefaultImportFile(File defaultFile) {
		File projectFile = project.getLogisimFile().getLoader().getMainFile();
		if (projectFile == null)
			return defaultFile;

		File compFolder;
		try {
			compFolder = new File(FileUtil.correctPath(projectFile.getParentFile().getCanonicalPath()) + EXPORT_DIR);
			if (!compFolder.exists() || (compFolder.exists() && !compFolder.isDirectory()))
				compFolder.mkdir();
			return new File(FileUtil.correctPath(compFolder.getCanonicalPath()));
		} catch (IOException ex) {
			return defaultFile;
		}
	}

	@Override
	public Object getValue() {
		return content.getStringContent();
	}

	@Override
	public void setValue(Object value) {
		content.updateContent(value.toString());

	}


}
